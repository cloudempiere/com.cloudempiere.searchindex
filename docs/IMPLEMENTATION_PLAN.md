# Implementation Plan: com.cloudempiere.searchindex

**Planning Date:** 2025-12-13
**Owner:** DevOps Team
**Status:** Template - Requires Completion

---

## Overview

This implementation plan outlines the roadmap for addressing critical issues and implementing planned enhancements for the com.cloudempiere.searchindex plugin.

**Planning Horizon:** Q4 2025 - Q2 2026

---

## Phase 1: Critical Fixes (Sprint 1-2, Weeks 1-2)

**Objective:** Address critical performance and data integrity issues

### 1.1 Performance: Migrate from POSITION to TS_RANK

**Issue:** ADR-005, docs/postgres-fts-performance-recap.md
**Impact:** 100× performance improvement
**Effort:** 2 days
**Risk:** Low

#### Tasks

- [ ] Update `ZkSearchIndexUI.java:189` to use `SearchType.TS_RANK`
- [ ] Update `DefaultQueryConverter.java:689` in cloudempiere-rest repo
- [ ] Update `ProductAttributeQueryConverter.java:505` in cloudempiere-rest repo
- [ ] Test search results quality (Slovak language)
- [ ] Performance benchmark (before/after)
- [ ] Update documentation

#### Success Criteria

- Average search time <10ms (currently ~500ms)
- 99th percentile <50ms (currently ~5s)
- Slovak language search quality maintained or improved

#### Dependencies

- None

#### Risks

| Risk | Mitigation |
|------|------------|
| Slovak language quality degradation | Keep POSITION as fallback option |
| Breaking changes in REST API | Version API endpoints |

---

### 1.2 Data Integrity: Multi-Tenant Unique Constraint

**Issue:** ADR-006
**Impact:** Prevents cross-tenant data corruption
**Effort:** 1-2 days
**Risk:** Medium (database migration)

#### Tasks

- [ ] Write database migration script (PostgreSQL)
- [ ] Update unique constraint: `(ad_table_id, record_id)` → `(ad_client_id, ad_table_id, record_id)`
- [ ] Test in dev environment
- [ ] Test multi-tenant scenarios
- [ ] Deploy to staging
- [ ] Validate data integrity
- [ ] Deploy to production

#### Success Criteria

- Zero cross-tenant index overwrites
- All existing indexes preserved
- Performance maintained

#### Dependencies

- Database downtime window (estimated 5-10 minutes)

#### Risks

| Risk | Mitigation |
|------|------------|
| Migration failure | Backup before migration, rollback script ready |
| Index rebuild required | Schedule during low-traffic window |
| Duplicate detection | Pre-migration data cleanup |

---

### 1.3 Documentation: Repository Standardization

**Issue:** /init-repo standardize plugin
**Impact:** CloudEmpiere governance compliance
**Effort:** 1 day
**Risk:** Low

#### Tasks

- [x] Create CHANGELOG.md
- [x] Create FEATURES.md
- [x] Create governance templates (STRATEGIC_REVIEW, CHANGELOG_SIGNIFICANT, IMPLEMENTATION_PLAN)
- [x] Create ADR template (000-template.md)
- [ ] Replace .claude/ files with symlinks to cloudempiere-workspace
- [ ] Update CLAUDE.md with governance references
- [ ] Commit standardization changes

#### Success Criteria

- All governance files present
- .claude/ properly symlinked
- Documentation complete and accurate

#### Dependencies

- None

#### Risks

| Risk | Mitigation |
|------|------------|
| Symlink path issues | Use relative paths |
| Merge conflicts | Coordinate with ongoing work |

---

## Phase 2: Slovak Language Enhancement (Sprint 3-4, Weeks 3-4)

**Objective:** Implement proper Slovak language support, eliminate POSITION workaround

### 2.1 PostgreSQL Slovak Text Search Configuration

**Issue:** docs/slovak-language-architecture.md, docs/LOW-COST-SLOVAK-ECOMMERCE-SEARCH.md
**Impact:** €36,000 cost savings vs. Algolia
**Effort:** 3 days
**Risk:** Medium

#### Tasks

