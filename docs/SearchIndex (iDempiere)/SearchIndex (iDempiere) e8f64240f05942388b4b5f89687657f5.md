# SearchIndex (iDempiere)

Product roadmap: Webui (https://www.notion.so/Webui-4ce96353fe704abb8e8be678aaf5ac36?pvs=21)
Process: Search
Status: Backlog
Team: Cloudempiere
Created time: May 2, 2024 9:12 PM

## GOAL

---

Implement a pluggable search mechanism, based on omnisearch from bx.

- **Reading**, about mostly how to implement
    
    [Harnessing the Power of Full-Text Search in PostgreSQL: A Comprehensive Guide](https://nameishari.medium.com/harnessing-the-power-of-full-text-search-in-postgresql-a-comprehensive-guide-4dfb3156117c)
    
    [Fulltext v databáze prakticky, alebo čo vám nadšenci nepovedia](https://linuxos.sk/blog/mirecove-dristy/detail/fulltext-v-databaze-prakticky-alebo-co-vam-na/)
    
    [How to use fuzzy string matching with Postgresql](https://www.freecodecamp.org/news/fuzzy-string-matching-with-postgresql/)
    
    [A powerful full-text search in PostgreSQL in less than 20 lines](https://leandronsp.com/a-powerful-full-text-search-in-postgresql-in-less-than-20-lines)
    
    [Implementing a modern E-Commerce Search](https://spinscale.de/posts/2020-06-22-implementing-a-modern-ecommerce-search.html)
    
    [Postgres Full-Text Search: A Search Engine in a Database | Crunchy Data Blog](https://www.crunchydata.com/blog/postgres-full-text-search-a-search-engine-in-a-database)
    
    [Using PostgreSQL to Create an Efficient Search Engine](https://www.alibabacloud.com/blog/using-postgresql-to-create-an-efficient-search-engine_595344)
    
    [Postgres full-text search is Good Enough!](https://rachbelaid.com/postgres-full-text-search-is-good-enough/)
    
    [12.2. Tables and Indexes](https://www.postgresql.org/docs/current/textsearch-tables.html#TEXTSEARCH-TABLES-INDEX)
    

### Cloudempiere effort till 2024 august.

ingest data by sql trigger postgres **unaccent** **way** , with text search, very primitive way. allow use like or ilike. 

<aside>
❓ **Challenges**

- the current solution us limited to products....and related tables
- used in mainly on info window - global-search-field
- no direct rest api support - only direct access to field in webui
- Only Synced indexing, not async supported (performance issue)
- no synonyms defined
- implementation postgres trigger only
- hardcoded tenant level rules in postgres trigger
- trigger run on event as 2nd sql - slow
- like operator only - vector only experimental
</aside>

### Important Resources on

**thread**: unaccented and algorythmic search https://mattermost.idempiere.org/idempiere/pl/xyten9e763nqmdnku1x7o6j1na

**issue**: reported to rest team: https://github.com/bxservice/idempiere-rest/issues/76

**improvement**: by Igor on REST https://github.com/cloudempiere/cloudempiere-rest/pull/8

```jsx
UPDATE m_product_trl p SET globalsearchtermsvector=  to_tsvector(coalesce(v_VendorProductNo, $20))|| to_tsvector([p.Name](http://p.name/)) || to_tsvector(unaccent_upper([p.Name](http://p.name/)))|| to_tsvector(v_value) || to_tsvector(COALESCE(v_sku,$21))
WHERE p.m_product_id=NEW.m_product_id
```

Later we start use it from REST-API

![iD-search-improvement-trigger-clde.png](SearchIndex%20(iDempiere)/iD-search-improvement-trigger-clde.png)

```jsx
OPTION #1. /api/v1/models/fv_m_product_category_v?$filter=similarity(tolower(unaccent(name)),'auto ') ge 0.30
```

```jsx
OPTION #2. /api/v1/models/fv_product_wstore_v?$filter=contains(tolower(globalsearchterms),'gerb')
```

## **Index  Ingesting**

```json
 IF (TG_TABLE_NAME = 'm_product_trl') THEN

 **-- Change New on M_Product**
    NEW.globalsearchtermsvector =  
    to_tsvector(coalesce( v_VendorProductNo, ' '))|| to_tsvector(NEW.Name)|| to_tsvector(unaccent_upper(NEW.Name)) || to_tsvector(v_value) || to_tsvector(COALESCE(v_upc,''))    || to_tsvector(COALESCE(v_sku,''));   ELSE  
--Update From Product PO
    UPDATE m_product_trl p SET globalsearchtermsvector=
      to_tsvector(coalesce(v_VendorProductNo, ' '))|| to_tsvector(p.Name)|| to_tsvector(unaccent_upper(p.Name))|| to_tsvector(v_value) || to_tsvector(COALESCE(v_upc,''))    || to_tsvector(COALESCE(v_sku,''))
WHERE p.m_product_id=NEW.m_product_id;
   END IF;
```

```json
  IF (TG_TABLE_NAME = 'm_product_trl') THEN

    -- Change New on M_Product
    NEW.globalsearchtermsvector =  
    to_tsvector(coalesce( v_VendorProductNo, ' '))|| to_tsvector(NEW.Name)|| to_tsvector(unaccent_upper(NEW.Name)) || to_tsvector(v_value) || to_tsvector(COALESCE(v_upc,''))    || to_tsvector(COALESCE(v_sku,''));   ELSE  
--Update From Product PO
    UPDATE m_product_trl p SET globalsearchtermsvector=
      to_tsvector(coalesce(v_VendorProductNo, ' '))|| to_tsvector(p.Name)|| to_tsvector(unaccent_upper(p.Name))|| to_tsvector(v_value) || to_tsvector(COALESCE(v_upc,''))    || to_tsvector(COALESCE(v_sku,''))
WHERE p.m_product_id=NEW.m_product_id;
   END IF;
```

### BX Omni Search

---

https://wiki.idempiere.org/en/Plugin:_BX_Service_Omnisearch

according to chat with CarlosRuiz, we can use BX omnisearch as starting point.

**The solution**

- The **de.bxservice.omnisearch** one is created as a general plugin, defining everything needed to create and work with most kind of indexes. The create index process and the document and index factories which are all there to be reused.
- **de.bxservice.omnisearch_tsearch_pg** is created as a service that uses the above-mentioned plug-in. It is the concrete implementation of the interfaces and abstract classes. This plug-in can be exchanged by other plugins using different index tools: Lucene, NoSQL databases, amazon cloud search, etc...
- **de.bxservice.omnisearchdp** is the UI plugin, is the one that creates the dashboard panel and interacts with the user.

**Challenges based on Analysis - OMNI SEARCH ANALYSIS (May 2024, by NB)**

- Fact/Technical limitations: Not all/any postgres libs are possible install to on extensions in RDS.
- Current BX installation Limited to PostgreSQL libraries - can be improved added new plugins.
- Initial BX implementation - The index assumes one language selected per tenant – no multi-language support. doesn't work for Slovak language (selected on ad_client).  (fallback can b standard language)
- Assumption: The current definition column in App Dict specify what will be indexed) Instead: A new table per tenant - would allow defining columns per table, per language let say search **index document definition.**
- SQL query column would allow defining joins to other tables, like Purchase Tab on Product.
- The indexed data lacks interface for INFO WINDOW, Window, REST API, and WebUI Global Search. Need to understand and implement special characters like Slovak "č", "š", unaccent, and synonym support.
- Only synchronous indexing supported – asynchronous would improve performance, e.g., for burst API imports.
- Scalability for REST frontend projects is critical – millisecond response times required.
- REST: Send search terms, return a document – need to return a custom document (e.g., search-box result).
- Ranking: Consider where the term was found, etc. For 12-character searches, order results by relevance, prioritising matches at the beginning of fields.

![iD-search-improvement-bx.png](SearchIndex%20(iDempiere)/iD-search-improvement-bx.png)

| Issue | Description | Priority | Status |
| --- | --- | --- | --- |
| Import BX plugin into CLDE |  |  | DONE |
| Refactor search configuration | Deprecate AD_Column - Text search index checkbox, define new Window

[GitHub Ticket](https://github.com/cloudempiere/iDempiereCLDE/issues/2784#issuecomment-2304484527) |  | DONE |
| Improve omnisearch to search by related tables | E.g. search M_Product by M_Product_PO.C_BPartner

[GitHub Ticket](https://github.com/cloudempiere/iDempiereCLDE/issues/2784#issuecomment-2304484527) |  | In Progress |
| Add AD_Language column to the BXS table + as process parameter | Assumption: the user never search in multiple languages at the same time |  | TODO |
| Add date from to the indexing process | Use case: products changed in last n minutes can be reindexed |  | Backlog |
| Setup partitioning |  | low | Backlog |
| Add standard columns to BXS table and load into app dict. |  |  | TODO |
| Missing index provider definition | table/method (+ suffix)
Add provider to the Omnisearch Config |  | In Progress |
| Add Omnisearch Config “Type” - like iD search definition | [iD wiki](https://wiki.idempiere.org/en/NF2.1_Document_Search_on_Menu_Lookup)
Options:
- table + column
- multi-table + column (proposal)
- query (like search definition)
- java (like logilite DMS) |  | Idea |
| We cannot filter | E.g. create a blacklist |  | Backlog |
| New window: Index Provider (AD_IndexProvider) | Motivated by Storage Provider and Logilite LTX.
Columns: URL, Classname |  | In Progress |
| New table: AD_IndexDefinition - highest level
Rename: AD_OmnisearchConfig → AD_IndexTable
AD_OmnisearchConfigLine → AD_IndexColumn | Columns: Index Provider, Indexing Core |  | In Progress |