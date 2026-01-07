# AI-Enhanced Search Strategy for iDempiere ERP (2025)

**Date:** 2025-12-12
**Version:** 1.0
**Status:** Strategic Analysis - Future Roadmap
**Context:** Cloudempiere Search Index Plugin Architecture

---

## Executive Summary

The Cloudempiere Search Index plugin's **provider architecture** (ISearchIndexProvider) positions it perfectly for **AI/ML integration** in the 2025 landscape. The existing OSGi plugin framework can be extended with modern AI search providers to deliver:

- **Semantic Search** - Understand intent, not just keywords
- **Vector Search** - Find conceptually similar products/documents
- **RAG Integration** - AI assistants grounded in ERP data
- **Natural Language Queries** - "Show me unpaid invoices from last quarter"
- **Multi-Modal Search** - Text + images + structured data
- **Personalized Results** - Context-aware ranking per user/role

**Key Insight:** The plugin's provider architecture (PostgreSQL, Elasticsearch stub) can be extended with **AI-powered providers** (pgvector, OpenAI embeddings, local LLMs) without changing core ERP integration.

**Strategic Value:**
- Transform ERP search from keyword matching to intelligent retrieval
- Enable AI copilots grounded in real business data (RAG)
- Competitive advantage: AI-powered ERP search in 2025
- Cost-effective: Leverage existing PostgreSQL + pgvector (no new infrastructure)

---

## Part 1: Current Architecture Readiness for AI

### Existing Provider Pattern (Perfect for AI Extension)

```java
// Current architecture
public interface ISearchIndexProvider {
    void createIndex(Properties ctx, SearchIndexData data, String trxName);
    void updateIndex(Properties ctx, SearchIndexData data, String trxName);
    List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName,
                                         String query, boolean isAdvanced,
                                         SearchType searchType, String orderBy);
    // ... other methods
}

// Current providers
- PGTextSearchIndexProvider    // PostgreSQL full-text search
- ElasticSearchIndexProvider   // Elasticsearch (stub)

// NEW AI-powered providers (2025+)
+ PGVectorSearchProvider       // PostgreSQL pgvector for embeddings
+ OpenAIEmbeddingProvider      // OpenAI embeddings API
+ LocalLLMProvider             // Self-hosted LLM (Ollama, llama.cpp)
+ HybridSearchProvider         // Combine keyword + semantic + vector
```

### Why This Architecture is AI-Ready

**1. Pluggable Providers**
- ✅ Can add new AI providers without modifying core plugin
- ✅ Each provider can use different AI technology (embeddings, LLMs, etc.)
- ✅ Providers can be combined (hybrid search)

**2. OSGi Service Registry**
- ✅ Dynamic provider discovery
- ✅ Hot-swapping providers at runtime
- ✅ A/B testing different AI models

**3. Multi-Tenant Isolation**
- ✅ Each client can use different AI provider
- ✅ Data isolation guaranteed (AD_Client_ID)
- ✅ Per-client AI model customization

**4. API Integration**
- ✅ REST API already supports search index
- ✅ Can expose AI search via OData endpoints
- ✅ Mobile apps can use AI-powered search

**5. Event-Driven Updates**
- ✅ Automatic embedding generation on data changes
- ✅ Real-time AI index updates
- ✅ No batch processing needed

---

## Part 2: AI/ML Integration Opportunities (2025 Paradigms)

### Opportunity 1: Vector Search / Semantic Similarity

**What It Is:**
Transform text into high-dimensional vectors (embeddings) that capture semantic meaning. Similar concepts cluster together in vector space.

**Example:**
```
Traditional keyword search:
  Query: "red rose"
  Finds: Products with exact words "red" and "rose"
  Misses: "crimson flower", "červená ruža" (Slovak)

Vector/semantic search:
  Query: "red rose"
  Embedding: [0.234, -0.891, 0.456, ...] (1536 dimensions)
  Finds: "crimson flower" (similar embedding)
        "červená ruža" (cross-language semantic match)
        "bouquet" (related concept)
```

**ERP Use Cases:**

1. **Product Discovery**
   ```
   User: "Something for Mother's Day"
   AI: Finds flowers, chocolates, gift cards (conceptually related)
   Not just: Products with "Mother's Day" in name
   ```

2. **Cross-Language Search**
   ```
   Query in English: "invoice"
   Finds: "faktúra" (Slovak), "Rechnung" (German)
   Automatic multilingual support
   ```

3. **Conceptual Parts Search**
   ```
   Engineer: "component that reduces vibration"
   AI: Finds dampers, shock absorbers, mounting brackets
   Not limited to: "vibration" in product description
   ```

**Technical Implementation:**

```java
// New provider: PGVectorSearchProvider
public class PGVectorSearchProvider implements ISearchIndexProvider {

    private OpenAIClient embeddingClient; // or local model

    @Override
    public void createIndex(Properties ctx, SearchIndexData data, String trxName) {
        // 1. Get text content
        String content = buildContentString(data);

        // 2. Generate embedding vector
        float[] embedding = embeddingClient.createEmbedding(content);
        // Example: [0.234, -0.891, 0.456, ...] (1536 dimensions for OpenAI)

        // 3. Store in PostgreSQL with pgvector extension
        String sql = "INSERT INTO searchindex_vector " +
                     "(ad_client_id, ad_table_id, record_id, embedding) " +
                     "VALUES (?, ?, ?, ?::vector)";
        PreparedStatement ps = DB.prepareStatement(sql, trxName);
        ps.setInt(1, data.getAD_Client_ID());
        ps.setInt(2, data.getAD_Table_ID());
        ps.setInt(3, data.getRecordID());
        ps.setObject(4, embedding); // pgvector type
        ps.executeUpdate();
    }

    @Override
    public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName,
                                                String query, boolean isAdvanced,
                                                SearchType searchType, String orderBy) {
        // 1. Convert query to embedding
        float[] queryEmbedding = embeddingClient.createEmbedding(query);

        // 2. Vector similarity search (cosine distance)
        String sql = "SELECT ad_table_id, record_id, " +
                     "       embedding <=> ?::vector AS distance " +
                     "FROM searchindex_vector " +
                     "WHERE ad_client_id = ? " +
                     "ORDER BY embedding <=> ?::vector " + // <=> is cosine distance
                     "LIMIT 10";

        // 3. Execute and return results
        // Results ranked by semantic similarity, not keyword match
    }
}
```

