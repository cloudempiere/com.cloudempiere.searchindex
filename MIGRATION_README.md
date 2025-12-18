# Search Index Migration Scripts

This document describes the standardized migration scripts for the CloudEmpiere Search Index plugin, following iDempiere migration conventions.

## Migration Script Locations

```
postgresql/migration/     - PostgreSQL-specific migration scripts
oracle/migration/         - Oracle-specific migration scripts
```

## Active Production Migration Scripts

### 202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql (PostgreSQL)

**Date**: 2025-12-18 08:01
**ADRs**: ADR-005, ADR-006
**Status**: ✅ Successfully applied in production

**What it does**:
1. Creates Slovak/Czech text search configuration (`sk_unaccent`)
   - Installs PostgreSQL `unaccent` extension
   - Creates text search configuration based on `simple` configuration
   - Configures word mappings to remove diacritics (č→c, š→s, ž→z, á→a, etc.)
   - Tests configuration with Slovak and Czech text samples

2. Fixes multi-tenant UNIQUE constraint
   - Scans all search index tables (`idx_*_ts`)
   - Removes duplicate entries (keeps most recent)
   - Drops old UNIQUE constraint: `(ad_table_id, record_id)`
   - Creates new UNIQUE constraint: `(ad_client_id, ad_table_id, record_id)`
   - Prevents cross-client data corruption

**Production Results**:
- Slovak text search configuration created successfully
- 0 duplicate records found (clean database)
- 5 tables migrated:
  - idx_product_ts
  - idx_bpartner_ts
  - idx_k_entry_ts
  - idx_productcategory_ts
  - idx_r_request_ts

**Prerequisites**:
- PostgreSQL with `unaccent` extension available
- Database user must have CREATE EXTENSION privileges

**Post-Migration Steps**:
1. Mark all Slovak/Czech search indexes as invalid:
   ```sql
   UPDATE AD_SearchIndex SET IsValid='N'
   WHERE AD_SearchIndexProvider_ID IN (
     SELECT AD_SearchIndexProvider_ID FROM AD_SearchIndexProvider
     WHERE ProviderClassName LIKE '%PGTextSearchIndexProvider'
   );
   ```
2. Re-run CreateSearchIndex process for affected indexes
3. Update `PGTextSearchIndexProvider.getTSConfig()` to return `"sk_unaccent"`

**Idempotency**: Safe to run multiple times. Checks for existing configuration and constraints before creating.

---

### 202512180801_MultiTenant_Fix.sql (Oracle)

**Date**: 2025-12-18 08:01
**ADR**: ADR-006
**Status**: ⚠️ Not tested in production (no Oracle deployment)

**What it does**:
1. Fixes multi-tenant UNIQUE constraint
   - Scans all search index tables (`searchindex_%`)
   - Removes duplicate entries (keeps most recent)
   - Drops old UNIQUE constraint: `(AD_TABLE_ID, RECORD_ID)`
   - Creates new UNIQUE constraint: `(AD_CLIENT_ID, AD_TABLE_ID, RECORD_ID)`
   - Prevents cross-client data corruption

**Note**: Slovak text search configuration is PostgreSQL-specific and not applicable to Oracle. Oracle uses Oracle Text (CONTEXT indexes) for full-text search. For Slovak language support in Oracle, configure:
- Oracle Text lexer preferences
- BASIC_LEXER with BASE_LETTER conversion

**Table Naming Differences**:
- PostgreSQL: `idx_<indexname>_ts`
- Oracle: `searchindex_<indexname>`

**Prerequisites**:
- Oracle database with appropriate privileges
- Database user must have CREATE INDEX privileges

**Idempotency**: Safe to run multiple times. Checks for existing constraints before creating.

---

## Deprecated Migration Scripts

The following scripts in `postgresql/migration/` are superseded by the standardized script above and should be removed:

### ❌ DEPRECATED: 202512_create_slovak_text_search_config.sql
**Replaced by**: Part 1 of `202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql`
**Reason**: Combined into comprehensive migration script with proper naming

