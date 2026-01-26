# Implementation Plan: POSITION → TS_RANK Migration

**Purpose:** Systematic, low-risk implementation of ADR-005 with testing at each isolation level

**Date:** 2025-12-17

**Estimated Time:** 3.5 days (28 hours)

---

## Testing Isolation Levels

We'll test from lowest level (SQL) to highest (REST API), validating each layer before moving up:

```
Level 6: REST API (External)           ← Final validation
         ↑
Level 5: UI Components (ZK)            ← User interface
         ↑
Level 4: Java SearchProvider           ← Business logic
         ↑
Level 3: JUnit Tests                   ← Automated verification
         ↑
Level 2: SQL (iDempiere Database)      ← Real data validation
         ↑
Level 1: Raw PostgreSQL (Isolated)     ← Pure SQL validation
```

**Strategy:** Pass all tests at Level N before proceeding to Level N+1

---

## Phase 0: Preparation (1 hour)

### Task 0.1: Backup Current State

```bash
# Backup production search indexes
pg_dump -h localhost -U idempiere -t 'idx_*' idempiere > backup_search_indexes.sql

# Document current POSITION search behavior
psql -U idempiere -d idempiere -f docs/capture-current-behavior.sql > current_position_results.txt
```

**Deliverable:** Rollback capability if needed

---

### Task 0.2: Create Testing Branch

```bash
git checkout -b feat/adr-005-tsrank-migration
git push -u origin feat/adr-005-tsrank-migration
```

**Deliverable:** Isolated development environment

---

## Level 1: Raw PostgreSQL Testing (2 hours)

**Goal:** Prove sk_unaccent works identically to POSITION regex at pure SQL level

### Task 1.1: Run Comparison SQL

```bash
cd /Users/developer/GitHub/com.cloudempiere.searchindex

# Execute comparison on isolated test database
psql -U postgres -d test_searchindex -f test-position-vs-tsrank-comparison.sql > level1_results.txt

# Review results
less level1_results.txt
```

**Validation Checklist:**

| Test | Expected | Pass/Fail |
|------|----------|-----------|
| sk_unaccent handles á→a | ✅ Yes | ⏳ |
| sk_unaccent handles č→c | ✅ Yes | ⏳ |
| sk_unaccent handles š→s | ✅ Yes | ⏳ |
| "muskat" matches "Muškát" | ✅ Yes | ⏳ |
| Position ranking correct | ✅ Yes | ⏳ |
| Proximity ranking works | ✅ Yes | ⏳ |
| Performance < 100ms (10K) | ✅ Yes | ⏳ |

**Success Criteria:** All 7 checks pass

**If Fail:** Debug at SQL level before proceeding

---

### Task 1.2: Test Edge Cases

```sql
-- Test multi-diacritic words
SELECT unaccent('dôsledne') = 'dosledne'; -- Expected: true

-- Test all Slovak characters
SELECT unaccent('áäčďéíľňóôŕšťúýž') = 'aacdeilnoorrstuyz'; -- Expected: true

-- Test mixed languages
SELECT to_tsvector('sk_unaccent', 'Muškát rose garden');
-- Expected: 'garden':3 'muskat':1 'rose':2
```

**Deliverable:** `level1_edge_cases_validation.sql` with all tests passing

---

## Level 2: iDempiere Database Testing (3 hours)

**Goal:** Validate sk_unaccent with real production data structure

### Task 2.1: Create sk_unaccent Configuration (Staging Only)

```sql
-- Connect to staging iDempiere database
\c idempiere_staging

-- Install unaccent (if not exists)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Create Slovak text search configuration
CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);

ALTER TEXT SEARCH CONFIGURATION sk_unaccent
  ALTER MAPPING FOR word, asciiword, hword, hword_asciipart, asciihword
  WITH unaccent, simple;

-- Verify configuration
SELECT * FROM pg_ts_config WHERE cfgname = 'sk_unaccent';
```

