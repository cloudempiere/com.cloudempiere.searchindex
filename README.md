# com.cloudempiere.searchindex

**Full-text search plugin for iDempiere ERP with PostgreSQL and Elasticsearch support**

[![Version](https://img.shields.io/badge/version-10.2.0-blue.svg)](CHANGELOG.md)
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

## 🎯 Current Project Focus (2025)

The project is undergoing strategic improvements to address performance, architecture, and operational challenges:

### ✅ **Phase 1: Performance & Quality** (Q1 2025)
1. **SearchType Migration** ([ADR-005](docs/adr/adr-005-searchtype-migration.md)) - 100× faster search
2. **Slovak Language Support** ([ADR-003](docs/adr/adr-003-slovak-text-search-configuration.md)) - Proper diacritic handling
3. **Multi-Language Search** ([ADR-009](docs/adr/adr-009-multilanguage-search-index.md)) - Per-language indexing

### 🔄 **Phase 2: Architecture & Security** (Q1-Q2 2025)
4. **Service Layer** ([ADR-008](docs/adr/adr-008-search-service-layer.md)) - Proper separation of concerns
5. **Multi-Tenant Integrity** ([ADR-006](docs/adr/adr-006-multi-tenant-integrity.md)) - Fix data isolation
6. **Automated Table DDL** ([ADR-010](docs/adr/adr-010-automated-search-index-table-ddl.md)) - Zero-touch deployment

**See [docs/adr/](docs/adr/) for complete Architecture Decision Records**

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

### 📋 **Core Documentation**

| Document | Purpose | Audience |
|----------|---------|----------|
| **[CHANGELOG.md](CHANGELOG.md)** | Complete change history | All |
| **[FEATURES.md](FEATURES.md)** | Feature matrix, roadmap, known issues | Product, Developers |
| **[CLAUDE.md](CLAUDE.md)** | Developer guide for Claude Code | AI Agents, Developers |
| **[docs/adr/](docs/adr/)** | ⭐ Architecture Decision Records | Architects, Developers |

### 🎯 **Architecture Decision Records (ADRs)**

**Start here for understanding project direction:**

| Priority | ADR | Focus | Status |
|----------|-----|-------|--------|
| 🔴 Critical | [ADR-005](docs/adr/adr-005-searchtype-migration.md) | SearchType Migration (100× faster) | Proposed |
| 🔴 Critical | [ADR-006](docs/adr/adr-006-multi-tenant-integrity.md) | Multi-Tenant Data Integrity | Proposed |
| 🔴 Critical | [ADR-010](docs/adr/adr-010-automated-search-index-table-ddl.md) | Automated Table Creation | Proposed |
| 🟡 High | [ADR-003](docs/adr/adr-003-slovak-text-search-configuration.md) | Slovak Language Support | Proposed |
| 🟡 High | [ADR-008](docs/adr/adr-008-search-service-layer.md) | Service Layer Architecture | Proposed |
| 🟡 High | [ADR-009](docs/adr/adr-009-multilanguage-search-index.md) | Multi-Language Search | Proposed |

**See [docs/adr/README.md](docs/adr/README.md) for complete ADR index and implementation roadmap**

### 📊 **Technical Guides**

| Document | Purpose |
|----------|---------|
| [docs/README.md](docs/README.md) | Documentation hub with navigation |
| [docs/guides/performance/](docs/guides/performance/) | Performance optimization guides |
| [docs/guides/slovak-language/](docs/guides/slovak-language/) | Slovak language implementation |
| [docs/migration/](docs/migration/) | Database migration scripts |

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

## Project Status & Roadmap

**Maturity:** Production (Since 2016) | **Active Development:** Yes | **Version:** 10.2.0

### Recent Activity
- 2025-12: v10.1.0 release with lazy initialization fix (CLD-1677)
- 2025-10: Security improvements (CLD-1528, CLD-1535)
- 2025-09: Diacritics support, TS_RANK migration (CLD-1487)

See [CHANGELOG.md](CHANGELOG.md) for complete history and [docs/adr/](docs/adr/) for architecture decisions (11 ADRs documented).

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

**Last Updated:** 2026-01-06 | **Governance:** CloudEmpiere Workspace v1.0 | **ADRs:** 11 documented decisions
