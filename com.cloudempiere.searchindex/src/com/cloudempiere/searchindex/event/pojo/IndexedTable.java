package com.cloudempiere.searchindex.event.pojo;

import java.util.HashSet;
import java.util.Set;

public class IndexedTable {

	/** AD_SearchIndex_ID */
	private int searchIndexId;
	/** AD_SearchIndex.SearchIndexName */
	private String searchIndexName;
	/** TableName */
	private String tableName;
	/** AD_Table_ID */
	private int tableId;
	/** Set of AD_Table_ID of Indexed Tables */
	private Set<Integer> fkTableIDs;
	/** Where Clause */
	private String whereClause;
	/** Table Names of Indexed FK Tables */
	private Set<String> fkTableNames;
	/** Indexed Columns - AD_Column_IDs */
	private Set<Integer> columnIds;

    public IndexedTable(int searchIndexId, String searchIndexName, String tableName, int tableId, String whereClause) {
    	this.searchIndexId = searchIndexId;
    	this.searchIndexName = searchIndexName;
        this.tableName = tableName;
        this.tableId = tableId;
        this.whereClause = whereClause;
        this.fkTableIDs = new HashSet<>();
        this.fkTableNames = new HashSet<>();
        this.columnIds = new HashSet<>();
    }

    public String getTableName() {
        return tableName;
    }
	
	public int getTableId() {
		return tableId;
	}
    
    public String getWhereClause() {
    	return whereClause;
    }
    
    public void addFKTableName(String fkTableName) {
    	this.fkTableNames.add(fkTableName);
    }
    
    public void addFKTableName(int fkTableId) {
    	this.fkTableIDs.add(fkTableId);
    }

    public Set<String> getFKTableNames() {
        return fkTableNames;
    }

	public int getSearchIndexId() {
		return searchIndexId;
	}
	
	public String getSearchIndexName() {
		return searchIndexName;
	}

	public void addColumnId(int columnId) {
		columnIds.add(columnId);
	}
	
	public Set<Integer> getColumnIDs() {
		return columnIds;
	}

}