**Validation:**
```sql
-- Test with Slovak text
SELECT to_tsvector('sk_unaccent', 'Muškát ruža');
-- Expected: 'muskat':1 'ruza':2  (diacritics removed)

SELECT 'ruza'::tsquery @@ to_tsvector('sk_unaccent', 'ruža');
-- Expected: true
```

**Success Criteria:** Configuration created, diacritics stripped correctly

---

### Task 2.2: Test on Real Product Data

```sql
-- Find a real search index table (e.g., idx_product_ts)
SELECT tablename FROM pg_tables WHERE tablename LIKE 'idx_%_ts' LIMIT 1;

-- Backup original index column
ALTER TABLE idx_product_ts ADD COLUMN idx_tsvector_backup tsvector;
UPDATE idx_product_ts SET idx_tsvector_backup = idx_tsvector;

-- Create sk_unaccent index column (parallel to original)
ALTER TABLE idx_product_ts ADD COLUMN idx_tsvector_sk tsvector;

UPDATE idx_product_ts
SET idx_tsvector_sk = to_tsvector('sk_unaccent', searchable_text);

CREATE INDEX idx_product_ts_sk_test ON idx_product_ts
USING GIN (idx_tsvector_sk);

-- Vacuum analyze for statistics
VACUUM ANALYZE idx_product_ts;
```

**Success Criteria:** Index created without errors

---

### Task 2.3: Compare Results Side-by-Side

```sql
-- Test Query: Search "muskat" on REAL production data
WITH
position_simulation AS (
  SELECT
    record_id,
    CASE
      WHEN idx_tsvector::text ~* 'muskat'
      THEN 1.0
      ELSE 0
    END as position_match
  FROM idx_product_ts
  LIMIT 100
),
tsrank_results AS (
  SELECT
    record_id,
    CASE
      WHEN idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
      THEN 1.0
      ELSE 0
    END as tsrank_match,
    ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2) as rank
  FROM idx_product_ts
  LIMIT 100
)
SELECT
  p.record_id,
  p.position_match,
  t.tsrank_match,
  t.rank,
  CASE
    WHEN p.position_match = t.tsrank_match THEN '✅ SAME'
    ELSE '❌ DIFFERENT'
  END as comparison
FROM position_simulation p
JOIN tsrank_results t ON p.record_id = t.record_id
ORDER BY p.record_id;
```

**Success Criteria:** 95%+ results identical, differences explainable

**Deliverable:** `level2_real_data_comparison.sql` showing results match

---

### Task 2.4: Performance Benchmark

```sql
-- Benchmark POSITION (simulated)
EXPLAIN ANALYZE
SELECT record_id, idx_tsvector::text
FROM idx_product_ts
WHERE idx_tsvector::text ~* 'muskat'
LIMIT 10;
-- Expected: Seq Scan, ~5000ms for 10K rows

-- Benchmark TS_RANK with sk_unaccent
EXPLAIN ANALYZE
SELECT record_id, ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2)
FROM idx_product_ts
WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
ORDER BY ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2) DESC
LIMIT 10;
-- Expected: Bitmap Index Scan, <100ms for 10K rows
```

**Success Criteria:** TS_RANK is 50-100× faster

**Deliverable:** `level2_performance_benchmark.txt` with EXPLAIN ANALYZE output

---

## Level 3: JUnit Test Implementation (4 hours)

**Goal:** Create automated tests proving TS_RANK works correctly

### Task 3.1: Extend TsRankCdPerformanceTest.java

```java
// Add comparison test
@Test
public void testTsRankCdVsPositionResults() throws Exception {
    // GIVEN: Test data with Slovak diacritics
    String insert = "INSERT INTO " + TEST_TABLE + " (name, idx_tsvector) VALUES (?, to_tsvector('sk_unaccent', ?))";
    DB.executeUpdateEx(insert, new Object[]{"Muškát ruža", "Muškát ruža"}, null);

    // WHEN: Search without diacritics
    String sql = "SELECT name, " +
        "ts_rank_cd(idx_tsvector, to_tsquery('sk_unaccent', 'muskat'), 2) as rank " +
        "FROM " + TEST_TABLE + " " +
        "WHERE idx_tsvector @@ to_tsquery('sk_unaccent', 'muskat')";

    PreparedStatement pstmt = DB.prepareStatement(sql, null);
    ResultSet rs = pstmt.executeQuery();

    // THEN: Should find "Muškát" when searching "muskat"
    assertThat(rs.next()).isTrue();
    assertThat(rs.getString("name")).contains("Muškát");
    assertThat(rs.getDouble("rank")).isGreaterThan(0);
}
```

