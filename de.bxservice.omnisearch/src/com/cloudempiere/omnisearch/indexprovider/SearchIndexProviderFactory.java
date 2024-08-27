package com.cloudempiere.omnisearch.indexprovider;

/**
 * 
 * Search Index Provider factory
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class SearchIndexProviderFactory {

	public ISearchIndexProvider getSearchIndexProvider(String Classname) {
		if(Classname.equals("com.cloudempiere.omnisearch.indexprovider.ElasticSearchIndexProvider"))
			return new ElasticSearchIndexProvider();
		return null;
	}

}
