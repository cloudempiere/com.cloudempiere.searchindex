package com.cloudempiere.omnisearch.indexprovider;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.cloudempiere.omnisearch.model.MSearchIndexProvider;
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
	public List<Object> searchIndexDocument(String queryString) {
		return null;
	}

	@Override
	public List<Object> searchIndexDocument(String queryString, int maxRow) {
		return null;
	}

	@Override
	public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexRecord>> indexRecordsMap, String trxName) {

	}

}
