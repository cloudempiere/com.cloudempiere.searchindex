# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records (ADRs) for the com.cloudempiere.searchindex project.

## What is an ADR?

An Architecture Decision Record (ADR) captures an important architectural decision made along with its context and consequences. ADRs help teams:
- Understand the reasoning behind architectural choices
- Avoid revisiting already-made decisions
- Onboard new team members effectively
- Track the evolution of the architecture over time

## ADR Format

We use the [MADR 3.0 format](https://adr.github.io/madr/) (Markdown Architectural Decision Records). See [000-template.md](./000-template.md) for the standard template.

---

## ADR Index

### Core Search Architecture

| ADR | Title | Status | Date | Description |
|-----|-------|--------|------|-------------|
| [ADR-003](./adr-003-slovak-text-search-configuration.md) | Slovak Text Search Configuration Architecture | **Proposed** | 2025-12-13 | Implements Slovak language support using PostgreSQL text search configuration with multi-weight indexing to replace POSITION workaround |
| [ADR-005](./adr-005-searchtype-migration.md) | SearchType Migration from POSITION to TS_RANK | **Proposed** | 2025-12-12 | Migrates default SearchType from POSITION (regex-based, 100× slower) to TS_RANK (native PostgreSQL function) |
| [ADR-007](./adr-007-search-technology-selection.md) | Search Technology Selection | **Implemented** | 2025-12-13 | Chose PostgreSQL FTS over Elasticsearch/Algolia (€36,700 cost savings, adequate for 10K-1M products) |

### API & Integration

| ADR | Title | Status | Date | Description |
|-----|-------|--------|------|-------------|
| [ADR-004](./adr-004-rest-api-odata-integration.md) | REST API OData Integration Architecture | **⚠️ Partially Superseded** | 2025-12-13 | Integrates search index with REST API via OData `searchindex()` filter function (superseded by ADR-008) |
| [ADR-008](./adr-008-search-service-layer.md) | Search Service Layer Architecture | **Proposed** | 2025-12-18 | Service layer with caching, security, and OSGi best practices (supersedes ADR-004) |

### Security & Data Integrity

| ADR | Title | Status | Date | Description |
|-----|-------|--------|------|-------------|
| [ADR-001](./adr-001-transaction-isolation.md) | Transaction Isolation | **Implemented** | 2025-12-12 | Ensures proper transaction isolation for search index operations |
| [ADR-002](./adr-002-sql-injection-prevention.md) | SQL Injection Prevention | **Implemented** | 2025-12-12 | Prevents SQL injection in search queries through input sanitization |
| [ADR-006](./adr-006-multi-tenant-integrity.md) | Multi-Tenant Integrity | **Proposed** | 2025-12-12 | Fixes unique constraint to include ad_client_id for proper multi-tenant data isolation |

### Operations & Automation

| ADR | Title | Status | Date | Description |
|-----|-------|--------|------|-------------|
| [ADR-010](./adr-010-automated-search-index-table-ddl.md) | Automated Search Index Table DDL Management | **Proposed** | 2025-12-18 | Automates PostgreSQL table creation for new search indexes, eliminating manual DBA intervention (900-3600× faster setup) |

---

## ADR Status Definitions

- **Proposed:** Decision documented but not yet implemented
- **Accepted:** Decision approved and ready for implementation
- **Implemented:** Decision implemented and deployed to production
- **Deprecated:** Decision no longer valid, superseded by newer ADR
- **Superseded:** Replaced by a newer ADR (link provided)

---

## ADR Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│ ADR Dependency Graph                                             │
└─────────────────────────────────────────────────────────────────┘

ADR-007: Technology Selection (PostgreSQL FTS vs Elasticsearch)
    │
    ├─→ ADR-003: Slovak Text Search Configuration
    │       │
    │       └─→ ADR-005: SearchType Migration (POSITION → TS_RANK)
    │               │
    │               ├─→ ADR-004: REST API OData Integration (current)
    │               │       │
    │               │       └─→ ADR-008: Service Layer ← supersedes ADR-004
    │               │               │
    │               │               ├─→ ADR-001: Transaction Isolation
    │               │               └─→ ADR-002: SQL Injection Prevention
    │               │
    │               └─→ ADR-001: Transaction Isolation
    │
    ├─→ ADR-006: Multi-Tenant Integrity
    │       │
    │       └─→ ADR-010: Automated Table DDL ← ensures ADR-006 schema
    │
    └─→ ADR-010: Automated Table DDL Management

Legend:
─→ depends on / related to / evolves to
```

---

## Critical Path for Implementation

**Phase 1: Quick Performance Fix** (1 week)
1. **ADR-005:** Migrate SearchType.POSITION → TS_RANK (100× faster immediately)
2. **ADR-004:** Update REST API to use TS_RANK (fix both query converters)

**Phase 2: Slovak Language Support** (2 weeks)
3. **ADR-003:** Implement Slovak text search configuration
4. **ADR-005:** Complete migration with multi-weight indexing

**Phase 3: Data Integrity** (1 week)
5. **ADR-006:** Fix multi-tenant unique constraint

---

## Creating a New ADR

1. **Copy template:**
   ```bash
   cp docs/adr/000-template.md docs/adr/ADR-XXX-short-title.md
   ```

2. **Fill in all sections:**
   - Context: What problem are we solving?
   - Decision Drivers: What factors influence the decision?
   - Considered Options: What alternatives did we evaluate?
   - Decision: What did we choose and why?
   - Consequences: What are the positive/negative/neutral outcomes?

3. **Get review:**
   - Peer review from team
   - Architecture review from tech lead
   - Approval from stakeholders

4. **Update this index:**
   - Add new ADR to appropriate category table
   - Update ADR relationships diagram if needed

5. **Cross-reference:**
   - Link related ADRs
   - Update parent/child ADRs with new relationships

---

## Quick Reference

### By Topic

**Performance:**
- [ADR-003: Slovak Text Search Configuration](./adr-003-slovak-text-search-configuration.md)
- [ADR-005: SearchType Migration](./adr-005-searchtype-migration.md)
- [ADR-007: Search Technology Selection](./adr-007-search-technology-selection.md)

**Security:**
- [ADR-001: Transaction Isolation](./adr-001-transaction-isolation.md)
- [ADR-002: SQL Injection Prevention](./adr-002-sql-injection-prevention.md)
- [ADR-006: Multi-Tenant Integrity](./adr-006-multi-tenant-integrity.md)

**Integration:**
- [ADR-004: REST API OData Integration](./adr-004-rest-api-odata-integration.md) (⚠️ partially superseded by ADR-008)
- [ADR-008: Search Service Layer](./adr-008-search-service-layer.md) (supersedes ADR-004)

**Operations:**
- [ADR-010: Automated Table DDL Management](./adr-010-automated-search-index-table-ddl.md)

### By Status

**Implemented:**
- ADR-001, ADR-002, ADR-007
- ADR-004 (⚠️ partially superseded by ADR-008)

**Proposed (Ready for Implementation):**
- ADR-003, ADR-005, ADR-006, ADR-008, ADR-010

**Deprecated:**
- None

---

## Implementation Tracking

| ADR | Implementation Status | Blocker | Target Date |
|-----|----------------------|---------|-------------|
| **ADR-003** | Not Started | Requires database migration script | Q1 2025 |
| **ADR-005** | Partially | Waiting for ADR-003 Slovak config | Q1 2025 |
| **ADR-004** | ⚠️ Implemented with gaps | Superseded by ADR-008 | Q1 2025 |
| **ADR-006** | Not Started | Requires schema migration | Q2 2025 |
| **ADR-008** | Not Started | None (ready to implement) | Q1 2025 |
| **ADR-010** | Not Started | None (ready to implement) | Q1 2025 |

---

## Additional Resources

### Implementation Guides
- [Slovak Language Architecture Guide](../slovak-language-architecture.md)
- [Slovak Language Use Cases](../slovak-language-use-cases.md)
- [Next Steps Implementation](../NEXT-STEPS.md)
- [LOW-COST-SLOVAK-ECOMMERCE-SEARCH.md](../LOW-COST-SLOVAK-ECOMMERCE-SEARCH.md)

### External References
- [MADR 3.0 Template](https://adr.github.io/madr/)
- [PostgreSQL Full-Text Search](https://www.postgresql.org/docs/current/textsearch.html)
- [linuxos.sk Slovak FTS Article](https://linuxos.sk)

---

**Last Updated:** 2025-12-18
**Maintainer:** Development Team