### ❌ DEPRECATED: 202512_cleanup_duplicate_search_index_data.sql
**Replaced by**: Part 2 (data cleanup) of `202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql`
**Reason**: Integrated into main migration script for atomic execution

### ❌ DEPRECATED: 202512_fix_multi_tenant_unique_index.sql
**Replaced by**: Part 2 (constraint fix) of `202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql`
**Reason**: Combined with data cleanup for atomic execution

---

## Documentation-Only Scripts

The following scripts in `docs/migration/` are for documentation/analysis purposes only and should NOT be used in production:

### 📋 REFERENCE ONLY: search_index_migration.sql
**Status**: Documentation
**Purpose**: Original combined migration script used for development/testing
**Action**: Keep for reference, but do not execute

### 📋 REFERENCE ONLY: 001-fix-all-search-index-constraints.sql
**Status**: Documentation
**Purpose**: Early version of multi-tenant constraint fix with specific table names
**Action**: Keep for reference, shows manual approach

### 📋 REFERENCE ONLY: QUICK-FIX-idx_product_ts.sql
**Status**: Documentation
**Purpose**: Emergency fix for single table during development
**Action**: Keep for reference, shows problem identification

### 📋 REFERENCE ONLY: fix-search-index-unique-constraint.sql
**Status**: Documentation
**Purpose**: Alternative approach to constraint fix
**Action**: Keep for reference

---

## Migration Script Naming Convention

iDempiere migration scripts follow this format:

```
YYYYMMDDHHMI_[TICKET-ID]_Description.sql
```

Example:
```
202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql
```

**Components**:
- `YYYYMMDDHHMI`: Timestamp (Year, Month, Day, Hour, Minute)
- `TICKET-ID`: Optional JIRA/Linear ticket reference (e.g., CLD-1582)
- `Description`: Brief description in PascalCase or snake_case

**Our Format** (without ticket ID):
```
202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql
```

---

## How to Apply Migration Scripts

### PostgreSQL

```bash
# Connect to iDempiere database
psql -U adempiere -d idempiere

# Run migration script
\i postgresql/migration/202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql
```

Or using command line:
```bash
psql -U adempiere -d idempiere -f postgresql/migration/202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql
```

### Oracle

```bash
# Connect to iDempiere database
sqlplus adempiere/password@idempiere

# Enable output
SQL> SET SERVEROUTPUT ON SIZE UNLIMITED

# Run migration script
SQL> @oracle/migration/202512180801_MultiTenant_Fix.sql
```

---

## Verification Queries

### PostgreSQL - Verify Slovak Text Search Configuration

```sql
-- Check if sk_unaccent configuration exists
SELECT * FROM pg_ts_config WHERE cfgname = 'sk_unaccent';

-- Test Slovak text with diacritics
SELECT to_tsvector('sk_unaccent', 'červená ruža môže');
-- Expected: 'cervena':1 'moze':3 'ruza':2

-- Test query matching
SELECT to_tsvector('sk_unaccent', 'červená ruža') @@ to_tsquery('sk_unaccent', 'cervena & ruza');
-- Expected: t (true)
```

### PostgreSQL - Verify Multi-Tenant Constraints

```sql
-- Check all search index table constraints
SELECT
    t.tablename,
    i.indexname,
    pg_get_indexdef(i.indexrelid) as index_definition
FROM pg_tables t
JOIN pg_indexes i ON t.tablename = i.tablename
WHERE t.schemaname = 'adempiere'
  AND t.tablename LIKE 'idx_%_ts'
  AND i.indexname LIKE '%client_table_record_key'
ORDER BY t.tablename;
```

### Oracle - Verify Multi-Tenant Constraints

```sql
-- Check all search index table constraints
SELECT
    table_name,
    index_name,
    uniqueness
FROM user_indexes
WHERE table_name LIKE 'SEARCHINDEX_%'
  AND index_name LIKE '%CLIENT_TABLE_REC_KEY'
ORDER BY table_name;
```

---

## Rollback Procedures

### ⚠️ WARNING: Rollback will lose Slovak language support and multi-tenant safety

