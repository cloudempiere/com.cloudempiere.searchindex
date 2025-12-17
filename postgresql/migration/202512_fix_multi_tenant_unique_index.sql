-- Migration: Fix multi-tenant UNIQUE constraint for search indexes
-- Issue: ADR-006 - Multi-Tenant Data Integrity
-- Date: 2025-12-17
-- Description: Adds ad_client_id to UNIQUE constraint to prevent cross-client data corruption
--
-- IMPORTANT: Run the data cleanup script BEFORE this migration if you have existing data!
-- See: 202512_cleanup_duplicate_search_index_data.sql

DO $$
DECLARE
    v_table_name TEXT;
    v_old_index_name TEXT;
    v_new_index_name TEXT;
    v_count INT;
BEGIN
    RAISE NOTICE 'Starting migration: Fix multi-tenant UNIQUE constraint';
    RAISE NOTICE 'Date: %', NOW();

    -- Find all search index tables (pattern: searchindex_*)
    FOR v_table_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'adempiere'
          AND tablename LIKE 'searchindex_%'
    LOOP
        RAISE NOTICE '========================================';
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
        ', v_table_name) INTO v_count;

        IF v_count > 0 THEN
            RAISE WARNING 'WARNING: Found % duplicate groups in % that will cause constraint violation!', v_count, v_table_name;
            RAISE WARNING 'Please run data cleanup script first: 202512_cleanup_duplicate_search_index_data.sql';
            RAISE EXCEPTION 'Cannot proceed with migration - duplicate data found';
        END IF;

        -- Drop old UNIQUE index (ad_table_id, record_id)
        v_old_index_name := 'idx_' || REPLACE(v_table_name, 'searchindex_', '') || '_table_record';

        IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = v_old_index_name) THEN
            EXECUTE format('DROP INDEX IF EXISTS %I', v_old_index_name);
            RAISE NOTICE '  ✓ Dropped old index: %', v_old_index_name;
        ELSE
            -- Try alternative naming patterns
            v_old_index_name := v_table_name || '_ad_table_id_record_id_key';
            IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = v_old_index_name) THEN
                EXECUTE format('DROP INDEX IF EXISTS %I', v_old_index_name);
                RAISE NOTICE '  ✓ Dropped old index: %', v_old_index_name;
            ELSE
                RAISE NOTICE '  ⚠ Old index not found (may not exist)';
            END IF;
        END IF;

        -- Create new UNIQUE index (ad_client_id, ad_table_id, record_id)
        v_new_index_name := v_table_name || '_client_table_record_key';

        BEGIN
            EXECUTE format(
                'CREATE UNIQUE INDEX %I ON %I (ad_client_id, ad_table_id, record_id)',
                v_new_index_name,
                v_table_name
            );
            RAISE NOTICE '  ✓ Created new index: %', v_new_index_name;
        EXCEPTION
            WHEN duplicate_table THEN
                RAISE NOTICE '  ⚠ Index % already exists, skipping', v_new_index_name;
            WHEN unique_violation THEN
                RAISE EXCEPTION '  ✗ UNIQUE constraint violation - duplicate data exists. Run cleanup script first!';
        END;

        RAISE NOTICE '  ✓ Table % migration completed', v_table_name;
    END LOOP;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE 'All search index tables now have multi-tenant safe UNIQUE constraints';
END $$;