**Database Setup:**
```sql
-- Install pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create vector index table
CREATE TABLE searchindex_vector (
    ad_client_id INT NOT NULL,
    ad_table_id INT NOT NULL,
    record_id INT NOT NULL,
    embedding vector(1536), -- OpenAI ada-002 dimension
    metadata JSONB, -- Store additional context
    created TIMESTAMP DEFAULT NOW(),
    updated TIMESTAMP DEFAULT NOW(),
    CONSTRAINT pk_searchindex_vector PRIMARY KEY (ad_client_id, ad_table_id, record_id)
);

-- Create IVFFlat index for fast similarity search
CREATE INDEX idx_searchindex_vector_embedding
    ON searchindex_vector
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- For even faster search with HNSW (requires pgvector 0.5.0+)
CREATE INDEX idx_searchindex_vector_hnsw
    ON searchindex_vector
    USING hnsw (embedding vector_cosine_ops);
```

**Cost Analysis:**

| Approach | Infrastructure | Embeddings Cost | Total (1M products, 1K searches/day) |
|----------|---------------|-----------------|--------------------------------------|
| OpenAI API | PostgreSQL + pgvector | $0.0001/1K tokens | ~$50/month (embeddings) + $0 (search) |
| Local LLM (Ollama) | PostgreSQL + pgvector + 1 GPU server | $0 | €200/month (GPU server) |
| Elasticsearch + Vector | Elasticsearch cluster | $0 (local model) | €500/month (infrastructure) |

**Recommendation:** PostgreSQL + pgvector + OpenAI embeddings (cheapest, best quality)

---

### Opportunity 2: RAG (Retrieval Augmented Generation) for ERP

**What It Is:**
Combine AI search (retrieval) with large language models (generation) to create AI assistants that answer questions grounded in your ERP data.

**Example:**
```
User: "Why is customer ACME Corp's balance so high?"

Traditional ERP: User must navigate:
  1. Customer window
  2. Find ACME Corp
  3. Check invoices tab
  4. Check payments tab
  5. Manually calculate
  6. Analyze patterns

RAG-powered AI Assistant:
  1. Retrieves relevant data (via search index):
     - Recent invoices for ACME Corp
     - Payment history
     - Credit terms
  2. Generates natural language answer:
     "ACME Corp has a balance of €45,230 due to:
      - 3 overdue invoices (€38,500) from Q4 2024
      - Payment terms: Net 60 (currently 72 days overdue)
      - Historical payment behavior: Usually pays within 65-70 days
      Recommendation: Contact accounts receivable department"
```

**ERP Use Cases:**

1. **Business Intelligence Assistant**
   ```
   User: "Which products are underperforming this quarter?"

   RAG flow:
   1. Search index retrieves: Sales data, inventory levels, targets
   2. LLM analyzes and generates report with insights
   3. User gets actionable answer, not raw data
   ```

2. **Process Guidance**
   ```
   User: "How do I create a purchase order for maintenance supplies?"

   RAG flow:
   1. Search index finds: Process documentation, examples, approval rules
   2. LLM creates step-by-step guide customized to user's role
   3. Links to specific ERP windows/processes
   ```

3. **Anomaly Detection**
   ```
   User: "Are there any unusual transactions today?"

   RAG flow:
   1. Search index retrieves: Today's transactions + historical patterns
   2. LLM identifies anomalies (high amounts, unusual vendors, etc.)
   3. Generates alert with context and recommended actions
   ```

**Technical Architecture:**

```
┌─────────────────────────────────────────────────────────────┐
│                     User Interface                          │
│  (Web UI, Mobile App, Slack Bot, API)                      │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ Natural language query
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              AI Agent / RAG Orchestrator                    │
│  - Query understanding (intent classification)              │
│  - Context building (user role, client, permissions)        │
│  - Search strategy selection (keyword, semantic, hybrid)    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ Structured search request
                 ▼
┌─────────────────────────────────────────────────────────────┐
│         Search Index Plugin (This Plugin!)                  │
│  Provider: HybridSearchProvider                             │
│    ├─ PGTextSearchIndexProvider (keywords)                  │
│    ├─ PGVectorSearchProvider (semantic)                     │
│    └─ StructuredQueryProvider (SQL for precise data)        │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ Search results + metadata
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              Context Assembly Layer                         │
│  - Combine search results                                   │
│  - Add business rules context                               │
│  - Apply RBAC filtering                                     │
│  - Format for LLM consumption                               │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ Enriched context + user query
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              LLM Provider                                    │
│  Options:                                                    │
│    - OpenAI GPT-4 (cloud)                                   │
│    - Claude 3.5 Sonnet (cloud)                              │
│    - Ollama + Llama 3 (local, private)                      │
│    - Custom fine-tuned model (domain-specific)              │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ Generated response
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              Response Formatter                             │
│  - Citation links to source records                         │
│  - Suggested actions (create PO, approve invoice, etc.)     │
│  - Audit trail (what data was accessed)                     │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ Final answer
                 ▼
┌─────────────────────────────────────────────────────────────┐
│                     User Interface                          │
│  Displays answer + citations + actions                      │
└─────────────────────────────────────────────────────────────┘
```

**Implementation Example:**

