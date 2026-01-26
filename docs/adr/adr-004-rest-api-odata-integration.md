# ADR-004: REST API OData Integration Architecture

**Status:** ⚠️ Partially Superseded (see ADR-008)
**Date:** 2025-12-13
**Issue:** N/A
**Deciders:** Development Team, API Team
**Superseded By:** [ADR-008: Search Service Layer Architecture](./adr-008-search-service-layer.md) (architectural improvements)

## Context

Frontend applications (web, mobile) require full-text search capabilities accessible via REST API. The iDempiere platform provides a REST API module (cloudempiere-rest) that needs integration with the search index plugin.

### Background

**Requirements**:
- Mobile applications need product search via HTTP API
- Web applications need search without direct database access
- Integration must follow OData filter conventions
- Search results must include relevance ranking
- Must support advanced query syntax

**Options for Integration**:
1. Create separate REST search endpoints
2. Integrate via OData filter functions (chosen)
3. Use GraphQL queries
4. Create WebSocket-based search

### Technical Constraints

- REST API module uses OData filter syntax (`$filter`, `$orderby`)
- Must work with existing IQueryConverter architecture
- Integration must be OSGi-compatible (bundle dependencies)
- Search provider must remain pluggable (PostgreSQL, Elasticsearch)

## Decision Drivers

- **OData Standards Compliance:** Follow existing REST API patterns
- **Developer Experience:** Easy to use from frontend applications
- **Performance:** Sub-100ms response time for typical searches
- **Flexibility:** Support both simple and advanced search queries
- **Ranking:** Search results must be sortable by relevance
- **Maintainability:** Use existing search infrastructure

## Considered Options

### Option 1: Separate REST Endpoints

**Description:** Create dedicated `/api/v1/search/*` endpoints outside OData filter system.

**Pros:**
- Complete control over request/response format
- Can optimize for search-specific use cases
- Simple to implement

**Cons:**
- Breaks OData consistency
- Requires separate documentation
- Duplicate filtering logic
- Cannot combine with other OData filters

**Cost/Effort:** Medium

**Verdict:** ❌ **REJECTED** - Breaks OData conventions

### Option 2: OData Filter Function Integration (Chosen)

**Description:** Integrate search as OData special method `searchindex()` within `$filter` parameter.

**Architecture:**
```
OData Query:
  GET /api/v1/models/m_product?$filter=searchindex('idx', 'query')&$orderby=searchindexrank desc

Flow:
  1. OData Parser → IQueryConverter.getSearchResults()
  2. SearchIndex Plugin → PGTextSearchIndexProvider.getSearchResults()
  3. Convert Results → SQL VALUES JOIN
  4. Execute Final Query → Ranked Results
```

**Pros:**
- ✅ **Follows OData standards** (filter functions are standard)
- ✅ **Combines with other filters** (`$filter=searchindex(...) and IsActive eq true`)
- ✅ **Standard ordering** (`$orderby=searchindexrank desc`)
- ✅ **Reuses existing infrastructure** (ISearchIndexProvider)
- ✅ **Consistent API patterns** (same syntax as other special methods)
- ✅ **Pluggable providers** (works with any ISearchIndexProvider)

**Cons:**
- Requires OData parser extension
- SearchType configuration not exposed to API
- Limited to string parameters (no complex JSON)

**Cost/Effort:** Medium (requires IQueryConverter interface extension)

**Verdict:** ✅ **ACCEPTED** - Best balance of standards compliance and functionality

### Option 3: GraphQL Queries

**Description:** Create GraphQL schema for search queries.

**Pros:**
- Flexible query structure
- Rich type system
- Client-driven queries

**Cons:**
- Requires separate GraphQL infrastructure
- Not compatible with existing OData API
- Higher complexity for simple search
- Additional learning curve for developers

**Cost/Effort:** High

**Verdict:** ❌ **REJECTED** - Too complex for the use case, incompatible with OData

## Decision

We will implement **Option 2: OData Filter Function Integration** using `searchindex()` special method within the existing OData filter system.

### Rationale

**Standards Compliance:**
- OData specification supports filter functions (e.g., `contains()`, `startswith()`)
- `searchindex()` follows same pattern as other custom functions
- Maintains API consistency across all endpoints

**Developer Experience:**
```javascript
// Simple search
fetch('/api/v1/models/m_product?$filter=searchindex("product_idx", "ruža")')

// Combined with filters
fetch('/api/v1/models/m_product?$filter=searchindex("product_idx", "ruža") and IsActive eq true')

// Ordered by relevance
fetch('/api/v1/models/m_product?$filter=searchindex("product_idx", "ruža")&$orderby=searchindexrank desc')
```

**Why better than alternatives:**
- **vs Separate Endpoints:** Maintains OData consistency, can combine filters
- **vs GraphQL:** Lower complexity, works with existing infrastructure
- Uses existing IQueryConverter architecture (minimal changes)

## Consequences

### Positive

