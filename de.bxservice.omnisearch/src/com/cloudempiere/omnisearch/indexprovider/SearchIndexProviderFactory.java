package com.cloudempiere.omnisearch.indexprovider;

import org.compiere.util.Util;

import com.cloudempiere.omnisearch.elasticsearch.ElasticSearchIndexProvider;
import com.cloudempiere.omnisearch.pgtextsearch.PGTextSearchIndexProvider;

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
		if(classname.equals("com.cloudempiere.omnisearch.indexprovider.ElasticSearchIndexProvider"))
			return new ElasticSearchIndexProvider();
		if(classname.equals("com.cloudempiere.omnisearch.indexprovider.PGTextSearchIndexProvider"))
			return new PGTextSearchIndexProvider();
		return null;
	}

}
