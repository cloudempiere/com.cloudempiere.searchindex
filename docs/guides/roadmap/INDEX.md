# Roadmap & Future Enhancements

This section contains implementation roadmaps, next steps, and future enhancement strategies for the search index module.

## Overview

The search index module has a clear roadmap for immediate fixes (performance) and future enhancements (AI, semantic search, etc.).

## Guides

### 1. [Next Steps](./next-steps.md)
**Immediate Implementation Roadmap (2 Weeks)**

- What we discovered (Slovak language root cause)
- The solution (Slovak text search config + multi-weight indexing)
- 5-phase implementation plan:
  1. Database setup (1 day)
  2. Code changes (2-3 days)
  3. Reindexing (1 day)
  4. Testing (2-3 days)
  5. Rollout (1 day)
- Expected results (100× faster)
- Quick win option (TS_RANK switch)

**Read this** for immediate next steps.

### 2. [AI-Enhanced Search Strategy](./ai-enhanced-search.md)
**Future Enhancement Roadmap (2025-2026)**

- Semantic/Vector search (pgvector)
- Faceted search & filtering
- AI-powered query understanding
- Spell correction & suggestions
- Search analytics & Learning to Rank
- Conversational search (AI agent)
- Image-based search (multimodal)
- Business impact analysis
- Phased implementation plan

**Read this** for long-term vision.

## Related ADRs

- [ADR-003: Slovak Text Search Configuration](../../adr/adr-003-slovak-text-search-configuration.md) - Immediate priority
- [ADR-005: SearchType Migration](../../adr/adr-005-searchtype-migration.md) - Quick win (1 hour)
- [ADR-007: Search Technology Selection](../../adr/adr-007-search-technology-selection.md) - Technology foundation

## Implementation Timeline

### Phase 1: Quick Performance Fix (1 Week)
- ✅ **Goal**: 100× faster search immediately
- 🎯 **Action**: Change SearchType.POSITION → TS_RANK
- 📚 **Guide**: [Performance Guides](../performance/position-vs-tsrank.md)

### Phase 2: Slovak Language Support (2 Weeks)
- ✅ **Goal**: Proper Slovak language ranking
- 🎯 **Action**: Implement Slovak text search configuration
- 📚 **Guide**: [Next Steps](./next-steps.md)

### Phase 3: Service Layer Architecture (3-4 Weeks)
- ✅ **Goal**: Clean REST API integration
- 🎯 **Action**: Implement ISearchIndexService
- 📚 **Guide**: [Integration Guides](../integration/) (coming soon)

### Phase 4: AI Enhancements (Q2-Q4 2025)
- ✅ **Goal**: Next-level search experience
- 🎯 **Action**: Semantic search, spell correction, etc.
- 📚 **Guide**: [AI-Enhanced Search](./ai-enhanced-search.md)

## Navigation

- [← Back to Guides](../)
- [→ Testing Guides](../testing/)
