package com.cloudempiere.searchindex.event.pojo;

import java.util.HashSet;
import java.util.Set;

public class SearchIndexTableConfigSimple {

	/* AD_SearchIndex_ID */
	private int searchIndexId;
	/* AD_SearchIndex.SearchIndexName */
	private String searchIndexName;
	/* TableName */
	private String tableName;
	/* Table Names of Indexed FK Tables */
    private Set<String> fkTableNames;

    public SearchIndexTableConfigSimple(int searchIndexId, String searchIndexName, String tableName) {
    	this.searchIndexId = searchIndexId;
    	this.searchIndexName = searchIndexName;
        this.tableName = tableName;
        this.fkTableNames = new HashSet<>();
    }

    public String getTableName() {
        return tableName;
    }

    public Set<String> getFKTableNames() {
        return fkTableNames;
    }

    public void addFKTableName(String fkTableName) {
        this.fkTableNames.add(fkTableName);
    }

	public int getSearchIndexId() {
		return searchIndexId;
	}
	
	public String getSearchIndexName() {
		return searchIndexName;
	}

}
