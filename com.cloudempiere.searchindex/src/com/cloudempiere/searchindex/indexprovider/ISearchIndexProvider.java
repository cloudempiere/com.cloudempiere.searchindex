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

package com.cloudempiere.searchindex.indexprovider;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.adempiere.util.IProcessUI;
import org.compiere.util.CLogger;

import com.cloudempiere.searchindex.model.MSearchIndexProvider;
import com.cloudempiere.searchindex.util.ISearchResult;
import com.cloudempiere.searchindex.util.pojo.SearchIndexTableData;

/**
 * 
 * Search Index Provider interface
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public interface ISearchIndexProvider
{
	public static CLogger log = CLogger.getCLogger (ISearchIndexProvider.class);
	
	public void init(MSearchIndexProvider searchIndexProvider, IProcessUI processUI);

	/**
	 * Create index
	 * @param ctx
	 * @param indexRecordsMap - key is AD_SearchIndex_ID
	 * @param trxName
	 */
	public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName);
	
	/**
	 * Update index
	 * @param ctx
	 * @param po
	 * @param indexTableName
	 * @param columnIDs
	 * @param trxName
	 */
	public void updateIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName);
	
	/**
	 * Delete index
	 * @param ctx
	 * @param indexRecordsMap - key is AD_SearchIndex_ID
	 * @param trxName
	 */
	public void deleteAllIndex(Properties ctx, String trxName);

	/**
	 * Delete index by query
	 * @param ctx
	 * @param searchIndexName
	 * @param query
	 * @param params
	 * @param trxName
	 */
	public void deleteIndexByQuery(Properties ctx, String searchIndexName, String query, Object[] params, String trxName);

	/**
	 * Recreate index
	 * @param ctx
	 * @param indexRecordsMap - key is AD_SearchIndex_ID
	 * @param trxName
	 */
	public void reCreateIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName);

	/**
	 * Get search results
	 * @param ctx
	 * @param searchIndexName
	 * @param queryString
	 * @param isAdvanced
	 * @param trxName
	 * @return
	 */
	public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName, String queryString, boolean isAdvanced, SearchType searchType, String trxName);
	
	/**
	 * Get search results
	 * @param ctx
	 * @param result
	 * @param query
	 * @param trxname
	 */
	public void setHeadline(Properties ctx, ISearchResult result, String query, String trxname);

	/**
	 * Get search results
	 * @param ctx
	 * @param searchIndexName
	 * @param trxName
	 * @return
	 */
	public boolean isIndexPopulated(Properties ctx, String searchIndexName, String trxName);
	
	/**
	 * Get AD_SearchIndexProvider_ID
	 * @return
	 */
	public int getAD_SearchIndexProvider_ID();
	
	/**
	 * Search type
	 */
	public enum SearchType {
	    TS_RANK,
	    POSITION
	}
	
}
