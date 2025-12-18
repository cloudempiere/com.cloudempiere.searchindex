# Integration Guides

This section contains guides for integrating the search index module with external systems (REST API, SOAP, etc.).

## Overview

The search index module provides OSGi-based service integration for external consumers. These guides cover REST API integration patterns and best practices.

## Guides

### 1. [REST API Integration](./rest-api.md)
**Comprehensive REST API OData Integration**

- Executive summary
- Integration architecture
- Dependencies (MANIFEST.MF imports)
- Integration points:
  - IQueryConverter interface
  - DefaultQueryConverter implementation
  - ProductAttributeQueryConverter implementation
  - OData special methods
- Usage examples (OData filter syntax)
- SQL generation flow
- **Critical Issue**: Hardcoded SearchType.POSITION
- Performance impact
- Recommended fixes
- Testing strategy

**Read this** for REST API integration.

### 2. [Service Layer Architecture](./service-layer.md)
**OSGi Service Layer for Clean Integration** *(Coming Soon - See ADR-008)*

- ISearchIndexService facade pattern
- OSGi declarative services
- ServiceTracker pattern for non-OSGi consumers
- Security & performance architecture
- RBAC validation
- Rate limiting & caching

**This guide will be created after ADR-008 is finalized.**

## Related ADRs

- [ADR-004: REST API OData Integration](../../adr/ADR-004-rest-api-odata-integration.md) - Current implementation
- [ADR-008: Search Service Layer Architecture](../../adr/ADR-008-search-service-layer.md) - Proposed architecture *(In Progress)*
- [ADR-002: SQL Injection Prevention](../../adr/ADR-002-sql-injection-prevention.md) - Security considerations
- [ADR-001: Transaction Isolation](../../adr/ADR-001-transaction-isolation.md) - Transaction management

## REST API Usage Examples

### Basic Search Filter
```
GET /api/v1/models/m_product?$filter=searchindex('product_idx', 'laptop')
```

### Search with Ranking
```
GET /api/v1/models/m_product?$filter=searchindex('product_idx', 'ruža')&$orderby=searchindexrank desc
```

### Complex Query
```
GET /api/v1/models/fv_product_wstore_v?
  $filter=(w_store_id eq 1000088 AND isactivestoreproduct eq true)
          AND searchindex('p','gerbera:*')
  &$orderby=searchindexrank asc
  &$select=m_product_id,name,slug,pricelist
  &$top=24
```

## Performance Impact

**Current (Hardcoded POSITION)**:
- 1,000 products: 500ms
- 10,000 products: 5s (timeout risk)

**After TS_RANK Fix**:
- 1,000 products: 5ms (100× faster)
- 10,000 products: 50ms (100× faster)

## Navigation

- [← Back to Guides](../)
- [→ Slovak Language Guides](../slovak-language/)
- [→ Performance Guides](../performance/)
