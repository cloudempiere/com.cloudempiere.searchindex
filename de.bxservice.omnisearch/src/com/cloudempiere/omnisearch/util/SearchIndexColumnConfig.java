package com.cloudempiere.omnisearch.util;

/**
 * Simplified class for AD_SearchIndexColumn
 */
public class SearchIndexColumnConfig {

	/* AD_Table_ID */
	private int tableId;
	/* TableName */
	private String tableName;
	/* AD_Column_ID */
	private int columnId;
	/* ColumnName */
	private String columnName;
	/* ParentColumn_ID */
	private int parentColumnId;
	/* Parent Column Name */
	private String parentColumnName;
	/* Parent Column Name for foreign key */
	private String parentTableName;

    public SearchIndexColumnConfig(int tableId, String tableName, int columnId, String columnName, int parentColumnId, String parentColumnName, String parentTableName) {
		this.tableId = tableId;
		this.tableName = tableName;
		this.columnId = columnId;
		this.columnName = columnName;
		this.parentColumnId = parentColumnId;
		this.parentColumnName = parentColumnName;
		this.parentTableName = parentTableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

	public String getParentColumnName() {
		return parentColumnName;
	}

	public void setParentColumnName(String parentColumnName) {
		this.parentColumnName = parentColumnName;
	}

	public int getColumnId() {
		return columnId;
	}

	public void setColumnId(int columnId) {
		this.columnId = columnId;
	}

	public int getParentColumnId() {
		return parentColumnId;
	}

	public void setParentColumnId(int parentColumnId) {
		this.parentColumnId = parentColumnId;
	}

	public int getTableId() {
		return tableId;
	}

	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getParentTableName() {
		return parentTableName;
	}

	public void setParentTableName(String parentTableName) {
		this.parentTableName = parentTableName;
	}

}
