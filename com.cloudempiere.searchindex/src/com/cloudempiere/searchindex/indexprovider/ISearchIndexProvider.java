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
import org.compiere.model.PO;
import org.compiere.util.CLogger;

import com.cloudempiere.searchindex.model.MSearchIndexProvider;
import com.cloudempiere.searchindex.util.ISearchResult;
import com.cloudempiere.searchindex.util.SearchIndexRecord;

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
	
	public void deleteAllIndex(String trxName);

	public void deleteIndexByQuery(String searchIndexName, String query, Object[] params, String trxName);

	public Object searchIndexNoRestriction(String searchIndexName, String queryString);

	public List<ISearchResult> searchIndexDocument(String searchIndexName, String queryString, boolean isAdvanced);
	
	public void setHeadline(ISearchResult result, String query);

	public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexRecord>> indexRecordsMap, String trxName);
	
	public void createIndex(String trxName, String indexTableName, int tableId, int recordId, int[] columnIDs);
	
	public void updateIndex(Properties ctx, PO po, String indexTableName, int[] columnIdList, String trxName);
	
	public void reCreateIndex(Properties ctx, Map<Integer, Set<SearchIndexRecord>> indexRecordsMap, String trxName);
	
	public boolean isIndexPopulated(String searchIndexName);
	
	public int getAD_SearchIndexProvider_ID();
	
}
