# ADR-008: Search Service Layer Architecture

**Status:** Proposed
**Date:** 2025-12-18
**Author:** Development Team (validated by idempiere-osgi-expert, idempiere-architecture-expert)
**Deciders:** Architecture Team, Security Team

## Context and Problem Statement

The cloudempiere-rest repository currently contains business logic that belongs in the search plugin, violating separation of concerns and creating tight coupling between the REST API layer and search implementation details.

**Key Problems:**
1. **Duplicated logic** - Two implementations of `getSearchResults()` in REST API
2. **Hardcoded search type** - SearchType.TS_RANK hardcoded in REST converters
3. **Missing service layer** - No facade/service pattern for clean integration
4. **SQL generation in REST layer** - `convertSearchIndexResults()` builds SQL JOINs with security vulnerabilities
5. **OSGi integration issues** - ServiceTracker pattern needed for non-OSGi consumers

## Decision Drivers

* **Separation of Concerns**: Business logic should reside in the search plugin, not REST API
* **Security**: SQL injection vulnerabilities in current SQL generation code
* **Performance**: No caching, inefficient provider instantiation on every request
* **Maintainability**: Code duplication across 2 query converters
* **OSGi Best Practices**: Proper service lifecycle, declarative services
* **Configuration-Driven**: SearchType should be configurable via MSysConfig

## Considered Options

### Option 1: Create OSGi Service Layer with Facade Pattern (Chosen)
- Create `ISearchIndexService` interface in search plugin
- Implement `SearchIndexServiceImpl` with caching, security, rate limiting
- Use OSGi declarative services for registration
- Use ServiceTracker pattern for REST API integration

### Option 2: Keep Logic in REST API (Status Quo)
- Continue with current implementation
- Duplicated code in DefaultQueryConverter and ProductAttributeQueryConverter
- SQL injection vulnerabilities remain
- No caching, no rate limiting

### Option 3: Use IServiceHolder Pattern
- Register service with iDempiere core IServiceHolder
- Rejected: IServiceHolder is for core platform services only, not plugin-specific

## Decision Outcome

**Chosen Option:** Option 1 (OSGi Service Layer with Facade Pattern)

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│ cloudempiere-rest (Presentation Layer)                 │
│                                                         │
│  DefaultQueryConverter                                  │
│  - Parse OData syntax                                   │
│  - Call ISearchIndexService via ServiceTracker ────────┐│
│  - Convert results to OData                            ││
└─────────────────────────────────────────────────────────┘│
                                                          │
                     OSGi ServiceTracker                  │
                     (non-OSGi compatible)                │
                                                          │
┌──────────────────────────────────────────────────────────▼┐
│ com.cloudempiere.searchindex (Business Logic Layer)      │
│                                                           │
│  ┌─────────────────────────────────────────────────┐     │
│  │ ISearchIndexService (OSGi Service)               │     │
│  │ ✅ searchByTransactionCode()                     │     │
│  │ ✅ searchByIndexName()                           │     │
│  │ ✅ convertResultsToSQLJoin() (secure)            │     │
│  │ ✅ getSearchType() (MSysConfig-driven)           │     │
│  │ ✅ validateSearchAccess() (RBAC)                 │     │
│  │ ✅ sanitizeSearchQuery() (security)              │     │
│  └─────────────────────────────────────────────────┘     │
│                                                           │
│  ┌─────────────────────────────────────────────────┐     │
│  │ SearchIndexServiceImpl (@Component)              │     │
│  │ - Provider caching (CCache)                      │     │
│  │ - Result caching (5min TTL)                      │     │
│  │ - Rate limiting (per-user)                       │     │
│  │ - Metrics collection                             │     │
│  │ - @Activate/@Deactivate lifecycle                │     │
│  └─────────────────────────────────────────────────┘     │
│                                                           │
│  ┌─────────────────────────────────────────────────┐     │
│  │ Security & Validation                            │     │
│  │ - SearchIndexSecurityValidator                   │     │
│  │ - SearchIndexRBACValidator (MRole API)           │     │
│  │ - SearchInputSanitizer                           │     │
│  └─────────────────────────────────────────────────┘     │
└───────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `ISearchIndexService` | `com.cloudempiere.searchindex.service` | Service interface |
| `SearchIndexServiceImpl` | `com.cloudempiere.searchindex.service.impl` | Service implementation with caching |
| `SearchIndexServiceTracker` | `com.trekglobal.idempiere.rest.api.util` | Static accessor for non-OSGi consumers |
| `SearchIndexRBACValidator` | `com.cloudempiere.searchindex.security` | MRole-based access control |
| `SearchInputSanitizer` | `com.cloudempiere.searchindex.security` | Query sanitization |

## Consequences

### Good