**Success Criteria:** Test passes, validates Slovak diacritics handling

---

### Task 3.2: Add Integration Test with Real MSearchIndex

Create: `com.cloudempiere.searchindex.test/src/.../integration/SearchTypeMigrationIntegrationTest.java`

```java
/**
 * Integration test comparing POSITION vs TS_RANK search results
 */
public class SearchTypeMigrationIntegrationTest extends AbstractTestCase {

    @Test
    public void testPositionVsTsRankResults() throws Exception {
        // Create test search index with Slovak data
        MSearchIndex searchIndex = createTestSearchIndex();

        // Test query
        String query = "muskat"; // Search without diacritics

        // Execute POSITION search
        List<ISearchResult> positionResults = provider.getSearchResults(
            ctx, searchIndex.getSearchIndexName(), query, false,
            SearchType.POSITION, getTrxName()
        );

        // Execute TS_RANK search
        List<ISearchResult> tsRankResults = provider.getSearchResults(
            ctx, searchIndex.getSearchIndexName(), query, false,
            SearchType.TS_RANK, getTrxName()
        );

        // Validate: Same record_ids found
        assertThat(tsRankResults).hasSameSizeAs(positionResults);

        // Validate: Ranking is similar (top 3 results should overlap)
        List<Integer> positionTop3 = positionResults.stream()
            .limit(3)
            .map(ISearchResult::getRecordId)
            .collect(Collectors.toList());

        List<Integer> tsRankTop3 = tsRankResults.stream()
            .limit(3)
            .map(ISearchResult::getRecordId)
            .collect(Collectors.toList());

        // Allow some ranking differences, but top 3 should mostly overlap
        long overlap = positionTop3.stream()
            .filter(tsRankTop3::contains)
            .count();

        assertThat(overlap).isGreaterThanOrEqualTo(2); // At least 2 out of 3 same
    }
}
```

**Success Criteria:** Integration test passes on staging database

**Deliverable:** Automated test validating migration

---

## Level 4: Java SearchProvider Update (3 hours)

**Goal:** Implement getTSConfig() and update PGTextSearchIndexProvider

### Task 4.1: Implement getTSConfig() Method

**File:** `com.cloudempiere.searchindex/src/.../indexprovider/PGTextSearchIndexProvider.java`

```java
/**
 * Get text search configuration based on language context
 *
 * @param ctx Properties context
 * @return PostgreSQL text search configuration name
 */
private String getTSConfig(Properties ctx) {
    String language = Env.getContext(ctx, "#AD_Language");

    // Slovak/Czech: Use sk_unaccent for diacritics
    if ("sk_SK".equals(language) || "cs_CZ".equals(language)) {
        // Check if sk_unaccent exists
        String checkSQL = "SELECT COUNT(*) FROM pg_ts_config WHERE cfgname = 'sk_unaccent'";
        int exists = DB.getSQLValue(null, checkSQL);

        if (exists > 0) {
            return "sk_unaccent";
        } else {
            log.warning("sk_unaccent configuration not found. " +
                       "Using 'simple' as fallback. See ADR-003.");
            return "simple";
        }
    }

    // Language-specific configurations
    return switch (language) {
        case "en_US", "en_GB" -> "english";
        case "de_DE", "de_AT" -> "german";
        case "es_ES", "es_MX" -> "spanish";
        case "fr_FR", "fr_CA" -> "french";
        case "it_IT" -> "italian";
        case "pt_PT", "pt_BR" -> "portuguese";
        case "ru_RU" -> "russian";
        default -> "simple"; // Fallback for unknown languages
    };
}
```

