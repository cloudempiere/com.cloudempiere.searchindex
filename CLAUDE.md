# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Cloudempiere Search Index plugin for iDempiere ERP. It provides configurable full-text search capabilities with support for multiple search providers (PostgreSQL text search, Elasticsearch). The plugin consists of two Eclipse/OSGi bundles that integrate with the iDempiere platform.

## Build System

This project uses Maven with Tycho for building Eclipse/OSGi plugins:

- **Parent POM**: Located at `../../iDempiereCLDE/org.idempiere.parent/pom.xml`
- **Build command**: `mvn clean install` (run from parent directory or individual plugin directories)
- **Java version**: JavaSE-11 (as specified in MANIFEST.MF)

## Module Structure

### com.cloudempiere.searchindex (Core Plugin)

The core backend plugin containing all search indexing logic:

**Package Structure:**
- `indexprovider/` - Search provider implementations (PostgreSQL, Elasticsearch)
- `model/` - Database model classes for search index configuration tables
- `process/` - iDempiere processes (e.g., CreateSearchIndex)
- `event/` - Event handlers that listen to database changes and update indexes
- `util/` - Utilities including SearchIndexConfigBuilder, SearchIndexUtils

**Key Architecture:**
- `ISearchIndexProvider` - Interface for pluggable search providers
- `SearchIndexEventHandler` - OSGi component that listens to PO events (create/update/delete) and triggers index updates
- `SearchIndexConfigBuilder` - Builds search index configuration from AD_SearchIndex* tables with caching
- Model classes (MSearchIndex, MSearchIndexTable, MSearchIndexColumn) represent the configuration tables

**OSGi Configuration:**
- Service components defined in `OSGI-INF/*.xml`
- Bundle activator: `com.cloudempiere.searchindex.Activator`
- Exports packages for use by other plugins (indexprovider, model, util)

### com.cloudempiere.searchindex.ui (UI Plugin)

**Important**: This is an OSGi fragment (Fragment-Host: org.adempiere.ui.zk), not a standalone bundle. Fragments attach to a host bundle and share its classloader.

ZK-based user interface components:

**Key Classes:**
- `ZkSearchIndexUI` - Main search interface component
- `SearchIndexItemRenderer` - Renders search results in the UI
- `DPSearchIndexPanel` - Dashboard panel for search
- `ISearchResultRenderer` - Interface for custom result rendering

**UI Components:**
- Combobox for search index selection
- Advanced search toggle
- Results listbox with custom rendering
- "No data" and "no index" state widgets

**Fragment Configuration:**
- Bundle-ClassPath includes `zul/` directory for ZK resources
- Requires `com.cloudempiere.searchindex` bundle (the core plugin)
- Exports `com.cloudempiere.searchindex.ui.dashboard` package

## Configuration Tables

The search index is configured through iDempiere Application Dictionary tables:

- **AD_SearchIndexProvider** - Defines which search technology to use (PostgreSQL, Elasticsearch)
- **AD_SearchIndex** - Groups related tables under a single searchable index
- **AD_SearchIndexTable** - Defines which tables to index, with optional WHERE clauses
- **AD_SearchIndexColumn** - Specifies columns to include in the index, supports FK relationships

**Important**: When defining SearchIndexTable where clauses, use standard PostgreSQL syntax WITHOUT the WHERE keyword. The table being indexed must be aliased as "main", while related tables use their actual table names as aliases.

## Search Provider Architecture

The plugin supports multiple search backends through `ISearchIndexProvider`:

**PostgreSQL Text Search** (`PGTextSearchIndexProvider`):
- Uses PostgreSQL's built-in full-text search (tsvector/tsquery)
- Supports ts_rank and position-based ranking (SearchType.TS_RANK, SearchType.POSITION)
- Headline generation for result highlighting
- Weighted columns using PostgreSQL weights (A=75%, B=50%, C=25%, D=0%)
- Text search configuration retrieved via getTSConfig() for language-specific searching
- Supports advanced query operators: & (AND), | (OR), ! (NOT), <-> (FOLLOWED BY)

**Elasticsearch** (`ElasticSearchIndexProvider`):
- Integration stub for Elasticsearch (implementation may be incomplete)

**Provider Methods:**
- `createIndex()` - Initial index creation
- `updateIndex()` - Incremental updates
- `deleteIndex()` - Remove documents from index
- `reCreateIndex()` - Drop and recreate entire index
- `getSearchResults()` - Execute search queries
- `setHeadline()` - Generate highlighted snippets

## Event-Driven Index Updates

