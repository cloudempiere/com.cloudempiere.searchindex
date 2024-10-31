package com.cloudempiere.searchindex.indexprovider;

import org.compiere.util.Util;

import com.cloudempiere.searchindex.elasticsearch.ElasticSearchIndexProvider;
import com.cloudempiere.searchindex.pgtextsearch.PGTextSearchIndexProvider;

/**
 * 
 * Search Index Provider factory
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class SearchIndexProviderFactory {

	public ISearchIndexProvider getSearchIndexProvider(String classname) {
		if(Util.isEmpty(classname))
			return null;
		else
			classname = classname.trim();
		if(classname.equals("com.cloudempiere.searchindex.indexprovider.ElasticSearchIndexProvider"))
			return new ElasticSearchIndexProvider();
		if(classname.equals("com.cloudempiere.searchindex.indexprovider.PGTextSearchIndexProvider"))
			return new PGTextSearchIndexProvider();
		return null;
	}

}
