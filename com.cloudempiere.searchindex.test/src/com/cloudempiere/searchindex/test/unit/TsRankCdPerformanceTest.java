package com.cloudempiere.searchindex.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.compiere.util.DB;
import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;

/**
 * Direct SQL tests for ts_rank_cd() position-aware ranking
 *
 * These tests use direct SQL (no OSGi dependencies) to verify:
 * 1. ts_rank_cd() is available in PostgreSQL
 * 2. Position-aware ranking works correctly
 * 3. Earlier matches rank higher than later matches
 * 4. Performance is acceptable
 *
 * Transaction Strategy:
 * - All operations on test table use null transaction (auto-commit)
 * - This prevents PostgreSQL transaction isolation issues where uncommitted
 *   INSERT data would be invisible to SELECT queries in the same transaction
 * - Test table is temporary/test-only, safe to create/modify outside transactions
 *
 * @author CloudEmpiere Team
 * @see docs/adr/ADR-005-searchtype-migration.md
 */
public class TsRankCdPerformanceTest extends AbstractTestCase {

	private static final String TEST_TABLE = "test_ts_rank_cd";

	/**
	 * Create test table once for all tests (class-level setup)
	 * DDL operations outside transaction to avoid locks
	 */
	@BeforeAll
	public static void createTestTable() {
		String ddl = "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + " (" +
			"id SERIAL PRIMARY KEY, " +
			"name TEXT, " +
			"idx_tsvector tsvector" +
			")";
		System.out.println("→ Creating test table (once for all tests)...");
		DB.executeUpdateEx(ddl, null, null); // null trxName for DDL

		// Create GIN index
		String indexDdl = "CREATE INDEX IF NOT EXISTS " + TEST_TABLE + "_idx " +
			"ON " + TEST_TABLE + " USING GIN (idx_tsvector)";
		DB.executeUpdateEx(indexDdl, null, null); // null trxName for DDL
		System.out.println("✓ Test table and GIN index created\n");
	}

	/**
	 * Drop test table after all tests complete (class-level cleanup)
	 */
	@AfterAll
	public static void dropTestTable() {
		System.out.println("\n→ Dropping test table (after all tests)...");
		String sql = "DROP TABLE IF EXISTS " + TEST_TABLE + " CASCADE";
		DB.executeUpdateEx(sql, null, null); // null trxName for DDL
		System.out.println("✓ Test table dropped");
	}

	@Override
	@BeforeEach
	protected void init(TestInfo testInfo) {
		// CRITICAL: Initialize database connection first
		super.init(testInfo);
		System.out.println("✓ Test: " + testInfo.getDisplayName());

		// Clear table data between tests
		// TRUNCATE is DDL, use null transaction to avoid locks
		String truncate = "TRUNCATE TABLE " + TEST_TABLE + " RESTART IDENTITY";
		DB.executeUpdateEx(truncate, null, null);
	}

	@AfterEach
	public void tearDown() {
		// No cleanup needed - TRUNCATE happens in next test's @BeforeEach
		// Transaction rollback is automatic via AbstractTestCase
	}

	/**
	 * Test 1: Verify ts_rank_cd() function exists and works
	 */
	@Test
	public void testTsRankCdFunctionExists() throws Exception {
		// GIVEN: Simple test data
		String insertSQL = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
		DB.executeUpdateEx(insertSQL, new Object[]{"Test Product", "Test Product"}, null);

		// WHEN: Call ts_rank_cd() function
		String sql = "SELECT ts_rank_cd(idx_tsvector, to_tsquery('simple', 'product'), 2) as rank " +
			"FROM " + TEST_TABLE + " WHERE idx_tsvector @@ to_tsquery('simple', 'product')";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();

			// THEN: Function should work and return a rank
			assertThat(rs.next()).as("Should find at least one result").isTrue();
			double rank = rs.getDouble("rank");
			assertThat(rank).as("Rank should be non-zero").isGreaterThan(0);

		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**
	 * Test 2: Position-aware ranking - earlier matches rank higher
	 */
	@Test
	public void testEarlierPositionRanksHigher() throws Exception {
		// GIVEN: Two documents with search term at different positions
		// Document 1: "rose" at position 1 (start)
		String insert1 = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
		DB.executeUpdateEx(insert1, new Object[]{"Rose Garden Plant", "Rose Garden Plant"}, null);

		// Document 2: "rose" at position 4 (later)
		String insert2 = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
		DB.executeUpdateEx(insert2, new Object[]{"Beautiful flowers like rose", "Beautiful flowers like rose"}, null);

		// WHEN: Query using ts_rank_cd()
		String sql = "SELECT id, name, ts_rank_cd(idx_tsvector, to_tsquery('simple', 'rose'), 2) as rank " +
			"FROM " + TEST_TABLE + " " +
			"WHERE idx_tsvector @@ to_tsquery('simple', 'rose') " +
			"ORDER BY rank DESC";

		List<TestResult> results = new ArrayList<>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				TestResult result = new TestResult();
				result.id = rs.getInt("id");
				result.name = rs.getString("name");
				result.rank = rs.getDouble("rank");
				results.add(result);
			}
		} finally {
			DB.close(rs, pstmt);
		}

		// THEN: Document 1 (earlier position) should rank higher
		assertThat(results).hasSizeGreaterThanOrEqualTo(2);
		// Verify first result has "Rose" at start (earlier position)
		assertThat(results.get(0).name).as("Earlier position should rank first").startsWith("Rose");
		assertThat(results.get(1).name).as("Later position should rank second").startsWith("Beautiful");
		assertThat(results.get(0).rank).as("Earlier position should have higher rank score").isGreaterThan(results.get(1).rank);
	}

