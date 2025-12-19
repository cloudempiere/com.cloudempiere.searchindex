# ADR-006: Multi-Tenant Data Integrity for Search Indexes

**Status:** ✅ Implemented
**Date:** 2025-12-12
**Implementation Date:** 2025-12-17
**Decision Makers:** Development Team, Database Team
**Related Issues:** Finding 5.2 (CRITICAL)
**Implementation Commits:** 378ec8b (ON CONFLICT clause fix)
**Migration Scripts:** docs/migration/001-fix-all-search-index-constraints.sql

---

## Context

The search index plugin has a **catastrophic multi-tenant data corruption bug** in the PostgreSQL UNIQUE constraint. The current implementation allows one client's index updates to overwrite another client's data.

### Current Implementation (BROKEN)

```java
// PGTextSearchIndexProvider.java:115
.append("ON CONFLICT (ad_table_id, record_id) DO UPDATE " +
        "SET idx_tsvector = EXCLUDED.idx_tsvector");
//               ^^^^^^^^^^^^^^^^^^^^
//               Missing: ad_client_id
```

### Database Schema (BROKEN)

```sql
-- Current (WRONG) - allows cross-client overwrites
CREATE UNIQUE INDEX idx_searchindex_table_record
  ON searchindex_product (ad_table_id, record_id);
```

### Exploitation Scenario

```
Initial State:
- Client A (ID=1000): M_Product record_id=500, Name="Product A"
  → Index entry: (ad_client_id=1000, ad_table_id=208, record_id=500, idx_tsvector='product':1 'a':2)

Attack:
- Client B (ID=1001): Creates M_Product record_id=500, Name="Product B"
  → Index update: (ad_table_id=208, record_id=500, idx_tsvector='product':1 'b':2)

ON CONFLICT Trigger:
- Constraint violated: (ad_table_id=208, record_id=500) already exists
- DO UPDATE: Overwrites with Client B's data
- idx_tsvector = 'product':1 'b':2

Result:
- Client A's index entry LOST
- Client B's index entry at wrong ad_client_id (1000 instead of 1001)
- Client A searches: finds nothing (or Client B's data)
- CLIENT A DATA CORRUPTION
```

### Impact

- **CRITICAL:** Data corruption in multi-tenant production environments
- **GDPR Violation:** Client A sees Client B's data (data leakage)
- **Data Loss:** Client A's search index permanently lost
- **Compliance Risk:** Violates iDempiere multi-tenancy guarantee

---

## Decision

We will **fix the UNIQUE constraint to include `ad_client_id`** as the first column in the composite key, ensuring proper multi-tenant isolation.

### Correct Implementation

```java
// PGTextSearchIndexProvider.java:115 (FIX)
.append("ON CONFLICT (ad_client_id, ad_table_id, record_id) DO UPDATE " +
        "SET idx_tsvector = EXCLUDED.idx_tsvector");
```

### Database Schema (CORRECT)

```sql
-- Multi-tenant safe constraint
CREATE UNIQUE INDEX idx_searchindex_client_table_record
  ON searchindex_product (ad_client_id, ad_table_id, record_id);
```

### Why ad_client_id First?

```sql
-- Query pattern (client-specific searches):
SELECT * FROM searchindex_product
WHERE ad_client_id = ? AND ad_table_id = ? AND idx_tsvector @@ to_tsquery(?)

-- Index scan order:
-- 1. Filter by ad_client_id (partition key) → 99% reduction
-- 2. Filter by ad_table_id → further reduction
-- 3. Filter by record_id (if needed) → single record

-- Placing ad_client_id first maximizes index efficiency
```

---

## Implementation Plan

### Phase 1: Code Fix (1 hour)

**File:** `PGTextSearchIndexProvider.java`

```java
// Line 101-120 (buildInsertOrUpdateQuery method)

private String buildInsertOrUpdateQuery(String tableName, List<String> columnNames) {
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ").append(tableName).append(" (");

    // Columns: ad_client_id, ad_table_id, record_id, idx_tsvector
    sql.append("ad_client_id, ad_table_id, record_id, idx_tsvector");
    sql.append(") VALUES (?, ?, ?, ?::tsvector) ");

    // FIX: Include ad_client_id in UNIQUE constraint
-   sql.append("ON CONFLICT (ad_table_id, record_id) DO UPDATE ");
+   sql.append("ON CONFLICT (ad_client_id, ad_table_id, record_id) DO UPDATE ");
    sql.append("SET idx_tsvector = EXCLUDED.idx_tsvector");

    return sql.toString();
}
```

### Phase 2: Database Migration (2 hours)

