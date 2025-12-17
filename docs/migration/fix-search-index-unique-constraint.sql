-- Migration Script: Fix Multi-Tenant UNIQUE Constraint on Search Index Tables
-- Issue: ADR-006 - Search index UNIQUE constraints missing ad_client_id
-- Impact: ON CONFLICT clause fails, potential cross-client data corruption
-- Reference: fix/adr-critical-issues branch

-- This script fixes ALL search index tables (idx_*_ts pattern)
-- Run this script on your iDempiere database BEFORE rebuilding search indexes

-- Step 1: Generate list of all search index tables
DO $$
DECLARE
    table_rec RECORD;
    constraint_name TEXT;
BEGIN
    -- Find all tables matching idx_*_ts pattern
    FOR table_rec IN
        SELECT
            schemaname,
            tablename
        FROM pg_tables
        WHERE tablename LIKE 'idx_%_ts'
          AND schemaname = 'adempiere'  -- Change if using different schema
    LOOP
        -- Find existing UNIQUE constraint on (ad_table_id, record_id)
        SELECT conname INTO constraint_name
        FROM pg_constraint
        WHERE conrelid = (table_rec.schemaname || '.' || table_rec.tablename)::regclass
          AND contype = 'u'
          AND array_length(conkey, 1) = 2;

        IF constraint_name IS NOT NULL THEN
            RAISE NOTICE 'Fixing table: %.%', table_rec.schemaname, table_rec.tablename;

            -- Drop old constraint
            EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
                table_rec.schemaname,
                table_rec.tablename,
                constraint_name);

            -- Add new constraint with ad_client_id
            EXECUTE format('ALTER TABLE %I.%I ADD CONSTRAINT %I UNIQUE (ad_client_id, ad_table_id, record_id)',
                table_rec.schemaname,
                table_rec.tablename,
                table_rec.tablename || '_unique');

            RAISE NOTICE 'Fixed constraint on %.%', table_rec.schemaname, table_rec.tablename;
        ELSE
            RAISE WARNING 'No UNIQUE constraint found on %.% - may need manual intervention',
                table_rec.schemaname, table_rec.tablename;
        END IF;
    END LOOP;
END $$;

-- Step 2: Verify all constraints are correct
SELECT
    t.schemaname,
    t.tablename,
    c.conname as constraint_name,
    pg_get_constraintdef(c.oid) as constraint_definition
FROM pg_tables t
LEFT JOIN pg_constraint c ON c.conrelid = (t.schemaname || '.' || t.tablename)::regclass
    AND c.contype = 'u'
WHERE t.tablename LIKE 'idx_%_ts'
  AND t.schemaname = 'adempiere'
ORDER BY t.tablename;

-- Expected output: Each table should have UNIQUE (ad_client_id, ad_table_id, record_id)