```java
// New OSGi service: RAGSearchService
@Component
public class RAGSearchService {

    @Reference
    private ISearchIndexProviderFactory providerFactory;

    @Reference
    private ILLMProvider llmProvider; // OpenAI, Ollama, etc.

    /**
     * Answer natural language question using RAG
     */
    public RAGResponse answerQuestion(Properties ctx, String question) {
        // 1. Understand intent
        QueryIntent intent = classifyIntent(question);
        // Examples: FIND_RECORD, ANALYZE_DATA, EXPLAIN_PROCESS

        // 2. Retrieve relevant data
        ISearchIndexProvider searchProvider = providerFactory.getProvider("hybrid");
        List<ISearchResult> searchResults = searchProvider.getSearchResults(
            ctx,
            intent.getSearchIndexName(),
            question,
            false, // Use AI semantic search, not advanced syntax
            SearchType.SEMANTIC,
            null
        );

        // 3. Build context for LLM
        String context = buildRAGContext(searchResults, intent);

        // 4. Generate answer
        String prompt = buildPrompt(question, context, getUserRole(ctx));
        String answer = llmProvider.generateCompletion(prompt);

        // 5. Add citations and audit trail
        return new RAGResponse(answer, searchResults, intent);
    }

    private String buildPrompt(String question, String context, String role) {
        return """
            You are an AI assistant for iDempiere ERP.
            User role: %s

            Context from ERP database:
            %s

            User question: %s

            Provide a helpful answer based ONLY on the context above.
            Include citations to specific records.
            If you don't have enough information, say so.
            """.formatted(role, context, question);
    }
}
```

**Privacy & Security Considerations:**

```java
// CRITICAL: Apply RBAC before sending to LLM
private String buildRAGContext(List<ISearchResult> results, QueryIntent intent) {
    MRole role = MRole.getDefault(ctx, false);

    StringBuilder context = new StringBuilder();
    for (ISearchResult result : results) {
        // Check if user has access to this record
        if (!role.isRecordAccess(result.getAD_Table_ID(), result.getRecordID(), true)) {
            continue; // SKIP - user cannot see this data
        }

        // Sanitize sensitive fields
        Map<String, Object> data = result.getData();
        if (containsSensitiveData(data)) {
            data = redactSensitiveFields(data); // Remove credit card, SSN, etc.
        }

        context.append(formatResultForLLM(data));
    }

    return context.toString();
}
```

**Cost Analysis (RAG):**

| Scenario | LLM Provider | Cost per Query | Cost @ 1K queries/day |
|----------|--------------|----------------|------------------------|
| Cloud (OpenAI GPT-4) | OpenAI API | $0.03 | $900/month |
| Cloud (Claude 3.5) | Anthropic API | $0.015 | $450/month |
| Local (Llama 3 70B) | Ollama + GPU | $0 | €400/month (GPU server) |
| Hybrid (GPT-4 Turbo) | OpenAI API | $0.01 | $300/month |

**Recommendation:** Start with GPT-4 Turbo for proof-of-concept, migrate to local LLM for production (data privacy)

---

### Opportunity 3: Natural Language Query Interface

**What It Is:**
Allow users to search/query ERP using natural language instead of learning complex SQL or filter syntax.

**Example:**
```
Traditional ERP query:
  1. Open Product window
  2. Click "Advanced Search"
  3. Add filter: Category = "Electronics"
  4. Add filter: Price > 100
  5. Add filter: InStock = true
  6. Sort by: Price DESC
  7. Click Search

Natural Language ERP query:
  User types: "expensive electronics we have in stock"
  AI translates to: Category="Electronics" AND Price>100 AND InStock=true ORDER BY Price DESC
  Results displayed immediately
```

**ERP Use Cases:**

1. **Sales Rep Finding Products**
   ```
   Traditional: Navigate menus, know exact product codes

   Natural Language:
   - "Show me all blue widgets under €50"
   - "Products similar to what customer ABC usually orders"
   - "Best sellers in the gardening category this month"
   ```

2. **Manager Analyzing Data**
   ```
   Traditional: Create custom report, know table names, write SQL

   Natural Language:
   - "Total sales by region last quarter"
   - "Customers who haven't ordered in 6 months"
   - "Profit margin trends for product line X"
   ```

3. **Warehouse Staff Inventory Check**
   ```
   Traditional: Memorize product codes, navigate multiple screens

   Natural Language:
   - "Where is the red paint stored?"
   - "How many units of SKU 12345 do we have?"
   - "Show me items below reorder point"
   ```

**Technical Implementation:**

```java
// Natural Language Query Provider
public class NLQueryProvider implements ISearchIndexProvider {

    @Reference
    private ILLMProvider llmProvider;

    @Override
    public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName,
                                                String query, boolean isAdvanced,
                                                SearchType searchType, String orderBy) {
        // 1. Parse natural language to structured query
        StructuredQuery structured = parseNaturalLanguage(ctx, query, searchIndexName);

        // 2. Execute structured query
        return executeStructuredQuery(ctx, structured);
    }

    private StructuredQuery parseNaturalLanguage(Properties ctx, String nlQuery,
                                                   String searchIndexName) {
        // Get search index schema
        SearchIndexConfig config = SearchIndexConfigBuilder.build(ctx, searchIndexName);
        String schemaDescription = buildSchemaPrompt(config);

        // LLM prompt for query translation
        String prompt = """
            Convert this natural language query to a structured search query.

            Available fields and their types:
            %s

            User query: "%s"

            Output JSON format:
            {
              "filters": [
                {"field": "Category", "operator": "=", "value": "Electronics"},
                {"field": "Price", "operator": ">", "value": 100}
              ],
              "orderBy": "Price DESC",
              "limit": 10
            }
            """.formatted(schemaDescription, nlQuery);

        String jsonResponse = llmProvider.generateCompletion(prompt);
        return parseStructuredQuery(jsonResponse);
    }
}
```

**Example Transformations:**

```
NL: "cheap red flowers"
→ Structured: {
    filters: [
      {field: "Color", op: "=", value: "Red"},
      {field: "Category", op: "=", value: "Flowers"},
      {field: "Price", op: "<", value: 20}
    ],
    orderBy: "Price ASC"
  }

NL: "products similar to what John bought last month"
→ Structured: {
    semanticQuery: "products matching order history for customer John",
    filters: [
      {field: "Customer", op: "=", value: "John"},
      {field: "OrderDate", op: ">", value: "2024-11-01"}
    ],
    useSemanticSimilarity: true
  }

NL: "overdue invoices for customers in Germany"
→ Structured: {
    filters: [
      {field: "IsPaid", op: "=", value: false},
      {field: "DueDate", op: "<", value: "TODAY"},
      {field: "CustomerCountry", op: "=", value: "Germany"}
    ],
    orderBy: "DueDate ASC"
  }
```