The `SearchIndexEventHandler` OSGi component subscribes to iDempiere events to automatically update search indexes:

**Event Registration:**
- `IEventTopics.PO_AFTER_NEW` - Triggers index updates when new records are created
- `IEventTopics.PO_AFTER_CHANGE` - Triggers updates when records are modified
- `IEventTopics.PO_AFTER_DELETE` - Triggers index deletion when records are removed

**Smart Update Logic:**
- Only updates indexes if changed columns are actually indexed
- Handles IsActive changes specially (triggers full index update)
- Supports FK table updates (when a referenced table changes, parent index is updated)
- Self-updates when SearchIndex configuration tables change (AD_SearchIndex, AD_SearchIndexTable, AD_SearchIndexColumn)

**OSGi Component Configuration:**
The handler uses `@Component` annotation with `@Reference` for IEventManager:
- `ReferencePolicy.STATIC` - Event manager binding is static
- `ReferenceCardinality.MANDATORY` - Event manager is required for handler to work
- Registered in `OSGI-INF/com.cloudempiere.searchindex.pgtextsearch.SearchIndexEventHandler.xml`

**Important**: Event-driven updates are controlled by `MSysConfig.ALLOW_SEARCH_INDEX_EVENT` - must be enabled in System Configurator for automatic indexing to work.

## iDempiere Model Class Pattern

This plugin follows standard iDempiere model class conventions:

- **X_ classes** (e.g., `X_AD_SearchIndex`) - Auto-generated from Application Dictionary, extend PO
- **M classes** (e.g., `MSearchIndex`) - Custom business logic, extend X_ classes, implement `ImmutablePOSupport`
- **I_ interfaces** - Auto-generated interfaces for each table

**Caching Pattern:**
All M classes use `ImmutableIntPOCache` for performance:
```java
private static ImmutableIntPOCache<Integer,MSearchIndex> s_cache =
    new ImmutableIntPOCache<Integer,MSearchIndex>(Table_Name, 20);
```
This provides efficient caching with a cache size of 20 entries per model.

## Factory Pattern (iDempiere OSGi)

**IProcessFactory** (`SearchIndexProcessFactory`):
- Registers processes with OSGi using `@Component` annotation
- Uses `service.ranking:Integer=100` property for priority
- Returns ProcessCall instances for registered class names
- Registered in `OSGI-INF/com.cloudempiere.searchindex.process.SearchIndexProcessFactory.xml`

**SearchIndexProviderFactory**:
- Non-OSGi factory for creating ISearchIndexProvider instances
- Maps classnames to provider implementations (PGTextSearchIndexProvider, ElasticSearchIndexProvider)

## System Configuration

The plugin uses **MSysConfig** for runtime configuration:
- `ALLOW_SEARCH_INDEX_EVENT` - Controls whether event-driven index updates are enabled (default: false)
- Check this SysConfig before deploying to production to enable/disable automatic indexing

## Development Notes

- This is an OSGi plugin - use declarative services (OSGI-INF/\*.xml) for component registration
- Extends iDempiere's base classes (SvrProcess for processes, AbstractEventHandler for events)
- Uses iDempiere's PO (Persistent Object) model layer for database access
- ZK framework is used for all UI components
- Search index configuration is cached via CCache for performance
- Role-based access control filters search results by AD_Role_ID
- Bundle must be marked as singleton in MANIFEST.MF (`Bundle-SymbolicName: ...; singleton:=true`)
- OSGi Service-Component header points to `OSGI-INF/*.xml` for declarative services

## Performance Considerations

### 🔴 CRITICAL: Slovak Language Root Cause

**See Complete Documentation**:
- `docs/complete-analysis-summary.md` - Executive summary of all findings
- `docs/guides/slovak-language/architecture.md` - Root cause deep dive
- `docs/guides/roadmap/next-steps.md` - Implementation roadmap
- `docs/guides/slovak-language/use-cases.md` - Real-world scenarios & best practices
- `docs/guides/integration/rest-api.md` - REST API analysis
- `docs/guides/performance/technology-comparison.md` - Technology comparison (PostgreSQL vs Elasticsearch vs Algolia)

**TL;DR**: POSITION search type was created to handle **Slovak language diacritics** (č, š, ž, á, etc.) because PostgreSQL lacked proper Slovak text search configuration. This workaround uses regex on tsvector text, causing 100× performance degradation.

**Real Solution**: Create Slovak-specific text search configuration with multi-weight indexing

**Quick Win**: Change SearchType.POSITION to TS_RANK (3 files) → 100× faster immediately!

