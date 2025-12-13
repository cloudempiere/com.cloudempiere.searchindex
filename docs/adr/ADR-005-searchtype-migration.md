# ADR-005: SearchType Migration from POSITION to TS_RANK

**Status:** Proposed
**Date:** 2025-12-12
**Decision Makers:** Development Team, Performance Team
**Related Issues:** Known Issue - SearchType.POSITION Performance (CLAUDE.md:91-124)

---

## Context

The search index plugin supports two ranking algorithms with **drastically different performance characteristics**:

1. **SearchType.TS_RANK** - Uses PostgreSQL's native `ts_rank()` function
2. **SearchType.POSITION** - Uses regex operations on tsvector text (current default)

### Performance Comparison

| Metric | TS_RANK | POSITION | Difference |
|--------|---------|----------|------------|
| Index Usage | ✅ GIN index | ❌ Bypasses index | **N/A** |
| Complexity | O(log n) | O(n × 6t) | **100× faster** |
| 1,000 rows | <10ms | ~500ms | **50×** |
| 10,000 rows | <50ms | ~5s | **100×** |
| 100,000 rows | ~100ms | ~50s (timeout) | **500×** |

**Where:** n=rows, t=search terms

### Current Implementation

**UI hardcodes POSITION:**
```java
// ZkSearchIndexUI.java:189
List<ISearchResult> results = searchIndexProvider.getSearchResults(
    ctx, searchIndex.getSearchIndexName(), query, isAdvanced,
    SearchType.POSITION,  // ← HARDCODED!
    null
);
```

**REST API also hardcodes POSITION:**
```java
// DefaultQueryConverter.java:689 (cloudempiere-rest repo)
return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
    query, isAdvanced, SearchType.POSITION, null);  // ← HARDCODED!

// ProductAttributeQueryConverter.java:505
return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
    query, isAdvanced, SearchType.POSITION, null);  // ← HARDCODED!
```

### Why POSITION Was Created

**Root Cause:** Slovak language diacritics handling (č, š, ž, á, etc.)

PostgreSQL lacked proper Slovak text search configuration when POSITION was implemented. The regex workaround was created to handle diacritics by matching position patterns in the tsvector text representation.

**Problem:** This workaround:
- Casts `idx_tsvector::text` (loses GIN index)
- Executes 6 regex operations per search term per row
- Does not scale beyond 1,000 rows

---

## Decision

We will **migrate default SearchType from POSITION to TS_RANK** and implement **proper Slovak language support** using PostgreSQL text search configurations.

### Strategy

1. **Phase 1:** Create Slovak text search configuration with unaccent
2. **Phase 2:** Change default SearchType to TS_RANK in all codebases
3. **Phase 3:** Deprecate POSITION (mark as legacy, remove in future version)

---

## Phase 1: Slovak Language Support (Proper Solution)

### Create Slovak Text Search Configuration

```sql
-- Install unaccent extension (if not exists)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Create Slovak-specific text search configuration
CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);

-- Use unaccent for word mapping (removes diacritics: č→c, š→s, ž→z)
ALTER TEXT SEARCH CONFIGURATION sk_unaccent
  ALTER MAPPING FOR word, asciiword, hword, hword_asciipart, asciihword
  WITH unaccent, simple;

-- Test:
SELECT to_tsvector('sk_unaccent', 'ruža môže');
-- Result: 'moze':2 'ruza':1  (diacritics removed)

SELECT to_tsquery('sk_unaccent', 'ruza');
-- Matches: 'ruža', 'ruza', 'rúža'  (all diacritic variants)
```

### Update Index Creation to Use Slovak Config

```java
// PGTextSearchIndexProvider.java
private String getTSConfig(Properties ctx) {
    // Check for Slovak language
    String language = Env.getContext(ctx, "#AD_Language");

    if ("sk_SK".equals(language) || "cs_CZ".equals(language)) {
        return "sk_unaccent";  // Slovak/Czech with diacritics
    }

    // Default configurations
    return switch (language) {
        case "en_US" -> "english";
        case "de_DE" -> "german";
        case "es_ES" -> "spanish";
        case "fr_FR" -> "french";
        default -> "simple";  // Fallback
    };
}

// Use in index creation:
sql.append("to_tsvector('").append(getTSConfig(ctx)).append("', ")
   .append(columnExpression).append(")");
```

