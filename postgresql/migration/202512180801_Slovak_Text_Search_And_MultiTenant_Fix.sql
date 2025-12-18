-- CLD-1652: Slovak Text Search Configuration and Multi-Tenant UNIQUE Constraint Fix
-- Migration Date: 2025-12-18 08:01
-- Description:
--   1. Creates Slovak/Czech text search configuration with unaccent support
--   2. Fixes multi-tenant UNIQUE constraint to include ad_client_id
-- Prerequisites: PostgreSQL unaccent extension
-- Related ADRs:
--   - ADR-005: SearchType Migration & Slovak Language Support
--   - ADR-006: Multi-Tenant Data Integrity
-- Linear Issue: https://linear.app/cloudempiere/issue/CLD-1652

-- Production Execution Results (2025-12-18 08:01:51)
-- ========================================
-- ✓ Unaccent extension already installed
-- ✓ Created sk_unaccent configuration
-- ✓ Configured word mappings with unaccent
-- Test 1 - Slovak text: 'cervena':1 'moze':3 'ruza':2
-- ✓ Test 1 PASSED: Query without diacritics matches text with diacritics
-- ✓ Test 2 PASSED: Czech diacritics handled correctly
-- Migration completed successfully!
--
-- Multi-Tenant Constraint Migration:
-- ✓ Data cleanup found 0 duplicates
-- ✓ Successfully migrated 5 tables:
--   - idx_product_ts
--   - idx_bpartner_ts
--   - idx_k_entry_ts
--   - idx_productcategory_ts
--   - idx_r_request_ts
-- ✓ Changed constraint from (ad_table_id, record_id)
--   to (ad_client_id, ad_table_id, record_id)
--
-- Next steps:
-- 1. Mark all Slovak/Czech search indexes as invalid: UPDATE AD_SearchIndex SET IsValid='N'
-- 2. Re-run CreateSearchIndex process for affected indexes

SELECT register_migration_script('202512180801_Slovak_Text_Search_And_MultiTenant_Fix.sql') FROM dual;

-- ========================================
-- PART 1: Slovak/Czech Text Search Configuration
-- ========================================

DO $$
BEGIN
    RAISE NOTICE 'Starting migration: Slovak text search configuration';
    RAISE NOTICE 'Date: %', NOW();
    RAISE NOTICE '========================================';

    -- Check if unaccent extension exists
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'unaccent') THEN
        RAISE NOTICE 'Installing unaccent extension...';
        CREATE EXTENSION IF NOT EXISTS unaccent;
        RAISE NOTICE '  ✓ Unaccent extension installed';
    ELSE
        RAISE NOTICE '  ✓ Unaccent extension already installed';
    END IF;

    -- Create Slovak/Czech text search configuration
    IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'sk_unaccent') THEN
        RAISE NOTICE 'Creating sk_unaccent text search configuration...';

        -- Copy from 'simple' configuration (language-independent)
        CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);

        -- Configure unaccent for word mappings
        -- This removes diacritics: č→c, š→s, ž→z, á→a, é→e, etc.
        ALTER TEXT SEARCH CONFIGURATION sk_unaccent
          ALTER MAPPING FOR word, asciiword, hword, hword_asciipart, asciihword
          WITH unaccent, simple;

        RAISE NOTICE '  ✓ Created sk_unaccent configuration';
        RAISE NOTICE '  ✓ Configured word mappings with unaccent';
    ELSE
        RAISE NOTICE '  ⚠ sk_unaccent configuration already exists, skipping creation';
    END IF;

    -- Test the configuration
    RAISE NOTICE 'Testing sk_unaccent configuration...';

    -- Test 1: Slovak text with diacritics
    DECLARE
        v_test_vector tsvector;
        v_test_query tsquery;
    BEGIN
        -- Create tsvector from Slovak text with diacritics
        v_test_vector := to_tsvector('sk_unaccent', 'červená ruža môže');

        -- Expected result: diacritics removed
        -- 'cervena':1 'moze':3 'ruza':2
        RAISE NOTICE '  Test 1 - Slovak text: %', v_test_vector;

        -- Create tsquery without diacritics
        v_test_query := to_tsquery('sk_unaccent', 'cervena & ruza');

        -- Test if query matches vector
        IF v_test_vector @@ v_test_query THEN
            RAISE NOTICE '  ✓ Test 1 PASSED: Query without diacritics matches text with diacritics';
        ELSE
            RAISE WARNING '  ✗ Test 1 FAILED: Query should match';
        END IF;
    END;

    -- Test 2: Czech text with diacritics
    DECLARE
        v_test_vector tsvector;
        v_test_query tsquery;
    BEGIN
        v_test_vector := to_tsvector('sk_unaccent', 'žluťoučký kůň');
        v_test_query := to_tsquery('sk_unaccent', 'zlutoucky & kun');

        IF v_test_vector @@ v_test_query THEN
            RAISE NOTICE '  ✓ Test 2 PASSED: Czech diacritics handled correctly';
        ELSE
            RAISE WARNING '  ✗ Test 2 FAILED: Czech query should match';
        END IF;
    END;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'PART 1 completed successfully!';