**Success Criteria:** Method compiles, returns correct config per language

---

### Task 4.2: Update Index Creation to Use getTSConfig()

```java
// In createIndex() method, around line 180
private void createIndexSQL(Properties ctx, MSearchIndex searchIndex) {
    String tsConfig = getTSConfig(ctx); // ← NEW

    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE ").append(indexTableName)
       .append(" SET idx_tsvector = ");

    // Use getTSConfig() instead of hardcoded 'simple'
    sql.append("to_tsvector('").append(tsConfig).append("', "); // ← CHANGED
    sql.append(columnExpression).append(")");

    // ... rest of method
}
```

**Success Criteria:** Index creation uses language-specific config

---

### Task 4.3: Add Logging for SearchType Usage

```java
// In getSearchResults() method, around line 670
public List<ISearchResult> getSearchResults(
    Properties ctx, String searchIndexName, String query,
    boolean isAdvanced, SearchType searchType, String trxName
) {
    // Log SearchType usage for monitoring
    if (log.isLoggable(Level.INFO)) {
        log.info("Search: index=" + searchIndexName +
                ", type=" + searchType +
                ", query=" + query +
                ", config=" + getTSConfig(ctx));
    }

    // Warn about POSITION performance
    if (searchType == SearchType.POSITION) {
        log.warning("DEPRECATED: SearchType.POSITION is slow. " +
                   "Consider migrating to TS_RANK. See ADR-005.");
    }

    // ... rest of method
}
```

**Success Criteria:** Logging shows SearchType usage, warnings visible

**Deliverable:** Updated `PGTextSearchIndexProvider.java` with logging

---

## Level 5: UI Component Update (2 hours)

**Goal:** Change UI to use TS_RANK by default (with fallback option)

### Task 5.1: Update ZkSearchIndexUI.java

**File:** `com.cloudempiere.searchindex.ui/src/.../ui/ZkSearchIndexUI.java`

```java
// Around line 189
private void performSearch() {
    String query = searchBox.getValue();

    // Get search type preference (default: TS_RANK)
    String defaultSearchType = MSysConfig.getValue(
        "SEARCH_INDEX_DEFAULT_TYPE",
        "TS_RANK", // ← NEW DEFAULT
        Env.getAD_Client_ID(ctx)
    );

    SearchType searchType = "POSITION".equals(defaultSearchType)
        ? SearchType.POSITION
        : SearchType.TS_RANK;

    // Log for debugging
    if (CLogMgt.isLevelFine()) {
        log.fine("UI Search: type=" + searchType + ", query=" + query);
    }

    // Execute search
    List<ISearchResult> results = searchIndexProvider.getSearchResults(
        ctx,
        searchIndex.getSearchIndexName(),
        query,
        isAdvanced,
        searchType, // ← NOW CONFIGURABLE
        null
    );

    // ... display results
}
```

**Success Criteria:** UI uses TS_RANK by default, configurable via SysConfig

---

### Task 5.2: Add Debug Panel (Optional)

```java
// Add debug checkbox to UI (development only)
private Checkbox debugSearchType;

private void initDebugMode() {
    if (MSysConfig.getBooleanValue("SEARCH_INDEX_DEBUG_MODE", false, 0)) {
        debugSearchType = new Checkbox("Use POSITION (legacy)");
        debugSearchType.setChecked(false);
        // Add to UI layout
    }
}
```

**Success Criteria:** Developers can toggle SearchType for testing

**Deliverable:** Updated `ZkSearchIndexUI.java`

---

## Level 6: REST API Update (3 hours)

**Goal:** Update REST API to use TS_RANK (in cloudempiere-rest repo)

### Task 6.1: Clone and Setup REST API Repo

```bash
cd /Users/norbertbede/github/
git clone https://github.com/cloudempiere/cloudempiere-rest.git
cd cloudempiere-rest
git checkout cloudempiere-development
git pull origin cloudempiere-development

# Create feature branch
git checkout -b feat/adr-005-tsrank-rest-api
```