**Migration Script:** `postgresql/migration/202512_add_client_to_unique_index.sql`

```sql
-- Migration: Fix multi-tenant UNIQUE constraint
-- Issue: CLD-XXXX
-- Date: 2025-12-12

DO $$
DECLARE
    v_table_name TEXT;
    v_old_index_name TEXT;
    v_new_index_name TEXT;
BEGIN
    -- Find all search index tables (pattern: searchindex_*)
    FOR v_table_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'adempiere'
          AND tablename LIKE 'searchindex_%'
    LOOP
        RAISE NOTICE 'Processing table: %', v_table_name;

        -- Drop old UNIQUE index (ad_table_id, record_id)
        v_old_index_name := 'idx_' || v_table_name || '_table_record';

        IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = v_old_index_name) THEN
            EXECUTE format('DROP INDEX IF EXISTS %I', v_old_index_name);
            RAISE NOTICE 'Dropped old index: %', v_old_index_name;
        END IF;

        -- Create new UNIQUE index (ad_client_id, ad_table_id, record_id)
        v_new_index_name := 'idx_' || v_table_name || '_client_table_record';

        EXECUTE format(
            'CREATE UNIQUE INDEX %I ON %I (ad_client_id, ad_table_id, record_id)',
            v_new_index_name,
            v_table_name
        );
        RAISE NOTICE 'Created new index: %', v_new_index_name;
    END LOOP;

    RAISE NOTICE 'Migration completed successfully';
END $$;
```

### Phase 3: Data Cleanup (Critical!)

Before applying the new UNIQUE constraint, **clean up existing duplicates**:

```sql
-- Data Cleanup: Identify and fix duplicate entries
-- Run BEFORE migration script

DO $$
DECLARE
    v_table_name TEXT;
    v_duplicate_count INT;
BEGIN
    FOR v_table_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'adempiere'
          AND tablename LIKE 'searchindex_%'
    LOOP
        RAISE NOTICE 'Checking duplicates in: %', v_table_name;

        -- Find duplicate (ad_table_id, record_id) entries
        EXECUTE format('
            WITH duplicates AS (
                SELECT ad_table_id, record_id, COUNT(*) as cnt
                FROM %I
                GROUP BY ad_table_id, record_id
                HAVING COUNT(*) > 1
            )
            SELECT COUNT(*) FROM duplicates
        ', v_table_name) INTO v_duplicate_count;

        IF v_duplicate_count > 0 THEN
            RAISE WARNING 'Found % duplicate groups in %', v_duplicate_count, v_table_name;

            -- Keep only the latest entry per (ad_table_id, record_id)
            -- Delete older entries
            EXECUTE format('
                DELETE FROM %I
                WHERE ctid IN (
                    SELECT ctid FROM (
                        SELECT ctid,
                               ROW_NUMBER() OVER (
                                   PARTITION BY ad_table_id, record_id
                                   ORDER BY updated DESC NULLS LAST, created DESC
                               ) AS rn
                        FROM %I
                    ) sub
                    WHERE rn > 1
                )
            ', v_table_name, v_table_name);

            RAISE NOTICE 'Cleaned up duplicates in %', v_table_name;
        ELSE
            RAISE NOTICE 'No duplicates found in %', v_table_name;
        END IF;
    END LOOP;

    RAISE NOTICE 'Data cleanup completed';
END $$;
```

### Phase 4: Verification (1 hour)

**Test Script:** `test_multi_tenant_integrity.sql`

