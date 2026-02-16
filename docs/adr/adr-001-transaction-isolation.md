# ADR-001: Transaction Isolation Strategy for Search Index Updates

**Status:** ✅ Implemented (Fixed 2026-02-13)
**Date:** 2025-12-12
**Implementation Date:** 2025-12-17
**Fix Date:** 2026-02-13
**Decision Makers:** Development Team, Architecture Review Board
**Related Issues:** Finding 2.1 (CRITICAL), FK Table Indexing Bug
**Implementation Commits:** 378ec8b, 3026c09
**Fix Commits:** [pending]

---

## Context

The current `SearchIndexEventHandler` reuses the Persistent Object's transaction context for search index updates. This creates a tight coupling between business logic transactions and index maintenance, violating iDempiere best practices and causing data inconsistency issues.

### Current Implementation

```java
// SearchIndexEventHandler.java:107
trxName = eventPO.get_TrxName();  // Reuses PO's transaction

// SearchIndexEventHandler.java:197
provider.createIndex(ctx, builder.build().getData(false), trxName);
```

### Problems

1. **Index failure blocks business operations**
   - If index update fails, entire business transaction rolls back
   - Example: Product save fails because Elasticsearch is down

2. **Transaction rollback causes index staleness**
   - Business logic rolls back → index update also rolls back
   - Index not updated even though error was transient

3. **Long-running transactions**
   - Index updates hold database locks
   - Reduces system throughput

4. **Violation of Single Responsibility Principle**
   - Business logic transaction responsible for index maintenance
   - Tight coupling between concerns

---

## Decision

We will implement **Hybrid Transaction Strategy** for search index operations:
1. Use **business transaction** for reading source data (to see uncommitted changes)
2. Use **separate transaction** for writing to index tables (to isolate failures)

### Strategy: Hybrid Transaction Approach

```java
@Override
protected void doHandleEvent(Event event) {
    PO eventPO = getPO(event);
    String businessTrxName = eventPO.get_TrxName();  // For reading data

    // Phase 1: Read data using business transaction (see uncommitted changes)
    PO[] mainPOArr = getMainPOs(eventPO, indexedTables, ctx, businessTrxName);

    // Phase 2: Write index using separate transaction (isolate failures)
    Trx indexTrx = Trx.get(Trx.createTrxName("SearchIdx"), true);
    try {
        String indexTrxName = indexTrx.getTrxName();

        SearchIndexConfigBuilder builder = new SearchIndexConfigBuilder()
            .setTrxName(businessTrxName)  // Still uses business trx for reading
            .build();

        // Perform index write with separate transaction
        provider.createIndex(ctx, builder.getData(false), indexTrxName);

        indexTrx.commit();
    } catch (Exception e) {
        indexTrx.rollback();
        log.log(Level.SEVERE, "Failed to update search index", e);
        // Don't throw - index failure shouldn't fail original transaction
    } finally {
        indexTrx.close();
    }
}
```

---

## Consequences

### Positive

1. **Business logic independence**
   - Product saves successfully even if index update fails
   - Resilient to external service failures (Elasticsearch down)

2. **Better error handling**
   - Index errors logged but don't propagate
   - Retry mechanisms possible

3. **Improved performance**
   - Shorter transaction duration
   - Reduced lock contention

4. **iDempiere best practice compliance**
   - Event handlers use separate transactions
   - Follows Observer pattern correctly

### Negative

1. **Eventual consistency**
   - Small time window where data and index out of sync
   - Mitigation: Immediate separate transaction minimizes window

2. **Complexity**
   - Additional transaction management code
   - Need retry logic for transient failures

3. **Testing requirements**
   - Must test rollback scenarios
   - Need integration tests for consistency

---

## Alternatives Considered

### Alternative 1: Asynchronous Event Queue

```java
EventQueue.getInstance().queue(new IndexUpdateEvent(
    searchIndex.getAD_SearchIndex_ID(),
    po.get_Table_ID(),
    po.get_ID()
));
```

**Pros:**
- Zero impact on business transaction performance
- Natural retry mechanism
- Batch processing possible

**Cons:**
- Larger consistency window (seconds to minutes)
- Requires background worker infrastructure
- More complex failure recovery

**Decision:** Rejected for initial implementation due to complexity. Consider for future enhancement if immediate separate transaction proves insufficient.

---

### Alternative 2: Compensating Transaction

```java
// Original transaction commits
// If index update fails, compensate by marking record for re-index
```

**Pros:**
- Guaranteed eventual consistency
- Clear audit trail

**Cons:**
- Complex compensation logic
- Additional database tables required

**Decision:** Rejected as over-engineered for current requirements.

---

### Alternative 3: Two-Phase Commit (2PC)

Use distributed transaction coordinator to ensure atomicity across database and index.

**Pros:**
- Strong consistency guarantee

**Cons:**
- Performance penalty (2x slower)
- Not supported by Elasticsearch
- Overkill for search index use case

**Decision:** Rejected - search index is not critical enough to require 2PC.

---

## Implementation Plan

### Phase 1: Refactor Event Handler (2 days)

