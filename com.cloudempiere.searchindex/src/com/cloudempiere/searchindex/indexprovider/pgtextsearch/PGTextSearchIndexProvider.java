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
package com.cloudempiere.searchindex.indexprovider.pgtextsearch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.IProcessUI;
import org.compiere.model.MClient;
import org.compiere.model.MRole;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider;
import com.cloudempiere.searchindex.model.MSearchIndexProvider;
import com.cloudempiere.searchindex.util.ISearchResult;
import com.cloudempiere.searchindex.util.pojo.SearchIndexData;

/**
 * 
 * Search Index Provider for PostgreSQL Text Search
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class PGTextSearchIndexProvider implements ISearchIndexProvider {

	private HashMap<Integer, String> indexQuery = new HashMap<>();
	private MSearchIndexProvider searchIndexProvider;
	private IProcessUI processUI;
	
    @Override
    public void init(MSearchIndexProvider searchIndexProvider, IProcessUI processUI) {
    	this.searchIndexProvider = searchIndexProvider;
    	this.processUI = processUI;
    }
    
    @Override
	public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexData>> indexRecordsMap, String trxName) {
		if (indexRecordsMap == null) {
			return;
		}
	
	    String tsConfig = getTSConfig(ctx, trxName);
	    Map<String, PreparedStatement> preparedStatementMap = new HashMap<>();
	
	    try {
	        for (Map.Entry<Integer, Set<SearchIndexData>> searchIndexRecordSet : indexRecordsMap.entrySet()) {
	            for (SearchIndexData searchIndexRecord : searchIndexRecordSet.getValue()) {
	                String tableName = searchIndexRecord.getSearchIndexName();
	                String upsertQuery = "INSERT INTO " + tableName + " " +
	                                     "(ad_client_id, ad_table_id, record_id, idx_tsvector) VALUES (?, ?, ?, to_tsvector(?::regconfig, ?::text)) " +
	                                     "ON CONFLICT (ad_table_id, record_id) DO UPDATE SET idx_tsvector = EXCLUDED.idx_tsvector";
	
	                PreparedStatement pstmt = preparedStatementMap.get(tableName);
	                if (pstmt == null) {
	                    pstmt = DB.prepareStatement(upsertQuery, trxName);
	                    preparedStatementMap.put(tableName, pstmt);
	                }
	                
	                int i = 0;
	                for (Map<String, Object> tableDataSet : searchIndexRecord.getTableData()) {
	                    if (tableDataSet.get("Record_ID") == null)
	                        continue;
	                    
	                    updateProcessUIStatus("Preparing " + tableName + "(" + i + "/" + searchIndexRecord.getTableData().size() + ")"); // TODO translate
	                    
	                    String documentContent = extractDocumentContent(tableDataSet);
	
	                    int idx = 1;
	                    pstmt.setInt(idx++, Env.getAD_Client_ID(ctx));
	                    pstmt.setInt(idx++, searchIndexRecord.getTableId());
	                    pstmt.setInt(idx++, Integer.parseInt(tableDataSet.get("Record_ID").toString()));
	                    pstmt.setString(idx++, tsConfig);
	                    pstmt.setString(idx++, documentContent);
	                    pstmt.addBatch();
	                    i++;
	                }
	            }
	        }
	
	        updateProcessUIStatus("Inserting data..."); // TODO translate
	        for (PreparedStatement pstmt : preparedStatementMap.values()) {
	            pstmt.executeBatch();
	            pstmt.close();
	        }
	
	    } catch (Exception e) {
	        throw new AdempiereException(e);
	    } finally {
	        for (PreparedStatement pstmt : preparedStatementMap.values()) {
	            try {
	                if (pstmt != null && !pstmt.isClosed()) {
	                    pstmt.close();
	                }
	            } catch (SQLException e) {
	                log.severe(e.getMessage());
	            }
	        }
	    }
	}
	
	@Override
	public void updateIndex(Properties ctx, Map<Integer, Set<SearchIndexData>> indexRecordsMap, String trxName) {
		createIndex(ctx, indexRecordsMap, trxName); // uses upsert
	}

	@Override
	public void deleteAllIndex(Properties ctx, String trxName) {
		updateProcessUIStatus("Preparing data to be deleted..."); // TODO translate
		List<String> tables = getAllSearchIndexTables(ctx, trxName);
		int i = 0;
		for (String tableName : tables) {
			String sql = "TRUNCATE TABLE " + tableName + " RESTART IDENTITY";
			DB.executeUpdateEx(sql, trxName);
			updateProcessUIStatus("Deleted " + i + "/" + tables.size()); // TODO translate
			i++;
		}
	}

    @Override
    public void deleteIndexByQuery(Properties ctx, String searchIndexName, String query, Object[] params, String trxName) {
    	if (Util.isEmpty(searchIndexName)) {
    		updateProcessUIStatus("Preparing data to be deleted..."); // TODO translate
    		List<String> tables = getAllSearchIndexTables(ctx, trxName);
    		String sql;
    		int i = 0;
    		for (String tableName : tables) {
    			if (Util.isEmpty(query))
    				sql = "TRUNCATE TABLE " + tableName + " RESTART IDENTITY";
    			else
    				sql = "DELETE FROM " + tableName + " WHERE " + query;
    			DB.executeUpdateEx(sql, params, trxName);
    			updateProcessUIStatus("Deleted " + i + "/" + tables.size()); // TODO translate
    			i++;
    		}
    	} else {
	        String sql;
	        if (Util.isEmpty(query))
				sql = "TRUNCATE TABLE " + searchIndexName + " RESTART IDENTITY";
			else
				sql = "DELETE FROM " + searchIndexName + " WHERE " + query;
	        DB.executeUpdateEx(sql, params, trxName);
    	}
    }
	
	@Override
	public void reCreateIndex(Properties ctx, Map<Integer, Set<SearchIndexData>> indexRecordsMap, String trxName) {
		deleteAllIndex(ctx, trxName);
		createIndex(ctx, indexRecordsMap, trxName);
	}

	@Override
	public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName, String query, boolean isAdvanced, String trxName) {
		ArrayList<ISearchResult> results = new ArrayList<>();
		indexQuery.clear();

		StringBuilder sql = new StringBuilder();
		List<String> tablesToSearch = new ArrayList<>();
		List<Object> params = new ArrayList<>();
		
		String tsConfig = getTSConfig(ctx, trxName);
		int clientId = Env.getAD_Client_ID(ctx);

		if (Util.isEmpty(searchIndexName)) {
			tablesToSearch.addAll(getAllSearchIndexTables(ctx, trxName));
		} else {
			tablesToSearch.add(searchIndexName);
		}

		for (int i = 0; i < tablesToSearch.size(); i++) {
			String tableName = tablesToSearch.get(i);
			sql.append("SELECT DISTINCT ad_table_id, record_id, ts_rank(idx_tsvector, to_tsquery(?::regconfig, ?::text)) as rank ");
			sql.append("FROM ");
			sql.append(tableName);
			sql.append(" WHERE idx_tsvector @@ ");
			params.add(tsConfig);
			params.add(isAdvanced ? convertQueryString(query) : query.replace(" ", " & "));			

			params.add(tsConfig);
			if (isAdvanced) {
				sql.append("to_tsquery(?::regconfig, ?::text) ");
				params.add(convertQueryString(query));
			} else {
				sql.append("plainto_tsquery(?::regconfig, ?::text) ");
				params.add(query.replace(" ", " & "));
			}

			sql.append("AND AD_CLIENT_ID IN (0,?) ");
			params.add(clientId);
			
			sql.append("ORDER BY rank DESC ");

			if (i < tablesToSearch.size() - 1) {
				sql.append(" UNION ");
			}
		}

		MRole role = MRole.getDefault(ctx, false);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), trxName);
			for (int i = 0; i < params.size(); i++) {
				pstmt.setObject(i + 1, params.get(i));
			}
			rs = pstmt.executeQuery();

			PGTextSearchResult result = null;
			int i = 0;
			while (rs.next()) {
				int AD_Table_ID = rs.getInt(1);
				int recordID = rs.getInt(2);
				double rank = rs.getDouble(3);
				
				// FIXME: uncomment and discuss
//				int AD_Window_ID = Env.getZoomWindowID(AD_Table_ID, recordID);
//				
//				if (AD_Window_ID > 0 && role.getWindowAccess(AD_Window_ID) == null)
//					continue;
//				if (AD_Window_ID > 0 && !role.isRecordAccess(AD_Table_ID, recordID, true))
//					continue;
				
				result = new PGTextSearchResult();
				result.setAD_Table_ID(AD_Table_ID);
				result.setRecord_ID(recordID);
				result.setRank(rank);
				results.add(result);

				if (i < 10) {
					setHeadline(ctx, result, query, trxName);
				}
				i++;
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql.toString(), e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return results;
	}
    
    @Override
	public void setHeadline(Properties ctx, ISearchResult result, String query, String trxName) { // TODO validate if this method must be in the SearchIndexProvider interface
		
		if (result.getHtmlHeadline() != null && !result.getHtmlHeadline().isEmpty())
			return;

		StringBuilder sql = new StringBuilder();

		if (indexQuery.get(result.getAD_Table_ID()) != null) {
			sql.append(indexQuery.get(result.getAD_Table_ID()));
		} else {

			ArrayList<Integer> columnIds = null;//getIndexedColumns(result.getAD_Table_ID()); // TODO

			if(columnIds == null || columnIds.isEmpty()) {
				result.setHtmlHeadline("");
				return;
			}

			sql.append("SELECT ts_headline(body, q) FROM (");			
//			sql.append(getIndexSql(columnIds, result.getAD_Table_ID())); // TODO

			indexQuery.put(result.getAD_Table_ID(), sql.toString());
		}

		//Bring the table ids that are indexed
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), trxName);
			pstmt.setString(1, convertQueryString(query));
			pstmt.setInt(2, Env.getAD_Client_ID(ctx));
			pstmt.setInt(3, result.getRecord_ID());
			rs = pstmt.executeQuery();

			while (!Thread.currentThread().isInterrupted() && rs.next())
			{
				result.setHtmlHeadline(rs.getString(1));
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	@Override
	public boolean isIndexPopulated(Properties ctx, String searchIndexName, String trxName) {
		int count = 0;
		String sqlSelect = "SELECT COUNT(1) "
				+ "FROM " + searchIndexName
				+ " WHERE AD_Client_ID IN (0,?)";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			pstmt = DB.prepareStatement(sqlSelect, trxName);
			pstmt.setInt(1, Env.getAD_Client_ID(ctx));
			rs = pstmt.executeQuery();
			if(rs.next()) {
				count = rs.getInt(1);
			}
		} catch (Exception e) {
			log.severe(e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return count > 0;
	}

	@Override
	public int getAD_SearchIndexProvider_ID() {
		if(searchIndexProvider != null) {
			return searchIndexProvider.getAD_SearchIndexProvider_ID();
		}
		return 0;
	}

	/**
	 * Get all search index tables.
	 * @return
	 */
	private List<String> getAllSearchIndexTables(Properties ctx, String trxName) {
		List<String> tables = new ArrayList<>();
		String sql = "SELECT SearchIndexName FROM AD_SearchIndex WHERE IsActive = 'Y' AND AD_Client_ID IN (0,?) AND AD_SearchIndexProvider_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, Env.getAD_Client_ID(ctx));
			pstmt.setInt(2, searchIndexProvider.getAD_SearchIndexProvider_ID());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				tables.add(rs.getString(1));
			}
		} catch (Exception e) {
			log.severe(e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return tables;
	}
    
    /**
     * Extracts the document content from the table data set.
     *
     * @param tableDataSet the table data set
     * @return the document content
     */
    private String extractDocumentContent(Map<String, Object> tableDataSet) {
        StringBuilder documentContent = new StringBuilder();
        for (Map.Entry<String, Object> entry : tableDataSet.entrySet()) {
            if (entry.getValue() != null) {
                documentContent.append(entry.getValue().toString()).append(" ");
            }
        }
        return documentContent.toString().trim();
    }
    
    /**
	 * Gets the text search configuration to use. 
	 * @return the text search configuration
	 */
    private String getTSConfig(Properties ctx, String trxName) {
		// Check if the specified text search configuration exists for language
        String tsConfig = MClient.get(ctx).getLanguage().getLocale().getDisplayLanguage(Locale.ENGLISH);
        String fallbackConfig = "unaccent";
        String checkConfigQuery = "SELECT COUNT(*) FROM pg_ts_config WHERE cfgname = ?";
        int configCount = DB.getSQLValue(trxName, checkConfigQuery, tsConfig);

        if (configCount == 0) {
            log.log(Level.INFO, "Text search configuration '" + tsConfig + "' does not exist. Falling back to '" + fallbackConfig + "'.");
            tsConfig = fallbackConfig;
        }
		return tsConfig;
	}
    
    /**
	 * Converts a String to a valid to_Tsquery String
	 * @param queryString
	 * @return
	 */
	private String convertQueryString(String queryString) {
		queryString = queryString.trim(); //(Remove leading and trailing spaces
		if (!queryString.contains("&"))
			queryString =  queryString.replace(" ", "&"); //If the query does not include &, handle spaces as & strings
		
		return queryString;
	}
	
	/**
	 * Update the process UI status
	 * @param message
	 */
	private void updateProcessUIStatus(String message) {
		if (processUI != null) {
			processUI.statusUpdate(message);
		}
	}
}