**UI Integration:**

```html
<!-- Natural Language Search Bar (ZK UI) -->
<div class="nl-search-container">
    <textbox id="nlSearchBox"
             placeholder="Ask me anything... (e.g., 'show expensive products in stock')"
             onOK="onNLSearch()"
             width="100%" />

    <div id="nlSuggestions" visible="false">
        <!-- AI-powered autocomplete suggestions -->
        <button onClick="selectSuggestion('products below reorder point')">
            🔍 Products below reorder point
        </button>
        <button onClick="selectSuggestion('customers with overdue invoices')">
            💰 Customers with overdue invoices
        </button>
    </div>
</div>

<script>
function onNLSearch() {
    String query = nlSearchBox.getValue();

    // Call NLQueryProvider via search index
    List<ISearchResult> results = searchIndexProvider.getSearchResults(
        ctx, "product_idx", query, false, SearchType.NATURAL_LANGUAGE, null
    );

    displayResults(results);
}
</script>
```

---

### Opportunity 4: Multi-Modal Search (Text + Images + Structured Data)

**What It Is:**
Search across different data types simultaneously - text descriptions, product images, technical specs, etc.

**Example:**
```
Traditional: Search products by text description only

Multi-Modal:
  User uploads photo of a chair
  AI finds:
  - Products with similar visual appearance
  - Products in same category (furniture)
  - Products with similar text descriptions
  Results ranked by combined similarity
```

**ERP Use Cases:**

1. **Visual Product Search**
   ```
   Scenario: Customer service rep receives photo from customer

   Traditional: Describe photo in words, search by text

   Multi-Modal:
   1. Upload photo
   2. AI extracts visual features (color, shape, material)
   3. Searches text descriptions AND image embeddings
   4. Finds matching products even if description incomplete
   ```

2. **Technical Drawing Search**
   ```
   Scenario: Engineer looking for similar parts

   Traditional: Search by part number or manual keyword entry

   Multi-Modal:
   1. Upload CAD drawing or photo of part
   2. AI understands shape, dimensions, features
   3. Finds similar parts across the catalog
   4. Ranks by visual + functional similarity
   ```

3. **Document Search with Context**
   ```
   Scenario: Finding contracts or specifications

   Traditional: Keyword search in document text

   Multi-Modal:
   1. Search query: "contract with payment terms 90 days"
   2. AI searches:
      - Text content (keywords "payment terms", "90 days")
      - Structured metadata (contract type, date, customer)
      - Scanned image content (OCR + visual layout)
   3. Finds relevant contracts even if scanned/non-searchable
   ```

**Technical Implementation:**

```java
// Multi-Modal Search Provider
public class MultiModalSearchProvider implements ISearchIndexProvider {

    @Reference
    private ILLMProvider llmProvider; // For text embeddings

    @Reference
    private IVisionProvider visionProvider; // For image embeddings (CLIP, etc.)

    @Override
    public void createIndex(Properties ctx, SearchIndexData data, String trxName) {
        // 1. Extract text content
        String textContent = buildContentString(data);
        float[] textEmbedding = llmProvider.createEmbedding(textContent);

        // 2. Extract image (if product has photo)
        byte[] imageData = getProductImage(data.getRecordID());
        float[] imageEmbedding = null;
        if (imageData != null) {
            imageEmbedding = visionProvider.createImageEmbedding(imageData);
        }

        // 3. Extract structured features
        Map<String, Object> structuredData = extractStructuredFeatures(data);
        // Example: {color: "red", category: "flowers", price: 15.99}

        // 4. Store all modalities
        String sql = """
            INSERT INTO searchindex_multimodal
            (ad_client_id, ad_table_id, record_id,
             text_embedding, image_embedding, structured_data)
            VALUES (?, ?, ?, ?::vector, ?::vector, ?::jsonb)
            """;
        PreparedStatement ps = DB.prepareStatement(sql, trxName);
        ps.setInt(1, data.getAD_Client_ID());
        ps.setInt(2, data.getAD_Table_ID());
        ps.setInt(3, data.getRecordID());
        ps.setObject(4, textEmbedding);
        ps.setObject(5, imageEmbedding);
        ps.setObject(6, new Gson().toJson(structuredData));
        ps.executeUpdate();
    }

    @Override
    public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName,
                                                String query, boolean isAdvanced,
                                                SearchType searchType, String orderBy) {
        // Check if query includes image
        if (query.startsWith("image:")) {
            return searchByImage(ctx, query.substring(6));
        }

        // Otherwise, hybrid text + structured search
        return searchByTextAndStructured(ctx, searchIndexName, query);
    }

    private List<ISearchResult> searchByImage(Properties ctx, String imageUrl) {
        // 1. Download/load image
        byte[] imageData = loadImage(imageUrl);

        // 2. Generate image embedding
        float[] queryEmbedding = visionProvider.createImageEmbedding(imageData);

        // 3. Find similar images
        String sql = """
            SELECT ad_table_id, record_id,
                   image_embedding <=> ?::vector AS image_distance,
                   text_embedding <=> ?::vector AS text_distance
            FROM searchindex_multimodal
            WHERE ad_client_id = ?
              AND image_embedding IS NOT NULL
            ORDER BY (image_embedding <=> ?::vector) * 0.7 +  -- 70% weight on image
                     (text_embedding <=> ?::vector) * 0.3     -- 30% weight on text
            LIMIT 10
            """;

        // Execute and return results
        // Results ranked by combined image + text similarity
    }
}
```

**Database Schema:**

