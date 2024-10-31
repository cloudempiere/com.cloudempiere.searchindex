package com.cloudempiere.searchindex.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Search Index Record
 */
public class SearchIndexRecord {

	/* AD_Table_ID */
    private int tableId;
    /* Key Column Name */
    private String keyColName;
	/* Index Table Name */
	private String searchIndexName;
    /* Data: ColumnName - Value */
    private Set<Map<String, Object>> tableData;

    public SearchIndexRecord(int tableId, String keyColName, String searchIndexName) {
        this.tableId = tableId;
        this.keyColName = keyColName;
        this.searchIndexName = searchIndexName;
        this.tableData = new HashSet<>();
    }

    public int getTableId() {
        return tableId;
    }
    
    public String getKeyColName() {
    	return keyColName;
    }

    public Set<Map<String, Object>> getTableData() {
        return tableData;
    }

    public void addTableData(Map<String, Object> data) {
        tableData.add(data);
    }

    public String getSearchIndexName() {
		return searchIndexName;
	}

	@Override
    public String toString() {
        return "SearchIndexRecord{" +
                "tableId='" + tableId + '\'' +
                ", tableData=" + tableData +
                '}';
    }
}