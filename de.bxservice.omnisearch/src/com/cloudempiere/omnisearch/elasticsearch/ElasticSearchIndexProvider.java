package com.cloudempiere.omnisearch.elasticsearch;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.cloudempiere.omnisearch.indexprovider.ISearchIndexProvider;
import com.cloudempiere.omnisearch.model.MSearchIndexProvider;
import com.cloudempiere.omnisearch.pgtextsearch.TextSearchResult;
import com.cloudempiere.omnisearch.util.ISearchResult;
import com.cloudempiere.omnisearch.util.SearchIndexRecord;

/**
 * 
 * Search index provider for Elastic Search
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class ElasticSearchIndexProvider implements ISearchIndexProvider {

	@Override
	public void init(MSearchIndexProvider searchIndexProvider, String searchIndexName) {
		
	}

	@Override
	public void deleteAllIndex() {

	}

	@Override
	public void deleteIndexByQuery(String query) {

	}

	@Override
	public Object searchIndexNoRestriction(String queryString) {
		return null;
	}

	@Override
	public List<ISearchResult> searchIndexDocument(String queryString, boolean isAdvanced) {
		return null;
	}

	@Override
	public void setHeadline(ISearchResult result, String query) {
		
	}
	
	@Override
	public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexRecord>> indexRecordsMap, String trxName) {

	}
	
	@Override
	public boolean isIndexPopulated() {
		return false;
	}

}
