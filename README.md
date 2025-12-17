# com.cloudempiere.searchindex

**Full-text search plugin for iDempiere ERP with PostgreSQL and Elasticsearch support**

[![Version](https://img.shields.io/badge/version-8.2+-blue.svg)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-GPL--2.0-green.svg)](LICENSE)
[![iDempiere](https://img.shields.io/badge/iDempiere-10+-orange.svg)](https://www.idempiere.org)

---

## Overview

Configurable full-text search for iDempiere ERP with PostgreSQL native text search, event-driven indexing, multi-weight columns, and role-based access control.

**Key Features:**
- PostgreSQL full-text search (tsvector/tsquery)
- Event-driven automatic index updates
- Multi-weight column indexing (A=75%, B=50%, C=25%, D=0%)
- Advanced query syntax (&, |, !, <->)
- Slovak/Czech diacritics support
- ZK UI components (search interface + dashboard)

**Use Cases:** Product catalog search, customer lookup, document search, multi-tenant search with client isolation.

---

## Quick Start

### Installation

**Prerequisites:** iDempiere 10+, Java 11+, PostgreSQL 9.6+, Maven 3.6+

```bash
# Build plugin
mvn clean install -DskipTests

# Deploy to iDempiere
cp com.cloudempiere.searchindex.extensions.p2/target/*.jar $IDEMPIERE_HOME/plugins/
```

### Enable Event-Driven Indexing

```sql
INSERT INTO AD_SysConfig (AD_SysConfig_ID, Name, Value, ConfigurationLevel)
VALUES (nextval('ad_sysconfig_seq'), 'ALLOW_SEARCH_INDEX_EVENT', 'Y', 'S');
```

### Create Search Index

1. **Window: Search Index Provider** → Create provider (PostgreSQL or Elasticsearch)
2. **Window: Search Index** → Create index, select provider
3. **Tab: Search Index Table** → Add tables to index
4. **Tab: Search Index Column** → Configure columns with weights
5. **Process: Create Search Index** → Build index

See [CLAUDE.md](CLAUDE.md) for detailed configuration guide.

---

## ⚠️ Critical Issues

| Issue | Severity | Impact | Resolution |
|-------|----------|--------|------------|
| **POSITION Search Type** | 🔴 Critical | 100× slower than TS_RANK | [ADR-005](docs/adr/ADR-005-searchtype-migration.md) |
| **Multi-Tenant Integrity** | 🔴 Critical | Cross-tenant data corruption | [ADR-006](docs/adr/ADR-006-multi-tenant-integrity.md) |
| **Cache Invalidation** | 🟡 Medium | Restart after config changes | Manual workaround |

**Performance Recommendation:** Use TS_RANK search type for production (not POSITION).

See [CHANGELOG.md](CHANGELOG.md) for complete issue tracking.

---

## Build & Development

```bash
# Build core plugin
cd com.cloudempiere.searchindex
mvn clean package -DskipTests

# Build all modules
mvn clean install -DskipTests

# Build p2 repository
mvn clean install -pl com.cloudempiere.searchindex.extensions.p2
```

**Development Environment:**
- iDempiere development setup ([guide](https://wiki.idempiere.org/en/Developer_Guide))
- Eclipse IDE with Maven + OSGi
- Parent POM: `../../iDempiereCLDE/org.idempiere.parent/pom.xml`

See [CLAUDE.md](CLAUDE.md) for complete developer guide and troubleshooting.

---

## Documentation

| Document | Description |
|----------|-------------|
| **[CHANGELOG.md](CHANGELOG.md)** | Complete change history (148 commits since 2016) |
| **[FEATURES.md](FEATURES.md)** | Feature matrix, known issues, roadmap |
| **[CLAUDE.md](CLAUDE.md)** | Developer guide, build commands, troubleshooting |
| **[docs/](docs/)** | Technical documentation and analysis |
| **[docs/adr/](docs/adr/)** | Architecture Decision Records |
| **[docs/STRATEGIC_REVIEW.md](docs/STRATEGIC_REVIEW.md)** | Strategic assessment |
| **[docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md)** | 6-month roadmap |

**Key Technical Docs:**
- [COMPLETE-ANALYSIS-SUMMARY.md](docs/COMPLETE-ANALYSIS-SUMMARY.md) - Executive summary
- [slovak-language-architecture.md](docs/slovak-language-architecture.md) - Root cause analysis
- [LOW-COST-SLOVAK-ECOMMERCE-SEARCH.md](docs/LOW-COST-SLOVAK-ECOMMERCE-SEARCH.md) - €36K cost savings guide
- [postgres-fts-performance-recap.md](docs/postgres-fts-performance-recap.md) - Performance analysis
- [rest-api-searchindex-integration.md](docs/rest-api-searchindex-integration.md) - REST API integration

---

## Module Structure

### com.cloudempiere.searchindex (Core Plugin)

Core search indexing functionality with PostgreSQL and Elasticsearch providers.

**Key Classes:**
- `PGTextSearchIndexProvider` - PostgreSQL text search implementation
- `SearchIndexEventHandler` - Event-driven index updates
- `SearchIndexConfigBuilder` - Configuration with caching
- `MSearchIndex`, `MSearchIndexTable`, `MSearchIndexColumn` - Model classes

### com.cloudempiere.searchindex.ui (UI Plugin)

ZK-based user interface components (OSGi fragment).

**Key Classes:**
- `ZkSearchIndexUI` - Main search interface
- `SearchIndexItemRenderer` - Result rendering
- `DPSearchIndexPanel` - Dashboard panel

---

## Contributing

Follow [CloudEmpiere governance](CLAUDE.md#development-standards) and [Conventional Commits](https://conventionalcommits.org/):

```
type(scope): description

feat(PGTextSearch): Add Slovak language support
fix(SearchIndex): Fix multi-tenant constraint, CLD-1234
docs: Update README
```

**Required Updates:**
- CHANGELOG.md (add entry under `[Unreleased]`)
- FEATURES.md (if changing features)
- ADR (for architectural decisions - use `/create-adr`)

See [CLAUDE.md](CLAUDE.md) for complete development workflow.

---

## Project Status

**Maturity:** Production (Since 2016) | **Active Maintenance:** Yes (2025) | **Commits:** 148 | **Version:** 8.2+

**Recent Activity:**
- 2025-10: Security improvements (CLD-1528, CLD-1535)
- 2025-09: Diacritics support (CLD-1487)
- 2025-02: Performance & caching (CLD-1206, CLD-2784)

---

## License

GPL-2.0 - See [LICENSE](LICENSE)

---

## Support

- **Documentation:** [docs/](docs/)
- **Issues:** GitHub Issues (if applicable)
- **iDempiere Community:** [Forums](https://www.idempiere.org/forums/)
- **Commercial Support:** CloudEmpiere team

---

**Configuration Tables:** AD_SearchIndexProvider, AD_SearchIndex, AD_SearchIndexTable, AD_SearchIndexColumn

**Search Providers:** PostgreSQL Text Search (production), Elasticsearch (stub)

**Performance:** <10ms average search (TS_RANK), <500ms index updates

---

**Last Updated:** 2025-12-13 | **Governance:** CloudEmpiere Workspace v1.0