- [ ] Research PostgreSQL Slovak dictionary options
- [ ] Create `sk_unaccent` text search configuration
- [ ] Implement multi-weight indexing for Slovak
- [ ] Test with real Slovak product data
- [ ] Benchmark relevance and performance
- [ ] Document configuration procedure
- [ ] Create migration guide

#### Success Criteria

- Slovak diacritics handled correctly (č→c, š→s, ž→z, á→a)
- Search quality ≥95% (currently ~70%)
- Performance maintained or improved
- Zero-result rate <5% (currently ~15%)

#### Dependencies

- Phase 1.1 completed (TS_RANK migration)

#### Risks

| Risk | Mitigation |
|------|------------|
| Slovak dictionary not available | Create custom unaccent mapping |
| Quality degradation | A/B test with current implementation |
| Performance impact | Benchmark at each step |

---

### 2.2 Cache Invalidation System

**Issue:** SearchIndexConfigBuilder:256
**Impact:** Eliminates stale configuration issues
**Effort:** 2 days
**Risk:** Low

#### Tasks

- [ ] Design cache invalidation strategy
- [ ] Implement explicit `clearCache()` method
- [ ] Hook into AD_SearchIndex* table events
- [ ] Add JMX/admin console cache controls
- [ ] Test cache invalidation scenarios
- [ ] Document cache management

#### Success Criteria

- Configuration changes reflected immediately
- Cache hit rate maintained (85%+)
- No manual restarts required

#### Dependencies

- None

#### Risks

| Risk | Mitigation |
|------|------------|
| Over-invalidation (performance impact) | Granular invalidation per index |
| Under-invalidation (stale cache) | Conservative approach, invalidate on any change |

---

## Phase 3: Quality Improvements (Sprint 5-6, Weeks 5-6)

**Objective:** Fix known bugs and limitations

### 3.1 WHERE Clause Alias Bug Fix

**Issue:** SearchIndexEventHandler:268, 305
**Impact:** Improved query flexibility
**Effort:** 2 days
**Risk:** Low

#### Tasks

- [ ] Analyze WHERE clause parsing logic
- [ ] Implement "main" alias support
- [ ] Test with complex WHERE clauses
- [ ] Update documentation
- [ ] Create example configurations

#### Success Criteria

- "main" alias works in WHERE clauses
- No breaking changes to existing configurations
- Backward compatibility maintained

#### Dependencies

- None

#### Risks

| Risk | Mitigation |
|------|------------|
| Breaking existing WHERE clauses | Thorough testing, backward compatibility check |

---

### 3.2 REST API Search Type Configuration

**Issue:** docs/rest-api-searchindex-integration.md
**Impact:** API performance and flexibility
**Effort:** 1 day
**Risk:** Low

#### Tasks

- [ ] Add search type parameter to OData filter
- [ ] Update DefaultQueryConverter.java
- [ ] Update ProductAttributeQueryConverter.java
- [ ] Test REST API with both search types
- [ ] Update API documentation
- [ ] Version API (if breaking)

#### Success Criteria

- API users can choose TS_RANK or POSITION
- Default is TS_RANK
- Backward compatibility maintained

#### Dependencies

- Phase 1.1 completed

#### Risks

| Risk | Mitigation |
|------|------------|
| API breaking change | Version endpoints, deprecation notice |

---

## Phase 4: Future Enhancements (Q1-Q2 2026)

**Objective:** Long-term improvements and scalability

### 4.1 Elasticsearch Production Support

**Effort:** 2 weeks
**Risk:** High
**Priority:** Low (only needed for >1M records)

#### Tasks

- [ ] Complete ElasticSearchIndexProvider implementation
- [ ] Implement index creation and management
- [ ] Implement search query translation
- [ ] Add monitoring and health checks
- [ ] Performance benchmarking
- [ ] Production deployment guide

#### Success Criteria

- Feature parity with PostgreSQL provider
- Horizontal scalability proven
- Monitoring dashboards available

---

### 4.2 Multi-Level FK Traversal

**Effort:** 1 week
**Risk:** Medium
**Priority:** Medium

#### Tasks

- [ ] Design recursive FK traversal algorithm
- [ ] Implement depth-limited FK resolution
- [ ] Add configuration for max FK depth
- [ ] Performance optimization
- [ ] Test with complex data models

