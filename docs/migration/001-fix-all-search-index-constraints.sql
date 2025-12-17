-- Migration Script 001: Fix Multi-Tenant UNIQUE Constraints on All Search Index Tables
-- Issue: ADR-006 - Search index UNIQUE constraints missing ad_client_id
-- Impact: ON CONFLICT clause fails, potential cross-client data corruption
-- Reference: fix/adr-critical-issues branch
-- Date: 2025-12-17

-- IMPORTANT: Run this migration BEFORE rebuilding any search indexes
-- This script will fix all idx_*_ts tables to include ad_client_id in UNIQUE constraint

BEGIN;

-- ==============================================================================
-- STEP 1: Check for duplicate records that would violate new constraint
-- ==============================================================================

DO $$
DECLARE
    v_duplicates INTEGER;
    v_table TEXT;
BEGIN
    -- Check idx_bpartner_ts
    SELECT COUNT(*) INTO v_duplicates
    FROM (
        SELECT ad_table_id, record_id, COUNT(*)
        FROM adempiere.idx_bpartner_ts
        GROUP BY ad_table_id, record_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF v_duplicates > 0 THEN
        RAISE WARNING 'idx_bpartner_ts has % duplicate records - will be cleaned', v_duplicates;
    END IF;

    -- Check idx_k_entry_ts
    SELECT COUNT(*) INTO v_duplicates
    FROM (
        SELECT ad_table_id, record_id, COUNT(*)
        FROM adempiere.idx_k_entry_ts
        GROUP BY ad_table_id, record_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF v_duplicates > 0 THEN
        RAISE WARNING 'idx_k_entry_ts has % duplicate records - will be cleaned', v_duplicates;
    END IF;

    -- Check idx_product_ts
    SELECT COUNT(*) INTO v_duplicates
    FROM (
        SELECT ad_table_id, record_id, COUNT(*)
        FROM adempiere.idx_product_ts
        GROUP BY ad_table_id, record_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF v_duplicates > 0 THEN
        RAISE WARNING 'idx_product_ts has % duplicate records - will be cleaned', v_duplicates;
    END IF;

    -- Check idx_productcategory
    SELECT COUNT(*) INTO v_duplicates
    FROM (
        SELECT ad_table_id, record_id, COUNT(*)
        FROM adempiere.idx_productcategory
        GROUP BY ad_table_id, record_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF v_duplicates > 0 THEN
        RAISE WARNING 'idx_productcategory has % duplicate records - will be cleaned', v_duplicates;
    END IF;

    -- Check idx_r_request_ts
    SELECT COUNT(*) INTO v_duplicates
    FROM (
        SELECT ad_table_id, record_id, COUNT(*)
        FROM adempiere.idx_r_request_ts
        GROUP BY ad_table_id, record_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF v_duplicates > 0 THEN
        RAISE WARNING 'idx_r_request_ts has % duplicate records - will be cleaned', v_duplicates;
    END IF;
END $$;

-- ==============================================================================
-- STEP 2: Clean duplicates (keep record with lowest ad_client_id)
-- ==============================================================================

-- Clean idx_bpartner_ts
DELETE FROM adempiere.idx_bpartner_ts
WHERE ctid NOT IN (
    SELECT MIN(ctid)
    FROM adempiere.idx_bpartner_ts
    GROUP BY ad_table_id, record_id
);

-- Clean idx_k_entry_ts
DELETE FROM adempiere.idx_k_entry_ts
WHERE ctid NOT IN (
    SELECT MIN(ctid)
    FROM adempiere.idx_k_entry_ts
    GROUP BY ad_table_id, record_id
);

-- Clean idx_product_ts
DELETE FROM adempiere.idx_product_ts
WHERE ctid NOT IN (
    SELECT MIN(ctid)
    FROM adempiere.idx_product_ts
    GROUP BY ad_table_id, record_id
);

-- Clean idx_productcategory
DELETE FROM adempiere.idx_productcategory
WHERE ctid NOT IN (
    SELECT MIN(ctid)
    FROM adempiere.idx_productcategory
    GROUP BY ad_table_id, record_id
);

-- Clean idx_r_request_ts
DELETE FROM adempiere.idx_r_request_ts
WHERE ctid NOT IN (
    SELECT MIN(ctid)
    FROM adempiere.idx_r_request_ts
    GROUP BY ad_table_id, record_id
);

-- ==============================================================================
-- STEP 3: Fix idx_bpartner_ts
-- ==============================================================================

-- Drop old constraint
ALTER TABLE adempiere.idx_bpartner_ts
    DROP CONSTRAINT IF EXISTS idx_bpartner_ts_unique;
ALTER TABLE adempiere.idx_bpartner_ts
    DROP CONSTRAINT IF EXISTS idx_bpartner_unique;
ALTER TABLE adempiere.idx_bpartner_ts
    DROP CONSTRAINT IF EXISTS idx_salesorder_unique;

-- Add correct constraint
ALTER TABLE adempiere.idx_bpartner_ts
    ADD CONSTRAINT idx_bpartner_ts_unique
    UNIQUE (ad_client_id, ad_table_id, record_id);

RAISE NOTICE 'Fixed constraint on idx_bpartner_ts';

-- ==============================================================================
-- STEP 4: Fix idx_k_entry_ts
-- ==============================================================================

-- Drop old constraint
ALTER TABLE adempiere.idx_k_entry_ts
    DROP CONSTRAINT IF EXISTS idx_k_entry_ts_unique;
ALTER TABLE adempiere.idx_k_entry_ts
    DROP CONSTRAINT IF EXISTS idx_k_entry_unique;
ALTER TABLE adempiere.idx_k_entry_ts
    DROP CONSTRAINT IF EXISTS idx_salesorder_unique;

-- Add correct constraint
ALTER TABLE adempiere.idx_k_entry_ts
    ADD CONSTRAINT idx_k_entry_ts_unique
    UNIQUE (ad_client_id, ad_table_id, record_id);

RAISE NOTICE 'Fixed constraint on idx_k_entry_ts';

-- ==============================================================================
-- STEP 5: Fix idx_product_ts
-- ==============================================================================

-- Drop old constraint
ALTER TABLE adempiere.idx_product_ts
    DROP CONSTRAINT IF EXISTS idx_product_ts_unique;
ALTER TABLE adempiere.idx_product_ts
    DROP CONSTRAINT IF EXISTS idx_product_unique;
ALTER TABLE adempiere.idx_product_ts
    DROP CONSTRAINT IF EXISTS idx_salesorder_unique;

-- Add correct constraint
ALTER TABLE adempiere.idx_product_ts
    ADD CONSTRAINT idx_product_ts_unique
    UNIQUE (ad_client_id, ad_table_id, record_id);

RAISE NOTICE 'Fixed constraint on idx_product_ts';

-- ==============================================================================
-- STEP 6: Fix idx_productcategory
-- ==============================================================================

-- Drop old constraint
ALTER TABLE adempiere.idx_productcategory
    DROP CONSTRAINT IF EXISTS idx_productcategory_unique;
ALTER TABLE adempiere.idx_productcategory
    DROP CONSTRAINT IF EXISTS idx_salesorder_unique;

-- Add correct constraint
ALTER TABLE adempiere.idx_productcategory
    ADD CONSTRAINT idx_productcategory_unique
    UNIQUE (ad_client_id, ad_table_id, record_id);

RAISE NOTICE 'Fixed constraint on idx_productcategory';

-- ==============================================================================
-- STEP 7: Fix idx_r_request_ts
-- ==============================================================================

-- Drop old constraint
ALTER TABLE adempiere.idx_r_request_ts
    DROP CONSTRAINT IF EXISTS idx_r_request_ts_unique;
ALTER TABLE adempiere.idx_r_request_ts
    DROP CONSTRAINT IF EXISTS idx_r_request_unique;
ALTER TABLE adempiere.idx_r_request_ts
    DROP CONSTRAINT IF EXISTS idx_salesorder_unique;

-- Add correct constraint
ALTER TABLE adempiere.idx_r_request_ts
    ADD CONSTRAINT idx_r_request_ts_unique
    UNIQUE (ad_client_id, ad_table_id, record_id);

RAISE NOTICE 'Fixed constraint on idx_r_request_ts';

-- ==============================================================================
-- STEP 8: Verify all constraints
-- ==============================================================================

DO $$
DECLARE
    constraint_rec RECORD;
    v_error_count INTEGER := 0;
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Verification: Checking all constraints';
    RAISE NOTICE '========================================';

    FOR constraint_rec IN
        SELECT
            t.tablename,
            c.conname as constraint_name,
            pg_get_constraintdef(c.oid) as constraint_def
        FROM pg_tables t
        LEFT JOIN pg_constraint c ON c.conrelid = ('adempiere.' || t.tablename)::regclass
            AND c.contype = 'u'
        WHERE t.tablename IN (
            'idx_bpartner_ts',
            'idx_k_entry_ts',
            'idx_product_ts',
            'idx_productcategory',
            'idx_r_request_ts'
        )
        AND t.schemaname = 'adempiere'
        ORDER BY t.tablename
    LOOP
        IF constraint_rec.constraint_def LIKE '%ad_client_id, ad_table_id, record_id%' THEN
            RAISE NOTICE '✓ % has correct constraint: %',
                constraint_rec.tablename,
                constraint_rec.constraint_name;
        ELSE
            RAISE WARNING '✗ % has incorrect constraint: % - %',
                constraint_rec.tablename,
                constraint_rec.constraint_name,
                constraint_rec.constraint_def;
            v_error_count := v_error_count + 1;
        END IF;
    END LOOP;

    IF v_error_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % tables have incorrect constraints', v_error_count;
    ELSE
        RAISE NOTICE '========================================';
        RAISE NOTICE '✓ All constraints verified successfully';
        RAISE NOTICE '========================================';
    END IF;
END $$;

COMMIT;

-- ==============================================================================
-- Post-Migration Instructions
-- ==============================================================================

-- After running this migration:
-- 1. Run CreateSearchIndex process for each search index to rebuild with correct data
-- 2. Verify multi-client data is properly isolated
-- 3. Test search functionality for all affected indexes

-- To verify constraint on a specific table:
-- SELECT
--     conname as constraint_name,
--     pg_get_constraintdef(oid) as definition
-- FROM pg_constraint
-- WHERE conrelid = 'adempiere.idx_product_ts'::regclass
--   AND contype = 'u';