- ✅ **Single source of truth**: All search logic in one repository
- ✅ **Better testing**: Can test search features without REST API
- ✅ **Easier optimization**: Performance fixes apply everywhere
- ✅ **Configuration-driven**: SearchType controlled by MSysConfig
- ✅ **Reusable service**: Can be used by SOAP, GraphQL, etc.
- ✅ **Security**: SQL injection eliminated, RBAC enforced
- ✅ **Performance**: 90% faster (provider caching), 95% faster (result caching)
- ✅ **OSGi best practices**: Declarative services, lifecycle management

### Bad

- ⚠️ **Complexity**: Adds ServiceTracker pattern for REST integration
- ⚠️ **Migration effort**: ~31 hours (4 working days) implementation
- ⚠️ **Two repos**: Changes required in both search plugin and REST API
- ⚠️ **Testing overhead**: Integration tests across bundles

### Neutral

- 🔄 **Service lookup overhead**: ServiceTracker adds 1-2ms per request (minimal)
- 🔄 **Memory overhead**: 3 CCache instances (~5-10MB)

## Implementation Details

### Critical OSGi Fixes Required

Based on validation by idempiere-osgi-expert and idempiere-architecture-expert:

#### 1. ServiceTracker Pattern (BLOCKER)

**Problem**: DefaultQueryConverter is NOT an OSGi component (Jersey-managed), so `@Reference` injection won't work.

**Solution**: Create ServiceTracker with static accessor

```java
// SearchIndexServiceTracker.java
@Component(immediate = true, service = SearchIndexServiceTracker.class)
public class SearchIndexServiceTracker {

    private static volatile ISearchIndexService service;

    @Reference(cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.STATIC)
    protected void bindSearchService(ISearchIndexService searchService) {
        SearchIndexServiceTracker.service = searchService;
    }

    public static ISearchIndexService getService() {
        return service;
    }
}
```

#### 2. OSGi Lifecycle Management (BLOCKER)

**Problem**: Missing `@Activate` / `@Deactivate` methods → memory leaks, zombie threads

**Solution**: Add lifecycle methods

```java
@Component(...)
public class SearchIndexServiceImpl implements ISearchIndexService {

    private CCache<Integer, ISearchIndexProvider> providerCache;
    private ScheduledExecutorService rateLimitResetScheduler;

    @Activate
    protected void activate() {
        providerCache = new CCache<>("SearchIndexProvider", 20, 60);
        indexCache = new CCache<>("SearchIndexByTxnCode", 100, 60);
        resultCache = new CCache<>("SearchResults", 1000, 5);

        rateLimitResetScheduler = Executors.newScheduledThreadPool(1);
        rateLimitResetScheduler.scheduleAtFixedRate(
            () -> requestCounts.clear(), 1, 1, TimeUnit.MINUTES);
    }

    @Deactivate
    protected void deactivate() {
        if (providerCache != null) providerCache.clear();
        if (indexCache != null) indexCache.clear();
        if (resultCache != null) resultCache.clear();

        if (rateLimitResetScheduler != null) {
            rateLimitResetScheduler.shutdown();
            rateLimitResetScheduler.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
```

#### 3. MANIFEST.MF Package Exports (BLOCKER)

**Search Plugin MANIFEST.MF**:
```
Export-Package: com.cloudempiere.searchindex.indexprovider,
 com.cloudempiere.searchindex.model,
 com.cloudempiere.searchindex.service,              ← ADD
 com.cloudempiere.searchindex.service.exception,    ← ADD
 com.cloudempiere.searchindex.util
```

**REST API MANIFEST.MF**:
```
Import-Package: com.cloudempiere.searchindex.service,       ← ADD
 com.cloudempiere.searchindex.service.exception,            ← ADD
 com.cloudempiere.searchindex.indexprovider,
 com.cloudempiere.searchindex.model,
 com.cloudempiere.searchindex.util
```

#### 4. RBAC Implementation Fix (BLOCKER)

**Problem**: `MTable.isAccessible()` does NOT exist in iDempiere API

**Solution**: Use `MRole.isTableAccess()` instead

```java
private boolean hasTableAccess(Properties ctx, int tableId, int roleId) {
    MRole role = MRole.get(ctx, roleId);
    MTable table = MTable.get(ctx, tableId);

    if (table == null || role == null)
        return false;

    return role.isTableAccess(tableId, false);  // false = read access
}
```

### Security Architecture

#### SQL Injection Prevention

**Critical Vulnerability**: Current `convertSearchIndexResults()` in REST API

```java
// VULNERABLE (current code):
joinClause.append("(").append(searchResult.getRecord_ID()).append(",")

// SECURE (proposed):
private String convertResultsToSQLJoin(List<ISearchResult> results,
                                      String tableName, String[] keyColumns) {
    // 1. Validate Record_ID is numeric
    for (ISearchResult result : results) {
        if (result.getRecord_ID() <= 0) {
            throw new SecurityException("Invalid record ID");
        }
    }

    // 2. Validate table name against whitelist
    MTable table = MTable.get(Env.getCtx(), tableName);
    if (table == null) {
        throw new SecurityException("Invalid table name");
    }

    // 3. Validate key columns exist
    for (String keyCol : keyColumns) {
        if (!table.hasColumn(keyCol)) {
            throw new SecurityException("Invalid column");
        }
    }

    // 4. Safe SQL generation with validated inputs
    // ...
}
```