- ✅ **OData Standards Compliance** - Follows filter function conventions
- ✅ **API Consistency** - Same patterns as other special methods
- ✅ **Filter Composition** - Can combine search with other filters
- ✅ **Relevance Ranking** - Built-in `searchindexrank` for sorting
- ✅ **Pluggable Providers** - Works with any ISearchIndexProvider
- ✅ **Reuses Infrastructure** - No duplicate search logic
- ✅ **Easy to Use** - Simple filter syntax for frontend developers
- ✅ **OSGi Compatible** - Proper bundle dependencies via MANIFEST.MF

### Negative (⚠️ Addressed by ADR-008)

- ⚠️ **SearchType Hardcoded** - Currently hardcoded to POSITION (performance issue)
  - **→ ADR-008 Solution:** MSysConfig-driven SearchType configuration
- ⚠️ **No Configuration** - SearchType not configurable from API
  - **→ ADR-008 Solution:** ISearchIndexService with flexible configuration
- ⚠️ **Business Logic in REST Layer** - SQL generation and search logic in query converters
  - **→ ADR-008 Solution:** Service layer with proper separation of concerns
- ⚠️ **SQL Injection Risk** - `convertSearchIndexResults()` builds SQL without validation
  - **→ ADR-008 Solution:** Security validators and safe SQL generation
- ⚠️ **No Caching** - Provider instantiated on every request
  - **→ ADR-008 Solution:** 3-tier caching (provider, index, results)
- ⚠️ **Two Implementations** - DefaultQueryConverter and ProductAttributeQueryConverter both implement
  - **→ ADR-008 Solution:** Single service implementation reused by all converters
- ⚠️ **String Parameters Only** - Cannot pass complex configuration as JSON
- ⚠️ **OData Parser Dependency** - Requires OData filter parser extension

### Neutral

- Integration requires MANIFEST.MF dependency imports
- IQueryConverter interface extension needed
- VALUES JOIN pattern for result conversion
- Special column mapping for searchindexrank

## Implementation

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│ REST API OData Integration Architecture                             │
└─────────────────────────────────────────────────────────────────────┘

Client Request:
  GET /api/v1/models/m_product?$filter=searchindex('product_idx', 'ruža')
                                       &$orderby=searchindexrank desc

        │
        ▼
┌────────────────────────────────────────┐
│ OData Parser                           │
│ (ODataUtils.SEARCH_INDEX)              │
└────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────┐
│ IQueryConverter                        │
│ - DefaultQueryConverter                │
│ - ProductAttributeQueryConverter       │
└────────────────────────────────────────┘
        │
        │ Extract params: ('product_idx', 'ruža')
        ▼
┌────────────────────────────────────────┐
│ MSearchIndex.get(ctx, 'product_idx')   │
│ → Returns AD_SearchIndex               │
└────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────┐
│ SearchIndexUtils.getSearchIndexProvider│
│ → Returns ISearchIndexProvider         │
│   (PGTextSearchIndexProvider)          │
└────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────┐
│ provider.getSearchResults(             │
│   ctx, 'product_idx', 'ruža',          │
│   isAdvanced=true,                     │
│   SearchType.POSITION ← ISSUE!         │
│ )                                      │
└────────────────────────────────────────┘
        │
        │ Returns: List<ISearchResult>
        │   [ {record_id: 1001, rank: 0.5},
        │     {record_id: 1023, rank: 1.2},
        │     {record_id: 1045, rank: 2.5} ]
        ▼
┌────────────────────────────────────────┐
│ convertSearchIndexResults()            │
│                                        │
│ Converts to SQL VALUES JOIN:           │
│                                        │
│ JOIN (VALUES                           │
│   (1001, 0.5),                         │
│   (1023, 1.2),                         │
│   (1045, 2.5)                          │
│ ) as v(record_id, rank)                │
│ ON (M_Product.M_Product_ID =          │
│     v.record_id)                       │
└────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────┐
│ Final SQL Query:                       │
│                                        │
│ SELECT * FROM M_Product                │
│ JOIN (VALUES ...) as v(...)            │
│   ON (M_Product.M_Product_ID = ...)    │
│ WHERE true                             │
│ ORDER BY v.rank DESC                   │
└────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────┐
│ JSON Response:                         │
│ [                                      │
│   {M_Product_ID: 1045, rank: 2.5, ...},│
│   {M_Product_ID: 1023, rank: 1.2, ...},│
│   {M_Product_ID: 1001, rank: 0.5, ...} │
│ ]                                      │
└────────────────────────────────────────┘
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| **ODataUtils** | `ODataUtils.java:64` | Defines `SEARCH_INDEX` constant |
| **SEARCH_INDEX_PARAMS_PATTERN** | `IQueryConverter.java` | Regex to extract params from `searchindex('idx', 'query')` |
| **DefaultQueryConverter** | `DefaultQueryConverter.java:580-593` | Main implementation for search index filtering |
| **ProductAttributeQueryConverter** | `ProductAttributeQueryConverter.java:502-506` | Alternative implementation for product attributes |
| **convertSearchIndexResults** | `DefaultQueryConverter.java:692-711` | Converts search results to SQL VALUES JOIN |
| **SEARCHINDEXRANK** | `ODataUtils.java:151` | Special column for `$orderby` |

### Code Locations

