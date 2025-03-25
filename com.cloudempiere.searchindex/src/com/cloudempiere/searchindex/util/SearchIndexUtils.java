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
* - Diego Ruiz, BX Service GmbH                                       *
* - Peter Takacs, Cloudempiere                                        *
**********************************************************************/
package com.cloudempiere.searchindex.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.util.IProcessUI;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.cloudempiere.searchindex.event.pojo.IndexedTable;
import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider;
import com.cloudempiere.searchindex.indexprovider.SearchIndexProviderFactory;
import com.cloudempiere.searchindex.model.MSearchIndexProvider;

/**
 * 
 * Search Index utility methods
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class SearchIndexUtils {

	/** Logger */
	private static final CLogger log = CLogger.getCLogger(SearchIndexUtils.class);
	
	/**
	 * Get and initialise Search Index Providers by Client
	 * @param clientId - AD_Client_ID
	 * @return
	 */
	public static List<ISearchIndexProvider> getSearchIndexProvidersByClient(Properties ctx, int clientId, IProcessUI processUI, String trxName) {
		List<ISearchIndexProvider> providerList = new ArrayList<>();
		SearchIndexProviderFactory factory = new SearchIndexProviderFactory();
		for(MSearchIndexProvider providerDef : MSearchIndexProvider.getByClient(ctx, clientId, trxName)) {		
			ISearchIndexProvider provider = factory.getSearchIndexProvider(providerDef.getClassname());
			if (provider != null) {
				provider.init(providerDef, processUI);
				providerList.add(provider);
			}
		}
		return providerList;
	}
	
	/**
	 * Get and initialise Search Index Provider
	 * @param searchIndexProviderId - AD_SearchIndexProvider_ID
	 * @return
	 */
	public static ISearchIndexProvider getSearchIndexProvider(Properties ctx, int searchIndexProviderId, IProcessUI processUI, String trxName) {
		MSearchIndexProvider providerDef = MSearchIndexProvider.get(ctx, searchIndexProviderId, trxName);		
		SearchIndexProviderFactory factory = new SearchIndexProviderFactory();
		ISearchIndexProvider provider = factory.getSearchIndexProvider(providerDef.getClassname());
		if (provider != null)
			provider.init(providerDef, processUI);
		return provider;
	}
	
	/**
	 * Get Search Index Names for Provider
	 * @param searchIndexProviderId - AD_SearchIndexProvider_ID
	 * @return
	 */
	public static String[] getSearchIndexNamesForProvider(Properties ctx, int searchIndexProviderId, String trxName) {
		String sql = "SELECT SearchIndexName FROM AD_SearchIndex WHERE IsActive = 'Y' AND AD_SearchIndexProvider_ID = ? ";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<String> searchIndexNames = new ArrayList<>();
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, searchIndexProviderId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				searchIndexNames.add(rs.getString("SearchIndexName"));
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
		}
		return searchIndexNames.toArray(new String[searchIndexNames.size()]);
	}
	
	/**
	 * Get Transaction Code from Search Index
	 * @param ctx
	 * @param clientId - AD_Client_ID
	 * @param searchIndexProviderId - AD_SearchIndexProvider_ID
	 * @param trxName
	 * @return
	 */
	public static Map<String, String> getTransactionCodesByClient(Properties ctx, int clientId, int searchIndexProviderId, String trxName) {
		Map<String, String> transactionCodeMap = new HashMap<>();
		String sql = "SELECT TransactionCode, SearchIndexName FROM AD_SearchIndex WHERE IsActive = 'Y' AND AD_Client_ID IN (?,0) AND AD_SearchIndexProvider_ID = ? ";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, clientId);
			pstmt.setInt(2, searchIndexProviderId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				transactionCodeMap.put(rs.getString("TransactionCode"), rs.getString("SearchIndexName"));
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
		}
		return transactionCodeMap;
	}
	
	/**
	 * Get indexed table names
	 * @param trxName
	 * @param clientId
	 * @return map of indexed tables (AD_Client_ID, Set<IndexedTable>)
	 */
	public static Map<Integer, Set<IndexedTable>> getSearchIndexConfigs(String trxName, int clientId) {
	    StringBuilder sql = new StringBuilder("SELECT sit.AD_SearchIndex_ID, t.TableName, mt.TableName, sit.WhereClause, si.SearchIndexName, ")
	    	.append("COALESCE(refCol.AD_Column_ID, sic.AD_Column_ID) AS AD_Column_ID, si.AD_Client_ID ")
	    	.append("FROM AD_SearchIndexColumn sic ")
	        .append("JOIN AD_SearchIndexTable sit ON sic.AD_SearchIndexTable_ID = sit.AD_SearchIndexTable_ID ")
	        .append("JOIN AD_SearchIndex si ON si.AD_SearchIndex_ID = sit.AD_SearchIndex_ID ")
	        .append("JOIN AD_Table t ON t.AD_Table_ID = sic.AD_Table_ID ")
	        .append("JOIN AD_Table mt ON mt.AD_Table_ID = sit.AD_Table_ID ") // main table
	        .append("LEFT JOIN AD_Reference ref ON ref.AD_Reference_ID = sic.AD_Reference_Value_ID ")
	        .append("LEFT JOIN AD_Ref_Table refTable ON refTable.AD_Reference_ID = ref.AD_Reference_ID ")
	        .append("LEFT JOIN AD_Column refCol ON refCol.AD_Column_ID = refTable.AD_Display ")
	        .append("WHERE sit.IsActive = 'Y' AND sic.IsActive = 'Y'");
	    if (clientId >= 0)
	    	sql.append(" AND sit.AD_Client_ID IN (0, ?)");

	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    Map<Integer, Set<IndexedTable>> indexedTablesByClient = new HashMap<>();
	    try {
	        pstmt = DB.prepareStatement(sql.toString(), trxName);
	        if (clientId >= 0)
	        	pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
	        rs = pstmt.executeQuery();
	        while (!Thread.currentThread().isInterrupted() && rs.next()) {
	        	int searchIndexId = rs.getInt(1);
	        	String fkTableName = rs.getString(2);
	        	String mainTableName = rs.getString(3);
	        	String whereClause = rs.getString(4);
	        	String searchIndexName = rs.getString(5);
	        	int columnId = rs.getInt(6); // refCol.AD_Column_ID or sic.AD_Column_ID
	        	int indexClientId = rs.getInt(7);

	        	Set<IndexedTable> indexedTables = indexedTablesByClient.computeIfAbsent(indexClientId, k -> new HashSet<>());
	        	
	        	IndexedTable indexedTable = indexedTables.stream()
	                .filter(config -> (config.getSearchIndexId() == searchIndexId && config.getTableName().equals(mainTableName)))
	                .findFirst()
	                .orElseGet(() -> {
	                	IndexedTable newIndexedTable = new IndexedTable(searchIndexId, searchIndexName, mainTableName, whereClause);
	                	indexedTables.add(newIndexedTable);
	                    return newIndexedTable;
	                });
	        	
	        	if (!mainTableName.equals(fkTableName))
	        		indexedTable.addFKTableName(fkTableName);
	        	
	        	indexedTable.addColumnId(columnId);
	        }
	    } catch (Exception e) {
	        log.log(Level.SEVERE, sql.toString(), e);
	    } finally {
	        DB.close(rs, pstmt);
	        rs = null;
	        pstmt = null;
	    }

	    return indexedTablesByClient;
	}
}