### Benefits of Slovak TS Config

- ✅ Handles diacritics correctly (unaccent)
- ✅ Uses GIN index (100× faster)
- ✅ Standard PostgreSQL feature (no regex hacks)
- ✅ Maintains search quality for Slovak users
- ✅ Scales to millions of rows

---

## Phase 2: Change Default SearchType to TS_RANK

### UI Change (ZkSearchIndexUI.java)

```java
// ZkSearchIndexUI.java:189
- List<ISearchResult> results = searchIndexProvider.getSearchResults(
-     ctx, searchIndex.getSearchIndexName(), query, isAdvanced,
-     SearchType.POSITION,  // OLD (slow)
-     null
- );

+ // Use TS_RANK for production performance
+ List<ISearchResult> results = searchIndexProvider.getSearchResults(
+     ctx, searchIndex.getSearchIndexName(), query, isAdvanced,
+     SearchType.TS_RANK,  // NEW (fast)
+     null
+ );
```

### REST API Change (cloudempiere-rest repo)

**File 1:** `DefaultQueryConverter.java:689`
```java
- return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
-     query, isAdvanced, SearchType.POSITION, null);

+ return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
+     query, isAdvanced, SearchType.TS_RANK, null);
```

**File 2:** `ProductAttributeQueryConverter.java:505`
```java
- return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
-     query, isAdvanced, SearchType.POSITION, null);

+ return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
+     query, isAdvanced, SearchType.TS_RANK, null);
```

### Configuration Option (Future Enhancement)

```java
// Add system config for user preference
String defaultSearchType = MSysConfig.getValue(
    "SEARCH_INDEX_DEFAULT_TYPE",  // Config key
    "TS_RANK",                     // Default value
    Env.getAD_Client_ID(ctx)
);

SearchType searchType = "POSITION".equals(defaultSearchType)
    ? SearchType.POSITION
    : SearchType.TS_RANK;
```

---

## Phase 3: Deprecate POSITION SearchType

### Mark as Deprecated

```java
public enum SearchType {
    TS_RANK,     // Recommended for production

    /**
     * @deprecated Use TS_RANK for production. POSITION uses regex on tsvector::text
     * which bypasses GIN index and causes 100× performance degradation.
     * Only use for datasets <1,000 rows or legacy compatibility.
     * Will be removed in version 2.0.
     */
    @Deprecated(since = "1.3.0", forRemoval = true)
    POSITION
}
```

### Add Performance Warning

```java
// PGTextSearchIndexProvider.java:670
public List<ISearchResult> getSearchResults(..., SearchType searchType, ...) {

    if (searchType == SearchType.POSITION) {
        // Warn about performance issue
        log.warning("PERFORMANCE: SearchType.POSITION is deprecated and slow. " +
                    "Use SearchType.TS_RANK for production. See ADR-005.");

        // Count records
        int recordCount = DB.getSQLValue(null,
            "SELECT COUNT(*) FROM " + searchIndexName);

        if (recordCount > 1000) {
            log.severe("PERFORMANCE RISK: " + recordCount + " records with POSITION search type. " +
                       "Expected query time: ~" + (recordCount * 0.5) + "ms. " +
                       "Migrate to TS_RANK (see ADR-005).");
        }
    }

    // Continue with search...
}
```

---

## Implementation Plan

### Week 1: Slovak Language Support (2 days)

1. **Create Slovak TS config** (1 hour)
   - Run SQL script on development database
   - Test with sample Slovak text

2. **Update getTSConfig() method** (1 hour)
   - Add Slovak/Czech language detection
   - Default to 'sk_unaccent' for these languages

3. **Re-index with new config** (2 hours)
   - Mark all Slovak indexes as invalid
   - Re-run CreateSearchIndex process
   - Verify diacritics handling

4. **Testing** (4 hours)
   - Test Slovak searches: "ruža" matches "ruza", "rúža", "růža"
   - Verify performance: <100ms for 10K records
   - Test other languages unchanged

### Week 2: Change Default SearchType (1 day)

1. **Update UI code** (1 hour)
   - Change ZkSearchIndexUI.java:189
   - Build and deploy to test environment

2. **Update REST API** (2 hours)
   - Clone cloudempiere-rest repository
   - Checkout cloudempiere-development branch
   - Change DefaultQueryConverter.java + ProductAttributeQueryConverter.java
   - Build and deploy

