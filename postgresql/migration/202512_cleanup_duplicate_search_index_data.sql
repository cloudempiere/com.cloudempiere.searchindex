-- Data Cleanup: Remove duplicate search index entries before migration
-- Issue: ADR-006 - Multi-Tenant Data Integrity
-- Date: 2025-12-17
-- Description: Cleans up duplicate (ad_table_id, record_id) entries before applying
--              new UNIQUE constraint that includes ad_client_id
--
-- IMPORTANT: Run this script BEFORE running 202512_fix_multi_tenant_unique_index.sql

DO $$
DECLARE
    v_table_name TEXT;
    v_duplicate_count INT;
    v_deleted_count INT;
    v_total_deleted INT := 0;
BEGIN
    RAISE NOTICE 'Starting data cleanup for multi-tenant UNIQUE constraint migration';
    RAISE NOTICE 'Date: %', NOW();
    RAISE NOTICE '========================================';

    FOR v_table_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'adempiere'
          AND tablename LIKE 'searchindex_%'
    LOOP
        RAISE NOTICE 'Checking table: %', v_table_name;

        -- Count duplicate (ad_table_id, record_id) entries
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
            RAISE WARNING '  ⚠ Found % duplicate groups in %', v_duplicate_count, v_table_name;

            -- Keep only the most recent entry per (ad_table_id, record_id)
            -- Delete older entries based on updated/created timestamps
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

            GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
            v_total_deleted := v_total_deleted + v_deleted_count;

            RAISE NOTICE '  ✓ Deleted % duplicate entries from %', v_deleted_count, v_table_name;

            -- Verify cleanup
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
                RAISE EXCEPTION '  ✗ Cleanup failed! Still have % duplicate groups', v_duplicate_count;
            ELSE
                RAISE NOTICE '  ✓ Cleanup verified - no duplicates remain';
            END IF;
        ELSE
            RAISE NOTICE '  ✓ No duplicates found in %', v_table_name;
        END IF;
    END LOOP;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'Data cleanup completed successfully!';
    RAISE NOTICE 'Total duplicate entries deleted: %', v_total_deleted;
    RAISE NOTICE '';
    RAISE NOTICE 'You can now run: 202512_fix_multi_tenant_unique_index.sql';
END $$;
