# Slovak Language Full-Text Search Guides

This section contains comprehensive guides for implementing and understanding Slovak language support in the search index module.

## Overview

Slovak language support is a critical feature due to the unique diacritical marks (č, š, ž, á, etc.) that affect search ranking and relevance. These guides explain the root cause, solution architecture, and best practices.

## Guides

### 1. [Architecture](./architecture.md)
**Root Cause Analysis & Technical Solution**

- Historical context (original POC)
- Slovak language challenges (14 diacritical marks)
- Why POSITION search exists (workaround explanation)
- Performance comparison (100× degradation)
- Proper solution: Slovak text search configuration
- Implementation in PGTextSearchIndexProvider
- Migration path (5 phases)

**Read this first** to understand the technical foundation.

### 2. [Use Cases](./use-cases.md)
**Real-World Slovak Language Search Scenarios**

- Slovak language characteristics
- 6 comprehensive use cases:
  1. E-commerce product search
  2. Typeahead autocomplete
  3. Multi-word search with Slovak grammar
  4. Mixed Slovak/Czech products
  5. Common typos and misspellings
  6. REST API mobile app search
- Best practices summary
- Validation test suite

**Read this** to understand practical implementation requirements.

### 3. [Implementation](./implementation.md)
**Complete €36K Cost-Saving Implementation Guide**

- Executive summary
- PostgreSQL FTS vs Elasticsearch/Algolia comparison
- Slovak language-specific implementation
- Multi-weight indexing strategy
- Search quality improvements
- Performance optimizations
- E-commerce integration patterns
- Production deployment checklist

**Use this** for step-by-step implementation.

## Related ADRs

- [ADR-003: Slovak Text Search Configuration](../../adr/adr-003-slovak-text-search-configuration.md) - Architecture decision for Slovak support
- [ADR-005: SearchType Migration](../../adr/adr-005-searchtype-migration.md) - POSITION → TS_RANK migration
- [ADR-007: Search Technology Selection](../../adr/adr-007-search-technology-selection.md) - PostgreSQL FTS decision

## Quick Links

- **Problem**: Search is 100× slower than expected
- **Root Cause**: POSITION search uses regex on tsvector (bypasses GIN index)
- **Quick Fix**: Change SearchType.POSITION → TS_RANK (1 hour, documented in [ADR-005](../../adr/adr-005-searchtype-migration.md))
- **Proper Solution**: Implement Slovak text search configuration (2 weeks, documented in [ADR-003](../../adr/adr-003-slovak-text-search-configuration.md))

## Navigation

- [← Back to Guides](../)
- [→ Performance Guides](../performance/)
- [→ Integration Guides](../integration/)
