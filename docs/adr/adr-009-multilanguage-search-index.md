# ADR-009: Multi-Language Search Index Architecture

**Status:** Proposed
**Date:** 2025-12-18
**Issue:** N/A
**Deciders:** Development Team, Architecture Team

## Context

The search index plugin currently supports **only one language per search index**, determined by the client's default language (`MClient.getLanguage().getAD_Language()`). This limitation creates problems in multi-language environments where:

1. **REST API** has locale context per request (HTTP Accept-Language header)
2. **Web UI** has user-specific language preferences (`Env.getAD_Language(ctx)`)
3. **Different users** in the same client may prefer different languages
4. **E-commerce sites** need to serve multiple language markets simultaneously

### Current Limitations

**Index Table Structure** (e.g., `idx_product_ts`):
```sql
CREATE TABLE idx_product_ts (
    ad_client_id INTEGER,
    ad_table_id INTEGER,
    record_id INTEGER,
    idx_tsvector tsvector,
    CONSTRAINT idx_product_ts_unique UNIQUE (ad_client_id, ad_table_id, record_id)
);
```

**Problem:** One record can only be indexed in **one language** (client's default).

**Language Detection** (PGTextSearchIndexProvider.java:579-613):
```java
private String getTSConfig(Properties ctx, String trxName) {
    String languageCode = MClient.get(ctx).getLanguage().getAD_Language();  // ← Client-level only!
    // ...
    return tsConfig;
}
```

**Problem:** User's preferred language is ignored; always uses client's default language.

### Requirements

**Functional:**
- Support multiple languages per search index simultaneously
- Allow users to search in their preferred language
- REST API should respect `Accept-Language` header or explicit locale parameter
- Web UI should respect user's language preference (`#AD_Language` context variable)
- Maintain backward compatibility with single-language deployments

**Non-Functional:**
- Minimal performance impact (<10% overhead)
- Scalable to 10+ languages per index
- Simple migration path from current architecture
- Acceptable storage increase (<3× for typical 3-language deployment)

## Decision Drivers

- **User Experience:** Users expect search results in their language
- **E-commerce Requirements:** Multi-market sites need multi-language search
- **REST API Integration:** cloudempiere-rest provides locale context that's currently unused
- **Backward Compatibility:** Many deployments use single language and shouldn't be affected
- **Performance:** Index size and query performance must remain acceptable
- **Maintainability:** Solution must be simple to understand and maintain

## Considered Options

### Option 1: Single Index with AD_Language Column (Recommended)

**Description:** Add `ad_language` column to index tables and maintain one tsvector per language per record.

**Architecture:**

```sql
-- Modified table structure
CREATE TABLE idx_product_ts (
    ad_client_id INTEGER NOT NULL,
    ad_table_id INTEGER NOT NULL,
    record_id INTEGER NOT NULL,
    ad_language VARCHAR(6) NOT NULL,  -- NEW: e.g., 'sk_SK', 'en_US', 'de_DE'
    idx_tsvector tsvector NOT NULL,
    CONSTRAINT idx_product_ts_unique UNIQUE (ad_client_id, ad_table_id, record_id, ad_language)
);

-- GIN index per language (optional optimization)
CREATE INDEX idx_product_ts_search_sk ON idx_product_ts USING GIN(idx_tsvector) WHERE ad_language = 'sk_SK';
CREATE INDEX idx_product_ts_search_en ON idx_product_ts USING GIN(idx_tsvector) WHERE ad_language = 'en_US';
```

**Example Data:**
```sql
-- Product "Rose" in 3 languages
INSERT INTO idx_product_ts (ad_client_id, ad_table_id, record_id, ad_language, idx_tsvector) VALUES
  (11, 208, 1000, 'sk_SK', to_tsvector('slovak', 'ruža červená')),
  (11, 208, 1000, 'en_US', to_tsvector('english', 'red rose')),
  (11, 208, 1000, 'cs_CZ', to_tsvector('czech', 'červená růže'));
```

**Search Query:**
```sql
-- User searches in Slovak
SELECT ad_table_id, record_id,
       ts_rank(array[1.0, 0.7, 0.4, 0.2], idx_tsvector, to_tsquery('slovak', 'ruža')) AS rank
FROM idx_product_ts
WHERE ad_language = 'sk_SK'  -- NEW: Filter by user's language
  AND idx_tsvector @@ to_tsquery('slovak', 'ruža')
ORDER BY rank DESC;
```

**Code Changes:**

**1. Modify `createIndex()` to support multiple languages:**
```java
@Override
public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName) {
    // Get list of languages to index (from AD_SearchIndex configuration or system config)
    List<String> languages = getIndexLanguages(ctx, trxName);  // NEW method

    for (String language : languages) {
        String tsConfig = getTSConfigForLanguage(language, trxName);  // NEW: language-specific

        for (Map.Entry<Integer, Set<SearchIndexTableData>> searchIndexRecordSet : indexRecordsMap.entrySet()) {
            for (SearchIndexTableData searchIndexRecord : searchIndexRecordSet.getValue()) {
                // ... existing code ...

                StringBuilder upsertQuery = new StringBuilder();
                upsertQuery.append("INSERT INTO ").append(tableName).append(" ")
                           .append("(ad_client_id, ad_table_id, record_id, ad_language, idx_tsvector) VALUES (?, ?, ?, ?, ")  // NEW: ad_language
                           .append(documentContent).append(") ")
                           .append("ON CONFLICT (ad_client_id, ad_table_id, record_id, ad_language) DO UPDATE SET ")  // NEW: ad_language in constraint
                           .append("idx_tsvector = EXCLUDED.idx_tsvector");

                params.add(language);  // NEW parameter
            }
        }
    }
}
```

**2. Modify `getSearchResults()` to use user's language:**
```java
@Override
public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName, String query,
                                            boolean isAdvanced, SearchType searchType, String trxName) {
    // Get user's language from context (REST API or Web UI)
    String userLanguage = getUserLanguage(ctx);  // NEW method
    String tsConfig = getTSConfigForLanguage(userLanguage, trxName);

    // ... existing code ...

    sql.append(" WHERE ad_language = ? AND idx_tsvector @@ ");  // NEW: Filter by user's language
    params.add(userLanguage);

    // ... rest of query ...
}
```

**3. Add new helper methods:**
```java
/**
 * Get user's preferred language from context.
 * Priority: 1) REST API locale, 2) User language, 3) Client default
 */
private String getUserLanguage(Properties ctx) {
    // Check if REST API locale is set (from Accept-Language header)
    String restLocale = ctx.getProperty("#Locale");
    if (!Util.isEmpty(restLocale)) {
        return localeToLanguageCode(restLocale);
    }

    // Check user's language preference
    String userLang = Env.getContext(ctx, "#AD_Language");
    if (!Util.isEmpty(userLang)) {
        return userLang;
    }

    // Fall back to client's default language
    return MClient.get(ctx).getLanguage().getAD_Language();
}

/**
 * Get list of languages to maintain in index.
 * Can be configured per AD_SearchIndex or globally via MSysConfig.
 */
private List<String> getIndexLanguages(Properties ctx, String trxName) {
    // Option 1: From AD_SearchIndex.Languages (NEW column)
    // Option 2: From MSysConfig.SEARCHINDEX_LANGUAGES (e.g., "sk_SK,en_US,de_DE")
    // Option 3: All system languages (SELECT DISTINCT AD_Language FROM AD_Language WHERE IsSystemLanguage='Y')

    String configValue = MSysConfig.getValue("SEARCHINDEX_LANGUAGES", "sk_SK", Env.getAD_Client_ID(ctx));
    return Arrays.asList(configValue.split(","));
}

/**
 * Get text search configuration for specific language.
 */
private String getTSConfigForLanguage(String adLanguage, String trxName) {
    // Map iDempiere language codes to PostgreSQL text search configs
    Map<String, String> languageMapping = Map.of(
        "sk_SK", "slovak",
        "cs_CZ", "czech",
        "en_US", "english",
        "de_DE", "german",
        "fr_FR", "french"
    );

    String tsConfig = languageMapping.getOrDefault(adLanguage, "simple");

    // Check if config exists, fall back to unaccent
    String checkQuery = "SELECT COUNT(*) FROM pg_ts_config WHERE cfgname = ?";
    if (DB.getSQLValue(trxName, checkQuery, tsConfig) == 0) {
        tsConfig = "unaccent";
    }

    return tsConfig;
}
```

**Pros:**
- ✅ **Unified index management:** Single table per entity type
- ✅ **Simple queries:** Just add `WHERE ad_language = ?` filter
- ✅ **Flexible language support:** Easy to add/remove languages
- ✅ **Partial indexes:** Can optimize with partial GIN indexes per language
- ✅ **Atomic updates:** Update single row per language
- ✅ **Easy migration:** ALTER TABLE + UPDATE script
- ✅ **REST API integration:** Respects `Accept-Language` header
- ✅ **User preferences:** Respects `#AD_Language` context variable

**Cons:**
- ⚠️ **Storage increase:** 3× storage for 3 languages (acceptable)
- ⚠️ **Index updates:** Must update N rows (one per language) instead of 1
- ⚠️ **Migration effort:** ALTER TABLE + data migration required
- ⚠️ **Backward compatibility:** Requires migration script for existing deployments

**Cost/Effort:** Medium (1-2 weeks)
- Database migration script: 1 day
- Code changes: 2-3 days
- Testing: 2-3 days
- Documentation: 1 day

**Verdict:** ✅ **RECOMMENDED** - Best balance of flexibility, performance, and maintainability

---

### Option 2: Per-Language Index Tables

**Description:** Create separate index tables for each language (e.g., `idx_product_ts_sk`, `idx_product_ts_en`, `idx_product_ts_de`).

**Architecture:**

```sql
-- Separate table per language
CREATE TABLE idx_product_ts_sk_SK (
    ad_client_id INTEGER,
    ad_table_id INTEGER,
    record_id INTEGER,
    idx_tsvector tsvector,
    UNIQUE (ad_client_id, ad_table_id, record_id)
);

CREATE TABLE idx_product_ts_en_US (
    ad_client_id INTEGER,
    ad_table_id INTEGER,
    record_id INTEGER,
    idx_tsvector tsvector,
    UNIQUE (ad_client_id, ad_table_id, record_id)
);

CREATE INDEX idx_product_ts_sk_search ON idx_product_ts_sk_SK USING GIN(idx_tsvector);
CREATE INDEX idx_product_ts_en_search ON idx_product_ts_en_US USING GIN(idx_tsvector);
```

**Search Query:**
```java
String tableName = getIndexTableName(searchIndexName, userLanguage);  // idx_product_ts_sk_SK
sql.append("SELECT * FROM ").append(tableName).append(" WHERE ...");
```

**Pros:**
- ✅ **No storage overhead:** Only index languages you need
- ✅ **Simpler queries:** No `ad_language` filter needed
- ✅ **Better index performance:** Dedicated GIN index per language
- ✅ **Backward compatible:** Can keep existing table as default language

**Cons:**
- ❌ **Table proliferation:** N tables per index × M languages = explosion of tables
- ❌ **Schema management nightmare:** Must create/drop tables dynamically
- ❌ **Complex queries:** Must union across tables for multi-language results
- ❌ **Migration complexity:** Must split existing data into N tables
- ❌ **Event handler complexity:** Must update N tables on record change
- ❌ **Configuration complexity:** Must track which tables exist for which languages
- ❌ **Backup/restore complexity:** Must handle dynamic table creation

**Cost/Effort:** High (3-4 weeks)

**Verdict:** ❌ **REJECTED** - Too complex, poor maintainability

---

### Option 3: Hybrid - Materialized View per Language

**Description:** Keep single source table with `ad_language` column, create materialized views per language for performance.

**Architecture:**

```sql
-- Source table (Option 1)
CREATE TABLE idx_product_ts (
    ad_client_id INTEGER,
    ad_table_id INTEGER,
    record_id INTEGER,
    ad_language VARCHAR(6),
    idx_tsvector tsvector,
    UNIQUE (ad_client_id, ad_table_id, record_id, ad_language)
);

-- Materialized views for frequently used languages (optional optimization)
CREATE MATERIALIZED VIEW idx_product_ts_sk AS
SELECT * FROM idx_product_ts WHERE ad_language = 'sk_SK';

CREATE INDEX idx_product_ts_sk_search ON idx_product_ts_sk USING GIN(idx_tsvector);

-- Refresh on index update
REFRESH MATERIALIZED VIEW CONCURRENTLY idx_product_ts_sk;
```

**Pros:**
- ✅ **Best of both worlds:** Flexibility of Option 1 + performance of Option 2
- ✅ **Optional optimization:** Only create views for hot languages
- ✅ **Concurrent refresh:** Can refresh without locking queries

**Cons:**
- ⚠️ **Refresh complexity:** Must trigger REFRESH on index updates
- ⚠️ **Storage overhead:** 2× storage (base table + materialized views)
- ⚠️ **Eventual consistency:** Views may be slightly stale

**Cost/Effort:** High (2-3 weeks)

**Verdict:** 🟡 **FUTURE OPTIMIZATION** - Implement only if Option 1 has performance issues

---

### Option 4: Language-Specific Columns in Single Row

**Description:** Store all language variants in a single row with JSONB or array columns.

**Architecture:**

```sql
CREATE TABLE idx_product_ts (
    ad_client_id INTEGER,
    ad_table_id INTEGER,
    record_id INTEGER,
    idx_tsvectors JSONB,  -- {"sk_SK": tsvector, "en_US": tsvector, ...}
    UNIQUE (ad_client_id, ad_table_id, record_id)
);

-- GIN index on JSONB path for specific language
CREATE INDEX idx_product_ts_search_sk ON idx_product_ts USING GIN ((idx_tsvectors->'sk_SK'));
```

**Pros:**
- ✅ **Single row per record:** Simplifies updates
- ✅ **Flexible schema:** Easy to add new languages

**Cons:**
- ❌ **PostgreSQL limitation:** Cannot store tsvector in JSONB (only text)
- ❌ **Index complexity:** JSONB GIN indexes don't work well with tsvector semantics
- ❌ **Performance degradation:** Significantly slower than native tsvector indexes
- ❌ **Complex queries:** Must extract JSONB values for search

**Cost/Effort:** High (experimental, may not work well)

**Verdict:** ❌ **REJECTED** - PostgreSQL doesn't support tsvector in JSONB efficiently

## Decision

We will implement **Option 1: Single Index with AD_Language Column** because it provides the best balance of:
- **Flexibility:** Easy to add/remove languages
- **Performance:** Native tsvector with partial GIN indexes
- **Maintainability:** Simple schema, clear data model
- **REST API Integration:** Can respect `Accept-Language` header
- **User Experience:** Each user searches in their preferred language

### Configuration Strategy

**MSysConfig:**
```
SEARCHINDEX_LANGUAGES = sk_SK,en_US,de_DE
```

**Or per-index configuration** (NEW column in AD_SearchIndex):
```sql
ALTER TABLE AD_SearchIndex ADD COLUMN Languages VARCHAR(255);
-- Example: 'sk_SK,en_US' for e-commerce site serving Slovakia + USA
```

### Migration Strategy

**Step 1: Alter Index Tables** (1 day)
```sql
-- Add ad_language column with default
ALTER TABLE idx_product_ts ADD COLUMN ad_language VARCHAR(6) DEFAULT 'sk_SK';

-- Update existing records with client's default language
UPDATE idx_product_ts SET ad_language = (SELECT AD_Language FROM AD_Client WHERE AD_Client_ID = idx_product_ts.ad_client_id);

-- Make NOT NULL after population
ALTER TABLE idx_product_ts ALTER COLUMN ad_language SET NOT NULL;

-- Drop old unique constraint
ALTER TABLE idx_product_ts DROP CONSTRAINT idx_product_ts_unique;

-- Add new unique constraint with ad_language
ALTER TABLE idx_product_ts ADD CONSTRAINT idx_product_ts_unique
  UNIQUE (ad_client_id, ad_table_id, record_id, ad_language);

-- Create partial indexes for each language
CREATE INDEX idx_product_ts_search_sk ON idx_product_ts USING GIN(idx_tsvector) WHERE ad_language = 'sk_SK';
CREATE INDEX idx_product_ts_search_en ON idx_product_ts USING GIN(idx_tsvector) WHERE ad_language = 'en_US';
```

**Step 2: Index Additional Languages** (1 day)
```java
// Run CreateSearchIndex process with new MSysConfig.SEARCHINDEX_LANGUAGES
// This will populate index with all configured languages
```

**Step 3: Code Changes** (2-3 days)
- Implement `getUserLanguage()` method
- Implement `getIndexLanguages()` method
- Implement `getTSConfigForLanguage()` method
- Update `createIndex()` to loop over languages
- Update `updateIndex()` to handle language-specific updates
- Update `getSearchResults()` to filter by user's language
- Update REST API integration to read `Accept-Language` header

**Step 4: Testing** (2-3 days)
- Test multi-language indexing
- Test REST API locale handling
- Test Web UI user language preferences
- Test backward compatibility (single language)
- Performance testing

## Consequences

### Positive

- ✅ **Multi-language search:** Users can search in their preferred language
- ✅ **REST API locale support:** Respects `Accept-Language` header
- ✅ **User language preferences:** Respects `#AD_Language` context
- ✅ **E-commerce ready:** Supports multi-market deployments
- ✅ **Flexible configuration:** Per-index or global language configuration
- ✅ **Backward compatible:** Single-language deployments still work
- ✅ **Performance:** Partial GIN indexes optimize language-specific searches
- ✅ **Maintainability:** Simple schema, clear semantics

### Negative

- ⚠️ **Storage increase:** 3× storage for 3 languages (acceptable for most deployments)
- ⚠️ **Index maintenance:** Must update N rows (one per language) on record change
- ⚠️ **Migration effort:** Requires ALTER TABLE + data migration
- ⚠️ **Configuration:** Must configure which languages to index

### Neutral

- ⚡ **New MSysConfig:** `SEARCHINDEX_LANGUAGES` (default: client's language only)
- ⚡ **Optional per-index config:** `AD_SearchIndex.Languages` column
- ⚡ **REST API integration:** Read locale from context properties

## Implementation

### Phase 1: Database Migration (1 day)

**Task 1.1:** Create migration script
- ALTER TABLE for all idx_* tables
- Add ad_language column
- Update UNIQUE constraint
- Create partial GIN indexes

**Task 1.2:** Test on development database
- Verify migration completes without errors
- Verify existing data is preserved
- Verify indexes are created

### Phase 2: Code Changes (2-3 days)

**Task 2.1:** Implement helper methods
- `getUserLanguage(Properties ctx)`
- `getIndexLanguages(Properties ctx, String trxName)`
- `getTSConfigForLanguage(String adLanguage, String trxName)`

**Task 2.2:** Update PGTextSearchIndexProvider
- Modify `createIndex()` for multi-language support
- Modify `updateIndex()` for language-specific updates
- Modify `getSearchResults()` to filter by user's language

**Task 2.3:** Update REST API integration
- Read `Accept-Language` header
- Set `#Locale` context property
- cloudempiere-rest: DefaultQueryConverter.java, ProductAttributeQueryConverter.java

### Phase 3: Configuration (1 day)

**Task 3.1:** Add MSysConfig
- `SEARCHINDEX_LANGUAGES` = "sk_SK,en_US,de_DE"

**Task 3.2:** (Optional) Add AD_SearchIndex.Languages column
- For per-index language configuration

### Phase 4: Testing (2-3 days)

**Task 4.1:** Unit tests
- Test getUserLanguage() with different contexts
- Test getIndexLanguages() configuration
- Test getTSConfigForLanguage() mapping

**Task 4.2:** Integration tests
- Test indexing in multiple languages
- Test search results filtered by language
- Test REST API locale handling
- Test Web UI language preferences

**Task 4.3:** Performance tests
- Compare single-language vs multi-language performance
- Verify partial indexes are used (EXPLAIN ANALYZE)

### Phase 5: Documentation (1 day)

**Task 5.1:** Update CLAUDE.md
- Document multi-language architecture
- Document configuration options
- Document migration procedure

**Task 5.2:** Update FEATURES.md
- Add multi-language search feature

**Task 5.3:** User guide
- How to configure languages
- How REST API uses locale
- How Web UI uses user language

### Timeline

- **Phase 1 (Database):** Day 1
- **Phase 2 (Code):** Days 2-4
- **Phase 3 (Config):** Day 5
- **Phase 4 (Testing):** Days 6-8
- **Phase 5 (Docs):** Day 9

**Total:** 2 weeks to production

## Related

- **Implements:** User language preferences for search
- **Enhances:** [ADR-003: Slovak Text Search Configuration](./ADR-003-slovak-text-search-configuration.md) - Multi-language extends Slovak support
- **Integrates with:** cloudempiere-rest REST API locale handling
- **Related to:** [ADR-004: REST API OData Integration](./ADR-004-rest-api-odata-integration.md) - Uses locale from REST context

## References

### Code Locations

- `PGTextSearchIndexProvider.java:579-613` - getTSConfig() method (needs modification)
- `PGTextSearchIndexProvider.java:91-145` - createIndex() method (needs multi-language loop)
- `PGTextSearchIndexProvider.java:214-283` - getSearchResults() method (needs language filter)
- cloudempiere-rest: `DefaultQueryConverter.java:684-711` - REST API integration
- cloudempiere-rest: `ProductAttributeQueryConverter.java:502-506` - Product search integration

### PostgreSQL Documentation

- [PostgreSQL Text Search](https://www.postgresql.org/docs/current/textsearch.html)
- [Partial Indexes](https://www.postgresql.org/docs/current/indexes-partial.html)
- [Text Search Configurations](https://www.postgresql.org/docs/current/textsearch-configuration.html)

### iDempiere Documentation

- [Language Support](https://wiki.idempiere.org/en/Multi-Language_Support)
- [Context Variables](https://wiki.idempiere.org/en/Context_Variables)

---

**Last Updated:** 2025-12-18
**Review Date:** 2026-06-18 (6 months from decision date)