**Deliverable:** REST API repo ready for changes

---

### Task 6.2: Update DefaultQueryConverter.java

**File:** `com.trekglobal.idempiere.rest.api/src/.../json/filter/DefaultQueryConverter.java`

```java
// Around line 689
private String convertSearchIndexFunction(...) {
    // ... existing code ...

    // Get search type from system config (default: TS_RANK)
    String defaultSearchType = MSysConfig.getValue(
        "SEARCH_INDEX_REST_API_TYPE",
        "TS_RANK", // ← NEW DEFAULT
        Env.getAD_Client_ID(ctx)
    );

    SearchType searchType = "POSITION".equals(defaultSearchType)
        ? SearchType.POSITION
        : SearchType.TS_RANK;

    // Log REST API search
    if (log.isLoggable(Level.INFO)) {
        log.info("REST API Search: type=" + searchType +
                ", index=" + searchIndex.getSearchIndexName() +
                ", query=" + query);
    }

    // Execute search with new default
    List<ISearchResult> results = provider.getSearchResults(
        ctx,
        searchIndex.getSearchIndexName(),
        query,
        isAdvanced,
        searchType, // ← CHANGED FROM HARDCODED POSITION
        null
    );

    // ... rest of method
}
```

**Success Criteria:** REST API uses TS_RANK by default

---

### Task 6.3: Update ProductAttributeQueryConverter.java

**File:** `org.cloudempiere.rest.api/src/.../json/filter/ProductAttributeQueryConverter.java`

```java
// Around line 505
// Same change as DefaultQueryConverter
SearchType searchType = "POSITION".equals(
    MSysConfig.getValue("SEARCH_INDEX_REST_API_TYPE", "TS_RANK", clientId)
) ? SearchType.POSITION : SearchType.TS_RANK;

List<ISearchResult> results = provider.getSearchResults(
    ctx, searchIndex.getSearchIndexName(),
    query, isAdvanced,
    searchType, // ← CHANGED
    null
);
```

**Success Criteria:** Product attribute search also uses TS_RANK

---

### Task 6.4: Build and Test REST API

```bash
cd cloudempiere-rest

# Build
mvn clean package -DskipTests

# Run integration tests
mvn test -Dtest=SearchIndexRestTest

# Manual test REST endpoint
curl -X GET "http://localhost:8080/api/v1/models/m_product?$filter=searchindex('product_idx','muskat')&$orderby=searchindexrank desc" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success Criteria:** REST API compiles, tests pass, manual test works

**Deliverable:** Updated REST API with TS_RANK default

---

## Integration Testing (6 hours)

### Task 7.1: End-to-End Test Scenarios

| Test # | Scenario | Expected Result | Pass/Fail |
|--------|----------|----------------|-----------|
| 1 | UI: Search "muskat" | Finds "Muškát" products | ⏳ |
| 2 | UI: Results < 100ms | Fast response | ⏳ |
| 3 | REST API: Search "muskat" | Same products as UI | ⏳ |
| 4 | REST API: Performance | < 100ms response | ⏳ |
| 5 | Ranking: Earlier match first | Correct order | ⏳ |
| 6 | Multi-term: "muskat ruza" | Both matched | ⏳ |
| 7 | Advanced: "muskat & ruza" | AND operator works | ⏳ |
| 8 | Advanced: "muskat | ruza" | OR operator works | ⏳ |
| 9 | Prefix: "musk:*" | Prefix search works | ⏳ |
| 10 | Fallback: SysConfig=POSITION | Uses old method | ⏳ |

**Deliverable:** `integration_test_results.md` with all tests passing

---

### Task 7.2: Performance Load Test

```bash
# Load test REST API with 1000 concurrent searches
ab -n 1000 -c 10 \
  -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/v1/models/m_product?$filter=searchindex('product_idx','muskat')"