3. **Integration testing** (4 hours)
   - Test UI search (verify TS_RANK used)
   - Test REST API search (verify TS_RANK used)
   - Compare performance: before/after

4. **Document migration** (1 hour)
   - Update CLAUDE.md
   - Add release notes

### Week 3: Deprecation & Monitoring (0.5 days)

1. **Add deprecation warnings** (1 hour)
   - Mark SearchType.POSITION as @Deprecated
   - Add performance warnings in code

2. **Setup monitoring** (2 hours)
   - Track SearchType usage metrics
   - Alert if POSITION used on large datasets

3. **User communication** (1 hour)
   - Email to existing users
   - Migration guide for custom code

---

## Testing & Validation

### Performance Benchmarks

```sql
-- Setup: 10,000 product records with Slovak text
INSERT INTO m_product (name, description, ...)
SELECT
    'Produkt ' || i || ' s diakritikou (čšžýá)',
    'Popis s rôznymi znakmi: môže, váže, túži',
    ...
FROM generate_series(1, 10000) i;

-- Rebuild index
UPDATE AD_SearchIndex SET IsValid='N' WHERE SearchIndexName='product_idx';
-- Run CreateSearchIndex process

-- Benchmark: POSITION (old)
\timing on
SELECT * FROM searchindex_product
WHERE idx_tsvector::text ~ '.*ruža.*'
ORDER BY ...;
-- Expected: ~5000ms (5 seconds)

-- Benchmark: TS_RANK with Slovak config (new)
SELECT * FROM searchindex_product
WHERE idx_tsvector @@ to_tsquery('sk_unaccent', 'ruza')
ORDER BY ts_rank(idx_tsvector, to_tsquery('sk_unaccent', 'ruza')) DESC;
-- Expected: <50ms

-- Result: 100× FASTER ✅
```

### Diacritics Accuracy Test

```sql
-- Test: Search "ruza" (no diacritics) should find "ruža" (with diacritics)

-- Insert test data
INSERT INTO m_product (name, ...) VALUES ('Červená ruža', ...);

-- Re-index
UPDATE AD_SearchIndex SET IsValid='N';

-- Search without diacritics
SELECT * FROM searchindex_product
WHERE idx_tsvector @@ to_tsquery('sk_unaccent', 'cervena & ruza');

-- Expected: Finds "Červená ruža" ✅
```

---

## Monitoring & Metrics

### Search Performance Dashboard

```sql
-- Track average search time by SearchType
CREATE TABLE search_audit_log (
    audit_id SERIAL PRIMARY KEY,
    search_type VARCHAR(20),
    query TEXT,
    result_count INT,
    duration_ms INT,
    ad_client_id INT,
    created TIMESTAMP DEFAULT NOW()
);

-- Query for performance comparison
SELECT
    search_type,
    AVG(duration_ms) AS avg_ms,
    MAX(duration_ms) AS max_ms,
    COUNT(*) AS searches
FROM search_audit_log
WHERE created > NOW() - INTERVAL '7 days'
GROUP BY search_type;

-- Expected results after migration:
-- search_type | avg_ms | max_ms | searches
-- TS_RANK     | 45     | 150    | 10,000
-- POSITION    | 4500   | 12000  | 10   (legacy only)
```

### Migration Progress Tracking

```sql
-- Count POSITION usage (should decrease over time)
SELECT COUNT(*) FROM search_audit_log
WHERE search_type = 'POSITION'
  AND created > NOW() - INTERVAL '1 day';

-- Alert if POSITION usage >1% of total searches
```

---

## Consequences

### Positive

1. **100× performance improvement**
   - Searches complete in <100ms (was 5-10s)
   - Users experience instant results

2. **Scalability**
   - Can handle 100K+ records (was limited to 1K)
   - Linear scaling instead of quadratic

3. **Proper language support**
   - Slovak/Czech diacritics handled correctly
   - No regex hacks needed

4. **Cost savings**
   - Reduced server load (CPU/memory)
   - Lower infrastructure costs

5. **Better user experience**
   - Fast, accurate search
   - No timeouts on large datasets

### Negative

1. **Migration effort**
   - Must update two repositories (searchindex + cloudempiere-rest)
   - Requires re-indexing all search indexes

