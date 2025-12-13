# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://conventionalcommits.org/).

---

## ⚠️ Critical Issues Requiring Attention

| Issue | Severity | Status | ADR Reference | Impact |
|-------|----------|--------|---------------|--------|
| **POSITION Search Performance** | 🔴 Critical | Open | [ADR-005](docs/adr/ADR-005-searchtype-migration.md), [ADR-003](docs/adr/ADR-003-slovak-text-search-configuration.md) | 100× slower than TS_RANK, 5s for 10K products |
| **Multi-Tenant Data Integrity** | 🔴 Critical | Open | [ADR-006](docs/adr/ADR-006-multi-tenant-integrity.md) | Cross-client record corruption risk |
| **REST API POSITION Hardcoded** | 🟡 Medium | Open | [ADR-004](docs/adr/ADR-004-rest-api-odata-integration.md) | REST API has same 100× performance issue |
| **Slovak Language Config Missing** | 🟡 Medium | Open | [ADR-003](docs/adr/ADR-003-slovak-text-search-configuration.md) | Missing €36,700 cost savings vs Elasticsearch |
| **Cache Invalidation** | 🟡 Medium | Open | N/A | Restart required after config changes |

**For comprehensive analysis and solutions, see:**
- **Architecture Decisions:** [docs/adr/README.md](docs/adr/README.md) - Complete ADR catalog with implementation roadmap
- **Root Cause Analysis:** [ADR-003: Slovak Text Search Configuration](docs/adr/ADR-003-slovak-text-search-configuration.md)
- **Performance Migration:** [ADR-005: SearchType Migration](docs/adr/ADR-005-searchtype-migration.md)
- **Cost Analysis:** [ADR-007: Search Technology Selection](docs/adr/ADR-007-search-technology-selection.md) (€36,700 savings)
- **Implementation Guide:** `docs/NEXT-STEPS.md` - 2-week implementation timeline

---

## [Unreleased]

### Added

#### Architecture Decision Records
- **ADR-003: Slovak Text Search Configuration Architecture** - Formalizes Slovak language support using PostgreSQL text search configuration with multi-weight indexing to replace POSITION workaround (see [docs/adr/ADR-003](docs/adr/ADR-003-slovak-text-search-configuration.md))
- **ADR-004: REST API OData Integration Architecture** - Documents REST API integration via OData `searchindex()` filter function (see [docs/adr/ADR-004](docs/adr/ADR-004-rest-api-odata-integration.md))
- **ADR-007: Search Technology Selection** - Captures decision to use PostgreSQL FTS over Elasticsearch/Algolia, saving €36,700 over 5 years (see [docs/adr/ADR-007](docs/adr/ADR-007-search-technology-selection.md))
- **ADR Index** - Created comprehensive ADR catalog with dependency graph and implementation tracking (see [docs/adr/README.md](docs/adr/README.md))

#### Documentation
- Complete documentation validation and reorganization
- Extracted architectural decisions from guide documents into formal ADRs
- Established clear relationships between all ADRs (dependency graph)
- Validated all code references in documentation (all paths accurate)

### Changed

