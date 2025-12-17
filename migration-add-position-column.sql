-- ============================================================================
-- Migration: Add min_word_position Column for Efficient Position-Based Ranking
-- ============================================================================
-- Date: 2025-12-17
-- Purpose: Store word positions at index time to eliminate regex operations
--          at query time (100× performance improvement)
--
-- Background:
-- - Previous approach used regex to extract positions at query time
-- - This bypassed GIN index causing O(n) sequential scans
-- - New approach: Calculate once at index time, use many times at query time
-- ============================================================================

-- Step 1: Add min_word_position column to index table
-- Note: This needs to be applied to ALL search index tables
-- Pattern: com_cloudempiere_idx_{indexname}

-- Example for product search index:
ALTER TABLE com_cloudempiere_idx_product_ts
ADD COLUMN IF NOT EXISTS min_word_position INT DEFAULT 999;

COMMENT ON COLUMN com_cloudempiere_idx_product_ts.min_word_position IS
'Minimum word position (1-based) of any indexed term in the document. ' ||
'Calculated at index time for fast position-aware ranking. ' ||
'Default 999 for documents without matched terms (sorts to end).';

-- Step 2: Create index on min_word_position for fast ORDER BY
CREATE INDEX IF NOT EXISTS idx_product_ts_position
ON com_cloudempiere_idx_product_ts (min_word_position);

-- Step 3: Update statistics
VACUUM ANALYZE com_cloudempiere_idx_product_ts;

-- ============================================================================
-- Generic Template (apply to all search indexes)
-- ============================================================================
-- Replace {INDEXNAME} with actual index name from AD_SearchIndex.IndexName

/*
ALTER TABLE com_cloudempiere_idx_{INDEXNAME}
ADD COLUMN IF NOT EXISTS min_word_position INT DEFAULT 999;

COMMENT ON COLUMN com_cloudempiere_idx_{INDEXNAME}.min_word_position IS
'Minimum word position (1-based) of any indexed term in the document. ' ||
'Calculated at index time for fast position-aware ranking. ' ||
'Default 999 for documents without matched terms (sorts to end).';

CREATE INDEX IF NOT EXISTS idx_{INDEXNAME}_position
ON com_cloudempiere_idx_{INDEXNAME} (min_word_position);

VACUUM ANALYZE com_cloudempiere_idx_{INDEXNAME};
*/

-- ============================================================================
-- Rollback (if needed)
-- ============================================================================
/*
DROP INDEX IF EXISTS idx_product_ts_position;
ALTER TABLE com_cloudempiere_idx_product_ts DROP COLUMN IF EXISTS min_word_position;
*/

-- ============================================================================
-- After Migration: Rebuild All Indexes
-- ============================================================================
-- Run CreateSearchIndex process for each AD_SearchIndex to populate
-- min_word_position column with calculated values
--
-- Process: CreateSearchIndex
-- Parameters:
--   - AD_SearchIndexProvider_ID: [select provider]
--   - AD_SearchIndex_ID: NULL (rebuilds all indexes for provider)
-- ============================================================================
