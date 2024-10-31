package com.cloudempiere.searchindex.util;

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
	/* Key Column Name */
	private String keyColName;
	/* WhereClause */
	private String sqlWhere;
	/* Search Index Columns */
    private List<SearchIndexColumnConfig> columns;

    public SearchIndexTableConfig(String tableName, int tableId, String keyColName, String sqlWhere) {
    	this.tableId = tableId;
        this.tableName = tableName;
        this.keyColName = keyColName;
        this.sqlWhere = sqlWhere;
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

	public String getKeyColName() {
		return keyColName;
	}

	public void setKeyColName(String keyColName) {
		this.keyColName = keyColName;
	}

	public String getSqlWhere() {
		return sqlWhere;
	}

	public void setSqlWhere(String sqlWhere) {
		this.sqlWhere = sqlWhere;
	}

}
