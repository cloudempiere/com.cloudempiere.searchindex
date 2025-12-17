-- ============================================================================
-- Real-World Slovak Product Names Test (Realistic E-commerce Examples)
-- ============================================================================
-- Based on typical products from Slovak garden/flower e-commerce sites
-- Testing: "muskat" search finds "muškát" products with position-aware ranking
-- ============================================================================

DROP TABLE IF EXISTS real_slovak_products CASCADE;

CREATE TABLE real_slovak_products (
    id SERIAL PRIMARY KEY,
    product_code VARCHAR(50),
    product_name TEXT,
    category VARCHAR(100),
    idx_tsvector_sk tsvector
);

-- Real-world product examples (like aquaseed.sk would have)
INSERT INTO real_slovak_products (product_code, product_name, category) VALUES
-- BEGINNING position products
('PEL-MUS-001', 'Muškát červený - Pelargonium zonale', 'Rastliny'),
('PEL-MUS-002', 'Muškát biely jednoduchý 10,5cm kvetináč', 'Rastliny'),
('PEL-MUS-003', 'Muškát ružový - veľkokvetý balkónový', 'Rastliny'),
('PEL-MUS-004', 'Muškát plnokvetý červený - Pelargonium grandiflorum', 'Rastliny'),
('PEL-MUS-005', 'Muškát anglický - Pelargonium regal', 'Rastliny'),
('PEL-MUS-006', 'Muškát tmavočervený - bordo odolný', 'Rastliny'),
('PEL-MUS-007', 'Muškát visiaci - Pelargonium peltatum', 'Rastliny'),
('PEL-MUS-008', 'Muškát  mix farieb - 12 ks balenie', 'Rastliny'),
('PEL-MUS-009', 'Muškát bordovo-červený dlhodobo kvitnúci', 'Rastliny'),
('PEL-MUS-010', 'Muškát svetloružový - perfektný na balkón', 'Rastliny'),

-- MIDDLE position products
('SEM-MUS-001', 'Semená balkónového muškátu červeného', 'Semená'),
('SEM-MUS-002', 'Semená voňavého muškátu pre záhradu', 'Semená'),
('SUB-MUS-001', 'Substrát pre muškáty a pelargónie 10L', 'Substráty'),
('SUB-MUS-002', 'Profesionálny substrát muškát optimum 20L', 'Substráty'),
('HNO-MUS-001', 'Tekuté hnojivo na muškáty a balkónové rastliny 1L', 'Hnojivá'),
('HNO-MUS-002', 'Granulov hnojivo muškát long-effect 500g', 'Hnojivá'),
('KVE-MUS-001', 'Plastový kvetináč pre muškáty 30cm s podmiskou', 'Kvetináče'),
('KVE-MUS-002', 'Závesný košík vhodný muškát 25cm terakota', 'Kvetináče'),
('OPO-MUS-001', 'Oporný stojan na muškáty a popínavé rastliny', 'Podpory'),
('NAV-MUS-001', 'Návod na pestovanie muškátu od A po Z', 'Knihy'),

-- END position products
('BAL-001', 'Najlepšie balkónové kvety odolné na slnko muškát', 'Rastliny'),
('BAL-002', 'Odporúčame začiatočníkom pestovať práve muškát', 'Rastliny'),
('BAL-003', 'Set na balkón: petúnie, surfínie a muškát', 'Sety'),
('BAL-004', 'Kvetinový box pre slnečný balkón s muškátom', 'Sety'),
('ZAH-001', 'Celý rok zelená záhrada vďaka muškátu', 'Rastliny'),
('TER-001', 'Terasový set farebných kvetov vrátane muškát', 'Sety'),
('VIS-001', 'Visiace ampelové rastliny ideálne muškát', 'Rastliny'),
('SON-001', 'Sortiment na slnečnú terasu so zabudnuteľným muškátom', 'Sety'),
('PRI-001', 'Prírodná ochrana pred škodcami bez chemikálií muškát', 'Ochrana'),
('KRA-001', 'Krásna farebná výsadba do truhlíku s muškátom', 'Sety'),

-- Additional realistic products with variations
('PEL-MUS-011', 'Muškát oranžový - zriedkavá farba', 'Rastliny'),
('PEL-MUS-012', 'Muškát fialový - Pelargonium unique', 'Rastliny'),
('PEL-MUS-013', 'Muškát dvojfarebný červeno-biely', 'Rastliny'),
('ACC-001', 'Automatické zavlažovanie pre muškáty 2L', 'Príslušenstvo'),
('ACC-002', 'Ochranná sieť proti vtákom muškát ochrana', 'Príslušenstvo'),
('ZIM-001', 'Zimovanie balkónových rastlín vrátane muškát', 'Starostlivosť'),
('BOL-001', 'Boj proti škodcom ekologicky muškát', 'Ochrana'),
('KNI-001', 'Kniha: Muškáty a pelargónie - pestovanie a starostlivosť', 'Knihy'),
('VID-001', 'Video kurz: Úspešné pestovanie muškát', 'Kurzy'),
('CON-001', 'Konzultácia s odborníkom pestovanie muškát online', 'Služby');

-- Create sk_unaccent configuration
CREATE EXTENSION IF NOT EXISTS unaccent;
DROP TEXT SEARCH CONFIGURATION IF EXISTS sk_unaccent CASCADE;
CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);
ALTER TEXT SEARCH CONFIGURATION sk_unaccent
  ALTER MAPPING FOR word, asciiword, hword, hword_asciipart, asciihword
  WITH unaccent, simple;