#### Success Criteria

- Support 2-3 levels of FK relationships
- Configurable depth limit
- Performance acceptable (<100ms overhead)

---

### 4.3 UI Search Type Toggle

**Effort:** 1 week
**Risk:** Low
**Priority:** Low

#### Tasks

- [ ] Add search type selector to ZkSearchIndexUI
- [ ] Implement user preference storage
- [ ] Add tooltips explaining TS_RANK vs POSITION
- [ ] Test UI workflow
- [ ] Update user documentation

#### Success Criteria

- Users can select search type
- Preference persists across sessions
- Clear explanation of trade-offs

---

## Timeline Overview

```
Q4 2025                          Q1 2026                          Q2 2026
|--------------------------------|--------------------------------|
Week 1-2: Phase 1 (Critical Fixes)
         |
         Sprint 1-2
                |
         Week 3-4: Phase 2 (Slovak Language)
                        |
                        Sprint 3-4
                                |
                         Week 5-6: Phase 3 (Quality)
                                        |
                                        Sprint 5-6
                                                |
                                         Weeks 7-14: Phase 4 (Future)
                                                        |
                                                        Ongoing
```

---

## Resource Allocation

### Development Team

| Phase | Developers | Effort | Duration |
|-------|-----------|--------|----------|
| Phase 1 | 1-2 | 4-5 days | 2 weeks |
| Phase 2 | 1 | 5 days | 2 weeks |
| Phase 3 | 1 | 3 days | 1 week |
| Phase 4 | 1-2 | 20 days | 8 weeks |

### Testing & QA

| Phase | QA Effort | Focus Areas |
|-------|-----------|-------------|
| Phase 1 | 2 days | Performance, data integrity |
| Phase 2 | 2 days | Slovak language quality |
| Phase 3 | 1 day | Regression testing |
| Phase 4 | 5 days | End-to-end, scalability |

---

## Success Metrics

### Phase 1 Metrics

| Metric | Baseline | Target | Timeline |
|--------|----------|--------|----------|
| Search latency (avg) | 500ms | <10ms | Week 2 |
| Cross-tenant corruption | Possible | Zero | Week 2 |
| Documentation coverage | 80% | 95% | Week 2 |

### Phase 2 Metrics

| Metric | Baseline | Target | Timeline |
|--------|----------|--------|----------|
| Slovak search quality | 70% | 95% | Week 4 |
| Zero-result rate | 15% | <5% | Week 4 |
| Cache invalidation time | Restart required | Immediate | Week 4 |

### Phase 3 Metrics

| Metric | Baseline | Target | Timeline |
|--------|----------|--------|----------|
| WHERE clause flexibility | Limited | Full support | Week 6 |
| REST API performance | Slow | Fast | Week 6 |

---

## Risk Management

### Overall Project Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Resource availability | High | Medium | Cross-train team members |
| Scope creep | Medium | High | Strict phase boundaries, defer to Phase 4 |
| Production issues | Critical | Low | Thorough testing, staged rollout |
| Slovak language complexity | Medium | Medium | Consult language experts, A/B testing |

---

## Dependencies

### External Dependencies

- **cloudempiere-rest repository:** REST API changes (Phase 1.1, 3.2)
- **PostgreSQL version:** Must support unaccent extension (Phase 2.1)
- **iDempiere platform:** Compatible with current version

### Internal Dependencies

- Phase 2 depends on Phase 1.1 (TS_RANK migration)
- Phase 3.2 depends on Phase 1.1
- Phase 4 can run in parallel with Phases 1-3

---

## Approval & Sign-Off

| Phase | Approver | Date | Status |
|-------|----------|------|--------|
| Phase 1 | TBD | - | Pending |
| Phase 2 | TBD | - | Pending |
| Phase 3 | TBD | - | Pending |
| Phase 4 | TBD | - | Pending |

---

## Notes

- This plan was generated during repository standardization on 2025-12-13
- Update this plan as work progresses
- Review and adjust timelines monthly
- Consider business priorities when scheduling phases

---

**Document Owner:** DevOps Team
**Last Updated:** 2025-12-13
**Next Review:** 2026-01-13