**MANIFEST.MF Dependencies:**
```xml
<!-- File: com.trekglobal.idempiere.rest.api/META-INF/MANIFEST.MF:10-12 -->
Import-Package: com.cloudempiere.searchindex.indexprovider,
 com.cloudempiere.searchindex.model,
 com.cloudempiere.searchindex.util,
```

**OData Special Method Registration:**
```java
// File: ODataUtils.java:64
public static final String SEARCH_INDEX = "searchindex";

// File: ODataUtils.java:196-203
private static final List<String> SUPPORTED_SPECIAL_METHODS =
    Collections.unmodifiableList(Arrays.asList(
        PRODUCT_ATTRIBUTE,
        SEARCH_INDEX,  // ← Registered here
        ISDESCENDANT,
        ...
    ));
```

**Search Execution (DefaultQueryConverter):**
```java
// File: DefaultQueryConverter.java:684-690
@Override
public List<ISearchResult> getSearchResults(Properties ctx, String transactionCode,
                                             String query, boolean isAdvanced, String trxName) {
    MSearchIndex searchIndex = MSearchIndex.get(ctx, transactionCode, trxName);
    ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(ctx,
                                        searchIndex.getAD_SearchIndexProvider_ID(), null, trxName);
    return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
                                      query, isAdvanced, SearchType.POSITION, null);  // ← ISSUE!
}
```

## Critical Issues

### Issue #1: SearchType Hardcoded to POSITION

**Problem:**
Both `DefaultQueryConverter` and `ProductAttributeQueryConverter` hardcode `SearchType.POSITION`, which means **all REST API searches suffer from 100× performance degradation**.

**Impact:**
| Dataset | Current (POSITION) | Expected (TS_RANK) | Improvement Needed |
|---------|-------------------|-------------------|-------------------|
| 1,000 products | 500ms | 5ms | **100×** |
| 10,000 products | 5s | 50ms | **100×** |
| 100,000 products | 50s (timeout) | 100ms | **500×** |

**Root Cause:**
```java
// Line 689 (DefaultQueryConverter)
SearchType.POSITION  // ← Hardcoded!

// Line 505 (ProductAttributeQueryConverter)
SearchType.POSITION  // ← Also hardcoded!
```

**Fix Required:**
Change both occurrences to `SearchType.TS_RANK`:
```java
// BEFORE
return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
                                  query, isAdvanced, SearchType.POSITION, null);

// AFTER
return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
                                  query, isAdvanced, SearchType.TS_RANK, null);
```

**Files to Change:**
1. `cloudempiere-rest/com.trekglobal.idempiere.rest.api/src/com/trekglobal/idempiere/rest/api/json/filter/DefaultQueryConverter.java:689`
2. `cloudempiere-rest/com.trekglobal.idempiere.rest.api/src/org/cloudempiere/rest/api/json/filter/ProductAttributeQueryConverter.java:505`

## Related

- **Superseded by:** [ADR-008: Search Service Layer Architecture](./adr-008-search-service-layer.md) - Architectural improvements to address issues identified in this ADR
- **Related to:** [ADR-003: Slovak Text Search Configuration](./adr-003-slovak-text-search-configuration.md) - Fixes Slovak language support
- **Related to:** [ADR-005: SearchType Migration](./adr-005-searchtype-migration.md) - Addresses POSITION → TS_RANK migration
- **Related to:** [ADR-002: SQL Injection Prevention](./adr-002-sql-injection-prevention.md) - Query sanitization applies here too

## Migration Path to ADR-008

This ADR describes the **current implementation** (as of 2025-12-13). The following issues have been identified:

| Issue | Current State (ADR-004) | Future State (ADR-008) | Status |
|-------|------------------------|------------------------|--------|
| **SearchType** | Hardcoded to POSITION | MSysConfig-driven | Proposed |
| **Business Logic** | In REST query converters | In service layer | Proposed |
| **SQL Injection** | Risk in `convertSearchIndexResults()` | Security validators | Proposed |
| **Caching** | None | 3-tier caching | Proposed |
| **Code Duplication** | 2 implementations | Single service | Proposed |

**Recommendation:** Implement ADR-008 to resolve architectural gaps while maintaining OData integration pattern.

**Timeline:** 4 working days (31 hours) - see ADR-008 for details

## References

### Implementation Guides

- [REST API Integration Analysis](../rest-api-searchindex-integration.md) - Complete integration details
- [REST API Investigation Summary](../REST-API-INVESTIGATION-SUMMARY.md) - Quick reference

### Code Repository

- **Repository:** [cloudempiere/cloudempiere-rest](https://github.com/cloudempiere/cloudempiere-rest)
- **Branch:** cloudempiere-development
- **Local Path:** `/Users/norbertbede/github/cloudempiere-rest`

### External Documentation

- [OData Filter Functions](https://www.odata.org/getting-started/basic-tutorial/#queryData) - OData standard
- [PostgreSQL VALUES](https://www.postgresql.org/docs/current/queries-values.html) - VALUES clause documentation

---

**Last Updated:** 2025-12-18
**Review Date:** Upon ADR-008 implementation
**Migration Status:** Architectural gaps identified, ADR-008 proposed for resolution