```sql
CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);
ALTER TEXT SEARCH CONFIGURATION sk_unaccent
  ALTER MAPPING FOR word, asciiword, hword, hword_asciipart, asciihword
  WITH pg_catalog.unaccent, simple;
```

**Impact**: Eliminates regex, maintains Slovak language quality, 100× faster.

### ⚠️ CRITICAL: Search Type Performance

The plugin supports two ranking algorithms with **drastically different performance characteristics**:

**SearchType.TS_RANK** (RECOMMENDED for production):
- Uses PostgreSQL's native `ts_rank()` function
- Fully utilizes GIN index on `idx_tsvector`
- Performance: O(log n) - scales to millions of rows
- Located: `PGTextSearchIndexProvider.java:657-669`

**SearchType.POSITION** (NOT RECOMMENDED for production):
- ⚠️ **Severe Performance Issue**: Uses regex operations on `idx_tsvector::text`
- Bypasses GIN index by casting tsvector to text (lines 692, 697, 705-711)
- Executes 6 regex operations per search term per row
- Performance: O(n × 6t) where n=rows, t=terms - **does not scale**
- Only acceptable for datasets <1,000 rows
- Located: `PGTextSearchIndexProvider.java:670-715`

**Performance Impact Example**:
- Dataset: 10,000 rows, 3 search terms
- TS_RANK: <100ms (uses index)
- POSITION: >5000ms (full table scan + 180,000 regex operations)

**Migration Path**:
1. Use `SearchType.TS_RANK` for all production indexes
2. For position-aware ranking, consider migrating to `ts_rank_cd()` with weight arrays
3. ✅ Backend UI now uses TS_RANK (`ZkSearchIndexUI.java:190`) - fixed per ADR-005

See `docs/guides/performance/postgres-fts.md` for detailed analysis and optimization strategies.

### Known Issues and Bugs

**1. Manual Table Creation Required** (`PGTextSearchIndexProvider.java:115`):
- **CRITICAL**: Search index tables must be created manually by DBA
- Creating new AD_SearchIndex record fails until DDL executed
- **Impact**: 15-60 minutes overhead per index, high error rate
- **Solution**: [ADR-010: Automated Table DDL Management](docs/adr/adr-010-automated-search-index-table-ddl.md)
- **Estimated Savings**: 900-3600× faster setup time

**2. WHERE Clause Alias Bug** (`SearchIndexEventHandler.java:268, 305`):
- Cannot use "main" alias in AD_SearchIndexTable WHERE clauses
- Causes "missing FROM-clause entry for table main" error
- **Workaround**: Use actual table name or no prefix in WHERE clauses

**3. Multi-Client Index Uniqueness** (`PGTextSearchIndexProvider.java:115`):
- UNIQUE constraint is `(ad_table_id, record_id)` - missing `ad_client_id`
- Record updated by Client B overwrites Client A's index entry
- **Impact**: Multi-tenant index corruption possible
- **Solution**: [ADR-006: Multi-Tenant Integrity](docs/adr/adr-006-multi-tenant-integrity.md)

**4. Cache Invalidation** (`SearchIndexConfigBuilder.java:39, 256`):
- Configuration cache is never explicitly cleared
- Stale entries persist after AD_SearchIndex modifications
- **Workaround**: Restart OSGi bundle or trigger event handler re-initialization

**5. FK Traversal Depth** (`SearchIndexConfigBuilder.java:288-312`):
- Only supports 1-level FK relationships
- Multi-level FK chains not fully implemented

**6. Unaccent Dictionary Schema Dependency** (Migration scripts):
- The `unaccent` extension may be installed in different schemas (`pg_catalog` vs `adempiere`)
- Unqualified dictionary references fail silently when schema doesn't match search_path
- **Symptom**: "text-search query contains only stop words or doesn't contain lexemes, ignored"
- **Solution**: Use schema-qualified dictionary: `WITH pg_catalog.unaccent, simple`
- **Fixed in**: Migration scripts now use `pg_catalog.unaccent` explicitly
- **Impact**: Prevents silent failures on fresh PostgreSQL installations

## REST API Integration

