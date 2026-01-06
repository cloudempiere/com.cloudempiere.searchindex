# com.cloudempiere.searchindex Features

## Version History

| Version | Date | Key Features |
|---------|------|--------------|
| 10.2.0 | 2026-01 | Current development (iDempiere 10 alignment) |
| 10.1.0 | 2025-12 | Lazy initialization fix (CLD-1677), TS_RANK migration |
| 10.0.0 | 2025-12 | P2 repository structure, Tycho build |
| 8.2 | 2020-12-26 | Full-text search with PostgreSQL, event-driven indexing, multi-weight columns |
| Initial | 2016-08-10 | First prototype omnisearch plugin |

## Current Version: 10.2.0-SNAPSHOT

**Last Updated:** 2026-01-06
**Modules:** 2 OSGi bundles (core + UI fragment)
**Java Classes:** 35 (30 core + 5 UI)

---

## Feature Matrix

### Core Search Functionality

| Feature | Status | Description | Notes |
|---------|--------|-------------|-------|
| **PostgreSQL Full-Text Search** | ✅ Implemented | Native tsvector/tsquery search | Primary provider |
| **Elasticsearch Provider** | ⚠️ Stub Only | Integration stub | Not production-ready |
| **Multi-Weight Indexing** | ✅ Implemented | A=75%, B=50%, C=25%, D=0% | PostgreSQL weights |
| **Advanced Query Syntax** | ✅ Implemented | &, \|, !, <-> operators | PostgreSQL tsquery |
| **Slovak Language Support** | ✅ Implemented | Diacritics handling (č, š, ž, á) | POSITION search type |
| **Headline Generation** | ✅ Implemented | Result highlighting/snippets | PostgreSQL ts_headline |

### Search Ranking

| Feature | Status | Description | Performance |
|---------|--------|-------------|-------------|
| **TS_RANK** | ✅ Implemented | PostgreSQL native ranking | Fast (GIN index) |
| **POSITION** | ⚠️ Implemented | Regex-based distance ranking | Slow (100× slower) |
| **Weighted Ranking** | ✅ Implemented | Multi-weight column scoring | TS_RANK mode |
| **Diacritics-Aware** | ✅ Implemented | Slovak character normalization | POSITION mode |

### Index Management

| Feature | Status | Description | Notes |
|---------|--------|-------------|-------|
| **Event-Driven Updates** | ✅ Implemented | Auto-update on PO events | Requires SysConfig |
| **Incremental Indexing** | ✅ Implemented | Update only changed records | Event handler |
| **Bulk Re-indexing** | ✅ Implemented | Full index recreation | CreateSearchIndex process |
| **FK Table Support** | ✅ Implemented | 1-level FK relationships | Cascade updates |
| **Configuration Caching** | ✅ Implemented | ImmutableIntPOCache | 20-entry cache |
| **Multi-Tenant Support** | ⚠️ Partial | Index isolation | Known bug: unique constraint |
| **Automated Table DDL** | 📋 Proposed | Auto-create PostgreSQL tables | ADR-010 (not implemented) |

### User Interface

| Feature | Status | Description | Module |
|---------|--------|-------------|--------|
| **Search UI Component** | ✅ Implemented | ZkSearchIndexUI | UI fragment |
| **Dashboard Panel** | ✅ Implemented | DPSearchIndexPanel | UI fragment |
| **Advanced Search Toggle** | ✅ Implemented | Basic vs. advanced queries | ZK UI |
| **Custom Result Rendering** | ✅ Implemented | ISearchResultRenderer interface | Extensible |
| **Index Selection** | ✅ Implemented | Combobox for multiple indexes | ZK UI |

### Configuration & Admin

| Feature | Status | Description | Tables |
|---------|--------|-------------|--------|
| **Provider Configuration** | ✅ Implemented | AD_SearchIndexProvider | Multiple providers |
| **Index Definition** | ✅ Implemented | AD_SearchIndex | Groups tables |
| **Table Configuration** | ✅ Implemented | AD_SearchIndexTable | WHERE clauses |
| **Column Mapping** | ✅ Implemented | AD_SearchIndexColumn | FK support |
| **Role-Based Access** | ✅ Implemented | AD_Role_ID filtering | Security |

