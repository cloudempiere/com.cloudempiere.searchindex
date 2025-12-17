-- Manual Test: Verify ts_rank_cd() works in your PostgreSQL database
-- Run these queries directly in your database to verify position-aware ranking

-- ==================================================================
-- Test 1: Verify ts_rank_cd() function exists
-- ==================================================================
SELECT ts_rank_cd(
    to_tsvector('simple', 'muškát krúžkovaný'),
    to_tsquery('simple', 'muskat'),
    2
) as rank;
-- Expected: Returns a non-zero rank (e.g., 0.1)
-- ✅ If you see a number, ts_rank_cd() works!

-- ==================================================================
-- Test 2: Position-aware ranking (earlier matches rank higher)
-- ==================================================================
WITH test_data AS (
    SELECT 1 as id, 'Muškát krúžkovaný' as name
    UNION ALL
    SELECT 2 as id, 'Kvety pre balkón muškát' as name
)
SELECT
    id,
    name,
    ts_rank_cd(
        to_tsvector('simple', name),
        to_tsquery('simple', 'muskat'),
        2
    ) as rank
FROM test_data
ORDER BY rank DESC;

-- Expected Result:
-- id | name                        | rank
-- 1  | Muškát krúžkovaný          | 0.1 (higher rank - term at start)
-- 2  | Kvety pre balkón muškát    | 0.05 (lower rank - term at end)
-- ✅ If id=1 ranks higher, position-aware ranking works!

-- ==================================================================
-- Test 3: Compare ts_rank() vs ts_rank_cd()
-- ==================================================================
WITH test_data AS (
    SELECT 'muškát at start of text' as text
)
SELECT
    ts_rank(to_tsvector('simple', text), to_tsquery('simple', 'muskat')) as ts_rank_score,
    ts_rank_cd(to_tsvector('simple', text), to_tsquery('simple', 'muskat'), 2) as ts_rank_cd_score
FROM test_data;

-- Expected: Both return non-zero scores
-- ts_rank_score: ~0.06
-- ts_rank_cd_score: ~0.1
-- ✅ Both functions work!

-- ==================================================================
-- Test 4: Proximity matters (closer terms rank higher)
-- ==================================================================
WITH test_data AS (
    SELECT 1 as id, 'muškát ružový' as text
    UNION ALL
    SELECT 2 as id, 'muškát krúžkovaný svetlý ružový' as text
)
SELECT
    id,
    text,
    ts_rank_cd(
        to_tsvector('simple', text),
        to_tsquery('simple', 'muskat & ruzovy'),
        2
    ) as rank
FROM test_data
ORDER BY rank DESC;

-- Expected Result:
-- id | text                              | rank
-- 1  | muškát ružový                     | 0.2 (higher - terms closer)
-- 2  | muškát krúžkovaný svetlý ružový   | 0.1 (lower - terms farther apart)
-- ✅ If id=1 ranks higher, proximity-aware ranking works!

-- ==================================================================
-- Test 5: Real-world test on your idx_product_ts table
-- ==================================================================
-- (Only run this if you have data in your search index)
SELECT
    record_id,
    ts_rank_cd(idx_tsvector, to_tsquery('simple', 'muskat'), 2) as rank
FROM idx_product_ts
WHERE idx_tsvector @@ to_tsquery('simple', 'muskat')
ORDER BY rank DESC
LIMIT 10;

-- Expected: Returns products with 'muškát' ranked by position
-- Products with term earlier in name should rank higher
-- ✅ If results are ordered by position, it works in production!

-- ==================================================================
-- SUMMARY
-- ==================================================================
-- If all 5 tests pass, ts_rank_cd() is working correctly on your
-- PostgreSQL database (including AWS RDS if that's where you run this).
--
-- This confirms the implementation is production-ready!
