# PostgreSQL Full-Text Search Performance Issue - Recap

## The Problem

You have a query using **regex operations on tsvector columns** that's performing very poorly:

```sql
SELECT DISTINCT ad_table_id, record_id, 
  (CASE WHEN EXISTS (SELECT 1 FROM regexp_matches(idx_tsvector::text, E'\\yrosa\\y')) 
    THEN 0.5 
    ELSE CASE WHEN EXISTS (SELECT 1 FROM regexp_matches(idx_tsvector::text, E'\\yrosa\\y')) 
      THEN 1 
      ELSE 10 
    END 
  END * COALESCE(
    (SELECT (regexp_match(idx_tsvector::text, E'rosa[^'']*'':(\\d+)([A])'))[1]::int),
    (SELECT (regexp_match(idx_tsvector::text, E'rosa[^'']*'':(\\d+)([BCD])'))[1]::int),
    ...
  )) as rank
FROM idx_product_ts
WHERE idx_tsvector @@ (to_tsquery('simple', $1) || to_tsquery($2, $3))
  AND AD_CLIENT_ID IN (0,$4)
ORDER BY rank ASC
```

## Why It's Slow

### 1. Casting Destroys Index Usage
```sql
idx_tsvector::text
```
- Casting tsvector to text **completely bypasses the GIN index**
- Forces full table scan on every row
- PostgreSQL cannot use the carefully constructed full-text search index

### 2. Regex Called 8-10+ Times Per Row
- `regexp_match()` and `regexp_matches()` are CPU-intensive
- Each function call processes the entire tsvector text
- Your query executes these operations multiple times per row

### 3. Duplicate Expressions
```sql
CASE WHEN EXISTS (SELECT 1 FROM regexp_matches(..., E'\\yrosa\\y')) THEN 0.5
ELSE CASE WHEN EXISTS (SELECT 1 FROM regexp_matches(..., E'\\yrosa\\y')) THEN 1
```
- Same regex pattern evaluated twice
- Wasteful computation

### 4. DISTINCT + ORDER BY Overhead
- Forces PostgreSQL to materialize all results
- Adds sorting/deduplication overhead
- Multiplies the cost of all regex operations

---

## Your Table Structure

```sql
CREATE TABLE adempiere.idx_product_ts (
  ad_client_id numeric(10) NOT NULL,
  ad_table_id numeric(10) NOT NULL,
  record_id numeric(10) NOT NULL,
  idx_tsvector tsvector NULL,
  CONSTRAINT idx_salesorder_unique UNIQUE (ad_table_id, record_id)
);

CREATE INDEX idx_product_ts_ad_client_id_idx 
  ON adempiere.idx_product_ts USING btree (ad_client_id, ad_table_id);

CREATE INDEX idx_product_ts_idx 
  ON adempiere.idx_product_ts USING gin (idx_tsvector);
```

**Good news**: You have the right index (GIN on idx_tsvector)  
**Bad news**: Your query doesn't use it effectively

---

## Solution 1: Optimized Regex Version (Short-Term Fix)

Use CTEs to compute regex **once per row** instead of 10+ times:

```sql
WITH filtered AS (
  -- Step 1: Use GIN index to filter rows first
  SELECT 
    ad_table_id,
    record_id,
    idx_tsvector::text AS txt
  FROM adempiere.idx_product_ts
  WHERE idx_tsvector @@ (
      to_tsquery('simple', $1) 
      || to_tsquery($2, $3)
    )
    AND ad_client_id IN (0, $4)
),
parsed AS (
  -- Step 2: Run regex only once per row
  SELECT
    ad_table_id,
    record_id,
    txt,
    
    /* Detect rosa token only once */
    (regexp_match(txt, E'\\yrosa\\y')) IS NOT NULL AS has_rosa,

    /* Extract number+class only once */
    regexp_match(txt, E'rosa[^'']*:([0-9]+)([A])')  AS m_a,
    regexp_match(txt, E'rosa[^'']*:([0-9]+)([BCD])') AS m_bcd
  FROM filtered
)
SELECT DISTINCT
  ad_table_id,
  record_id,
  
  /* Compute rank from pre-computed values */
  (
    CASE 
      WHEN has_rosa THEN 0.5
      WHEN m_a IS NOT NULL THEN 1
      ELSE 10
    END
    *
    COALESCE(
      (m_a)[1]::int,
      (m_bcd)[1]::int,
      1000
    )
  ) AS rank
FROM parsed
ORDER BY rank ASC;
```

### Performance Improvements