```sql
-- Multi-modal index table
CREATE TABLE searchindex_multimodal (
    ad_client_id INT NOT NULL,
    ad_table_id INT NOT NULL,
    record_id INT NOT NULL,
    text_embedding vector(1536),      -- Text embeddings (OpenAI ada-002)
    image_embedding vector(512),      -- Image embeddings (CLIP ViT-B/32)
    structured_data JSONB,            -- Structured features (color, size, etc.)
    image_url TEXT,                   -- Link to product image
    created TIMESTAMP DEFAULT NOW(),
    updated TIMESTAMP DEFAULT NOW(),
    CONSTRAINT pk_searchindex_multimodal PRIMARY KEY (ad_client_id, ad_table_id, record_id)
);

-- Indexes for fast similarity search
CREATE INDEX idx_multimodal_text_embedding
    ON searchindex_multimodal
    USING hnsw (text_embedding vector_cosine_ops);

CREATE INDEX idx_multimodal_image_embedding
    ON searchindex_multimodal
    USING hnsw (image_embedding vector_cosine_ops);

-- GIN index for structured data queries
CREATE INDEX idx_multimodal_structured
    ON searchindex_multimodal
    USING gin (structured_data jsonb_path_ops);
```

**Vision Models for Image Embeddings:**

| Model | Embedding Size | Use Case | Cost |
|-------|----------------|----------|------|
| CLIP ViT-B/32 | 512 | General products | Free (local) |
| CLIP ViT-L/14 | 768 | High-quality images | Free (local) |
| OpenAI CLIP | 512 | Best accuracy | $0.002/image |
| Google Vision API | Custom | Specialized (fashion, etc.) | $1.50/1K images |

**Recommendation:** Start with local CLIP ViT-B/32 (free, good quality)

---

### Opportunity 5: Personalized & Context-Aware Search

**What It Is:**
Rank search results based on user context (role, history, preferences, location, time).

**Example:**
```
Same query: "tools"

Warehouse Manager sees:
  - Inventory management tools
  - Barcode scanners
  - Picking optimization software

Maintenance Technician sees:
  - Repair tools (wrenches, screwdrivers)
  - Diagnostic equipment
  - Spare parts

Sales Rep sees:
  - CRM tools
  - Product catalogs
  - Customer engagement tools

→ Same keyword, different context → different results
```

**ERP Use Cases:**

1. **Role-Based Ranking**
   ```sql
   -- Traditional: Same results for everyone
   SELECT * FROM M_Product WHERE Name LIKE '%computer%'

   -- AI-powered: Personalized ranking
   SELECT * FROM M_Product
   WHERE Name LIKE '%computer%'
   ORDER BY
     CASE
       WHEN AD_Role_ID = 1000 THEN /* IT Manager */
         (Price_High_Weight * 0.2 + Specs_Weight * 0.8)
       WHEN AD_Role_ID = 2000 THEN /* Sales */
         (Price_Low_Weight * 0.5 + Availability_Weight * 0.5)
       ELSE
         Default_Ranking
     END DESC
   ```

2. **Purchase History Personalization**
   ```
   User: "ink cartridges"

   Traditional: All ink cartridges, sorted by name

   AI-powered:
   1. Check user's printer model (from purchase history)
   2. Filter to compatible cartridges FIRST
   3. Rank by:
      - Previous purchases (user usually buys black, not color)
      - Price preference (user prefers mid-range)
      - Availability (user needs it fast, prioritize in-stock)
   ```

3. **Seasonal/Temporal Context**
   ```
   Query: "decorations"

   December: Christmas decorations ranked first
   October: Halloween decorations ranked first
   Spring: Garden decorations ranked first

   → Same query, different time → different ranking
   ```

**Technical Implementation:**

```java
// Context-Aware Search Provider
public class ContextAwareSearchProvider implements ISearchIndexProvider {

    @Override
    public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName,
                                                String query, boolean isAdvanced,
                                                SearchType searchType, String orderBy) {
        // 1. Build user context
        UserContext userContext = buildUserContext(ctx);

        // 2. Get base search results
        ISearchIndexProvider baseProvider = getBaseProvider(); // Vector, FTS, etc.
        List<ISearchResult> baseResults = baseProvider.getSearchResults(
            ctx, searchIndexName, query, isAdvanced, searchType, null
        );

        // 3. Re-rank based on context
        return personalizeResults(baseResults, userContext);
    }

    private UserContext buildUserContext(Properties ctx) {
        return UserContext.builder()
            .userId(Env.getAD_User_ID(ctx))
            .roleId(Env.getAD_Role_ID(ctx))
            .orgId(Env.getAD_Org_ID(ctx))
            .purchaseHistory(getUserPurchaseHistory(ctx))
            .searchHistory(getUserSearchHistory(ctx))
            .preferences(getUserPreferences(ctx))
            .location(getUserLocation(ctx))
            .timeContext(LocalDateTime.now())
            .build();
    }

    private List<ISearchResult> personalizeResults(List<ISearchResult> results,
                                                     UserContext context) {
        // Calculate personalization score for each result
        results.forEach(result -> {
            double personalScore = 0.0;

            // Factor 1: Role affinity (30% weight)
            personalScore += calculateRoleAffinity(result, context.getRoleId()) * 0.3;

            // Factor 2: Purchase history similarity (25% weight)
            personalScore += calculateHistorySimilarity(result, context.getPurchaseHistory()) * 0.25;

            // Factor 3: Price preference (20% weight)
            personalScore += calculatePricePreference(result, context.getPreferences()) * 0.2;

            // Factor 4: Availability (15% weight)
            personalScore += calculateAvailability(result, context.getLocation()) * 0.15;

            // Factor 5: Seasonal relevance (10% weight)
            personalScore += calculateSeasonalRelevance(result, context.getTimeContext()) * 0.1;

            result.setPersonalizationScore(personalScore);
        });

        // Re-rank by combined score (base relevance + personalization)
        results.sort((a, b) -> {
            double scoreA = a.getBaseRelevance() * 0.6 + a.getPersonalizationScore() * 0.4;
            double scoreB = b.getBaseRelevance() * 0.6 + b.getPersonalizationScore() * 0.4;
            return Double.compare(scoreB, scoreA);
        });

        return results;
    }
}
```

**Machine Learning Model for Personalization:**

```python
# Training data: user interactions with search results
# Features: user role, query, result clicked, purchase made, time spent

import pandas as pd
from sklearn.ensemble import GradientBoostingClassifier

# Load training data
data = pd.read_csv("search_interactions.csv")
# Columns: user_id, role_id, query, result_id, clicked, purchased, time_spent

# Features
X = data[['role_id', 'query_embedding', 'result_embedding',
          'price', 'in_stock', 'purchase_history_match', 'seasonal_score']]

# Label: Did user click/purchase?
y = data['clicked'] * 0.5 + data['purchased'] * 0.5

# Train model
model = GradientBoostingClassifier(n_estimators=100)
model.fit(X, y)

# Save model for use in Java via ONNX or REST API
model.save('personalization_model.pkl')
```

