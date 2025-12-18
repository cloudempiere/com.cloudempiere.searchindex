# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://conventionalcommits.org/).

---

## ⚠️ Critical Issues Status

| Issue | Severity | Status | ADR Reference | Impact |
|-------|----------|--------|---------------|--------|
| **POSITION Search Performance (UI)** | 🔴 Critical | ✅ Fixed | [ADR-005](docs/adr/ADR-005-searchtype-migration.md) | Backend UI now uses TS_RANK (100× faster) |
| **Multi-Tenant Data Integrity** | 🔴 Critical | ✅ Fixed | [ADR-006](docs/adr/ADR-006-multi-tenant-integrity.md) | UNIQUE constraint now includes ad_client_id |
| **SQL Injection Vulnerabilities** | 🔴 Critical | ✅ Fixed | [ADR-002](docs/adr/ADR-002-sql-injection-prevention.md) | Defense-in-depth validation added |
| **Transaction Isolation** | 🔴 Critical | ✅ Fixed | [ADR-001](docs/adr/ADR-001-transaction-isolation.md) | Index ops use separate transactions |
| **Slovak Language Config** | 🟡 Medium | ✅ Fixed | [ADR-003](docs/adr/ADR-003-slovak-text-search-configuration.md) | sk_unaccent config support added |
| **REST API POSITION Hardcoded** | 🟡 Medium | ⚠️ Open | [ADR-004](docs/adr/ADR-004-rest-api-odata-integration.md) | Requires fix in cloudempiere-rest repo |
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

### Changed

### Fixed

---

## [migration-2025-12-18] - 2025-12-18

Migration scripts release for Slovak text search configuration and multi-tenant integrity fixes (CLD-1652).

### Added

#### Database Migration Scripts (CLD-1652)
- `postgresql/migration/202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql` - Comprehensive PostgreSQL migration
  - Creates Slovak text search configuration (sk_unaccent) for proper diacritics handling
  - Fixes multi-tenant UNIQUE constraint to include ad_client_id
  - Cleans up duplicate search index data
  - Includes rollback procedures
- `oracle/migration/202512180801_MultiTenant_Fix.sql` - Oracle-specific multi-tenant fix
  - Applies UNIQUE constraint fix for Oracle databases
  - Includes rollback procedures
- `MIGRATION_README.md` - Migration governance documentation
  - Timestamped naming convention (YYYYMMDDHHmm_Description.sql)
  - PostgreSQL and Oracle migration standards
  - Rollback procedure requirements

### Changed

#### Migration Script Governance
- Standardized migration script naming to use timestamps for ordering
- Organized scripts into database-specific directories (postgresql/, oracle/)
- Removed deprecated migration scripts with non-standard naming
- Enhanced migration documentation with rollback procedures

### Fixed

#### Security (ADR-002: SQL Injection Prevention)
- **CRITICAL:** Added SQL injection prevention with defense-in-depth strategy
  - Created `SearchIndexSecurityValidator` with three-layer protection:
    1. Input validation (dangerous patterns and safe characters)
    2. Whitelist verification (table/column names against AD_Table/AD_Column)
    3. Parameterized queries where possible
  - Fixed SQL injection vulnerabilities in:
    - `PGTextSearchIndexProvider.java`: WHERE clause concatenation (lines 148-151, 167-170)
    - `PGTextSearchIndexProvider.java`: Table name concatenation (lines 144, 163, 361)
    - `SearchIndexConfigBuilder.java`: WHERE clause concatenation (lines 320-326)
  - See [ADR-002](docs/adr/ADR-002-sql-injection-prevention.md) for complete analysis

#### Data Integrity (ADR-006: Multi-Tenant Integrity)
- **CRITICAL:** Fixed multi-tenant data corruption vulnerability
  - Updated UNIQUE constraint from `(ad_table_id, record_id)` to `(ad_client_id, ad_table_id, record_id)`
  - Fixed `PGTextSearchIndexProvider.java:116` ON CONFLICT clause
  - Migration scripts standardized in `postgresql/migration/202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql`
  - Prevents cross-client record overwrites
  - See [ADR-006](docs/adr/ADR-006-multi-tenant-integrity.md) for impact analysis

#### Transaction Isolation (ADR-001: Transaction Isolation Strategy)
- **CRITICAL:** Implemented separate transaction isolation for search index operations
  - Refactored `SearchIndexEventHandler` to use dedicated transactions
  - Removed instance variables `ctx` and `trxName` (thread safety issue)
  - Added `executeIndexUpdateWithSeparateTransaction()` helper method
  - Index failures no longer rollback business transactions
  - Improved performance by reducing lock contention
  - Updated helper methods to accept ctx/trxName as parameters:
    - `getMainPOs()`, `getMainPOsOfTable()`, `applyWhereClause()`, `handleSearchIndexConfigChange()`
  - See [ADR-001](docs/adr/ADR-001-transaction-isolation.md) for rationale

### Changed

#### Performance (ADR-005: SearchType Migration)
- **CRITICAL:** Changed default SearchType from POSITION to TS_RANK for 100× performance improvement
  - Updated `ZkSearchIndexUI.java:189` to use `SearchType.TS_RANK` (backend UI)
  - **Impact:** Search queries complete in <100ms instead of 5-10s for 10K records
  - **Breaking Change:** Result ranking may differ from POSITION (uses ts_rank instead of regex position)
  - **Action Required:** REST API still hardcoded to POSITION (cloudempiere-rest repository)
  - See [ADR-005](docs/adr/ADR-005-searchtype-migration.md) for migration guide

#### Language Support (ADR-003: Slovak/Czech Text Search Configuration)
- Added Slovak/Czech language diacritics support with proper text search configuration
  - Updated `getTSConfig()` method to detect Slovak (`sk_SK`) and Czech (`cs_CZ`) languages
  - Returns `sk_unaccent` configuration for proper diacritics handling
  - Migration scripts in `postgresql/migration/202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql`
  - Enables searches without diacritics to match text with diacritics
  - Example: searching "ruza" matches "ruža", "růža", "rúža"
  - See [ADR-003](docs/adr/ADR-003-slovak-text-search-configuration.md) for architecture

#### Security Infrastructure
- `SearchIndexSecurityValidator` utility class for SQL injection prevention
  - `validateWhereClause()` - Validates WHERE clause for dangerous patterns
  - `validateTableName()` - Validates table name against AD_Table whitelist
  - `validateColumnName()` - Validates column name against AD_Column whitelist

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
