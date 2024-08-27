package com.cloudempiere.omnisearch.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified class for AD_SearchIndexTable
 */
public class SearchIndexTableConfig {

	/* AD_Table_ID */
	private int tableId;
	/* TableName */
	private String tableName;
	/* Search Index Columns */
    private List<SearchIndexColumnConfig> columns;

    public SearchIndexTableConfig(String tableName, int tableId) {
    	this.tableId = tableId;
        this.tableName = tableName;
        this.columns = new ArrayList<>();
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<SearchIndexColumnConfig> getColumns() {
        return columns;
    }

    public void addColumn(SearchIndexColumnConfig column) {
        this.columns.add(column);
    }

	public int getTableId() {
		return tableId;
	}

	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

}