**Deployment:**

```java
// Load ML model in OSGi service
@Component
public class PersonalizationModelService {

    private ONNXModel model;

    @Activate
    public void activate() {
        // Load ONNX model
        model = ONNXModel.load("/models/personalization_model.onnx");
    }

    public double predictRelevance(UserContext user, ISearchResult result) {
        // Prepare features
        float[] features = new float[] {
            user.getRoleId(),
            ...
            result.getPrice(),
            result.isInStock() ? 1.0f : 0.0f,
            ...
        };

        // Run inference
        return model.predict(features)[0];
    }
}
```

---

## Part 3: ERP-Specific AI Opportunities

### 1. Intelligent Document Processing

**Use Case:** Automatically index and search scanned documents (invoices, contracts, purchase orders)

```java
// Document AI Provider
public class DocumentAIProvider implements ISearchIndexProvider {

    @Reference
    private IOCRService ocrService; // Tesseract, Google Vision, etc.

    @Reference
    private IDocumentUnderstanding duService; // Extract entities, relationships

    @Override
    public void createIndex(Properties ctx, SearchIndexData data, String trxName) {
        // 1. Get document (PDF, image)
        byte[] documentData = getAttachment(data.getRecordID());

        // 2. OCR if scanned image
        String extractedText = ocrService.extractText(documentData);

        // 3. Understand document structure
        DocumentStructure structure = duService.analyze(extractedText);
        // Extracts: invoice number, dates, line items, total, vendor, etc.

        // 4. Create searchable index with structured metadata
        String sql = """
            INSERT INTO searchindex_documents
            (ad_client_id, ad_table_id, record_id, full_text, entities, metadata)
            VALUES (?, ?, ?, to_tsvector(?), ?::jsonb, ?::jsonb)
            """;

        PreparedStatement ps = DB.prepareStatement(sql, trxName);
        ps.setString(4, extractedText); // Full-text search
        ps.setString(5, new Gson().toJson(structure.getEntities())); // Named entities
        ps.setString(6, new Gson().toJson(structure.getMetadata())); // Structured data
        ps.executeUpdate();
    }
}
```

**Example Queries:**
```
"Find all invoices from vendor ACME over €10,000 from last year"
  → AI extracts: vendor="ACME", amount>10000, date>2024-01-01
  → Searches both text AND structured entities
  → Works even on scanned/OCR'd documents
```

---

### 2. Anomaly Detection in Business Data

**Use Case:** Automatically flag unusual transactions, orders, or inventory changes

```java
// Anomaly Detection Provider
public class AnomalyDetectionProvider implements ISearchIndexProvider {

    @Reference
    private IAnomalyDetectionModel anomalyModel;

    @Override
    public void createIndex(Properties ctx, SearchIndexData data, String trxName) {
        // 1. Extract features for anomaly detection
        Map<String, Double> features = extractFeatures(data);
        // Example: transaction amount, time of day, vendor history, etc.

        // 2. Calculate anomaly score
        double anomalyScore = anomalyModel.predict(features);
        // Score: 0.0 (normal) to 1.0 (highly anomalous)

        // 3. Store with metadata
        String sql = """
            INSERT INTO searchindex_anomalies
            (ad_client_id, ad_table_id, record_id, anomaly_score, features, flagged)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """;

        ps.setDouble(4, anomalyScore);
        ps.setBoolean(6, anomalyScore > 0.8); // Flag if score > threshold
        ps.executeUpdate();

        // 4. Send alert if anomaly detected
        if (anomalyScore > 0.8) {
            sendAnomalyAlert(ctx, data, anomalyScore);
        }
    }
}
```

**Example Alerts:**
```
"Unusual invoice detected:
 - Amount: €50,000 (10× typical amount for this vendor)
 - Vendor: New supplier (first transaction)
 - Time: 11:47 PM (outside normal business hours)

 Recommendation: Review before approval"
```

---

### 3. Predictive Search & Recommendations

**Use Case:** Suggest what user will search for next, recommend products proactively

```java
// Predictive Search Provider
public class PredictiveSearchProvider implements ISearchIndexProvider {

    @Reference
    private IRecommendationModel recommendationModel;

    public List<String> predictNextQuery(Properties ctx, String currentQuery) {
        // Based on:
        // - User's search history
        // - Other users with similar patterns
        // - Business context (time, role, recent activities)

        UserContext user = buildUserContext(ctx);
        List<String> suggestions = recommendationModel.predictQueries(user, currentQuery);

        return suggestions;
        // Example: ["red roses"] → suggests ["white roses", "rose vase", "flower delivery"]
    }

    public List<ISearchResult> recommendProducts(Properties ctx, int productId) {
        // Product-to-product recommendations
        // "Customers who viewed this also viewed..."

        // 1. Get embedding for current product
        float[] productEmbedding = getProductEmbedding(productId);

        // 2. Find similar products
        String sql = """
            SELECT ad_table_id, record_id,
                   embedding <=> ?::vector AS distance
            FROM searchindex_vector
            WHERE ad_client_id = ?
              AND record_id != ? -- Exclude current product
            ORDER BY embedding <=> ?::vector
            LIMIT 10
            """;

        // 3. Combine with business rules
        // - Filter out out-of-stock items
        // - Boost frequently purchased together
        // - Apply margin/profitability weighting

        return results;
    }
}
```

**UI Integration:**

```html
<!-- Autocomplete with AI predictions -->
<textbox id="searchBox" onChange="onSearchInput()" />

<div id="predictions">
    <!-- Auto-populated by AI -->
    <div class="prediction">🔍 red roses (your search history)</div>
    <div class="prediction">🔍 white roses (similar products)</div>
    <div class="prediction">🔍 rose vase (frequently bought together)</div>
</div>

<!-- Product recommendations -->
<div class="recommendations">
    <h3>You might also like:</h3>
    <!-- AI-generated recommendations -->
</div>
```