# Expected:
# - Average response time < 100ms
# - 0 failed requests
# - CPU usage < 50%
```

**Success Criteria:** Performance meets expectations under load

---

## Deployment Plan (2 hours)

### Task 8.1: Staging Deployment

```bash
# 1. Deploy to staging environment
# 2. Run smoke tests
# 3. Monitor logs for 24 hours
# 4. Collect performance metrics
```

---

### Task 8.2: Production Rollout (Gradual)

**Week 1:** Enable for 10% of clients
```sql
-- System Configurator
INSERT INTO AD_SysConfig (Name, Value, Description)
VALUES ('SEARCH_INDEX_DEFAULT_TYPE', 'TS_RANK',
        'Use TS_RANK for better performance (ADR-005)');

-- Enable only for specific client (test client)
UPDATE AD_SysConfig
SET AD_Client_ID = 1000000 -- Test client only
WHERE Name = 'SEARCH_INDEX_DEFAULT_TYPE';
```

**Week 2:** Enable for 50% of clients (if successful)

**Week 3:** Enable for all clients (if successful)

---

## Rollback Plan (30 minutes)

If issues occur:

```sql
-- Emergency rollback: Switch back to POSITION
UPDATE AD_SysConfig
SET Value = 'POSITION'
WHERE Name = 'SEARCH_INDEX_DEFAULT_TYPE';

-- Restart iDempiere to clear caches
```

**Recovery Time:** < 5 minutes

---

## Monitoring & Validation (Ongoing)

### Key Metrics to Track

```sql
-- Create monitoring table
CREATE TABLE search_performance_log (
    log_id SERIAL PRIMARY KEY,
    search_type VARCHAR(20),
    query TEXT,
    result_count INT,
    duration_ms INT,
    ad_client_id INT,
    created TIMESTAMP DEFAULT NOW()
);

-- Daily performance report
SELECT
    search_type,
    COUNT(*) as searches,
    AVG(duration_ms) as avg_ms,
    MAX(duration_ms) as max_ms,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY duration_ms) as p95_ms
FROM search_performance_log
WHERE created > NOW() - INTERVAL '1 day'
GROUP BY search_type;

-- Expected after migration:
-- search_type | searches | avg_ms | max_ms | p95_ms
-- TS_RANK     | 10,000   | 45     | 150    | 80
-- POSITION    | 50       | 4500   | 12000  | 8000
```

---

## Timeline Summary

| Phase | Duration | Cumulative |
|-------|----------|------------|
| 0. Preparation | 1h | 1h |
| 1. PostgreSQL Testing | 2h | 3h |
| 2. Database Testing | 3h | 6h |
| 3. JUnit Tests | 4h | 10h |
| 4. Java SearchProvider | 3h | 13h |
| 5. UI Update | 2h | 15h |
| 6. REST API Update | 3h | 18h |
| 7. Integration Testing | 6h | 24h |
| 8. Deployment | 2h | 26h |
| **Total** | **26 hours** | **~3.5 days** |

---

## Success Criteria

**Phase Complete When:**
- ✅ All Level 1-6 tests pass
- ✅ Performance is 50-100× faster than POSITION
- ✅ Search results are 95%+ identical to POSITION
- ✅ No production issues after 7 days in staging
- ✅ User feedback is positive (faster searches)

---

## Next Immediate Action

**Start with Level 1:**
```bash
cd /Users/developer/GitHub/com.cloudempiere.searchindex

# Run Level 1 comparison on test database
psql -U postgres -d test_searchindex -f test-position-vs-tsrank-comparison.sql > level1_results.txt

# Review results
cat level1_results.txt | grep "==="
```

**Expected Output:**
```
=== TEST 1: CURRENT POSITION (REGEX) APPROACH ===
=== POSITION Performance Stats ===
=== TEST 2: TS_RANK WITH SIMPLE CONFIG (NO DIACRITICS) ===
=== TEST 3: CREATE SK_UNACCENT CONFIGURATION ===
=== COMPREHENSIVE SIDE-BY-SIDE COMPARISON ===
=== PERFORMANCE COMPARISON ===
```

If all tests pass → Proceed to Level 2

---

**Document Status:** ✅ Complete
**Approval Required:** Yes (Development Lead)
**Implementation Start:** After approval
