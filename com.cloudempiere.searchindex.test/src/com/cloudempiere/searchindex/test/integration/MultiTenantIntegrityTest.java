package com.cloudempiere.searchindex.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for ADR-006: Multi-Tenant Data Integrity
 *
 * Verifies that search index UNIQUE constraint includes ad_client_id
 * to prevent cross-client data corruption.
 *
 * Test Requirements from ADR-006:
 * - Multi-tenant isolation: Different clients with same record_id
 * - UNIQUE constraint enforcement: Same client cannot have duplicates
 * - Multi-client search: Each client sees only their own data
 *
 * CRITICAL: These tests verify the fix for the catastrophic
 * multi-tenant bug where Client B could overwrite Client A's index.
 *
 * @author CloudEmpiere Team
 * @see docs/adr/ADR-006-multi-tenant-integrity.md
 */
public class MultiTenantIntegrityTest extends AbstractTestCase {

	/**
	 * ADR-006 Requirement: Test multi-tenant isolation
	 *
	 * Test case from ADR-006 Testing Strategy (lines 288-306)
	 *
	 * Verifies that two clients can have products with the same
	 * record_id without conflict.
	 */
	@Test
	public void testMultiTenantIsolation() {
		// GIVEN: Test setup with idx_product_ts table
		String tableName = "idx_product_ts";
		int testTableId = 259; // M_Product table ID
		int testRecordId = 999999; // Using high ID to avoid conflicts

		// Clean up any existing test data
		String cleanupSQL = "DELETE FROM " + tableName + " WHERE record_id=? AND ad_table_id=?";
		DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());