---

## Part 4: Technical Architecture for AI Integration

### Provider Architecture Extension

```
Current Architecture:
ISearchIndexProvider (interface)
  ├── PGTextSearchIndexProvider (implemented)
  └── ElasticSearchIndexProvider (stub)

AI-Enhanced Architecture (2025+):
ISearchIndexProvider (interface)
  ├── Traditional Providers
  │   ├── PGTextSearchIndexProvider
  │   └── ElasticSearchIndexProvider
  │
  ├── AI/ML Providers
  │   ├── PGVectorSearchProvider (semantic search via pgvector)
  │   ├── OpenAIEmbeddingProvider (cloud embeddings)
  │   ├── OllamaLocalProvider (local LLM)
  │   ├── MultiModalSearchProvider (text + images)
  │   ├── NLQueryProvider (natural language)
  │   └── ContextAwareSearchProvider (personalization)
  │
  └── Hybrid Providers
      ├── HybridSearchProvider (keyword + semantic)
      └── RAGSearchProvider (retrieval + generation)
```

### OSGi Service Registration (Dynamic AI Providers)

```java
// AI Provider as OSGi Service
@Component(
    service = ISearchIndexProvider.class,
    property = {
        "provider.type=pgvector",
        "provider.name=PostgreSQL Vector Search",
        "provider.capabilities=semantic,multilingual,similarity",
        "service.ranking:Integer=200"  // Higher priority than traditional providers
    }
)
public class PGVectorSearchProvider implements ISearchIndexProvider {

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC
    )
    private volatile IEmbeddingProvider embeddingProvider;

    // If OpenAI available, use it; otherwise fall back to local model
    // Dynamic binding allows hot-swapping providers at runtime
}

// Embedding Provider (pluggable)
@Component(service = IEmbeddingProvider.class)
public class OpenAIEmbeddingProvider implements IEmbeddingProvider {

    private OpenAIClient client;

    @Activate
    public void activate(Map<String, Object> config) {
        String apiKey = (String) config.get("openai.api.key");
        client = new OpenAIClient(apiKey);
    }

    @Override
    public float[] createEmbedding(String text) {
        return client.embeddings().create(text, "text-embedding-3-small");
    }
}

// Alternative: Local Embedding Provider
@Component(
    service = IEmbeddingProvider.class,
    property = "service.ranking:Integer=100"  // Lower priority than OpenAI
)
public class OllamaEmbeddingProvider implements IEmbeddingProvider {

    private OllamaClient client;

    @Activate
    public void activate() {
        client = new OllamaClient("http://localhost:11434");
    }

    @Override
    public float[] createEmbedding(String text) {
        return client.embed("nomic-embed-text", text);
    }
}
```

### Configuration Management

```java
// System Configurator entries for AI features
AD_SysConfig entries:

SEARCH_AI_ENABLED (default: false)
  → Enable/disable AI search features globally

SEARCH_AI_PROVIDER (default: "pgvector")
  → Which AI provider to use: pgvector, openai, ollama, hybrid

SEARCH_EMBEDDING_MODEL (default: "text-embedding-3-small")
  → OpenAI model or local model name

SEARCH_LLM_PROVIDER (default: "ollama")
  → For RAG: openai, anthropic, ollama

SEARCH_RAG_ENABLED (default: false)
  → Enable RAG-powered question answering

SEARCH_PERSONALIZATION_ENABLED (default: false)
  → Enable context-aware ranking

SEARCH_MULTIMODAL_ENABLED (default: false)
  → Enable image + text search

// Per-client configuration
SEARCH_AI_MAX_COST_PER_MONTH (default: 100)
  → Spending limit for cloud API calls (USD)

SEARCH_AI_FALLBACK_PROVIDER (default: "pgtextsearch")
  → Fallback if AI provider unavailable
```

---

## Part 5: Implementation Roadmap (AI Enhancement)

### Phase 1: Foundation (2-4 weeks)

**Goal:** Enable basic AI search infrastructure

**Tasks:**
1. **Install pgvector extension** (1 day)
   ```sql
   CREATE EXTENSION vector;
   CREATE TABLE searchindex_vector (...);
   ```

2. **Create IEmbeddingProvider interface** (2 days)
   ```java
   public interface IEmbeddingProvider {
       float[] createEmbedding(String text);
       float[][] createBatchEmbeddings(List<String> texts);
   }
   ```

3. **Implement OpenAIEmbeddingProvider** (3 days)
   - API integration
   - Error handling, rate limiting
   - Cost tracking

4. **Implement PGVectorSearchProvider** (5 days)
   - Index creation with embeddings
   - Vector similarity search
   - Hybrid search (keyword + semantic)

5. **Testing** (3 days)
   - Unit tests
   - Integration tests
   - Performance benchmarks

**Deliverable:** Release 2.0.0 with semantic search capability

**Cost:** OpenAI embeddings ~$50/month for 1M products

---

### Phase 2: RAG Integration (4-6 weeks)

**Goal:** Enable AI assistant powered by ERP data

**Tasks:**
1. **Create RAGSearchService** (5 days)
   - Query intent classification
   - Context assembly
   - LLM integration

2. **Implement ILLMProvider interface** (3 days)
   ```java
   public interface ILLMProvider {
       String generateCompletion(String prompt);
       String chat(List<Message> conversation);
   }
   ```

3. **Implement OllamaProvider (local LLM)** (5 days)
   - Ollama API integration
   - Model management
   - Streaming responses

4. **RBAC security layer** (3 days)
   - Filter search results by role
   - Redact sensitive fields
   - Audit trail

5. **UI for RAG chat interface** (5 days)
   - ZK chatbot component
   - Citation display
   - Suggested actions

6. **Testing & validation** (5 days)
   - Security testing (RBAC bypass attempts)
   - RAG accuracy validation
   - Performance testing

**Deliverable:** Release 2.1.0 with AI assistant

**Cost:** Local LLM (Ollama) + GPU server €200-400/month

---

### Phase 3: Advanced Features (8-12 weeks)

**Goal:** Multi-modal, personalization, natural language