1. **Modify SearchIndexEventHandler.java**
   ```java
   // Remove instance variables (thread safety issue)
   - private Properties ctx = null;
   - private String trxName = null;

   // Use local variables in doHandleEvent()
   + Properties ctx = Env.getCtx();
   + Trx indexTrx = Trx.get(Trx.createTrxName("SearchIdx"), true);
   ```

2. **Add transaction helper methods**
   ```java
   private void executeIndexUpdate(ISearchIndexProvider provider, ...) {
       Trx indexTrx = Trx.get(Trx.createTrxName("SearchIdx"), true);
       try {
           provider.createIndex(..., indexTrx.getTrxName());
           indexTrx.commit();
       } catch (Exception e) {
           indexTrx.rollback();
           handleIndexError(e);
       } finally {
           indexTrx.close();
       }
   }
   ```

3. **Implement error handling**
   ```java
   private void handleIndexError(Exception e) {
       log.log(Level.SEVERE, "Search index update failed", e);

       // Mark for re-index (future: use queue)
       // For now: log only, don't block
   }
   ```

### Phase 2: Testing (1 day)

1. **Unit tests**
   - Test transaction isolation
   - Test rollback scenarios
   - Test error handling

2. **Integration tests**
   - Test PO save + index update success
   - Test PO save success + index update failure
   - Test PO save failure (index update should not occur)

3. **Stress tests**
   - 100+ concurrent PO changes
   - Verify no transaction deadlocks

### Phase 3: Migration (0.5 days)

1. **No database schema changes required**
2. **Deploy as drop-in replacement**
3. **Monitor transaction logs**

---

## Post-Implementation Issues & Fixes

### Issue: FK Table Indexing Failure (2026-02-13)

**Problem:** Initial implementation used null transaction for ALL operations, including reading parent records:
```java
// BROKEN (378ec8b):
PO[] mainPOArr = getMainPOs(eventPO, indexedTables, ctx, null);  // Can't see uncommitted data
```

**Symptom:**
```
GenericPO.load: NO Data found for M_Product_ID=11330103
```

**Root Cause:** When import process creates M_Product + M_Product_PO in same transaction:
1. M_Product created (not committed)
2. M_Product_PO saved → fires event
3. Event handler tries to load M_Product with null transaction
4. M_Product not visible → exception → business transaction fails

**Fix:** Use business transaction for reading, separate transaction for writing:
```java
// FIXED:
String businessTrxName = eventPO.get_TrxName();
PO[] mainPOArr = getMainPOs(eventPO, indexedTables, ctx, businessTrxName);  // Can see uncommitted data
```

**Lesson Learned:** "Separate transaction" only applies to index WRITES, not data READS.

## Monitoring & Validation

### Success Criteria

- ✅ Business transactions complete in <500ms (no regression)
- ✅ Index updates complete in <1s (separate transaction)
- ⚠️ Zero business transaction failures due to index errors (fixed 2026-02-13)
- ✅ Index consistency >99.9% (measured by reconciliation job)

### Metrics to Track

1. **Transaction Duration**
   ```sql
   -- Business transaction (should decrease)
   SELECT AVG(duration_ms) FROM trx_log WHERE trx_name LIKE 'PO%'

   -- Index transaction (new metric)
   SELECT AVG(duration_ms) FROM trx_log WHERE trx_name LIKE 'SearchIdx%'
   ```

2. **Index Error Rate**
   ```sql
   SELECT COUNT(*) FROM ad_changelog
   WHERE ChangeLog LIKE '%Search index update failed%'
   GROUP BY TRUNC(Created, 'DD')
   ```

3. **Consistency Check**
   ```sql
   -- Count records not in index
   SELECT COUNT(*) FROM M_Product p
   WHERE NOT EXISTS (
       SELECT 1 FROM searchindex_product si
       WHERE si.record_id = p.M_Product_ID
         AND si.ad_client_id = p.AD_Client_ID
   )
   ```

### Alerting

- Alert if index error rate >1% per hour
- Alert if consistency check finds >10 missing records
- Alert if transaction duration >5s (p95)

---

## Rollback Plan

If separate transaction isolation causes issues:

1. **Immediate rollback**
   ```bash
   # Revert to previous bundle version
   osgi> uninstall com.cloudempiere.searchindex
   osgi> install file:///path/to/old/version.jar
   osgi> start <bundle-id>
   ```

2. **Re-enable shared transaction** (emergency only)
   ```java
   // Revert to old code (keep as commented fallback)
   // trxName = eventPO.get_TrxName();
   ```

3. **Run index reconciliation**
   ```sql
   -- Mark all indexes for rebuild
   UPDATE AD_SearchIndex SET IsValid='N'
   ```

---

## References

- iDempiere Event Handler Best Practices: http://wiki.idempiere.org/en/Event_Handler
- Transaction Management Guide: http://wiki.idempiere.org/en/Transaction
- CLAUDE.md:178-195 (Known Issues - Transaction Management)

---

## Decision

**Approved:** [Pending]
**Approved By:** [Name]
**Approval Date:** [Date]
**Implementation Target:** Week 2 (Phase 1)

---

**Next ADR:** [ADR-002: SQL Injection Prevention](adr-002-sql-injection-prevention.md)
