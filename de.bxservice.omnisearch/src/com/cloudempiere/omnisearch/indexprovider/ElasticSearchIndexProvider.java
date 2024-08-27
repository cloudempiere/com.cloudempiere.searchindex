package com.cloudempiere.omnisearch.indexprovider;

import java.util.List;

import com.cloudempiere.omnisearch.model.MSearchIndexProvider;

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
	public void createIndex(int ad_table_id, int record_id, String... values) {

	}

}