**Tasks:**
1. **Multi-modal search** (6 weeks)
   - Image embedding generation
   - CLIP model integration
   - Combined ranking

2. **Natural language query** (4 weeks)
   - NL → structured query translation
   - Schema-aware prompt engineering
   - Validation & error handling

3. **Personalization engine** (6 weeks)
   - User context building
   - ML model training
   - A/B testing framework

4. **Document AI** (4 weeks)
   - OCR integration
   - Document structure understanding
   - Entity extraction

**Deliverable:** Release 2.2.0 with advanced AI features

---

## Part 6: Cost-Benefit Analysis (AI Features)

### Total Cost of Ownership (3 years)

| Approach | Infrastructure | API Costs | Development | Total (3 years) |
|----------|----------------|-----------|-------------|-----------------|
| **No AI (current)** | €0 | €0 | €0 | **€0** |
| **Cloud AI (OpenAI)** | €0 | €1,800/year | €40,000 | **€45,400** |
| **Local AI (Ollama)** | €400/month GPU | €0 | €50,000 | **€64,400** |
| **Hybrid (OpenAI embed + local LLM)** | €200/month | €600/year | €45,000 | **€52,200** |

**Recommendation:** Start with Cloud AI (lowest initial investment), migrate to Hybrid for production (data privacy + cost optimization)

---

### Business Value (ROI)

**Productivity Gains:**
- Time saved per search: 30 seconds → 5 seconds (semantic search)
- Queries per day: 1,000
- Time saved: 25,000 seconds/day = **7 hours/day**
- Value: €50/hour × 7 hours × 20 days = **€7,000/month**

**Revenue Impact:**
- Better product discovery → Higher conversion rate
- Example: 15% → 18% conversion (e-commerce)
- Revenue: €500,000/month × 3% increase = **€15,000/month**

**Support Cost Reduction:**
- AI assistant handles Tier-1 queries
- Reduction: 30% of support tickets
- Savings: €10,000/month × 30% = **€3,000/month**

**Total Value:** €25,000/month = €300,000/year

**ROI:** (€300,000 - €52,200) / €52,200 = **474% return**

---

## Part 7: Competitive Advantage (2025 Landscape)

### Why This Matters in 2025

**Trend 1: AI-First User Interfaces**
- Users expect natural language interaction
- "Google-like" search in every application
- Voice/chat interfaces becoming standard

**Trend 2: Data Governance & Privacy**
- EU AI Act compliance required
- GDPR strict enforcement
- Local/self-hosted AI preferred for sensitive data

**Trend 3: Personalization as Standard**
- Generic search results no longer acceptable
- Context-aware ranking expected
- Real-time adaptation to user behavior

**Trend 4: Multi-Modal Everything**
- Text + images + video + structured data
- Users upload photos to search products
- Documents understood visually, not just text

**Your Position:**
- ✅ ERP + AI = differentiated offering
- ✅ Provider architecture = flexible, future-proof
- ✅ OSGi plugin = isolated, secure, multi-tenant safe
- ✅ Open-source foundation = customizable, no vendor lock-in

---

## Part 8: Recommendations & Next Steps

### Immediate Actions (This Month)

1. **Approve AI Strategy**
   - Review this document with stakeholders
   - Decide: Cloud AI or Local AI or Hybrid?
   - Allocate budget (€40-65K for year 1)

2. **Proof of Concept (2 weeks)**
   - Install pgvector extension
   - Implement basic OpenAI embedding provider
   - Test semantic search on 1,000 products
   - Demo to stakeholders

3. **Measure Baseline**
   - Current search performance metrics
   - User satisfaction scores
   - Support ticket volumes (search-related)

### Short-Term (Q1 2025)

4. **Phase 1 Implementation**
   - Semantic search with OpenAI embeddings
   - Hybrid search (keyword + semantic)
   - Performance benchmarks

5. **Pilot with Key Customers**
   - Select 2-3 friendly customers
   - Enable AI search in pilot
   - Collect feedback, iterate

### Long-Term (2025-2026)

6. **Phase 2: RAG Integration**
   - AI assistant for ERP
   - Deploy local LLM (Ollama)
   - Security & compliance validation

7. **Phase 3: Advanced Features**
   - Multi-modal search
   - Natural language queries
   - Personalization

8. **Continuous Improvement**
   - Monitor AI model quality
   - A/B testing new approaches
   - Stay current with AI advancements

---

## Conclusion

The Cloudempiere Search Index plugin is **perfectly positioned** for AI integration in 2025:

- ✅ **Provider architecture** makes AI providers plug-and-play
- ✅ **OSGi framework** ensures isolation and security
- ✅ **Multi-tenant design** scales to enterprise needs
- ✅ **API integration** enables modern frontends
- ✅ **Event-driven updates** keep AI indexes fresh

**Strategic Recommendations:**
1. **Start small:** Semantic search with OpenAI (low risk, high impact)
2. **Prove value:** Pilot with 2-3 customers, measure ROI
3. **Scale thoughtfully:** Migrate to local AI for data privacy
4. **Differentiate:** AI-powered ERP search is rare in 2025 - competitive advantage

**Expected Outcomes:**
- 7 hours/day saved (productivity)
- 3% revenue increase (e-commerce conversion)
- 30% support cost reduction
- 474% ROI in year 1

**The future of ERP search is AI-powered. Your architecture is ready.**

---

**Document Version:** 1.0
**Last Updated:** 2025-12-12
**Next Review:** 2025-Q1 (post-PoC)
**Approval:** [Pending]

---

**Related Documents:**
- [ARCHITECTURAL-ANALYSIS-2025.md](ARCHITECTURAL-ANALYSIS-2025.md) - Current state analysis
- [IMPLEMENTATION-ROADMAP-2025.md](implementation-plan/IMPLEMENTATION-ROADMAP-2025.md) - Foundation fixes
- [LOW-COST-SLOVAK-ECOMMERCE-SEARCH.md](LOW-COST-SLOVAK-ECOMMERCE-SEARCH.md) - PostgreSQL FTS implementation
- [slovak-language-architecture.md](slovak-language-architecture.md) - Language-specific challenges
