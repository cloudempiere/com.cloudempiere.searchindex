# ADR-011: Lazy Initialization for SearchIndexEventHandler

**Status:** Accepted
**Date:** 2024-12-24
**Issue:** N/A (startup failure during Eclipse/iDempiere initialization)
**Deciders:** CloudEmpiere Development Team

## Context

### Background

The `SearchIndexEventHandler` OSGi Declarative Services component was failing during Eclipse/iDempiere startup with a `NullPointerException`:

```
SEVERE: No Database Connection
java.lang.NullPointerException
    at org.compiere.db.PreparedStatementProxy.<init>(PreparedStatementProxy.java:41)
    at com.cloudempiere.searchindex.util.SearchIndexUtils.getSearchIndexConfigs(SearchIndexUtils.java:170)
    at com.cloudempiere.searchindex.event.SearchIndexEventHandler.initialize(SearchIndexEventHandler.java:69)
    at org.adempiere.base.event.AbstractEventHandler.bindEventManager(AbstractEventHandler.java:58)
```

The root cause:
1. OSGi SCR activates `SearchIndexEventHandler` when `IEventManager` service becomes available
2. The `bindEventManager()` method calls `initialize()` which immediately queries the database
3. The database connection pool may not be initialized yet at this point in startup
4. This causes a `NullPointerException` when trying to execute SQL

### Requirements

- Event handler must start without errors during OSGi startup
- Event handler must function correctly once database is available
- Solution must work across all environments: Eclipse IDE, server, Docker, cloud
- Must follow iDempiere established patterns

## Decision Drivers

- **Reliability:** Must not fail during startup regardless of timing
- **Compatibility:** Must work in all deployment scenarios
- **Maintainability:** Should follow existing iDempiere patterns
- **Simplicity:** Avoid complex configuration or external dependencies

## Considered Options

### Option 1: Lazy Initialization with DB.isConnected() Check

**Description:** Defer database queries until `DB.isConnected()` returns true. Register static table events in `initialize()`, and dynamically register indexed tables on first event when DB is ready.

**Pros:**
- Follows established iDempiere pattern (used in `Msg.java`, `Language.java`, `EMail.java`, `Env.java`)
- Self-healing: automatically recovers when DB becomes available
- Works in all environments without configuration changes
- Thread-safe with double-checked locking

**Cons:**
- Slight delay on first event after DB is ready
- Adds complexity to event handler code

**Cost/Effort:** Low

### Option 2: OSGi Service Dependency on AdempiereDatabase

**Description:** Add `@Reference` to `AdempiereDatabase` service to ensure component only activates when database service is available.

**Pros:**
- Uses OSGi dependency injection
- Explicit service dependency

**Cons:**
- Database service availability doesn't guarantee connection pool is ready
- Would not solve the root cause
- Requires MANIFEST.MF changes

**Cost/Effort:** Low

### Option 3: OSGi Start Level Configuration

**Description:** Configure bundle start levels to ensure searchindex bundle starts after database bundle.

**Pros:**
- Standard OSGi approach for bundle ordering

**Cons:**
- Start levels only control bundle start order, not DS component activation
- DS components activate when service references are satisfied, not when bundle starts
- Harder to maintain across deployments
- Environment-specific configuration required

**Cost/Effort:** Medium

### Option 4: Server Start Event Listener

**Description:** Listen for iDempiere server start event and initialize tables then.

**Pros:**
- Clean separation of concerns
- Guaranteed platform is fully ready

**Cons:**
- May not fire in Eclipse IDE development mode
- Only works in server context
- Would still need fallback for non-server contexts
- More complex event wiring

**Cost/Effort:** Medium

## Decision

We will pursue **Option 1: Lazy Initialization with DB.isConnected() Check** because it follows established iDempiere patterns, is self-healing, and works in all environments without configuration changes.

### Rationale

1. **Established Pattern:** iDempiere core uses this pattern extensively:
   - `Msg.java:161` - `if (!DB.isConnected()) return null;`
   - `Language.java:118,135` - Defers DB operations until connected
   - `EMail.java:220,441,460` - Guards DB calls with connection check
   - `Env.java:199` - Conditional DB access

2. **Self-Healing:** The lazy initialization automatically retries on subsequent events, so no manual intervention is needed once DB is ready.

3. **Universal Compatibility:** Works in Eclipse, Maven builds, Docker containers, and cloud deployments without any configuration changes.

4. **Thread Safety:** Implementation uses double-checked locking with volatile flag for safe concurrent access.

## Consequences

### Positive

- Startup no longer fails with NullPointerException
- Works across all deployment environments
- Self-healing behavior when DB becomes available
- Follows iDempiere coding conventions
- No external configuration required

### Negative

- Slight delay on first event after DB becomes ready (one-time cost)
- Dynamic table registration happens on first event rather than at startup

### Neutral

- Log message indicates when tables are registered
- Code is slightly more complex but follows established patterns

## Implementation

### Tasks

- [x] Add `tablesRegistered` volatile flag
- [x] Create `ensureTablesRegistered()` method with DB.isConnected() check
- [x] Move DB query from `initialize()` to `ensureTablesRegistered()`
- [x] Call `ensureTablesRegistered()` at start of `doHandleEvent()`
- [x] Add null-safe handling for `indexedTablesByClient`
- [x] Reset flag in `handleSearchIndexConfigChange()` for config refresh

### Timeline

- Implemented: 2024-12-24 (single session fix)

### Dependencies

- None - uses existing iDempiere infrastructure

### Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| First event delayed | Low | Medium | Acceptable one-time cost |
| DB never connects | High | Very Low | Standard iDempiere behavior, not specific to this fix |

## Related

- **Related to:** ADR-001 (Transaction Isolation)
- **Files Modified:** `SearchIndexEventHandler.java`

## References

- iDempiere Core Pattern: `org.compiere.util.Msg` line 161
- iDempiere Core Pattern: `org.compiere.util.Language` lines 118, 135
- iDempiere Core Pattern: `org.compiere.util.EMail` lines 220, 441, 460
- iDempiere Core Pattern: `org.compiere.util.Env` line 199
- OSGi Declarative Services: https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html

---

**Last Updated:** 2024-12-24
**Review Date:** 2025-06-24 (6 months from decision date)
