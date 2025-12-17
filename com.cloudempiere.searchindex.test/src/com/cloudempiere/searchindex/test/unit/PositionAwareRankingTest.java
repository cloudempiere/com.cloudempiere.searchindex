package com.cloudempiere.searchindex.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudempiere.searchindex.indexprovider.ISearchResult;
import com.cloudempiere.searchindex.indexprovider.SearchType;
import com.cloudempiere.searchindex.indexprovider.pgtextsearch.PGTextSearchIndexProvider;

/**
 * Unit tests for position-aware ranking with ts_rank_cd()
 *
 * Tests ADR-005 refinement: ts_rank_cd() provides position-based ranking
 * that is 100× faster than POSITION search type's regex approach.
 *
 * Test Requirements:
 * - Earlier matches rank higher than later matches
 * - Product names (early) rank higher than descriptions (late)
 * - Proximity matters: closer terms rank higher
 * - Works with Slovak diacritics (č, š, ž, á, etc.)
 *
 * @author CloudEmpiere Team
 * @see docs/adr/ADR-005-searchtype-migration.md
 */
public class PositionAwareRankingTest extends AbstractTestCase {

	private static final String TEST_TABLE = "idx_test_position_ranking";
	private static final int TEST_TABLE_ID = 259; // M_Product
	private PGTextSearchIndexProvider provider;
	private Properties ctx;

	@BeforeEach
	public void setUp() {
		ctx = Env.getCtx();
		provider = new PGTextSearchIndexProvider();

		// Create test search index table
		createTestTable();
	}

	@AfterEach
	public void tearDown() {
		// Clean up test table
		dropTestTable();
	}

	/**
	 * Test that ts_rank_cd() ranks earlier matches higher than later matches
	 *
	 * Scenario:
	 * - Product A: "Muškát krúžkovaný ružový" (term at position 1)
	 * - Product B: "Kvety pre balkón muškát" (term at position 4)
	 * - Expected: Product A ranks higher
	 */
	@Test
	public void testEarlierMatchesRankHigher() {
		// GIVEN: Two products with "muskát" at different positions
		insertTestProduct(1, "Muškát krúžkovaný ružový", "Balenie 8 kusov");
		insertTestProduct(2, "Kvety pre balkón muškát", "Vhodné na zasadenie");

		// WHEN: Search for "muskát" using TS_RANK (ts_rank_cd)
		List<ISearchResult> results = provider.getSearchResults(
			ctx, TEST_TABLE, "muskat", false, SearchType.TS_RANK, getTrxName()
		);

		// THEN: Product 1 (earlier position) ranks higher
		assertThat(results).hasSizeGreaterThanOrEqualTo(2);

		ISearchResult first = results.get(0);
		ISearchResult second = results.get(1);

		assertThat(first.getRecord_ID()).as("Product with term at start should rank first").isEqualTo(1);
		assertThat(second.getRecord_ID()).as("Product with term at end should rank second").isEqualTo(2);

		// Verify rank values (first should have higher rank)
		assertThat(first.getRank()).as("Earlier position should have higher rank").isGreaterThan(second.getRank());
	}

