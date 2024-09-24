package com.cloudempiere.omnisearch.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified class for AD_SearchIndex
 */
public class SearchIndexConfig {

	/* AD_SearchIndex_ID */
	private int searchIndexId;
	/* SearchIndexName */
	private String searchIndexName;
	/* List of Search Index Tables */
    private List<SearchIndexTableConfig> tableConfigs;

    public SearchIndexConfig(int searchIndexId, String searchIndexName) {
    	this.searchIndexId = searchIndexId;
    	this.searchIndexName = searchIndexName;
        this.tableConfigs = new ArrayList<>();
    }

    public List<SearchIndexTableConfig> getTableConfigs() {
        return tableConfigs;
    }

    public void addTableConfig(SearchIndexTableConfig tableConfig) {
        this.tableConfigs.add(tableConfig);
    }

	public int getSearchIndexId() {
		return searchIndexId;
	}

	public void setSearchIndexId(int searchIndexId) {
		this.searchIndexId = searchIndexId;
	}

	public String getSearchIndexName() {
		return searchIndexName;
	}

	public void setSearchIndexName(String searchIndexName) {
		this.searchIndexName = searchIndexName;
	}

}