2. **Behavioral changes**
   - TS_RANK ranking may differ from POSITION
   - Users may notice different result ordering

3. **One-time effort**
   - All clients must re-index
   - Estimated downtime: 30 minutes per large index

---

## Alternatives Considered

### Alternative 1: Keep POSITION, Optimize Regex

Optimize the 6 regex operations in POSITION search type:
```java
// Combine into single regex
- 6 separate regex operations
+ 1 combined regex with capturing groups
```

**Pros:** No migration needed

**Cons:**
- Still bypasses GIN index (still O(n) complexity)
- At best 6× faster, still 15× slower than TS_RANK
- **REJECTED:** Band-aid on fundamental design issue

---

### Alternative 2: Hybrid Approach (POSITION for <1K, TS_RANK for >1K)

Automatically choose SearchType based on dataset size:
```java
int recordCount = getIndexRecordCount(searchIndexName);
SearchType type = recordCount < 1000 ? SearchType.POSITION : SearchType.TS_RANK;
```

**Pros:** Backward compatibility

**Cons:**
- Complex logic
- Inconsistent results across clients
- **REJECTED:** Confusing for users

---

### Alternative 3: Elasticsearch Migration

Replace PostgreSQL FTS with Elasticsearch entirely.

**Pros:**
- Even better performance
- Advanced features (fuzzy search, facets)

**Cons:**
- Major infrastructure change
- Additional service to maintain
- Cost: ~€36K for 3 years (see CLAUDE.md)
- **DEFERRED:** Consider for future if TS_RANK insufficient

---

## Rollback Plan

If TS_RANK causes issues:

1. **Immediate rollback** (emergency)
   ```java
   // Revert code changes
   - SearchType.TS_RANK
   + SearchType.POSITION
   ```

2. **Re-index with old config**
   ```sql
   -- Drop Slovak config, use 'simple'
   UPDATE AD_SearchIndex SET IsValid='N';
   ```

3. **Gradual rollback** (less disruptive)
   ```java
   // Add system config toggle
   boolean useTsRank = MSysConfig.getBooleanValue(
       "SEARCH_INDEX_USE_TS_RANK", true, clientId
   );
   ```

---

## Cost-Benefit Analysis

### Investment

| Phase | Effort | Cost (€500/day) |
|-------|--------|-----------------|
| Slovak TS config | 2 days | €1,000 |
| Change default SearchType | 1 day | €500 |
| Deprecation & monitoring | 0.5 days | €250 |
| **Total** | **3.5 days** | **€1,750** |

### Savings

**Infrastructure:**
- Reduced CPU usage: 90% reduction (100× faster queries)
- Estimated monthly savings: €500 (less server capacity needed)
- **Break-even:** 3.5 months

**User productivity:**
- Time saved per search: 4.5s average
- 1,000 searches/day = 4,500s = 75 minutes/day
- Value: ~€300/day (€6,000/month)

**Total ROI:** €6,500/month (infrastructure + productivity)
**Payback period:** <1 month

---

## References

- PostgreSQL Full Text Search: https://www.postgresql.org/docs/current/textsearch.html
- Unaccent Extension: https://www.postgresql.org/docs/current/unaccent.html
- CLAUDE.md:91-124 (SearchType Performance Analysis)
- CLAUDE.md:55-89 (Slovak Language Root Cause)
- LOW-COST-SLOVAK-ECOMMERCE-SEARCH.md (Complete implementation guide)

---

## Decision

**Approved:** [Pending]
**Approved By:** [Name]
**Approval Date:** [Date]
**Implementation Target:** Week 3-4 (Phase 3 - Performance Optimization)

---

**Related ADRs:**
- [ADR-001: Transaction Isolation](ADR-001-transaction-isolation.md)
- [ADR-002: SQL Injection Prevention](ADR-002-sql-injection-prevention.md)
- [ADR-003: Slovak Text Search Configuration](ADR-003-slovak-text-search-configuration.md) - **Root cause solution** for Slovak language support
- [ADR-004: REST API OData Integration](ADR-004-rest-api-odata-integration.md) - REST API also needs SearchType migration
- [ADR-006: Multi-Tenant Integrity](ADR-006-multi-tenant-integrity.md)
- [ADR-007: Search Technology Selection](ADR-007-search-technology-selection.md) - Why PostgreSQL FTS was chosen