	/**
	 * Test that product names rank higher than descriptions
	 *
	 * Scenario:
	 * - Product A: Name="Muškát", Description="Balkónové kvety"
	 * - Product B: Name="Balkónové kvety", Description="Pre muškáty a pelargónie"
	 * - Expected: Product A ranks higher (term in name > term in description)
	 */
	@Test
	public void testProductNameRanksHigherThanDescription() {
		// GIVEN: Products with search term in different weighted fields

		// Product 1: "muskát" in name (weight A - highest)
		String sql1 = "INSERT INTO " + TEST_TABLE +
			" (ad_client_id, ad_table_id, record_id, idx_tsvector) VALUES " +
			"(11, ?, 101, " +
			"setweight(to_tsvector('simple', 'Muškát krúžkovaný'), 'A') || " + // Name (A)
			"setweight(to_tsvector('simple', 'Balkónové kvety'), 'C'))";        // Description (C)
		DB.executeUpdateEx(sql1, new Object[]{TEST_TABLE_ID}, getTrxName());

		// Product 2: "muskát" in description (weight C - lower)
		String sql2 = "INSERT INTO " + TEST_TABLE +
			" (ad_client_id, ad_table_id, record_id, idx_tsvector) VALUES " +
			"(11, ?, 102, " +
			"setweight(to_tsvector('simple', 'Balkónové kvety'), 'A') || " +     // Name (A)
			"setweight(to_tsvector('simple', 'Pre muškáty a pelargónie'), 'C'))"; // Description (C)
		DB.executeUpdateEx(sql2, new Object[]{TEST_TABLE_ID}, getTrxName());

		// WHEN: Search for "muskát"
		List<ISearchResult> results = provider.getSearchResults(
			ctx, TEST_TABLE, "muskat", false, SearchType.TS_RANK, getTrxName()
		);

		// THEN: Product with term in name (101) ranks higher
		assertThat(results).hasSizeGreaterThanOrEqualTo(2);
		assertThat(results.get(0).getRecord_ID()).as("Product with term in NAME should rank first").isEqualTo(101);
		assertThat(results.get(1).getRecord_ID()).as("Product with term in DESCRIPTION should rank second").isEqualTo(102);
	}

	/**
	 * Test that proximity matters: closer terms rank higher
	 *
	 * Scenario:
	 * - Product A: "muškát ružový" (2 words apart)
	 * - Product B: "muškát krúžkovaný svetločervený ružový" (4 words apart)
	 * - Search: "muskát ružový"
	 * - Expected: Product A ranks higher (terms closer together)
	 */
	@Test
	public void testProximityMatters() {
		// GIVEN: Products with search terms at different distances
		insertTestProduct(201, "Muškát ružový", "Balenie 8ks");
		insertTestProduct(202, "Muškát krúžkovaný svetločervený ružový", "Balenie 8ks");

		// WHEN: Search for both terms
		List<ISearchResult> results = provider.getSearchResults(
			ctx, TEST_TABLE, "muskat ruzovy", false, SearchType.TS_RANK, getTrxName()
		);

		// THEN: Product with closer terms (201) ranks higher
		assertThat(results).hasSizeGreaterThanOrEqualTo(2);
		assertThat(results.get(0).getRecord_ID()).as("Product with closer terms should rank first").isEqualTo(201);
		assertThat(results.get(1).getRecord_ID()).as("Product with farther terms should rank second").isEqualTo(202);
	}

	/**
	 * Test that ts_rank_cd() works correctly with Slovak diacritics
	 *
	 * Scenario:
	 * - Product has "muškát" (with diacritics)
	 * - Search for "muskat" (without diacritics)
	 * - Expected: Match found with proper ranking
	 */
	@Test
	public void testSlovakDiacritics() {
		// GIVEN: Product with Slovak diacritics
		insertTestProduct(301, "Muškát krúžkovaný ružový", "Balkónové kvety");

		// WHEN: Search without diacritics
		List<ISearchResult> results = provider.getSearchResults(
			ctx, TEST_TABLE, "muskat kruzkovany ruzovy", false, SearchType.TS_RANK, getTrxName()
		);

		// THEN: Should find the product
		assertThat(results).hasSizeGreaterThanOrEqualTo(1);
		assertThat(results.get(0).getRecord_ID()).isEqualTo(301);
		assertThat(results.get(0).getRank()).as("Should have non-zero rank").isGreaterThan(0);
	}