		try {
			// WHEN: Insert for Client 11 (Garden World)
			String insertSQL1 = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (11, ?, ?, to_tsvector('english', 'test product client 11'))";
			int result1 = DB.executeUpdateEx(insertSQL1, new Object[]{testTableId, testRecordId}, getTrxName());
			assertThat(result1).isEqualTo(1);

			// Insert for System Client (0)
			String insertSQL2 = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (0, ?, ?, to_tsvector('english', 'test product client 0'))";
			int result2 = DB.executeUpdateEx(insertSQL2, new Object[]{testTableId, testRecordId}, getTrxName());
			assertThat(result2).isEqualTo(1);

			// THEN: Both entries exist independently
			String countSQL = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_table_id=? AND record_id=? AND ad_client_id=?";

			int countClient11 = DB.getSQLValueEx(getTrxName(), countSQL, testTableId, testRecordId, 11);
			assertThat(countClient11).as("Client 11 should have exactly 1 entry").isEqualTo(1);

			int countClient0 = DB.getSQLValueEx(getTrxName(), countSQL, testTableId, testRecordId, 0);
			assertThat(countClient0).as("Client 0 should have exactly 1 entry").isEqualTo(1);

			// Verify total is 2 (not 1, which would indicate overwriting)
			String totalSQL = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_table_id=? AND record_id=?";
			int total = DB.getSQLValueEx(getTrxName(), totalSQL, testTableId, testRecordId);
			assertThat(total).as("Total entries for record_id should be 2 (one per client)").isEqualTo(2);

		} finally {
			// Cleanup
			DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());
		}
	}

	/**
	 * ADR-006 Requirement: Test UNIQUE constraint enforcement
	 *
	 * Test case from ADR-006 Testing Strategy (lines 308-318)
	 *
	 * Verifies that same client cannot have duplicate entries
	 * for same (ad_table_id, record_id).
	 */
	@Test
	public void testUniqueConstraintEnforced() {
		// GIVEN: Test setup
		String tableName = "idx_product_ts";
		int testTableId = 259; // M_Product table ID
		int testRecordId = 999998;
		int clientId = 11; // Garden World

		// Clean up any existing test data
		String cleanupSQL = "DELETE FROM " + tableName +
			" WHERE record_id=? AND ad_table_id=? AND ad_client_id=?";
		DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId, clientId}, getTrxName());

		try {
			// Insert first entry for Client 11
			String insertSQL = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (?, ?, ?, to_tsvector('english', 'original entry'))";
			int result1 = DB.executeUpdateEx(insertSQL, new Object[]{clientId, testTableId, testRecordId}, getTrxName());
			assertThat(result1).isEqualTo(1);

			// WHEN: Try to insert same (client_id, table_id, record_id) again
			// With ON CONFLICT DO UPDATE, this should succeed and update the existing row
			String upsertSQL = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (?, ?, ?, to_tsvector('english', 'updated entry')) " +
				"ON CONFLICT (ad_client_id, ad_table_id, record_id) DO UPDATE " +
				"SET idx_tsvector = EXCLUDED.idx_tsvector";
			int result2 = DB.executeUpdateEx(upsertSQL, new Object[]{clientId, testTableId, testRecordId}, getTrxName());

			// THEN: Should succeed (upsert behavior)
			assertThat(result2).as("Upsert should succeed").isGreaterThan(0);

			// Verify only ONE entry exists (not two)
			String countSQL = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_client_id=? AND ad_table_id=? AND record_id=?";
			int count = DB.getSQLValueEx(getTrxName(), countSQL, clientId, testTableId, testRecordId);
			assertThat(count).as("Should have exactly 1 entry (upserted, not duplicated)").isEqualTo(1);

			// Verify the entry was updated (not duplicated)
			String contentSQL = "SELECT idx_tsvector::text FROM " + tableName +
				" WHERE ad_client_id=? AND ad_table_id=? AND record_id=?";
			String content = DB.getSQLValueStringEx(getTrxName(), contentSQL, clientId, testTableId, testRecordId);
			assertThat(content).contains("updat");

		} finally {
			// Cleanup
			DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId, clientId}, getTrxName());
		}
	}

	/**
	 * ADR-006 Requirement: Test multi-client search isolation
	 *
	 * Test case from ADR-006 Testing Strategy (lines 320-351)
	 *
	 * Verifies that search results are properly filtered by client.
	 *
	 * NOTE: This test verifies database-level isolation. Full search functionality
	 * testing requires ISearchIndexProvider integration which is tested separately.
	 */
	@Test
	public void testMultiClientSearch() {
		// GIVEN: Test setup
		String tableName = "idx_product_ts";
		int testTableId = 259; // M_Product table ID
		int testRecordId = 999997;

		// Clean up any existing test data
		String cleanupSQL = "DELETE FROM " + tableName + " WHERE record_id=? AND ad_table_id=?";
		DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());

		try {
			// Insert entry for Client 11 with "widget" keyword
			String insertSQL1 = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (11, ?, ?, to_tsvector('english', 'widget pro amazing'))";
			DB.executeUpdateEx(insertSQL1, new Object[]{testTableId, testRecordId}, getTrxName());

			// Insert entry for Client 0 with "gadget" keyword (different record_id)
			String insertSQL2 = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (0, ?, ?, to_tsvector('english', 'gadget plus awesome'))";
			DB.executeUpdateEx(insertSQL2, new Object[]{testTableId, testRecordId + 1}, getTrxName());

			// WHEN/THEN: Query as Client 11 searching for "widget"
			String searchSQL = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_client_id IN (0, ?) AND ad_table_id=? " +
				"AND idx_tsvector @@ to_tsquery('english', 'widget')";
			int client11Results = DB.getSQLValueEx(getTrxName(), searchSQL, 11, testTableId);
			assertThat(client11Results).as("Client 11 should see the 'widget' entry").isEqualTo(1);

			// Verify Client 11 does NOT see Client 0's specific entry
			String verifySQL = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_client_id=? AND ad_table_id=? AND record_id=?";
			int crossClientCheck = DB.getSQLValueEx(getTrxName(), verifySQL, 11, testTableId, testRecordId + 1);
			assertThat(crossClientCheck).as("Client 11 should NOT have Client 0's record_id").isEqualTo(0);

			// WHEN/THEN: Query as Client 0 searching for "gadget"
			String searchSQL2 = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_client_id IN (0, ?) AND ad_table_id=? " +
				"AND idx_tsvector @@ to_tsquery('english', 'gadget')";
			int client0Results = DB.getSQLValueEx(getTrxName(), searchSQL2, 11, testTableId);
			assertThat(client0Results).as("Should find 'gadget' entry from System client").isEqualTo(1);

		} finally {
			// Cleanup
			DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());
			String cleanupSQL2 = "DELETE FROM " + tableName + " WHERE record_id=? AND ad_table_id=?";
			DB.executeUpdateEx(cleanupSQL2, new Object[]{testRecordId + 1, testTableId}, getTrxName());
		}
	}

	/**
	 * ADR-006 Requirement: Verify UNIQUE index structure
	 *
	 * Verifies that the database has correct UNIQUE constraint
	 * on (ad_client_id, ad_table_id, record_id).
	 */
	@Test
	public void testUniqueIndexStructure() {
		// GIVEN: idx_product_ts table should exist
		String tableName = "idx_product_ts";

		// WHEN: Query pg_indexes for UNIQUE index on this table
		String indexSQL = "SELECT indexdef FROM pg_indexes " +
			"WHERE tablename = ? AND indexdef LIKE '%UNIQUE%'";
		String indexDef = DB.getSQLValueStringEx(getTrxName(), indexSQL, tableName);

		// THEN: Index definition should include ad_client_id, ad_table_id, record_id
		assertThat(indexDef)
			.as("UNIQUE index should exist")
			.isNotNull()
			.isNotEmpty();

		assertThat(indexDef.toLowerCase())
			.as("Index should include ad_client_id for multi-tenant isolation")
			.contains("ad_client_id");

		assertThat(indexDef.toLowerCase())
			.as("Index should include ad_table_id")
			.contains("ad_table_id");

		assertThat(indexDef.toLowerCase())
			.as("Index should include record_id")
			.contains("record_id");

		// Verify correct order: ad_client_id should appear before record_id
		int clientIdPos = indexDef.toLowerCase().indexOf("ad_client_id");
		int recordIdPos = indexDef.toLowerCase().indexOf("record_id");
		assertThat(clientIdPos)
			.as("ad_client_id should appear before record_id in index (for multi-tenant isolation)")
			.isLessThan(recordIdPos);
	}

	/**
	 * ADR-006 Requirement: Test cross-client data leakage prevention
	 *
	 * Verifies that Client B cannot see or update Client A's index entries.
	 */
	@Test
	public void testCrossClientDataLeakage() {
		// GIVEN: Test setup
		String tableName = "idx_product_ts";
		int testTableId = 259;
		int testRecordId = 999996;

		// Clean up
		String cleanupSQL = "DELETE FROM " + tableName + " WHERE record_id=? AND ad_table_id=?";
		DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());

		try {
			// Insert entry for Client 11 only
			String insertSQL = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (11, ?, ?, to_tsvector('english', 'client 11 private data'))";
			DB.executeUpdateEx(insertSQL, new Object[]{testTableId, testRecordId}, getTrxName());

			// WHEN: Try to query as Client 0 (should NOT see Client 11's data unless searching with IN (0, 11))
			// Client 0 (System) can see all data, so we test that specific client filter works
			String restrictedSQL = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_client_id=? AND ad_table_id=? AND record_id=?";
			int client0OnlyCount = DB.getSQLValueEx(getTrxName(), restrictedSQL, 0, testTableId, testRecordId);

			// THEN: Client 0 should NOT have this specific record (it belongs to Client 11)
			assertThat(client0OnlyCount)
				.as("Client 0 should not have Client 11's record when filtering by ad_client_id=0")
				.isEqualTo(0);

			// Verify the record exists for Client 11
			int client11Count = DB.getSQLValueEx(getTrxName(), restrictedSQL, 11, testTableId, testRecordId);
			assertThat(client11Count)
				.as("Client 11 should have its own record")
				.isEqualTo(1);

			// Verify cross-client update isolation: try to update Client 11's record as if we were Client 0
			String updateSQL = "UPDATE " + tableName +
				" SET idx_tsvector = to_tsvector('english', 'hacked data') " +
				" WHERE ad_client_id=0 AND ad_table_id=? AND record_id=?";
			int updateCount = DB.executeUpdateEx(updateSQL, new Object[]{testTableId, testRecordId}, getTrxName());

			// THEN: Update should affect 0 rows (cannot update Client 11's data as Client 0)
			assertThat(updateCount)
				.as("Cannot update another client's data")
				.isEqualTo(0);

			// Verify Client 11's data unchanged
			String verifySQL = "SELECT idx_tsvector::text FROM " + tableName +
				" WHERE ad_client_id=11 AND ad_table_id=? AND record_id=?";
			String content = DB.getSQLValueStringEx(getTrxName(), verifySQL, testTableId, testRecordId);
			assertThat(content)
				.as("Client 11's data should be unchanged")
				.contains("private");

		} finally {
			// Cleanup
			DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());
		}
	}

	/**
	 * ADR-006 Requirement: Test index update isolation
	 *
	 * Verifies that when Client B updates their record,
	 * it doesn't affect Client A's record with same record_id.
	 */
	@Test
	public void testIndexUpdateIsolation() {
		// GIVEN: Test setup
		String tableName = "idx_product_ts";
		int testTableId = 259;
		int testRecordId = 999995;

		// Clean up
		String cleanupSQL = "DELETE FROM " + tableName + " WHERE record_id=? AND ad_table_id=?";
		DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());

		try {
			// Insert for Client 11: record_id=999995, idx_tsvector='product a'
			String insertSQL1 = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (11, ?, ?, to_tsvector('english', 'product a original'))";
			DB.executeUpdateEx(insertSQL1, new Object[]{testTableId, testRecordId}, getTrxName());

			// Insert for Client 0: record_id=999995, idx_tsvector='product b'
			String insertSQL2 = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (0, ?, ?, to_tsvector('english', 'product b original'))";
			DB.executeUpdateEx(insertSQL2, new Object[]{testTableId, testRecordId}, getTrxName());

			// Verify both exist
			String countSQL = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_table_id=? AND record_id=?";
			int totalCount = DB.getSQLValueEx(getTrxName(), countSQL, testTableId, testRecordId);
			assertThat(totalCount).as("Should have 2 entries initially").isEqualTo(2);

			// WHEN: Client 0 updates their record
			String updateSQL = "UPDATE " + tableName +
				" SET idx_tsvector = to_tsvector('english', 'product b updated by client 0') " +
				"WHERE ad_client_id=0 AND ad_table_id=? AND record_id=?";
			int updateCount = DB.executeUpdateEx(updateSQL, new Object[]{testTableId, testRecordId}, getTrxName());
			assertThat(updateCount).as("Update should affect exactly 1 row").isEqualTo(1);

			// THEN: Client 11's entry UNCHANGED
			String client11SQL = "SELECT idx_tsvector::text FROM " + tableName +
				" WHERE ad_client_id=11 AND ad_table_id=? AND record_id=?";
			String client11Content = DB.getSQLValueStringEx(getTrxName(), client11SQL, testTableId, testRecordId);
			assertThat(client11Content)
				.as("Client 11's data should be unchanged after Client 0's update")
				.contains("original")
				.doesNotContain("updated");

			// Verify Client 0's entry WAS updated
			String client0SQL = "SELECT idx_tsvector::text FROM " + tableName +
				" WHERE ad_client_id=0 AND ad_table_id=? AND record_id=?";
			String client0Content = DB.getSQLValueStringEx(getTrxName(), client0SQL, testTableId, testRecordId);
			assertThat(client0Content)
				.as("Client 0's data should be updated")
				.contains("updated");

			// Verify still exactly 2 entries (no data corruption)
			int finalCount = DB.getSQLValueEx(getTrxName(), countSQL, testTableId, testRecordId);
			assertThat(finalCount).as("Should still have exactly 2 entries").isEqualTo(2);

		} finally {
			// Cleanup
			DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());
		}
	}

	/**
	 * ADR-006 Requirement: Regression test for the original bug
	 *
	 * This test documents the EXACT bug scenario from ADR-006 Context.
	 * If this test passes, the bug is fixed.
	 */
	@Test
	public void testOriginalBug_ClientBOverwritesClientA() {
		// ORIGINAL BUG SCENARIO (lines 32-53 of ADR-006):
		// 1. Client A inserts record_id=500
		// 2. Client B inserts record_id=500
		// 3. OLD: ON CONFLICT (ad_table_id, record_id) ← MISSING ad_client_id
		// 4. OLD: DO UPDATE overwrites Client A's data with Client B's
		//
		// EXPECTED BEHAVIOR AFTER FIX:
		// 1. Client A inserts record_id=500 → Success
		// 2. Client B inserts record_id=500 → Success (different client)
		// 3. NEW: ON CONFLICT (ad_client_id, ad_table_id, record_id) ← INCLUDES ad_client_id
		// 4. NEW: Both entries exist independently (NO OVERWRITE)

		String tableName = "idx_product_ts";
		int testTableId = 259;
		int testRecordId = 999994;

		// Clean up
		String cleanupSQL = "DELETE FROM " + tableName + " WHERE record_id=? AND ad_table_id=?";
		DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());

		try {
			// Step 1: Client 11 (Client A) inserts record_id=999994
			String upsertSQL = "INSERT INTO " + tableName +
				" (ad_client_id, ad_table_id, record_id, idx_tsvector) " +
				"VALUES (?, ?, ?, to_tsvector('english', ?)) " +
				"ON CONFLICT (ad_client_id, ad_table_id, record_id) DO UPDATE " +
				"SET idx_tsvector = EXCLUDED.idx_tsvector";

			DB.executeUpdateEx(upsertSQL, new Object[]{11, testTableId, testRecordId, "client A data"}, getTrxName());

			// Verify Client A's data exists
			String verifySQL = "SELECT idx_tsvector::text FROM " + tableName +
				" WHERE ad_client_id=? AND ad_table_id=? AND record_id=?";
			String clientAData = DB.getSQLValueStringEx(getTrxName(), verifySQL, 11, testTableId, testRecordId);
			assertThat(clientAData).contains("client") .contains("a");

			// Step 2: Client 0 (Client B) inserts SAME record_id=999994
			DB.executeUpdateEx(upsertSQL, new Object[]{0, testTableId, testRecordId, "client B data"}, getTrxName());

			// CRITICAL TEST: Verify Client A's data is NOT overwritten
			String clientADataAfter = DB.getSQLValueStringEx(getTrxName(), verifySQL, 11, testTableId, testRecordId);
			assertThat(clientADataAfter)
				.as("CRITICAL: Client A's data must NOT be overwritten by Client B")
				.contains("client")
				.contains("a")
				.doesNotContain("b");

			// Verify Client B's data exists independently
			String clientBData = DB.getSQLValueStringEx(getTrxName(), verifySQL, 0, testTableId, testRecordId);
			assertThat(clientBData)
				.as("Client B should have its own entry")
				.contains("client")
				.contains("b");

			// Verify both entries exist (total = 2)
			String countSQL = "SELECT COUNT(*) FROM " + tableName +
				" WHERE ad_table_id=? AND record_id=?";
			int totalCount = DB.getSQLValueEx(getTrxName(), countSQL, testTableId, testRecordId);
			assertThat(totalCount)
				.as("Both Client A and Client B entries should exist independently")
				.isEqualTo(2);

			// This test passing means ADR-006 fix is working correctly!

		} finally {
			// Cleanup
			DB.executeUpdateEx(cleanupSQL, new Object[]{testRecordId, testTableId}, getTrxName());
		}
	}
}
