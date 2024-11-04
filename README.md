# Cloudempiere Search Index

**Version: 1.0.0**

# Search Index Provider

Determines the method/class how the records will be indexed. The table is based on Logilite's LTX_Indexing_Conf table.

# Search Index

This table will represent an index definition for an Index Provider. Defines a group of tables that will be indexed with a common Search Index Provider.

# Search Index Table

Represents a table for the Index Definition. Configurable where clause can be defined to filter the records set to be indexed. As where clause is accepted without the WHERE keyword, use standard PostgreSQL syntax and must use table names as aliases, except the alias for the table defined on the actual record is "main".

# Search Index Column

Represents the columns, which should the index consist of. Possible to define columns from related tables to the parent table (related through FK). Fill out the parent column, if the link column has a different name on the parent table. Fill out the Reference to define the display columns which will be indexed from the joined table.

# Technical Documentation

## com.cloudempiere.searchindex

This plugin contains the core functionality for search indexing. It includes classes and methods for managing search results, parsing variables, and interacting with the database.

### Key Classes and Interfaces

- [`com.cloudempiere.searchindex.pgtextsearch.PGTextSearchIndexProvider`](com.cloudempiere.searchindex/src/com/cloudempiere/searchindex/pgtextsearch/PGTextSearchIndexProvider.java): Implements the `ISearchIndexProvider` interface for PostgreSQL text search integration.

- [`com.cloudempiere.searchindex.util.SearchIndexRecord`](com.cloudempiere.searchindex/src/com/cloudempiere/searchindex/util/SearchIndexRecord.java): Represents a record in the search index.
- [`com.cloudempiere.searchindex.tools.AbstractSearchIndexDocument`](com.cloudempiere.searchindex/src/com/cloudempiere/searchindex/tools/AbstractSearchIndexDocument.java): Abstract class for search index documents.
- [`com.cloudempiere.searchindex.tools.SearchIndexHelper`](com.cloudempiere.searchindex/src/com/cloudempiere/searchindex/tools/SearchIndexHelper.java): Utility class for managing search index documents and indexes.
- [`com.cloudempiere.searchindex.util.ISearchResult`](com.cloudempiere.searchindex/src/com/cloudempiere/searchindex/util/ISearchResult.java): Interface for search results.

### Key Methods

- `PGTextSearchIndexProvider.searchIndexDocument(String searchIndexName, String query, boolean isAdvanced)`: Searches the index for documents matching the query.
- `PGTextSearchIndexProvider.createIndex(Properties ctx, Map<Integer, Set<SearchIndexRecord>> indexRecordsMap, String trxName)`: Creates the search index.
- `SearchIndexHelper.recreateIndex(String indexType)`: Recreates the search index.
- `SearchIndexHelper.updateDocument(String documentType, PO po, boolean isNew)`: Updates a document in the search index.

## com.cloudempiere.searchindex.ui

This plugin provides the user interface components for the search indexing functionality. It includes UI elements and configurations to allow users to interact with the search index.

### Key Classes

- [`com.cloudempiere.searchindex.ui.SearchIndexItemRenderer`](com.cloudempiere.searchindex.ui/src/com/cloudempiere/searchindex/ui/SearchIndexItemRenderer.java): Renders search index items in the UI.
- [`com.cloudempiere.searchindex.ui.ZkSearchIndexUI`](com.cloudempiere.searchindex.ui/src/com/cloudempiere/searchindex/ui/ZkSearchIndexUI.java): Main UI component for search indexing.
- [`com.cloudempiere.searchindex.ui.dashboard.DPSearchIndexPanel`](com.cloudempiere.searchindex.ui/src/com/cloudempiere/searchindex/ui/dashboard/DPSearchIndexPanel.java): Dashboard panel for search indexing.

### Key Methods

- `ZkSearchIndexUI.onEvent(Event e)`: Handles UI events.
- `ZkSearchIndexUI.showResults(boolean show, ErrorLabel error)`: Displays search results in the UI.
- `SearchIndexItemRenderer.render(Listitem item, TextSearchResult result, int index)`: Renders a search result item.
- `DPSearchIndexPanel.onEvent(Event e)`: Handles events in the dashboard panel.

### UI Components

- `Combobox searchCombobox`: Dropdown for selecting search options.
- `Checkbox cbAdvancedSearch`: Checkbox for enabling advanced search.
- `Listbox resultListbox`: Listbox for displaying search results.
- `WNoData noRecordsWidget`: Widget for displaying "no records found" message.
- `WNoData noIndexWidget`: Widget for displaying "no index found" message.

# License

This project is licensed under the terms of the GNU General Public License (GPL) version 2. See the [LICENSE](LICENSE) file for details.