#### Governance
- Standardized repository with CloudEmpiere governance standards
- Added CHANGELOG.md following [Keep a Changelog](https://keepachangelog.com/) format
- Added FEATURES.md with feature matrix and status tracking
- Created governance templates (ADR template, STRATEGIC_REVIEW.md, IMPLEMENTATION_PLAN.md)
- Enhanced CLAUDE.md with Development Standards section
- Enhanced README.md to governance-compliant format (concise, links to detailed docs)

#### Architecture Decision Records
- **ADR-005: SearchType Migration** - Enhanced with cross-references to ADR-003 (Slovak config root cause), ADR-004 (REST API impact), and ADR-007 (technology selection rationale)

#### Documentation Structure
- Documented critical performance issues with POSITION search type (see [ADR-005](docs/adr/ADR-005-searchtype-migration.md))
- Documented Slovak language root cause analysis (see [ADR-003](docs/adr/ADR-003-slovak-text-search-configuration.md))
- Documented REST API performance impact (see [ADR-004](docs/adr/ADR-004-rest-api-odata-integration.md))
- Documented cost analysis and technology comparison (see [ADR-007](docs/adr/ADR-007-search-technology-selection.md))

### Known Issues

#### Performance (Critical - See ADR-003, ADR-005)
- **POSITION Search Type:** 100× slower than TS_RANK due to regex operations bypassing GIN index
  - **Impact:** 5 seconds for 10,000 products (unusable at scale)
  - **Root Cause:** Workaround for Slovak diacritics without proper text search configuration (see [ADR-003](docs/adr/ADR-003-slovak-text-search-configuration.md))
  - **Solution:** Migrate to TS_RANK + Slovak text search config (see [ADR-005](docs/adr/ADR-005-searchtype-migration.md))
  - **Files Affected:**
    - `ZkSearchIndexUI.java:189` (backend UI)
    - `DefaultQueryConverter.java:689` (REST API)
    - `ProductAttributeQueryConverter.java:505` (REST API)

#### Data Integrity (Critical - See ADR-006)
- **Multi-Tenant Unique Constraint:** Missing `ad_client_id` in unique constraint
  - **Impact:** Records can be overwritten across clients (data corruption)
  - **Current Constraint:** `UNIQUE (ad_table_id, record_id)`
  - **Required Constraint:** `UNIQUE (ad_client_id, ad_table_id, record_id)`
  - **Solution:** See [ADR-006](docs/adr/ADR-006-multi-tenant-integrity.md)

#### Operations (Medium)
- **Cache Invalidation:** Configuration cache does not invalidate automatically
  - **Impact:** Restart required after AD_SearchIndex configuration changes
  - **Workaround:** Restart OSGi bundle or trigger event handler re-initialization

#### Cost Optimization (Medium - See ADR-007)
- **Slovak Language Configuration:** Not yet implemented, missing €36,700 cost savings
  - **Impact:** Using POSITION workaround instead of proper Slovak text search config
  - **Benefit:** 100× performance improvement + €36,700 savings vs Elasticsearch
  - **Solution:** See [ADR-003](docs/adr/ADR-003-slovak-text-search-configuration.md)

---

## [8.2] - 2020-12-26

### Added
- PostgreSQL full-text search provider
- Elasticsearch provider stub
- Event-driven index updates
- Multi-weight column indexing
- Role-based access control for search results

### Changed
- Reorganized plugin structure
- Improved search ranking algorithms

---

## Recent Changes (2025)

### [2025-10-07]

#### Fixed
- Improved and documented sanitizeQuery method (CLD-1528)
- Enhanced search input sanitization (CLD-1535)

### [2025-10-02]

#### Fixed
- Added record ID check in SearchIndexEventHandler (CLD-1527)

### [2025-09-10]

#### Fixed
- Set result rank by iteration index for fixed order (CLD-1487)

### [2025-09-05]

#### Fixed
- NPE when creating new business partner without isCustomer='Y' (CLD-1489)

### [2025-09-03]

#### Improved
- Search index rank to consider diacritics (CLD-1487)

### [2025-08-21]

#### Improved
- Ranking logic for POSITION search type (CLD-1206)

### [2025-08-20]

#### Improved
- Text search by normalizing document content

### [2025-06-16]

#### Fixed
- Search index Event Handler not firing when record activated (CLD-1326)

### [2025-05-07]

#### Fixed
- Unnecessary table call in SearchIndexEventHandler (CLD-1109)

### [2025-03-25]

#### Fixed
- Cross-tenant integrity issues (#3562)

### [2025-02-26]

#### Fixed
- Escape special characters in search term before applying regex (#3486)
- NPE for FK indexed tables blocking record saves (#2784)

### [2025-02-25]

#### Fixed
- Indexing newly created records (#2784)
- Sanitized search query for PostgreSQL text search provider (#3486)

### [2025-02-21]

#### Improved
- Rank by position with weighted ranking (#3456)

### [2025-02-13]

#### Improved
- Event Handler performance (#3417)

### [2025-02-12]

#### Improved
- Rank by position (distance from beginning) (#3417)

### [2025-02-11]

#### Fixed
- Support for system indexes (#2784)

### [2025-02-10]

#### Fixed
- Java model corrections (#2784)

### [2025-02-07]

#### Improved
- Added SysConfig for Event Handler (#2784)
- Return rank in search results (#2784)

### [2025-02-05]

#### Improved
- Added caching for configuration (#2784)
- Event handler performance, added IsValid to Search Index (#2784)

### [2025-02-03]

#### Improved
- Implemented unaccent, order by relevance (#2784)

### [2025-01-31]

#### Improved
- Support for changes in foreign tables (#2784)

---

## Historical Releases

### [2017-07-14]

#### Changed
- Set default index type if none selected

### [2017-02-25]

#### Changed
- Reorganized tsearch_pg implementation

### [2017-02-24]

#### Changed
- Reorganized as plugin de.bxservice.omnisearch

### [2017-02-14]

#### Fixed
- Indexing logic
- 2Pack configuration
- Dashboard registration

#### Changed
- Code cleanup

### [2017-01-13]

#### Improved
- Code quality and UI
- Added packout support

### [2016-08-30]

#### Added
- Renderer for extended info in result pages
- Logic for retrieving queries

### [2016-08-12]

#### Added
- Initial commit

### [2016-08-11]

#### Added
- Document and index creation in database
- Join clauses for FK columns
- Log messages
- Search config language detection
- Index type reading from process parameters

#### Improved
- Join clauses when same table chosen multiple times

### [2016-08-10]

#### Added
- First prototype of omnisearch plugin

---

**Note:** This CHANGELOG was generated from git history during repository standardization on 2025-12-13. Historical entries before 2025 may lack detail. Please update with additional context as needed.
