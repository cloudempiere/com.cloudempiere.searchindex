-- Quick Fix: idx_product_ts table constraint
-- This fixes the immediate error for idx_product_ts
-- For a complete fix of all search index tables, run fix-search-index-unique-constraint.sql

-- Drop incorrect constraint (named idx_salesorder_unique, wrong name)
ALTER TABLE adempiere.idx_product_ts
    DROP CONSTRAINT IF EXISTS idx_salesorder_unique;

-- Add correct constraint with ad_client_id included
ALTER TABLE adempiere.idx_product_ts
    ADD CONSTRAINT idx_product_ts_unique
    UNIQUE (ad_client_id, ad_table_id, record_id);

-- Verify the constraint
SELECT
    conname as constraint_name,
    pg_get_constraintdef(oid) as definition
FROM pg_constraint
WHERE conrelid = 'adempiere.idx_product_ts'::regclass
  AND contype = 'u';

-- Expected output:
-- idx_product_ts_unique | UNIQUE (ad_client_id, ad_table_id, record_id)
