# Next Steps: Slovak Language Search Implementation

**Date**: 2025-12-12
**Priority**: CRITICAL
**Estimated Effort**: 2 weeks

---

## 🎯 What We Discovered

### The Real Problem

Your performance issue is **NOT** an architectural flaw - it's a **missing PostgreSQL configuration**!

**Root Cause**:
- Slovak language requires diacritic-aware search (č, š, ž, á, í, ó, ú, etc.)
- PostgreSQL doesn't have built-in Slovak text search configuration
- POSITION search type was a **workaround** using regex to rank exact diacritic matches higher
- Regex on `idx_tsvector::text` bypasses GIN index → 100× slower

**Evidence**:
1. Original POC (line 128): "Need to implement special characters like Slovak 'č', 'š'"
2. Slovak FTS expert article (linuxos.sk): Shows proper solution using custom configs
3. Code analysis: POSITION search checks for exact vs unaccented matches via regex

### Why It Was Fast, Then Slow

**Timeline**:
1. **Initially**: Simple `to_tsvector('simple', text)` - fast but no diacritic ranking
2. **"Fixed search cases"**: Added POSITION search with regex for Slovak diacritics
3. **Performance degraded**: Regex runs on every row (O(n × 6t) instead of O(log n))

---

## ✅ The Solution

### Create Slovak Text Search Configuration

Instead of regex workaround, use PostgreSQL's native multi-weight indexing:

```sql
-- 1. Create Slovak configuration
CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);
ALTER TEXT SEARCH CONFIGURATION sk_unaccent
  ALTER MAPPING FOR word, asciiword
  WITH unaccent, simple;

-- 2. Multi-weight indexing
setweight(to_tsvector('simple', 'ruža'), 'A')       -- Exact Slovak: highest rank
|| setweight(to_tsvector('sk_unaccent', 'ruža'), 'B')  -- Normalized: medium rank
|| setweight(to_tsvector('simple', unaccent('ruža')), 'C')  -- Unaccented: fallback

-- 3. Search with native ts_rank (NO REGEX!)
SELECT *, ts_rank(array[1.0, 0.7, 0.4, 0.2], idx_tsvector, query) AS rank
FROM idx_product_ts
WHERE idx_tsvector @@ to_tsquery('sk_unaccent', 'ruža')
ORDER BY rank DESC;
```

**Benefits**:
- ✅ 100× faster (uses GIN index)
- ✅ Slovak diacritics rank correctly
- ✅ Supports Czech, Polish, Hungarian too
- ✅ No regex needed
- ✅ Scales to millions of rows

---

## 🚀 Implementation Plan

### Phase 1: Database Setup (1 day)

**File**: Create migration script `migration/V1.0__Slovak_Text_Search.sql`

```sql
-- Create configurations for Central European languages
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);
ALTER TEXT SEARCH CONFIGURATION sk_unaccent
  ALTER MAPPING FOR asciiword, asciihword, hword_asciipart, word, hword, hword_part
  WITH unaccent, simple;

-- Repeat for Czech, Polish, Hungarian
CREATE TEXT SEARCH CONFIGURATION cs_unaccent (COPY = sk_unaccent);
CREATE TEXT SEARCH CONFIGURATION pl_unaccent (COPY = sk_unaccent);
CREATE TEXT SEARCH CONFIGURATION hu_unaccent (COPY = sk_unaccent);
```

**Test**:
```sql
-- Should normalize diacritics
SELECT to_tsvector('sk_unaccent', 'červená ruža');
SELECT to_tsquery('sk_unaccent', 'cervena & ruza');
```

### Phase 2: Code Changes (2-3 days)

**File**: `PGTextSearchIndexProvider.java`

**Change 1**: Update `getTSConfig()` (Line 94-95)
```java
private String getTSConfig(Properties ctx, String trxName) {
  String language = Env.getAD_Language(ctx);

  switch (language) {
    case "sk_SK": return "sk_unaccent";
    case "cs_CZ": return "cs_unaccent";
    case "pl_PL": return "pl_unaccent";
    case "hu_HU": return "hu_unaccent";
    default: return "simple";
  }
}
```

**Change 2**: Multi-weight indexing in `documentContentToTsvector()` (Lines 426-454)
```java
// Weight A: Exact match
documentContent.append("setweight(to_tsvector('simple', ?::text), 'A') || ");
params.add(value);

// Weight B: Language-normalized
documentContent.append("setweight(to_tsvector('").append(tsConfig).append("', ?::text), 'B') || ");
params.add(value);

// Weight C: Fully unaccented
documentContent.append("setweight(to_tsvector('simple', unaccent(?::text)), 'C') || ");
params.add(value);
```

**Change 3**: DELETE POSITION search type (Lines 670-715) - **REMOVE ENTIRELY**

**Change 4**: Update TS_RANK with weight array (Lines 657-669)
```java
case TS_RANK:
  rankSql.append("ts_rank(")
         .append("array[1.0, 0.7, 0.4, 0.2], ")  // A, B, C, D weights
         .append("idx_tsvector, ")
         .append("to_tsquery(?::regconfig, ?::text))");
  params.add(tsConfig);
  params.add(sanitizedQuery);
  break;
```