### PostgreSQL Rollback

```sql
-- Rollback Part 1: Drop Slovak text search configuration
DROP TEXT SEARCH CONFIGURATION IF EXISTS sk_unaccent;
-- Note: unaccent extension is left installed as it may be used by other features

-- Rollback Part 2: Restore old UNIQUE constraint
-- This is DANGEROUS in multi-tenant environments!
DO $$
DECLARE
    v_table_name TEXT;
    v_new_constraint TEXT;
    v_old_constraint TEXT;
BEGIN
    FOR v_table_name IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = 'adempiere' AND tablename LIKE 'idx_%_ts'
    LOOP
        -- Drop new constraint
        v_new_constraint := v_table_name || '_client_table_record_key';
        EXECUTE format('DROP INDEX IF EXISTS %I', v_new_constraint);

        -- Restore old constraint (NOT RECOMMENDED!)
        v_old_constraint := v_table_name || '_ad_table_id_record_id_key';
        EXECUTE format(
            'CREATE UNIQUE INDEX %I ON %I (ad_table_id, record_id)',
            v_old_constraint, v_table_name
        );
    END LOOP;
END $$;
```

### Oracle Rollback

```sql
-- Rollback: Restore old UNIQUE constraint (NOT RECOMMENDED!)
DECLARE
    v_table_name VARCHAR2(128);
    v_new_constraint VARCHAR2(128);
    v_old_constraint VARCHAR2(128);
    v_sql VARCHAR2(4000);

    CURSOR c_tables IS
        SELECT table_name FROM user_tables
        WHERE table_name LIKE 'SEARCHINDEX_%';
BEGIN
    FOR rec IN c_tables LOOP
        v_table_name := rec.table_name;

        -- Drop new constraint
        v_new_constraint := v_table_name || '_CLIENT_TABLE_REC_KEY';
        BEGIN
            v_sql := 'DROP INDEX ' || v_new_constraint;
            EXECUTE IMMEDIATE v_sql;
        EXCEPTION WHEN OTHERS THEN NULL;
        END;

        -- Restore old constraint (NOT RECOMMENDED!)
        v_old_constraint := v_table_name || '_AD_TABLE_ID_RECORD_ID_KEY';
        v_sql := 'CREATE UNIQUE INDEX ' || v_old_constraint ||
                 ' ON ' || v_table_name || ' (AD_TABLE_ID, RECORD_ID)';
        EXECUTE IMMEDIATE v_sql;
    END LOOP;
END;
/
```

---

## Performance Impact

### Migration Execution Time

**Estimated time** (based on production execution):
- Slovak text search configuration: ~1 second
- Multi-tenant constraint migration per table: ~100ms
- Total for 5 tables: ~2-3 seconds

**Actual production time**: 2 seconds (2025-12-18 08:01:51)

### Runtime Performance Impact

**After migration**:
- Slovak text search: 0% performance impact (same as before, but now works correctly)
- Multi-tenant constraint: 0-5% performance impact on INSERT/UPDATE (additional column in UNIQUE index)
- Search queries: No performance impact

---

## Related Documentation

- **ADR-005**: SearchType Migration & Slovak Language Support
  - Location: `docs/adr/ADR-005-searchtype-migration.md`
  - Explains root cause of POSITION search type and Slovak language issues

- **ADR-006**: Multi-Tenant Data Integrity
  - Location: `docs/adr/ADR-006-multi-tenant-unique-constraint.md`
  - Explains cross-client data corruption bug and fix

- **Slovak Language Architecture**:
  - Location: `docs/slovak-language-architecture.md`
  - Deep dive into Slovak text search implementation

- **Complete Analysis Summary**:
  - Location: `docs/COMPLETE-ANALYSIS-SUMMARY.md`
  - Executive summary of all performance and language issues

---

## Support

**Issues**: Create GitHub issue with label `migration`
**Questions**: Contact CloudEmpiere development team
**Production Issues**: Follow standard CloudEmpiere incident response procedures

---

**Last Updated**: 2025-12-18
**Migration Version**: 202512180801
**Status**: ✅ Production-ready