	/**
	 * Test that document length normalization works
	 *
	 * ts_rank_cd(..., 2) divides by document length + 1
	 * This prevents long documents from dominating short documents
	 *
	 * Scenario:
	 * - Product A: Short description with "muškát" (10 words)
	 * - Product B: Long description with "muškát" repeated (100 words)
	 * - Expected: Ranking accounts for document length
	 */
	@Test
	public void testDocumentLengthNormalization() {
		// GIVEN: Short document with one mention
		String shortDoc = "Muškát ružový";
		insertTestProduct(401, shortDoc, "");

		// Long document with same term repeated
		String longDoc = "Muškát krúžkovaný " + "veľmi dlhý popis ".repeat(20) + " muškát";
		insertTestProduct(402, longDoc, "");

		// WHEN: Search for "muskát"
		List<ISearchResult> results = provider.getSearchResults(
			ctx, TEST_TABLE, "muskat", false, SearchType.TS_RANK, getTrxName()
		);

		// THEN: Normalization should prevent long document from dominating
		assertThat(results).hasSizeGreaterThanOrEqualTo(2);

		// Find the two products in results
		ISearchResult shortDocResult = results.stream()
			.filter(r -> r.getRecord_ID() == 401)
			.findFirst()
			.orElseThrow();

		ISearchResult longDocResult = results.stream()
			.filter(r -> r.getRecord_ID() == 402)
			.findFirst()
			.orElseThrow();

		// With normalization, short doc should rank competitively
		// (Without normalization, long doc would dominate just by having more words)
		double rankRatio = shortDocResult.getRank() / longDocResult.getRank();
		assertThat(rankRatio).as("Normalization should prevent long docs from dominating").isGreaterThan(0.3);
	}

	/**
	 * Performance test: Verify ts_rank_cd() is fast (uses GIN index)
	 *
	 * This is a smoke test - not a rigorous benchmark.
	 * Real benchmarks should be done separately.
	 */
	@Test
	public void testPerformance() {
		// GIVEN: Insert 1000 test products
		for (int i = 500; i < 1500; i++) {
			insertTestProduct(i, "Test produkt " + i, "Popis produktu číslo " + i);
		}

		// WHEN: Search (should use GIN index)
		long startTime = System.currentTimeMillis();
		List<ISearchResult> results = provider.getSearchResults(
			ctx, TEST_TABLE, "produkt", false, SearchType.TS_RANK, getTrxName()
		);
		long duration = System.currentTimeMillis() - startTime;

		// THEN: Should complete quickly (<500ms for 1000 records)
		assertThat(duration).as("Search should be fast with GIN index").isLessThan(500L);
		assertThat(results).as("Should find test products").hasSizeGreaterThan(0);
	}

	// ==================== Helper Methods ====================

	private void createTestTable() {
		String ddl = "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + " (" +
			"ad_client_id INT NOT NULL, " +
			"ad_table_id INT NOT NULL, " +
			"record_id INT NOT NULL, " +
			"idx_tsvector tsvector, " +
			"CONSTRAINT " + TEST_TABLE + "_uk UNIQUE (ad_client_id, ad_table_id, record_id)" +
			")";
		DB.executeUpdateEx(ddl, null, getTrxName());

		// Create GIN index on tsvector
		String indexDdl = "CREATE INDEX IF NOT EXISTS " + TEST_TABLE + "_idx " +
			"ON " + TEST_TABLE + " USING GIN (idx_tsvector)";
		DB.executeUpdateEx(indexDdl, null, getTrxName());
	}

	private void dropTestTable() {
		String sql = "DROP TABLE IF EXISTS " + TEST_TABLE + " CASCADE";
		DB.executeUpdateEx(sql, null, getTrxName());
	}

	private void insertTestProduct(int recordId, String name, String description) {
		// Use simple config (no accent removal) for test data
		String sql = "INSERT INTO " + TEST_TABLE +
			" (ad_client_id, ad_table_id, record_id, idx_tsvector) VALUES " +
			"(11, ?, ?, " +
			"setweight(to_tsvector('simple', ?), 'A') || " +  // Name with weight A
			"setweight(to_tsvector('simple', ?), 'C'))";      // Description with weight C

		DB.executeUpdateEx(sql,
			new Object[]{TEST_TABLE_ID, recordId, name, description},
			getTrxName());
	}
}