### Security & Safety

| Feature | Status | Description | Reference |
|---------|--------|-------------|-----------|
| **SQL Injection Prevention** | ✅ Implemented | Query sanitization | ADR-002 |
| **Input Sanitization** | ✅ Implemented | Special character escaping | CLD-1535 |
| **Transaction Isolation** | ✅ Implemented | Proper TX handling | ADR-001 |
| **Multi-Tenant Integrity** | ⚠️ Partial | Client/org filtering | ADR-006 (bug exists) |

---

## Known Issues & Limitations

### Critical Performance Issues

| Issue | Impact | Workaround | Reference |
|-------|--------|------------|-----------|
| **POSITION Search Type Performance** | 100× slower than TS_RANK | Use TS_RANK search type | docs/postgres-fts-performance-recap.md |
| **Hardcoded POSITION in REST API** | REST search endpoints slow | Modify DefaultQueryConverter.java | docs/rest-api-searchindex-integration.md |
| **Slovak Language Config Missing** | Falls back to regex workaround | Create sk_unaccent config | docs/slovak-language-architecture.md |

### Data Integrity Issues

| Issue | Impact | Status | Reference |
|-------|--------|--------|-----------|
| **Multi-Client Index Corruption** | Client B overwrites Client A index | Open | ADR-006 |
| **WHERE Clause Alias Bug** | Cannot use "main" alias | Open | SearchIndexEventHandler:268 |
| **Cache Invalidation** | Stale config after changes | Open | SearchIndexConfigBuilder:256 |

### Operational Issues

| Issue | Impact | Status | Reference |
|-------|--------|--------|-----------|
| **Manual Table Creation** | 15-60 min overhead per index, 20-30% error rate | Open | ADR-010 |
| **Inconsistent Table Schema** | Missing ad_client_id in UNIQUE constraint | Open | ADR-006, ADR-010 |
| **No Schema Validation** | Wrong DDL deployed to production | Open | ADR-010 |

### Feature Gaps

| Feature | Status | Notes |
|---------|--------|-------|
| **Multi-level FK Traversal** | Not Implemented | Only 1-level FK supported |
| **Elasticsearch Production Support** | Not Implemented | Stub only |
| **Search Type UI Toggle** | ✅ Implemented | Backend UI uses TS_RANK (REST API still POSITION) |

---

## Roadmap

### Proposed Features (See ADRs)

| Feature | Status | Priority | Estimated Savings | Reference |
|---------|--------|----------|------------------|-----------|
| **Automated Table DDL** | Proposed | High | 900-3600× faster setup | ADR-010 |
| **Slovak Text Search Config** | Proposed | High | €36K vs Algolia | ADR-003 |
| **SearchType Migration (UI)** | ✅ Done | Critical | 100× performance | ADR-005 |
| **SearchType Migration (REST)** | Open | Critical | 100× performance | ADR-004 |
| **Multi-Tenant Fix** | Proposed | Critical | Data integrity | ADR-006 |

See [docs/guides/roadmap/next-steps.md](docs/guides/roadmap/next-steps.md) for:
- Slovak text search configuration
- Migration from POSITION to TS_RANK
- Multi-tenant unique constraint fix
- Automated table creation mechanism
- Performance optimization strategies

---

## Architecture Documentation

| Document | Description |
|----------|-------------|
| `CLAUDE.md` | Claude Code guidance, build commands |
| `docs/complete-analysis-summary.md` | Executive summary of all findings |
| `docs/guides/slovak-language/architecture.md` | Root cause analysis |
| `docs/adr/adr-001-transaction-isolation.md` | Transaction handling patterns |
| `docs/adr/adr-002-sql-injection-prevention.md` | Security patterns |
| `docs/adr/adr-005-searchtype-migration.md` | Performance optimization plan |
| `docs/adr/adr-006-multi-tenant-integrity.md` | Multi-tenancy fixes |
| `docs/adr/adr-010-automated-search-index-table-ddl.md` | Automated table creation |

---

**Note:** This feature matrix was last updated on 2026-01-06. Please update as features evolve.