```sql
-- Test multi-tenant isolation after fix

-- Setup: Create test data for two clients
INSERT INTO searchindex_product (ad_client_id, ad_table_id, record_id, idx_tsvector)
VALUES
    (1000, 208, 500, to_tsvector('english', 'Product A Client 1000')),
    (1001, 208, 500, to_tsvector('english', 'Product B Client 1001'));

-- Verify: Both entries exist independently
SELECT ad_client_id, record_id, idx_tsvector::text
FROM searchindex_product
WHERE ad_table_id = 208 AND record_id = 500
ORDER BY ad_client_id;

-- Expected:
-- ad_client_id | record_id | idx_tsvector
-- 1000         | 500       | 'product':1 'a':2 'client':3 '1000':4
-- 1001         | 500       | 'product':1 'b':2 'client':3 '1001':4

-- Update Client 1001's entry
UPDATE searchindex_product
SET idx_tsvector = to_tsvector('english', 'Product B Updated')
WHERE ad_client_id = 1001 AND ad_table_id = 208 AND record_id = 500;

-- Verify: Client 1000's entry unchanged
SELECT ad_client_id, record_id, idx_tsvector::text
FROM searchindex_product
WHERE ad_client_id = 1000 AND ad_table_id = 208 AND record_id = 500;

-- Expected:
-- ad_client_id | record_id | idx_tsvector
-- 1000         | 500       | 'product':1 'a':2 'client':3 '1000':4
--                             ^^^^^^ NOT CHANGED (correct!)

-- Cleanup
DELETE FROM searchindex_product
WHERE ad_table_id = 208 AND record_id = 500;
```

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testMultiTenantIsolation() {
    // Client 1000: Insert product
    PGTextSearchIndexProvider provider = new PGTextSearchIndexProvider();
    provider.createIndex(ctx1000, indexConfig, null);

    // Client 1001: Insert same record_id
    provider.createIndex(ctx1001, indexConfig, null);

    // Verify: Both entries exist
    int count1000 = DB.getSQLValue(null,
        "SELECT COUNT(*) FROM searchindex_product " +
        "WHERE ad_client_id=1000 AND record_id=500");
    assertEquals(1, count1000);

    int count1001 = DB.getSQLValue(null,
        "SELECT COUNT(*) FROM searchindex_product " +
        "WHERE ad_client_id=1001 AND record_id=500");
    assertEquals(1, count1001);
}

@Test(expected = SQLException.class)
public void testUniqueConstraintEnforced() {
    // Same client, same record_id → should fail
    PGTextSearchIndexProvider provider = new PGTextSearchIndexProvider();

    provider.createIndex(ctx1000, indexConfig, null);
    provider.createIndex(ctx1000, indexConfig, null);  // Duplicate!

    // Should throw SQLException (UNIQUE constraint violation)
}
```

### Integration Tests

```java
@Test
public void testMultiClientSearch() {
    // Setup: Client 1000 and 1001 have products with same record_id
    createProduct(1000, 500, "Widget Pro");
    createProduct(1001, 500, "Gadget Plus");

    // Search as Client 1000
    Env.setContext(ctx, "#AD_Client_ID", 1000);
    List<ISearchResult> results = provider.getSearchResults(
        ctx, "searchindex_product", "widget", false, SearchType.TS_RANK, null
    );

    // Verify: Only Client 1000's product found
    assertEquals(1, results.size());
    assertEquals(1000, results.get(0).getAD_Client_ID());
    assertEquals("Widget Pro", results.get(0).getName());

    // Search as Client 1001
    Env.setContext(ctx, "#AD_Client_ID", 1001);
    results = provider.getSearchResults(
        ctx, "searchindex_product", "gadget", false, SearchType.TS_RANK, null
    );

    // Verify: Only Client 1001's product found
    assertEquals(1, results.size());
    assertEquals(1001, results.get(0).getAD_Client_ID());
    assertEquals("Gadget Plus", results.get(0).getName());
}
```

---

## Rollback Plan

If migration causes issues:

### 1. Immediate Rollback (Emergency)

```sql
-- Revert to old index (NOT RECOMMENDED - loses data integrity)
DO $$
DECLARE
    v_table_name TEXT;
BEGIN
    FOR v_table_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'adempiere'
          AND tablename LIKE 'searchindex_%'
    LOOP
        -- Drop new index
        EXECUTE format(
            'DROP INDEX IF EXISTS idx_%s_client_table_record',
            REPLACE(v_table_name, 'searchindex_', '')
        );

        -- Recreate old index (WARNING: allows corruption)
        EXECUTE format(
            'CREATE UNIQUE INDEX idx_%s_table_record ON %I (ad_table_id, record_id)',
            REPLACE(v_table_name, 'searchindex_', ''),
            v_table_name
        );
    END LOOP;
END $$;
```

### 2. Data Recovery (If Corruption Occurred)

```sql
-- Identify corrupted entries (wrong ad_client_id)
SELECT si.ad_client_id AS index_client,
       t.ad_client_id AS actual_client,
       si.record_id,
       t.tablename
FROM searchindex_product si
JOIN ad_table t ON si.ad_table_id = t.ad_table_id
JOIN m_product p ON si.record_id = p.m_product_id
WHERE si.ad_client_id != p.ad_client_id;

-- Fix: Update to correct client
UPDATE searchindex_product si
SET ad_client_id = p.ad_client_id
FROM m_product p
WHERE si.record_id = p.m_product_id
  AND si.ad_client_id != p.ad_client_id;