END $$;

-- ========================================
-- PART 2: Multi-Tenant UNIQUE Constraint Fix
-- ========================================

DO $$
DECLARE
    v_table_name TEXT;
    v_constraint_name TEXT;
    v_duplicate_count INT;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE 'Starting migration: Multi-tenant UNIQUE constraint fix';
    RAISE NOTICE 'Date: %', NOW();
    RAISE NOTICE '========================================';

    -- Process all search index tables
    FOR v_table_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'adempiere'
          AND tablename LIKE 'idx_%_ts'
        ORDER BY tablename
    LOOP
        RAISE NOTICE 'Processing table: %', v_table_name;

        -- Check for duplicate entries that would violate new constraint
        EXECUTE format('
            WITH duplicates AS (
                SELECT ad_table_id, record_id, COUNT(*) as cnt
                FROM %I
                GROUP BY ad_table_id, record_id
                HAVING COUNT(*) > 1
            )
            SELECT COUNT(*) FROM duplicates
        ', v_table_name) INTO v_duplicate_count;

        IF v_duplicate_count > 0 THEN
            RAISE WARNING '  ⚠ Found % duplicate groups in % - cleaning up...', v_duplicate_count, v_table_name;

            -- Clean up duplicates - keep only most recent entry
            EXECUTE format('
                DELETE FROM %I
                WHERE ctid IN (
                    SELECT ctid FROM (
                        SELECT ctid,
                               ROW_NUMBER() OVER (
                                   PARTITION BY ad_table_id, record_id
                                   ORDER BY
                                       COALESCE(updated, created) DESC NULLS LAST,
                                       created DESC NULLS LAST,
                                       ad_client_id DESC
                               ) AS rn
                        FROM %I
                    ) sub
                    WHERE rn > 1
                )
            ', v_table_name, v_table_name);

            RAISE NOTICE '  ✓ Cleaned up duplicates from %', v_table_name;
        ELSE
            RAISE NOTICE '  ✓ No duplicates found in %', v_table_name;
        END IF;

        -- Drop old UNIQUE constraint/index if exists
        -- Try multiple possible naming patterns
        v_constraint_name := v_table_name || '_ad_table_id_record_id_key';
        IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = v_constraint_name) THEN
            EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', v_table_name, v_constraint_name);
            RAISE NOTICE '  ✓ Dropped old constraint: %', v_constraint_name;
        END IF;

        -- Also try dropping index-based constraint
        IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = v_constraint_name) THEN
            EXECUTE format('DROP INDEX IF EXISTS %I', v_constraint_name);
            RAISE NOTICE '  ✓ Dropped old index: %', v_constraint_name;
        END IF;

        -- Create new multi-tenant safe UNIQUE constraint
        v_constraint_name := v_table_name || '_client_table_record_key';

        BEGIN
            EXECUTE format(
                'CREATE UNIQUE INDEX %I ON %I (ad_client_id, ad_table_id, record_id)',
                v_constraint_name,
                v_table_name
            );
            RAISE NOTICE '  ✓ Created new constraint: %', v_constraint_name;
        EXCEPTION
            WHEN duplicate_table THEN
                RAISE NOTICE '  ⚠ Constraint % already exists, skipping', v_constraint_name;
            WHEN unique_violation THEN
                RAISE EXCEPTION '  ✗ UNIQUE constraint violation - duplicate data still exists!';
        END;

        RAISE NOTICE '  ✓ Table % migration completed', v_table_name;
        RAISE NOTICE '';
    END LOOP;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'PART 2 completed successfully!';
    RAISE NOTICE 'All search index tables now have multi-tenant safe UNIQUE constraints';
END $$;

-- ========================================
-- Final Migration Summary
-- ========================================

DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'MIGRATION COMPLETED SUCCESSFULLY!';
    RAISE NOTICE '========================================';
    RAISE NOTICE '';
    RAISE NOTICE 'Changes applied:';
    RAISE NOTICE '1. ✓ Slovak/Czech text search configuration (sk_unaccent) created';
    RAISE NOTICE '2. ✓ Multi-tenant UNIQUE constraints fixed for all search index tables';
    RAISE NOTICE '';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '1. Mark all Slovak/Czech search indexes as invalid:';
    RAISE NOTICE '   UPDATE AD_SearchIndex SET IsValid=''N'' WHERE AD_SearchIndexProvider_ID IN (';
    RAISE NOTICE '     SELECT AD_SearchIndexProvider_ID FROM AD_SearchIndexProvider';
    RAISE NOTICE '     WHERE ProviderClassName LIKE ''%PGTextSearchIndexProvider''';
    RAISE NOTICE '   );';
    RAISE NOTICE '';
    RAISE NOTICE '2. Re-run CreateSearchIndex process for affected indexes';
    RAISE NOTICE '';
    RAISE NOTICE '3. Update existing indexes to use sk_unaccent configuration:';
    RAISE NOTICE '   - Modify PGTextSearchIndexProvider.getTSConfig() to return "sk_unaccent"';
    RAISE NOTICE '   - Or configure per-index text search configuration in AD_SearchIndexProvider';
    RAISE NOTICE '';
END $$;