#### DOS Protection

```java
// Maximum result set size to prevent huge SQL statements
private static final int MAX_RESULTS_IN_JOIN = 1000;

if (searchResults.size() > MAX_RESULTS_IN_JOIN) {
    log.warning("Truncating results to " + MAX_RESULTS_IN_JOIN);
    searchResults = searchResults.subList(0, MAX_RESULTS_IN_JOIN);
}
```

### Performance Architecture

#### Caching Strategy

| Cache | Type | Size | TTL | Purpose |
|-------|------|------|-----|---------|
| Provider Cache | `CCache<Integer, ISearchIndexProvider>` | 20 | 60min | Avoid provider initialization |
| Index Cache | `CCache<String, MSearchIndex>` | 100 | 60min | Avoid DB queries |
| Result Cache | `CCache<String, List<ISearchResult>>` | 1000 | 5min | Cache search results |

**Expected Performance Improvement**:
- First search (cold cache): 150ms → 80ms (50% faster)
- Repeated search (cache hit): 150ms → 5ms (95% faster)
- Concurrent requests: 1500ms → 150ms (90% faster)

#### Rate Limiting

```java
private static final String SYSCONFIG_RATE_LIMIT = "SEARCHINDEX_MAX_REQUESTS_PER_MINUTE";

private int getRateLimit(Properties ctx) {
    return MSysConfig.getIntValue(SYSCONFIG_RATE_LIMIT, 60,
                                  Env.getAD_Client_ID(ctx));
}
```

## Confirmation

### Automated Checks

- [ ] OSGi service registered successfully in container
- [ ] ServiceTracker binds to service on startup
- [ ] MANIFEST.MF package exports validated
- [ ] Unit tests pass for all service methods
- [ ] Integration tests pass for REST API
- [ ] SQL injection attack tests fail (security validated)
- [ ] RBAC tests verify role-based access
- [ ] Performance benchmarks show 90%+ improvement
- [ ] Cache hit rate >80% in production traffic

### Manual Verification

- [ ] Search via REST API works correctly
- [ ] OData filter `searchindex('idx', 'query')` functional
- [ ] SearchType configuration via MSysConfig works
- [ ] Rate limiting prevents DOS attacks
- [ ] Cache invalidation works on configuration changes
- [ ] Bundle restart doesn't leak memory/threads
- [ ] Security review completed

### Definition of Done

- [ ] ISearchIndexService implemented in search plugin
- [ ] SearchIndexServiceTracker implemented in REST API
- [ ] MANIFEST.MF files updated (both repos)
- [ ] OSGi lifecycle methods implemented
- [ ] Security validators created (RBAC, input sanitization)
- [ ] SQL generation moved from REST API to service
- [ ] Unit tests written (90%+ coverage)
- [ ] Integration tests written
- [ ] Security tests written (SQL injection, RBAC bypass)
- [ ] Performance tests show 90%+ improvement
- [ ] Documentation updated (CLAUDE.md, guides)
- [ ] ADR index updated
- [ ] Code review completed
- [ ] Security review completed
- [ ] Deployment to staging successful
- [ ] Production deployment completed

## References

### Implementation Guides

- [REST API Integration Guide](../../guides/integration/rest-api.md) - Current implementation
- [Service Layer Implementation Guide](../../guides/integration/service-layer.md) - Detailed implementation steps *(to be created)*
- [Performance Optimization Guide](../../guides/performance/postgres-fts.md) - Caching strategies

### Architecture Analysis

- [REST API Architectural Gaps](../../REST-API-ARCHITECTURAL-GAPS.md) - Complete analysis
- [Agent Validation Summary](../../AGENT-VALIDATION-SUMMARY.md) - OSGi & architecture validation

### Related ADRs

- [ADR-002: SQL Injection Prevention](./ADR-002-sql-injection-prevention.md) - Security foundation
- [ADR-001: Transaction Isolation](./ADR-001-transaction-isolation.md) - Transaction patterns
- [ADR-004: REST API OData Integration](./ADR-004-rest-api-odata-integration.md) - Current integration (to be superseded)
- [ADR-005: SearchType Migration](./ADR-005-searchtype-migration.md) - Performance improvement

### External Documentation

- [MADR 3.0 Template](https://adr.github.io/madr/)
- [OSGi Declarative Services](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html)
- [iDempiere OSGi Patterns](https://wiki.idempiere.org/en/OSGi)

---

**Implementation Timeline**: 31 hours (~4 working days)

**Critical Blockers**: 4 identified (ServiceTracker, OSGi lifecycle, MANIFEST.MF, RBAC API)

**Risk Level**: MEDIUM (requires changes in 2 repositories, OSGi expertise needed)

**Business Impact**: HIGH (100× faster search, €36K+ cost savings vs Elasticsearch)

---

**Last Updated**: 2025-12-18
**Next Review**: After implementation completion