-- Re-index all affected clients
UPDATE AD_SearchIndex SET IsValid='N';
```

---

## Performance Impact

### Index Size Analysis

**Before (2-column index):**
```
(ad_table_id, record_id)
Approx. size: 8 bytes (int4) + 8 bytes (int4) = 16 bytes per entry
```

**After (3-column index):**
```
(ad_client_id, ad_table_id, record_id)
Approx. size: 8 + 8 + 8 = 24 bytes per entry
```

**Impact:** +50% index size (acceptable for data integrity)

### Query Performance

**Before:**
```sql
-- Query:
SELECT * FROM searchindex_product
WHERE ad_client_id = 1000 AND ad_table_id = 208 AND record_id = 500;

-- Index usage: Partial (ad_table_id, record_id)
-- Rows scanned: All clients with (208, 500) → filter by ad_client_id
```

**After:**
```sql
-- Same query
-- Index usage: Full (ad_client_id, ad_table_id, record_id)
-- Rows scanned: Only matching client
```

**Result:** **IMPROVED performance** (better index selectivity)

---

## Monitoring & Validation

### Daily Integrity Check

```sql
-- Scheduled job: Check for cross-client data leakage
SELECT si.ad_client_id AS index_client,
       p.ad_client_id AS actual_client,
       COUNT(*) AS mismatches
FROM searchindex_product si
JOIN m_product p ON si.record_id = p.m_product_id
WHERE si.ad_client_id != p.ad_client_id
GROUP BY si.ad_client_id, p.ad_client_id;

-- Alert if COUNT(*) > 0
```

### Constraint Validation

```sql
-- Verify UNIQUE constraint exists
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'searchindex_product'
  AND indexdef LIKE '%ad_client_id%ad_table_id%record_id%';

-- Expected:
-- indexname: idx_searchindex_client_table_record
-- indexdef: CREATE UNIQUE INDEX ... ON searchindex_product (ad_client_id, ad_table_id, record_id)
```

---

## Consequences

### Positive

1. **Data integrity guaranteed**
   - No cross-client overwrites possible
   - Multi-tenancy contract enforced

2. **GDPR compliance**
   - Client data properly isolated
   - No data leakage across tenants

3. **Better performance**
   - Improved index selectivity
   - Faster queries for specific clients

4. **Audit trail**
   - Constraint violations logged
   - Easy to detect corruption attempts

### Negative

1. **Migration complexity**
   - Must clean up existing duplicates
   - Requires downtime (estimated 10-30 minutes)

2. **Index size increase**
   - +50% storage for UNIQUE index
   - Acceptable for data integrity benefit

3. **One-time effort**
   - All clients must be migrated simultaneously
   - Cannot roll back without data loss risk

---

## Alternatives Considered

### Alternative 1: Application-Level Checks

Add ad_client_id validation in Java code:
```java
// Before insert, verify no conflicting entry
String sql = "SELECT COUNT(*) FROM searchindex_product " +
             "WHERE ad_table_id=? AND record_id=? AND ad_client_id != ?";
int count = DB.getSQLValue(null, sql, tableId, recordId, clientId);
if (count > 0) {
    throw new AdempiereException("Cross-client conflict detected");
}
```

**Pros:** No database migration needed

**Cons:**
- Race condition window (check → insert delay)
- Not atomic (unreliable in concurrent environment)
- **REJECTED:** Database constraint is only reliable solution

---

### Alternative 2: Separate Tables Per Client

Create `searchindex_product_1000`, `searchindex_product_1001`, etc.

**Pros:** Perfect isolation

**Cons:**
- Schema explosion (unmanageable)
- Complex query routing
- **REJECTED:** Violates iDempiere design principles

---

### Alternative 3: Partition by ad_client_id

Use PostgreSQL table partitioning:
```sql
CREATE TABLE searchindex_product PARTITION BY LIST (ad_client_id);
CREATE TABLE searchindex_product_1000 PARTITION OF searchindex_product FOR VALUES IN (1000);
```

**Pros:** Performance + isolation

**Cons:**
- Requires PostgreSQL 10+
- More complex migration
- **DEFERRED:** Consider for future optimization

---

## References

- iDempiere Multi-Tenancy Best Practices: http://wiki.idempiere.org/en/Multi-Tenancy
- PostgreSQL UNIQUE Constraints: https://www.postgresql.org/docs/current/ddl-constraints.html
- CLAUDE.md:238-255 (Multi-Tenant Bug Analysis)

---

## Decision

**Approved:** [Pending]
**Approved By:** [Name]
**Approval Date:** [Date]
**Implementation Target:** Week 1 (Phase 0 Hotfix)

**CRITICAL: This fix must be deployed before any multi-tenant production use**

---

**Next ADR:** [ADR-007: Role-Based Access Control](adr-007-rbac-enforcement.md)