### Phase 3: Reindexing (1 day)

1. Run `CreateSearchIndex` process for all indexes
2. Monitor progress and index sizes
3. Verify GIN indexes with `EXPLAIN ANALYZE`

### Phase 4: Testing (2-3 days)

**Test Cases**:

1. **Slovak Exact Match**:
   ```
   Query: "ruža"
   Expected: Products with "ruža" rank #1
   ```

2. **Slovak Unaccented**:
   ```
   Query: "ruza"
   Expected: Still finds "ruža", ranks slightly lower
   ```

3. **Czech Variant**:
   ```
   Query: "růže"
   Expected: Finds both Czech and Slovak roses
   ```

4. **Performance Benchmark**:
   ```sql
   EXPLAIN ANALYZE
   SELECT * FROM idx_product_ts
   WHERE idx_tsvector @@ to_tsquery('sk_unaccent', 'červená & ruža')
   ORDER BY ts_rank(array[1.0, 0.7, 0.4, 0.2], idx_tsvector, query) DESC;

   Expected: Index Scan using GIN, <100ms for 10K rows
   ```

### Phase 5: Rollout (1 day)

1. Deploy to staging
2. User acceptance testing with real Slovak products
3. Production deployment
4. Monitor metrics

---

## 📊 Expected Results

### Performance Improvement

| Scenario | Current (POSITION) | After (TS_RANK) | Improvement |
|----------|-------------------|-----------------|-------------|
| 1,000 rows | 500ms | 5ms | **100×** |
| 10,000 rows | 5,000ms | 50ms | **100×** |
| 100,000 rows | 50,000ms (unusable) | 100ms | **500×** |

### Search Quality

| Query Type | Current | After | Notes |
|------------|---------|-------|-------|
| Slovak exact ("ruža") | ✅ Works | ✅ Better ranking | Weight A = highest |
| Unaccented ("ruza") | ✅ Works | ✅ Works | Weight C = fallback |
| Czech variant ("růže") | ⚠️ Lower rank | ✅ Good rank | Weight B = medium |
| Typo ("ruzha") | ❌ No results | ⚠️ Fuzzy possible | Future enhancement |

---

## 🎓 Lessons Learned

### What Went Wrong

1. **Missing PostgreSQL expertise**: Didn't know about custom text search configs
2. **Workaround culture**: Used regex instead of researching proper solution
3. **No performance baseline**: Didn't measure before/after when adding POSITION
4. **Inadequate documentation**: Slovak requirements not documented in code

### What Went Right

1. **Plugin architecture**: Easy to add new providers/configurations
2. **Event-driven sync**: Real-time indexing works well
3. **Configuration flexibility**: AD_SearchIndex tables allow customization
4. **Code organization**: Clear separation makes fixes easier

### Best Practices Going Forward

1. **Document language requirements** in CLAUDE.md and code comments
2. **Performance benchmarks** before/after architectural changes
3. **Use PostgreSQL native features** before implementing workarounds
4. **Consult language-specific FTS resources** (like linuxos.sk article)
5. **Test with production data** from the beginning

---

## 📚 Resources Created

1. **`docs/slovak-language-architecture.md`** - Complete root cause analysis and solution
2. **`docs/postgres-fts-performance-recap.md`** - Performance analysis (existing)
3. **`CLAUDE.md`** - Updated with Slovak language context
4. **`.claude/agents`** - Symlinked to cloudempiere-workspace (for future development)
5. **`.claude/commands`** - Symlinked to cloudempiere-workspace

---

## 🤝 Next Actions

**For You**:
1. Review `docs/slovak-language-architecture.md`
2. Decide on timeline (recommended: 2 weeks)
3. Approve database migration approach
4. Provide test data (Slovak product names, descriptions)

**For Development**:
1. Create migration script for Slovak text search config
2. Implement code changes in `PGTextSearchIndexProvider.java`
3. Write unit tests for Slovak language scenarios
4. Performance testing with real data
5. Staging deployment

**For Future**:
1. Consider ispell dictionary for Slovak stemming (Phase 2)
2. Add synonym support for product search
3. Implement spell correction for typos
4. Vector search for semantic similarity (see architectural analysis)

---

## 💡 Quick Win Option

**If you need immediate relief**:

Simply switch from POSITION to TS_RANK in the UI:

**File**: `ZkSearchIndexUI.java:189`
```java
// Change this:
SearchType.POSITION

// To this:
SearchType.TS_RANK
```

**Impact**: 50-100× faster immediately, but loses Slovak diacritic ranking quality

**Then**: Implement proper Slovak config solution for both speed AND quality

---

**Questions? Need clarification on any step?**

I can help with:
- Writing the migration scripts
- Implementing the code changes
- Creating test cases
- Setting up benchmarks
- Reviewing before deployment