-- Build tsvector
UPDATE real_slovak_products
SET idx_tsvector_sk = to_tsvector('sk_unaccent', product_name);

-- Create GIN index
CREATE INDEX idx_real_slovak_sk ON real_slovak_products
USING GIN (idx_tsvector_sk);

VACUUM ANALYZE real_slovak_products;

-- ============================================================================
-- TEST: Search "muskat" (81 products total like aquaseed.sk)
-- ============================================================================

\echo '╔════════════════════════════════════════════════════════════════════════════════╗'
\echo '║  REAL-WORLD SLOVAK E-COMMERCE: 40 Products with "muškát"                      ║'
\echo '║  Search: "muskat" (customer types without diacritics)                         ║'
\echo '╚════════════════════════════════════════════════════════════════════════════════╝'
\echo ''

-- Show all results with ranking
WITH search_results AS (
    SELECT
        id,
        product_code,
        product_name,
        category,
        ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2) as rank,
        CASE
            WHEN product_name ~* '^Muškát' THEN 'BEGINNING'
            WHEN product_name ~* 'muškát$' OR product_name ~* 'muškátom$' THEN 'END'
            ELSE 'MIDDLE'
        END as position_type
    FROM real_slovak_products
    WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
    ORDER BY rank DESC, id
)
SELECT
    ROW_NUMBER() OVER (ORDER BY rank DESC, id) as "#",
    product_code as "Code",
    CASE
        WHEN LENGTH(product_name) > 42 THEN SUBSTRING(product_name, 1, 42) || '...'
        ELSE product_name
    END as "Product Name",
    category as "Category",
    ROUND(rank::numeric, 6) as "Rank",
    ROUND((rank * 100)::numeric, 2) || '%' as "%",
    position_type as "Pos Type"
FROM search_results;

\echo ''
\echo '╔════════════════════════════════════════════════════════════════════════════════╗'
\echo '║  RANKING BY POSITION TYPE                                                     ║'
\echo '╚════════════════════════════════════════════════════════════════════════════════╝'
\echo ''

-- Summary by position type
WITH search_results AS (
    SELECT
        CASE
            WHEN product_name ~* '^Muškát' THEN 'BEGINNING'
            WHEN product_name ~* 'muškát$' OR product_name ~* 'muškátom$' THEN 'END'
            ELSE 'MIDDLE'
        END as position_type,
        ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2) as rank
    FROM real_slovak_products
    WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
)
SELECT
    position_type as "Position",
    COUNT(*) as "Count",
    ROUND(AVG(rank)::numeric, 6) as "Avg Rank",
    ROUND(MIN(rank)::numeric, 6) as "Min",
    ROUND(MAX(rank)::numeric, 6) as "Max",
    ROUND((AVG(rank) * 100)::numeric, 2) || '%' as "Avg %"
FROM search_results
GROUP BY position_type
ORDER BY AVG(rank) DESC;

\echo ''
\echo '╔════════════════════════════════════════════════════════════════════════════════╗'
\echo '║  TOP 10 RESULTS (What customer sees first)                                    ║'
\echo '╚════════════════════════════════════════════════════════════════════════════════╝'
\echo ''

WITH search_results AS (
    SELECT
        product_code,
        product_name,
        category,
        ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2) as rank
    FROM real_slovak_products
    WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
    ORDER BY rank DESC, id
    LIMIT 10
)
SELECT
    ROW_NUMBER() OVER () as "#",
    product_code as "Code",
    product_name as "Product Name",
    ROUND(rank::numeric, 6) as "Rank",
    ROUND((rank * 100)::numeric, 2) || '%' as "Score"
FROM search_results;

\echo ''
\echo '╔════════════════════════════════════════════════════════════════════════════════╗'
\echo '║  PERFORMANCE TEST                                                              ║'
\echo '╚════════════════════════════════════════════════════════════════════════════════╝'
\echo ''
\echo 'Query Plan (should use Bitmap Index Scan on GIN index):'
\echo ''

EXPLAIN (COSTS OFF)
SELECT product_code, product_name,
       ts_rank_cd(idx_tsvector_sk, to_tsquery('sk_unaccent', 'muskat'), 2) as rank
FROM real_slovak_products
WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
ORDER BY rank DESC
LIMIT 10;

\echo ''
\echo '╔════════════════════════════════════════════════════════════════════════════════╗'
\echo '║  SUCCESS METRICS                                                               ║'
\echo '╚════════════════════════════════════════════════════════════════════════════════╝'
\echo ''

SELECT
    'Total Products' as "Metric",
    COUNT(*)::text as "Value"
FROM real_slovak_products
UNION ALL
SELECT
    'Products with "muškát"',
    COUNT(*)::text
FROM real_slovak_products
WHERE product_name ~* 'muškát'
UNION ALL
SELECT
    'Found by "muskat" search',
    COUNT(*)::text
FROM real_slovak_products
WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat')
UNION ALL
SELECT
    'Match Rate',
    ROUND((COUNT(*) * 100.0 / (SELECT COUNT(*) FROM real_slovak_products WHERE product_name ~* 'muškát'))::numeric, 1)::text || '%'
FROM real_slovak_products
WHERE idx_tsvector_sk @@ to_tsquery('sk_unaccent', 'muskat');

\echo ''
\echo '✓ Test Complete! Table "real_slovak_products" ready for inspection'
\echo 'Run: DROP TABLE real_slovak_products CASCADE; to cleanup'
\echo ''