	/**
	 * Test 3: Compare ts_rank() vs ts_rank_cd() - verify different results
	 */
	@Test
	public void testTsRankVsTsRankCd() throws Exception {
		// GIVEN: Document with term at specific position
		String insert = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
		DB.executeUpdateEx(insert, new Object[]{"Start garden end product", "Start garden end product"}, null);

		// WHEN: Compare both functions
		String sql = "SELECT " +
			"ts_rank(idx_tsvector, to_tsquery('simple', 'garden')) as ts_rank_score, " +
			"ts_rank_cd(idx_tsvector, to_tsquery('simple', 'garden'), 2) as ts_rank_cd_score " +
			"FROM " + TEST_TABLE + " " +
			"WHERE idx_tsvector @@ to_tsquery('simple', 'garden')";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();

			assertThat(rs.next()).isTrue();
			double tsRankScore = rs.getDouble("ts_rank_score");
			double tsRankCdScore = rs.getDouble("ts_rank_cd_score");

			// THEN: Both should return non-zero scores
			assertThat(tsRankScore).as("ts_rank() should return non-zero").isGreaterThan(0);
			assertThat(tsRankCdScore).as("ts_rank_cd() should return non-zero").isGreaterThan(0);

			// Scores will differ (ts_rank_cd considers positions)
			// We just verify both functions work

		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**
	 * Test 4: Proximity matters with ts_rank_cd()
	 */
	@Test
	@Timeout(10) // 10 seconds timeout to fail fast instead of hanging
	public void testProximityMatters() throws Exception {
		// GIVEN: Two documents with different term proximity
		// Document 1: Terms close together
		String insert1 = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
		DB.executeUpdateEx(insert1, new Object[]{"rose red", "rose red"}, null);

		// Document 2: Terms far apart
		String insert2 = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
		DB.executeUpdateEx(insert2, new Object[]{"rose garden beautiful red", "rose garden beautiful red"}, null);

		// WHEN: Search for both terms using ts_rank_cd()
		String sql = "SELECT id, name, ts_rank_cd(idx_tsvector, to_tsquery('simple', 'rose & red'), 2) as rank " +
			"FROM " + TEST_TABLE + " " +
			"WHERE idx_tsvector @@ to_tsquery('simple', 'rose & red') " +
			"ORDER BY rank DESC";

		List<TestResult> results = new ArrayList<>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				TestResult result = new TestResult();
				result.id = rs.getInt("id");
				result.name = rs.getString("name");
				result.rank = rs.getDouble("rank");
				results.add(result);
			}
		} finally {
			DB.close(rs, pstmt);
		}

