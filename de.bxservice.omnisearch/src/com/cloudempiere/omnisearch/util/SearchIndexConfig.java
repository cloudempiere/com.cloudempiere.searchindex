package com.cloudempiere.omnisearch.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified class for AD_SearchIndex
 */
public class SearchIndexConfig {

	/* AD_SearchIndex_ID */
	private int searchIndexId;
	/* List of Search Index Tables */
    private List<SearchIndexTableConfig> tableConfigs;

    public SearchIndexConfig(int searchIndexId) {
    	this.searchIndexId = searchIndexId;
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

}
