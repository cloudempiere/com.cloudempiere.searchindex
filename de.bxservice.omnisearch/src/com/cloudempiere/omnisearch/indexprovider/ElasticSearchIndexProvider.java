/**
 * 
 */
package com.cloudempiere.omnisearch.indexprovider;

import java.util.List;

import com.cloudempiere.omnisearch.model.MSearchIndexProvider;

/**
 * @author developer
 *
 */
public class ElasticSearchIndexProvider implements ISearchIndexProvider {

	/**
	 * 
	 */
	public ElasticSearchIndexProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(MSearchIndexProvider searchIndexProvider, String searchIndexName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllIndex() {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteIndexByQuery(String query) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object searchIndexNoRestriction(String queryString) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> searchIndexDocument(String queryString) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> searchIndexDocument(String queryString, int maxRow) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createIndex(int ad_table_id, int record_id, String... values) {
		// TODO Auto-generated method stub

	}

}
