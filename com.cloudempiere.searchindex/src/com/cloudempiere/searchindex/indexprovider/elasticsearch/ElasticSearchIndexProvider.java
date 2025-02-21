/**********************************************************************
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Peter Takacs, Cloudempiere                                        *
 **********************************************************************/
package com.cloudempiere.searchindex.indexprovider.elasticsearch;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.adempiere.util.IProcessUI;

import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider;
import com.cloudempiere.searchindex.model.MSearchIndexProvider;
import com.cloudempiere.searchindex.util.ISearchResult;
import com.cloudempiere.searchindex.util.pojo.SearchIndexTableData;

/**
 * 
 * Search index provider for Elastic Search
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class ElasticSearchIndexProvider implements ISearchIndexProvider {

	@Override
	public void init(MSearchIndexProvider searchIndexProvider, IProcessUI processUI) {
		
	}

	@Override
	public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName) {
		
	}

	@Override
	public void updateIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName) {
		
	}

	@Override
	public void deleteAllIndex(Properties ctx, String trxName) {
		
	}

	@Override
	public void deleteIndexByQuery(Properties ctx, String searchIndexName, String query, Object[] params, String trxName) {
		
	}

	@Override
	public void reCreateIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName) {
		
	}

	@Override
	public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName, String queryString,
			boolean isAdvanced, SearchType searchType, String trxName) {
		return null;
	}

	@Override
	public void setHeadline(Properties ctx, ISearchResult result, String query, String trxname) {
		
	}

	@Override
	public boolean isIndexPopulated(Properties ctx, String searchIndexName, String trxName) {
		return false;
	}

	@Override
	public int getAD_SearchIndexProvider_ID() {
		return 0;
	}
}