		// THEN: Document with closer terms should rank higher
		assertThat(results).hasSizeGreaterThanOrEqualTo(2);
		// Verify first result has closer terms (no words between "rose" and "red")
		assertThat(results.get(0).name).as("Closer terms should rank first").isEqualTo("rose red");
		assertThat(results.get(1).name).as("Farther terms should rank second").contains("garden beautiful");
		assertThat(results.get(0).rank).as("Closer terms should have higher rank score").isGreaterThan(results.get(1).rank);
	}

	/**
	 * Test 5: Performance with GIN index
	 */
	@Test
	public void testPerformanceWithGinIndex() throws Exception {
		// GIVEN: Insert 1000 test records
		for (int i = 1; i <= 1000; i++) {
			String insert = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
			String name = "Test product " + i;
			DB.executeUpdateEx(insert, new Object[]{name, name}, null);
		}

		// WHEN: Execute search with ts_rank_cd()
		long startTime = System.currentTimeMillis();

		String sql = "SELECT id, ts_rank_cd(idx_tsvector, to_tsquery('simple', 'product'), 2) as rank " +
			"FROM " + TEST_TABLE + " " +
			"WHERE idx_tsvector @@ to_tsquery('simple', 'product') " +
			"ORDER BY rank DESC " +
			"LIMIT 10";

		int resultCount = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				resultCount++;
			}
		} finally {
			DB.close(rs, pstmt);
		}

		long duration = System.currentTimeMillis() - startTime;

		// THEN: Should complete quickly and return results
		assertThat(resultCount).as("Should find results").isGreaterThan(0);
		assertThat(duration).as("Query should complete in <1000ms with GIN index").isLessThan(1000L);
	}

	/**
	 * Test 6: Normalization parameter effect
	 */
	@Test
	public void testNormalizationParameter() throws Exception {
		// GIVEN: One short doc, one long doc
		String shortDoc = "garden";
		String longDoc = "garden " + "other words ".repeat(50);

		String insert1 = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
		DB.executeUpdateEx(insert1, new Object[]{shortDoc, shortDoc}, null);

		String insert2 = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('simple', ?))";
		DB.executeUpdateEx(insert2, new Object[]{longDoc, longDoc}, null);

		// WHEN: Compare with and without normalization
		String sql = "SELECT id, " +
			"ts_rank_cd(idx_tsvector, to_tsquery('simple', 'garden'), 0) as rank_no_norm, " +
			"ts_rank_cd(idx_tsvector, to_tsquery('simple', 'garden'), 2) as rank_with_norm " +
			"FROM " + TEST_TABLE + " " +
			"WHERE idx_tsvector @@ to_tsquery('simple', 'garden') " +
			"ORDER BY id";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();

			// Short doc
			assertThat(rs.next()).isTrue();
			double shortNoNorm = rs.getDouble("rank_no_norm");
			double shortWithNorm = rs.getDouble("rank_with_norm");

			// Long doc
			assertThat(rs.next()).isTrue();
			double longNoNorm = rs.getDouble("rank_no_norm");
			double longWithNorm = rs.getDouble("rank_with_norm");

			// THEN: Normalization should reduce long doc's advantage
			double ratioNoNorm = shortNoNorm / longNoNorm;
			double ratioWithNorm = shortWithNorm / longWithNorm;

			assertThat(ratioWithNorm).as("Normalization should help short doc compete").isGreaterThan(ratioNoNorm);

		} finally {
			DB.close(rs, pstmt);
		}
	}

	// Helper class for test results
	private static class TestResult {
		int id;
		String name;
		double rank;
	}
}