The search index module is integrated with the **cloudempiere-rest** repository (https://github.com/cloudempiere/cloudempiere-rest, cloudempiere-development branch) through OData filter functions.

**See**: `docs/guides/integration/rest-api.md` for comprehensive analysis

### Integration Architecture

**Repository**: `/Users/norbertbede/github/cloudempiere-rest`
**Branch**: cloudempiere-development

The REST API provides search functionality via OData `$filter` syntax:

```
GET /api/v1/models/m_product?$filter=searchindex('product_idx', 'ruža')&$orderby=searchindexrank desc
```

### Key Integration Points

**Files**:
- `com.trekglobal.idempiere.rest.api/src/com/trekglobal/idempiere/rest/api/json/filter/DefaultQueryConverter.java`
  - Lines 580-593: SearchIndex special method handling
  - Lines 684-690: ⚠️ **CRITICAL** - Hardcoded SearchType.POSITION
  - Lines 692-711: Convert search results to SQL VALUES JOIN

- `org.cloudempiere.rest.api.json.filter.ProductAttributeQueryConverter.java`
  - Lines 502-506: ⚠️ **CRITICAL** - Also hardcoded SearchType.POSITION

**Dependencies** (MANIFEST.MF):
```
Import-Package: com.cloudempiere.searchindex.indexprovider,
 com.cloudempiere.searchindex.model,
 com.cloudempiere.searchindex.util,
```

### ⚠️ CRITICAL REST API Issue

**Both query converters hardcode SearchType.POSITION**, which means:
- REST API search endpoints suffer from the **same 100× performance degradation** as the backend UI
- Every REST API search request uses the problematic POSITION search type
- No way to configure search type from REST API (hardcoded in implementation)

**Example**:
```java
// DefaultQueryConverter.java:689
return provider.getSearchResults(ctx, searchIndex.getSearchIndexName(),
                                  query, isAdvanced, SearchType.POSITION, null);  // ← PROBLEM!
```

### REST API Performance Impact

| Dataset | Current (POSITION) | Expected (TS_RANK) | Improvement |
|---------|-------------------|-------------------|-------------|
| 1,000 products | 500ms | 5ms | **100×** |
| 10,000 products | 5s | 50ms | **100×** |
| 100,000 products | 50s (timeout) | 100ms | **500×** |

### Quick Fix for REST API

Change hardcoded SearchType in both files:

**DefaultQueryConverter.java:689**:
```java
// Change from:
SearchType.POSITION
// To:
SearchType.TS_RANK
```

**ProductAttributeQueryConverter.java:505**:
```java
// Change from:
SearchType.POSITION
// To:
SearchType.TS_RANK
```

**Impact**: 100× faster REST API search immediately

## Common Tasks

**Running processes**: Execute `CreateSearchIndex` process from iDempiere UI with parameters:
- `AD_SearchIndexProvider_ID` (mandatory)
- `AD_SearchIndex_ID` (optional, omit to rebuild all indexes for the provider)

**TS_RANK search status**:
- ✅ Backend UI already uses `SearchType.TS_RANK` (`ZkSearchIndexUI.java:190`)
- ⚠️ REST API still hardcodes `SearchType.POSITION` (see ADR-004, fix in cloudempiere-rest repo)

**Debugging event handlers**: Check if `SearchIndexEventHandler` is properly registered via OSGi console

**Testing search**: Use the search UI component (`ZkSearchIndexUI`) or dashboard panel

## Dependencies

Core dependencies from iDempiere:
- `org.adempiere.base` (bundle-version="10.0.0")
- `org.adempiere.plugin.utils`
- `org.eclipse.osgi.services`

The plugin expects to find the parent iDempiere platform at `../../iDempiereCLDE/`

## Troubleshooting

**Index not updating automatically:**
1. Check `MSysConfig.ALLOW_SEARCH_INDEX_EVENT` is set to `true` (default is false)
2. Verify SearchIndexEventHandler is registered: Check OSGi console with `ss | grep searchindex`
3. Check event topics are registered for your tables in SearchIndexEventHandler.initialize()

**Search returns no results:**
1. Verify index is populated: Use `isIndexPopulated()` method
2. Check if user has role access to the records (role-based filtering is active)
3. Verify PostgreSQL text search configuration is correct for your language
4. For advanced search, ensure query syntax is valid (use &, |, !, <->)

**Process fails during index creation:**
1. Check SearchIndexConfigBuilder cache - may need clearing after configuration changes
2. Verify WHERE clauses in AD_SearchIndexTable use correct syntax (no WHERE keyword, "main" alias)
3. Check database logs for PostgreSQL errors during index creation
4. Verify SearchIndexProviderFactory can instantiate the provider class

**Performance issues:**
1. Check ImmutableIntPOCache size (default 20) - may need tuning for large datasets
2. Verify SearchIndexConfigBuilder cache is working (check cache hits/misses)
3. Consider PostgreSQL index optimization for tsvector columns
4. Review ts_rank vs position-based ranking for performance

**OSGi bundle not starting:**
1. Verify MANIFEST.MF has correct Bundle-RequiredExecutionEnvironment (JavaSE-11)
2. Check Service-Component header points to OSGI-INF/*.xml
3. Ensure singleton:=true is set in Bundle-SymbolicName
4. Verify all required bundles are available and started

---

## Development Standards

This project follows **CloudEmpiere workspace governance** standards.

### Commit Convention

- **Standard:** [Conventional Commits](https://conventionalcommits.org/)
- **Reference:** `../cloudempiere-workspace/docs/CONVENTIONAL-COMMITS.md` (if available)
- **Format:** `type(scope): description`
  - **type:** feat, fix, docs, chore, refactor, test, perf, ci, build
  - **scope:** Component affected (e.g., PGTextSearch, SearchIndexEventHandler)
  - **description:** Concise summary in present tense

**Examples:**
```
fix(PGTextSearch): Improve search input sanitization, CLD-1535
feat(SearchIndex): Add support for Slovak language diacritics
docs: Add CHANGELOG.md and governance templates
chore: Standardize repository with CloudEmpiere governance
```

### Changelog Maintenance

- **Format:** [Keep a Changelog](https://keepachangelog.com/)
- **Location:** `CHANGELOG.md`
- **Update on:** Every significant change (features, fixes, breaking changes)
- **Categories:** Added, Changed, Deprecated, Removed, Fixed, Security

### Feature Documentation

- **Location:** `FEATURES.md`
- **Update when:** Adding new features or marking features as deprecated
- **Include:** Feature status, description, known issues, performance characteristics

### Architecture Decision Records (ADRs)

- **Location:** `docs/adr/`
- **Template:** `docs/adr/000-template.md`
- **Numbering:** Sequential (ADR-001, ADR-002, etc.)
- **Create when:** Making significant architectural decisions
- **Use slash command:** `/create-adr` for guided ADR creation
- **Validate with:** `/validate-adr` to check completeness

**Existing ADRs:**
- ADR-001: Transaction Isolation
- ADR-002: SQL Injection Prevention
- ADR-003: Slovak Text Search Configuration
- ADR-004: REST API OData Integration
- ADR-005: SearchType Migration (POSITION → TS_RANK)
- ADR-006: Multi-Tenant Integrity
- ADR-007: Search Technology Selection
- ADR-008: Search Service Layer
- ADR-009: Multilanguage Search Index
- ADR-010: Automated Search Index Table DDL Management
- ADR-011: Lazy Initialization for SearchIndexEventHandler

### Project Governance

- **Strategic Review Template:** `docs/implementation-plan/strategic-review-template.md`
- **Roadmap:** `docs/implementation-plan/roadmap-2025.md`
- **Commands:** `.claude/commands/` - Symlinked to workspace
- **Agents:** `.claude/agents/` - Symlinked to workspace

### Quality Standards

- **Code Review:** Use `.claude/agents/quality/` agents for automated reviews
- **Testing:** Write tests for critical business logic
- **Documentation:** Keep CLAUDE.md, README.md, and inline docs up to date
- **Performance:** Benchmark critical paths (search queries, index updates)

### Continuous Improvement

Use these workspace commands:
- `/project-status` - Generate project health report
- `/known-issues` - Document known bugs and limitations
- `/recap` - Session recap and decisions made

---

## Quick Reference

### Build Commands

```bash
# Build core plugin
cd com.cloudempiere.searchindex
mvn clean package -DskipTests

# Build all modules
mvn clean install -DskipTests

# Build p2 repository
mvn clean install -pl com.cloudempiere.searchindex.extensions.p2
```

### Development Workflow

1. **Create feature branch:** `git checkout -b feat/CLD-XXXX-description`
2. **Write code** following iDempiere patterns
3. **Update documentation:** CHANGELOG.md, FEATURES.md, ADRs if needed
4. **Commit with conventional format:** `feat(scope): description, CLD-XXXX`
5. **Create PR** with Linear issue reference
6. **Review** using quality agents

### Key Files

| File | Purpose |
|------|---------|
| `CHANGELOG.md` | Complete change history |
| `FEATURES.md` | Feature matrix and status |
| `CLAUDE.md` | This file - Claude Code guidance |
| `README.md` | User-facing documentation |
| `docs/adr/` | Architecture decisions |
| `docs/implementation-plan/roadmap-2025.md` | Implementation roadmap |
| `docs/guides/` | Technical guides by topic |

---

**Last Updated:** 2026-01-06
**Governance Version:** CloudEmpiere Workspace v1.0
