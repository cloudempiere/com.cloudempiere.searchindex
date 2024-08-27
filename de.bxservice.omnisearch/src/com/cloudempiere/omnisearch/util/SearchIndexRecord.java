package com.cloudempiere.omnisearch.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Search Index Record
 */
public class SearchIndexRecord {

    private String tableName;
    private Set<Map<String, Object>> tableData;

    public SearchIndexRecord(String tableName) {
        this.tableName = tableName;
        this.tableData = new HashSet<>();
    }

    public String getTableName() {
        return tableName;
    }

    public Set<Map<String, Object>> getTableData() {
        return tableData;
    }

    public void addTableData(Map<String, Object> data) {
        tableData.add(data);
    }

    @Override
    public String toString() {
        return "SearchIndexRecord{" +
                "tableName='" + tableName + '\'' +
                ", tableData=" + tableData +
                '}';
    }
}