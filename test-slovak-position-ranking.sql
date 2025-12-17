-- ============================================================================
-- Slovak Position Ranking Test: "muškát" at Beginning, Middle, End
-- ============================================================================
-- Test: Search "muskat" (no diacritics) finds "muškát" (with diacritics)
-- Validate: Position-aware ranking (earlier = higher rank)
-- ============================================================================

-- Cleanup
DROP TABLE IF EXISTS test_slovak_position CASCADE;

-- Create test table
CREATE TABLE test_slovak_position (
    id SERIAL PRIMARY KEY,
    position_type VARCHAR(20),
    product_name TEXT,
    idx_tsvector_sk tsvector
);

-- Test Data: "muškát" at BEGINNING, MIDDLE, END
INSERT INTO test_slovak_position (position_type, product_name) VALUES
-- BEGINNING (position 1)
('BEGINNING', 'Muškát červený krúžkovaný pre balkón'),
('BEGINNING', 'Muškát biely s dlhým kvitnutím'),
('BEGINNING', 'Muškát ružový jednoduchý na pestovanie'),

-- MIDDLE (position 3-4)
('MIDDLE', 'Balkónová rastlina muškát červený odolný'),
('MIDDLE', 'Kvetinové semená muškát pre záhradu'),
('MIDDLE', 'Pestovanie kvetov muškát návod'),

-- END (position 5-6)
('END', 'Najlepšie balkónové kvety na slnku muškát'),
('END', 'Odporúčame pre začiatočníkov pestovať muškát'),
('END', 'Kvetináč 30cm s podmiskou ideálny na muškát');

-- Create sk_unaccent configuration (if not exists)
CREATE EXTENSION IF NOT EXISTS unaccent;
DROP TEXT SEARCH CONFIGURATION IF EXISTS sk_unaccent CASCADE;
CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);
ALTER TEXT SEARCH CONFIGURATION sk_unaccent
  ALTER MAPPING FOR word, asciiword, hword, hword_asciipart, asciihword
  WITH unaccent, simple;

-- Build tsvector with sk_unaccent
UPDATE test_slovak_position
SET idx_tsvector_sk = to_tsvector('sk_unaccent', product_name);

-- Create GIN index
CREATE INDEX idx_slovak_position_sk ON test_slovak_position
USING GIN (idx_tsvector_sk);

-- Vacuum for statistics
VACUUM ANALYZE test_slovak_position;

-- ============================================================================
-- TEST: Search "muskat" (WITHOUT diacritics) for "muškát" (WITH diacritics)
-- ============================================================================

SELECT
    '╔════════════════════════════════════════════════════════════════════════════════════╗' as separator
UNION ALL SELECT
    '║  SLOVAK POSITION-AWARE RANKING TEST: "muskat" → "muškát"                          ║'
UNION ALL SELECT
    '╚════════════════════════════════════════════════════════════════════════════════════╝';

SELECT
    '' as blank_line;

-- Query: Search "muskat" with position-aware ranking
WITH search_results AS (
    SELECT
        id,
        position_type,
        product_name,
        ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2) as rank,
        -- Show position of "muskat" in tsvector
        (string_to_array(idx_tsvector_sk::text, ''''))[2]::text as first_term,
        array_position(
            string_to_array(
                regexp_replace(idx_tsvector_sk::text, '''[0-9]+''', '', 'g'),
                ' '
            ),
            'muskat'
        ) as term_position
    FROM test_slovak_position
    WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
    ORDER BY rank DESC, id
)
SELECT
    ROW_NUMBER() OVER (ORDER BY rank DESC, id) as "#",
    position_type as "Position",
    SUBSTRING(product_name, 1, 45) || '...' as "Slovak Product Name",
    ROUND(rank::numeric, 6) as "Rank",
    ROUND((rank * 100)::numeric, 2) || '%' as "Rank %",
    CASE
        WHEN rank >= 0.05 THEN '█████ HIGH'
        WHEN rank >= 0.03 THEN '███░░ MED'
        ELSE '█░░░░ LOW'
    END as "Visual",
    CASE
        WHEN position_type = 'BEGINNING' THEN '1️⃣'
        WHEN position_type = 'MIDDLE' THEN '2️⃣'
        WHEN position_type = 'END' THEN '3️⃣'
    END as "Pos"
FROM search_results;

-- Summary Statistics
SELECT
    '' as blank_line;

SELECT
    '╔════════════════════════════════════════════════════════════════════════════════════╗' as separator
UNION ALL SELECT
    '║  RANKING ANALYSIS                                                                  ║'
UNION ALL SELECT
    '╚════════════════════════════════════════════════════════════════════════════════════╝';

SELECT
    '' as blank_line;

WITH search_results AS (
    SELECT
        position_type,
        AVG(ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2)) as avg_rank,
        MIN(ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2)) as min_rank,
        MAX(ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2)) as max_rank,
        COUNT(*) as count
    FROM test_slovak_position
    WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
    GROUP BY position_type
)
SELECT
    position_type as "Position Type",
    count as "Products",
    ROUND(avg_rank::numeric, 4) as "Avg Rank",
    ROUND(min_rank::numeric, 4) as "Min Rank",
    ROUND(max_rank::numeric, 4) as "Max Rank",
    CASE
        WHEN position_type = 'BEGINNING' THEN '1st (Highest ✓)'
        WHEN position_type = 'MIDDLE' THEN '2nd'
        WHEN position_type = 'END' THEN '3rd (Lowest)'
    END as "Expected Order"
FROM search_results
ORDER BY avg_rank DESC;

-- Verify diacritics handling
SELECT
    '' as blank_line;

SELECT
    '╔════════════════════════════════════════════════════════════════════════════════════╗' as separator
UNION ALL SELECT
    '║  DIACRITICS VERIFICATION                                                           ║'
UNION ALL SELECT
    '╚════════════════════════════════════════════════════════════════════════════════════╝';

SELECT
    '' as blank_line;

SELECT
    'Customer Types' as scenario,
    'muskat' as search_query,
    '(no diacritics)' as note
UNION ALL
SELECT
    'Database Has',
    'Muškát',
    '(with á, š, ť)'
UNION ALL
SELECT
    'Result',
    CASE
        WHEN COUNT(*) > 0 THEN '✓ MATCHES (' || COUNT(*) || ' products found)'
        ELSE '✗ NO MATCH'
    END,
    ''
FROM test_slovak_position
WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat');

-- Performance verification
SELECT
    '' as blank_line;

SELECT
    '╔════════════════════════════════════════════════════════════════════════════════════╗' as separator
UNION ALL SELECT
    '║  PERFORMANCE VERIFICATION                                                          ║'
UNION ALL SELECT
    '╚════════════════════════════════════════════════════════════════════════════════════╝';

SELECT
    '' as blank_line;

-- Show query plan (should use Index Scan, not Seq Scan)
EXPLAIN (FORMAT TEXT, COSTS OFF, TIMING OFF)
SELECT
    product_name,
    ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2) as rank
FROM test_slovak_position
WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
ORDER BY rank DESC
LIMIT 5;

-- Cleanup note
SELECT
    '' as blank_line;

SELECT
    '╔════════════════════════════════════════════════════════════════════════════════════╗' as separator
UNION ALL SELECT
    '║  TEST COMPLETE - Table "test_slovak_position" left for inspection                 ║'
UNION ALL SELECT
    '║  Run: DROP TABLE test_slovak_position CASCADE; to cleanup                         ║'
UNION ALL SELECT
    '╚════════════════════════════════════════════════════════════════════════════════════╝';