| Problem | Optimized Fix |
|---------|---------------|
| Regex called 10+ times per row | Regex called max 2× per row |
| Regex run on full table | GIN index filters first |
| Duplicate expressions | Computed once, reused |
| Casting everywhere | Cast only once in filtered rows |

**Expected speedup**: 5-10× faster (still not ideal, but much better)

---

## Solution 2: Proper FTS Design (Long-Term Fix)

### The Root Issue

You appear to be encoding metadata inside tsvector tokens like:
```
rosa:12A
rosa:30C
```

Then using regex to extract:
- Token name (`rosa`)
- Numeric weight (`12`, `30`)
- Classification (`A`, `B`, `C`, `D`)

**This is a design smell.** Regex on tsvector text defeats the purpose of full-text search.

### Proper Approach: Store Structured Tokens

Instead of encoding `rosa:12A` as a single opaque token, store multiple **searchable lexemes**:

```sql
-- Instead of this:
'rosa:12A'

-- Store these separate tokens:
'rosa'
'rosa_num_12'
'rosa_class_a'
```

### Example Redesign

```sql
-- When inserting/updating, create structured tokens
UPDATE idx_product_ts
SET idx_tsvector = 
  setweight(to_tsvector('simple', 'rosa'), 'A') ||
  setweight(to_tsvector('simple', 'rosa_num_12'), 'B') ||
  setweight(to_tsvector('simple', 'rosa_class_a'), 'C');
```

### Query Becomes Simple and Fast

```sql
SELECT 
  ad_table_id,
  record_id,
  ts_rank(idx_tsvector, to_tsquery('simple', 'rosa')) AS rank
FROM adempiere.idx_product_ts
WHERE idx_tsvector @@ to_tsquery('simple', 'rosa')
  AND ad_client_id IN (0, $4)
ORDER BY rank ASC;
```

**Benefits**:
- ✅ Uses GIN index fully
- ✅ No regex needed
- ✅ 100× faster
- ✅ Scalable to millions of rows

---

## Alternative Approaches

### Option A: Separate Metadata Table
```sql
CREATE TABLE product_attributes (
  ad_table_id numeric(10),
  record_id numeric(10),
  token_name text,
  token_weight int,
  token_class char(1),
  PRIMARY KEY (ad_table_id, record_id, token_name)
);

CREATE INDEX ON product_attributes (token_name, token_class, token_weight);
```

### Option B: JSONB for Metadata
```sql
ALTER TABLE idx_product_ts 
ADD COLUMN attrs jsonb;

CREATE INDEX ON idx_product_ts USING gin (attrs);

-- Store as:
{"rosa": {"weight": 12, "class": "A"}}
```

### Option C: Built-in tsvector Positions and Weights
PostgreSQL tsvector already supports weights (A, B, C, D):

```sql
-- Use setweight() to encode classification
setweight(to_tsvector('simple', 'rosa'), 'A')  -- highest priority
setweight(to_tsvector('simple', 'other'), 'D') -- lowest priority
```

Then use `ts_rank()` with weight arrays:

```sql
ts_rank(array[0.1, 0.2, 0.4, 1.0], idx_tsvector, query)
```

---

## Next Steps

To provide a complete redesign, I need to understand:

1. **Sample data**: What does a typical `idx_tsvector` value look like?
   ```sql
   SELECT idx_tsvector::text 
   FROM idx_product_ts 
   LIMIT 5;
   ```

2. **Business logic**: What does `rosa:12A` actually mean?
   - `rosa` = product name/code?
   - `12` = weight/priority?
   - `A` = quality/classification?

3. **Constraints**: 
   - Is this legacy data you must support?
   - Can you modify how `idx_tsvector` is populated?
   - Are there other ranking factors beyond this example?

---

## Summary

| Approach | Speed | Complexity | Recommendation |
|----------|-------|------------|----------------|
| **Current query** | ❌ Very slow | High | Don't use in production |
| **Optimized CTE** | ⚠️ Acceptable | Medium | Short-term fix only |
| **Proper FTS design** | ✅ Very fast | Low | Long-term solution |

**Bottom line**: 
- Current query won't scale beyond a few thousand rows
- Optimized CTE version is a bandaid that buys time
- Real fix requires redesigning how you store searchable attributes
- With proper design, this becomes a simple, fast, index-friendly query

---

## Resources

- [PostgreSQL Full-Text Search Documentation](https://www.postgresql.org/docs/current/textsearch.html)
- [GIN Index Performance](https://www.postgresql.org/docs/current/gin-intro.html)
- [ts_rank() Function](https://www.postgresql.org/docs/current/textsearch-controls.html#TEXTSEARCH-RANKING)

---

*Generated: 2025-12-12*
