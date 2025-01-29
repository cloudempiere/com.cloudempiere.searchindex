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
package com.cloudempiere.searchindex.pgtextsearch;

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
import org.compiere.model.MColumn;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider;
import com.cloudempiere.searchindex.model.MSearchIndexProvider;
import com.cloudempiere.searchindex.util.ISearchResult;
import com.cloudempiere.searchindex.util.SearchIndexRecord;

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
	public void deleteAllIndex(String trxName) {
		updateProcessUIStatus("Preparing data to be deleted..."); // TODO translate
		List<String> tables = getAllSearchIndexTables();
		int i = 0;
		for (String tableName : tables) {
			String sql = "TRUNCATE TABLE " + tableName + " RESTART IDENTITY";
			DB.executeUpdate(sql, trxName);
			updateProcessUIStatus("Deleted " + i + "/" + tables.size()); // TODO translate
			i++;
		}
	}

    @Override
    public void deleteIndexByQuery(String searchIndexName, String query, Object[] params, String trxName) {
    	if (Util.isEmpty(searchIndexName)) {
    		updateProcessUIStatus("Preparing data to be deleted..."); // TODO translate
    		List<String> tables = getAllSearchIndexTables();
    		String sql;
    		int i = 0;
    		for (String tableName : tables) {
    			if (Util.isEmpty(query))
    				sql = "TRUNCATE TABLE " + tableName + " RESTART IDENTITY";
    			else
    				sql = "DELETE FROM " + tableName + " WHERE " + query;
    			DB.executeUpdate(sql, params, false, trxName, 0);
    			updateProcessUIStatus("Deleted " + i + "/" + tables.size()); // TODO translate
    			i++;
    		}
    	} else {
	        String sql;
	        if (Util.isEmpty(query))
				sql = "TRUNCATE TABLE " + searchIndexName + " RESTART IDENTITY";
			else
				sql = "DELETE FROM " + searchIndexName + " WHERE " + query;
	        DB.executeUpdate(sql, params, false, trxName, 0);
    	}
    }

    @Override
    public Object searchIndexNoRestriction(String searchIndexName, String queryString) {
        ArrayList<PGTextSearchResult> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ad_table_id, record_id ");
        sql.append("FROM ");
        sql.append(searchIndexName);
        sql.append(" WHERE to_tsvector('english', document) @@ to_tsquery('english', ?)");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql.toString(), null);
            pstmt.setString(1, queryString);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                PGTextSearchResult result = new PGTextSearchResult();
                result.setAD_Table_ID(rs.getInt("ad_table_id"));
                result.setRecord_ID(rs.getInt("record_id"));
                results.add(result);
            }
        } catch (Exception e) {
            throw new AdempiereException(e);
        } finally {
            DB.close(rs, pstmt);
        }
        return results;
    }

	@Override
	public List<ISearchResult> searchIndexDocument(String searchIndexName, String query, boolean isAdvanced) {
		ArrayList<ISearchResult> results = new ArrayList<>();
		indexQuery.clear();

		StringBuilder sql = new StringBuilder();
		List<String> tablesToSearch = new ArrayList<>();

		if (Util.isEmpty(searchIndexName)) {
			tablesToSearch.addAll(getAllSearchIndexTables());
		} else {
			tablesToSearch.add(searchIndexName);
		}

		for (int i = 0; i < tablesToSearch.size(); i++) {
			String tableName = tablesToSearch.get(i);
			sql.append("SELECT DISTINCT ad_table_id, record_id ");
			sql.append("FROM ");
			sql.append(tableName);
			sql.append(" WHERE idx_tsvector @@ ");

			if (isAdvanced) 
				sql.append("to_tsquery('" + convertQueryString(query) + "') ");
			else
				sql.append("plainto_tsquery('" + query + "') ");

			sql.append("AND AD_CLIENT_ID IN (0,?) ");

			if (i < tablesToSearch.size() - 1) {
				sql.append(" UNION ");
			}
		}

		MRole role = MRole.getDefault(Env.getCtx(), false);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), null);
			for (int i = 0; i < tablesToSearch.size(); i++) {
				pstmt.setInt(i + 1, Env.getAD_Client_ID(Env.getCtx()));
			}
			rs = pstmt.executeQuery();

			PGTextSearchResult result = null;
			int i = 0;
			while (rs.next()) {
				int AD_Table_ID = rs.getInt(1);
				int recordID = rs.getInt(2);
				
				int AD_Window_ID = Env.getZoomWindowID(AD_Table_ID, recordID);
				
				if (AD_Window_ID > 0 && role.getWindowAccess(AD_Window_ID) == null)
					continue;
				if (AD_Window_ID > 0 && !role.isRecordAccess(AD_Table_ID, recordID, true))
					continue;
				
				result = new PGTextSearchResult();
				result.setAD_Table_ID(AD_Table_ID);
				result.setRecord_ID(recordID);
				results.add(result);

				if (i < 10) {
					setHeadline(result, query);
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

	/**
	 * Get all search index tables.
	 * @return
	 */
	private List<String> getAllSearchIndexTables() {
		List<String> tables = new ArrayList<>();
		String sql = "SELECT SearchIndexName FROM AD_SearchIndex WHERE IsActive = 'Y' AND AD_Client_ID IN (0,?) AND AD_SearchIndexProvider_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
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
    
    @Override
	public void setHeadline(ISearchResult result, String query) { // TODO validate if this method must be in the SearchIndexProvider interface
		
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
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setString(1, convertQueryString(query));
			pstmt.setInt(2, Env.getAD_Client_ID(Env.getCtx()));
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
	public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexRecord>> indexRecordsMap, String trxName) {
		if (indexRecordsMap == null) {
			return;
		}
	
	    String tsConfig = getTSConfig();
	    Map<String, PreparedStatement> preparedStatementMap = new HashMap<>();
	
	    try {
	        for (Map.Entry<Integer, Set<SearchIndexRecord>> searchIndexRecordSet : indexRecordsMap.entrySet()) {
	            for (SearchIndexRecord searchIndexRecord : searchIndexRecordSet.getValue()) {
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
	public void createIndex(String trxName, String indexTableName, int tableId, int recordId, int[] columnIDs) {
		if (columnIDs == null || columnIDs.length <= 0)
			return;

		log.log(Level.INFO, "Indexing " + tableId + " " + columnIDs);

		StringBuilder upsertQuery = new StringBuilder();
	    upsertQuery.append("INSERT INTO ");
	    upsertQuery.append(indexTableName);
	    upsertQuery.append(" (ad_client_id, ad_table_id, record_id, idx_tsvector) ");

	    String selectQuery = getSelectQuery(tableId, columnIDs, false, recordId > 0);

	    if (selectQuery == null) {
	        log.log(Level.WARNING, "A table with more than one key column cannot be indexed");
	    } else {
	        upsertQuery.append(selectQuery);
	        upsertQuery.append(" ON CONFLICT (AD_Table_ID, Record_ID) DO UPDATE SET idx_tsvector = EXCLUDED.idx_tsvector ");

	        Object[] params = null;
	        if (recordId > 0)
	            params = new Object[]{Env.getAD_Client_ID(Env.getCtx()), recordId};
	        else if (Env.getAD_Client_ID(Env.getCtx()) > 0)
	            params = new Object[]{Env.getAD_Client_ID(Env.getCtx())};

	        try {
	            DB.executeUpdateEx(upsertQuery.toString(), params, trxName);
	        } catch (Exception e) {
	            log.log(Level.SEVERE, upsertQuery.toString());
	            throw new AdempiereException(e);
	        }
	    }
	}
	
	@Override
	public void updateIndex(Properties ctx, PO po, String indexTableName, int[] columnIDs, String trxName) {
		/*
		 * index table name
		 * indexed column IDs
		 */
		StringBuilder updateQuery = new StringBuilder();
		updateQuery.append("UPDATE ");
		updateQuery.append(indexTableName);
		updateQuery.append(" SET ");
		updateQuery.append("idx_tsvector ");
		
		updateQuery.append(" = (");
		MTable table = MTable.get(Env.getCtx(), po.get_Table_ID());
		String mainTableAlias = "a";
		
		updateQuery.append("SELECT ");
		updateQuery.append("to_tsvector(");
		updateQuery.append("'" + getTSConfig() + "', "); //Language Parameter config

		//Columns that want to be indexed
		MColumn column = null;
		//TableName, List of validations after the ON clause
		ArrayList<String> joinClauses = null;
		for (int i = 0; i < columnIDs.length; i++) {
			column = MColumn.get(Env.getCtx(), columnIDs[i]);
			String foreignTableName = column.getReferenceTableName();

			if (foreignTableName != null) {
				String foreignAlias = "a" + i;
				MTable foreignTable = MTable.get(Env.getCtx(), foreignTableName);

				if (joinClauses == null)
					joinClauses = new ArrayList<>();

				joinClauses.add(getJoinClause(foreignTable, mainTableAlias, foreignAlias, column));
				updateQuery.append(getForeignValues(foreignTable, foreignAlias));

			} else {
				updateQuery.append("COALESCE(");
				updateQuery.append(mainTableAlias);
				updateQuery.append(".");
				updateQuery.append(column.getColumnName());
			}

			if (i < columnIDs.length -1)
				updateQuery.append(",'') || ' ' || "); //space between words
			else
				updateQuery.append(",'') ");
		}
		updateQuery.append(") ");

		updateQuery.append(" FROM ");
		updateQuery.append(table.getTableName());
		updateQuery.append(" " + mainTableAlias);

		if (joinClauses != null && joinClauses.size() > 0) {
			for (String joinClause : joinClauses)
				updateQuery.append(joinClause);
		}

		updateQuery.append(" WHERE " + mainTableAlias + ".AD_Client_ID = ?");
		updateQuery.append(" AND ");
		updateQuery.append(mainTableAlias + ".");
		updateQuery.append(table.getKeyColumns()[0]); //Record_ID
		updateQuery.append(" = ? )");
		
		updateQuery.append(" WHERE AD_Table_ID = ? AND Record_ID = ?");
		
		DB.executeUpdateEx(updateQuery.toString(), 
				new Object[] {Env.getAD_Client_ID(Env.getCtx()), po.get_ID(), po.get_Table_ID(), po.get_ID()}, 
				trxName);
	}
	
	@Override
	public void reCreateIndex(Properties ctx, Map<Integer, Set<SearchIndexRecord>> indexRecordsMap, String trxName) {
		deleteAllIndex(trxName);
		createIndex(ctx, indexRecordsMap, trxName);
	}

	@Override
	public boolean isIndexPopulated(String searchIndexName) {
		int count = 0;
		String sqlSelect = "SELECT COUNT(1) "
				+ "FROM " + searchIndexName
				+ " WHERE AD_Client_ID IN (0,?)";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			
			pstmt = DB.prepareStatement(sqlSelect, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
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
    private String getTSConfig() {
		// Check if the specified text search configuration exists
        String tsConfig = MClient.get(Env.getCtx()).getLanguage().getLocale().getDisplayLanguage(Locale.ENGLISH);
        String fallbackConfig = "simple";
        String checkConfigQuery = "SELECT COUNT(*) FROM pg_ts_config WHERE cfgname = ?";
        int configCount = DB.getSQLValue(null, checkConfigQuery, tsConfig);

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

	@Override
	public int getAD_SearchIndexProvider_ID() {
		if(searchIndexProvider != null) {
			return searchIndexProvider.getAD_SearchIndexProvider_ID();
		}
		return 0;
	}
	
	/**
	 * Gets the values of the identifiers columns when a FK is selected as Index
	 */
	private String getForeignValues(MTable table, String tableAlias) {

		String[] identifierColumns = table.getIdentifierColumns(); 

		if (identifierColumns != null && identifierColumns.length > 0) {
			StringBuilder foreingColumns = new StringBuilder();

			for (int i = 0; i < identifierColumns.length; i++) {
				foreingColumns.append("COALESCE(");
				foreingColumns.append(tableAlias);
				foreingColumns.append(".");
				foreingColumns.append(identifierColumns[i]);

				if (i < identifierColumns.length -1)
					foreingColumns.append(",'') || ' ' || "); //space between words
			}

			return foreingColumns.toString();
		}
		else
			return null;
	}

	private String getJoinClause(MTable foreignTable, String tableAlias, String foreignTableAlias, MColumn currentColumn) {

		if (foreignTable == null || currentColumn == null)
			return null;

		StringBuilder joinClause = new StringBuilder();
		joinClause.append(" LEFT JOIN ");
		joinClause.append(foreignTable.getTableName());
		joinClause.append(" " + foreignTableAlias);
		joinClause.append(" ON ");
		joinClause.append(tableAlias);
		joinClause.append(".");
		joinClause.append(currentColumn.getColumnName());
		joinClause.append(" = ");
		joinClause.append(foreignTableAlias);
		joinClause.append(".");
		joinClause.append(foreignTable.getKeyColumns()[0]);

		return joinClause.toString();
	}
	
	private String getSelectQuery(int AD_Table_ID, int[] columnIDs, boolean isSearch, boolean isSingleRecord) {

		MTable table = MTable.get(Env.getCtx(), AD_Table_ID);
		String mainTableAlias = "a";
		StringBuilder selectQuery = new StringBuilder();
		selectQuery.append("SELECT ");
		
		if (!isSearch) {
			selectQuery.append(mainTableAlias + ".AD_Client_ID"); //AD_Client_ID
			selectQuery.append(", ");
			selectQuery.append(AD_Table_ID); //AD_Table_ID
			selectQuery.append(", ");

			if (table.getKeyColumns() != null && table.getKeyColumns().length == 1) 
				selectQuery.append(table.getKeyColumns()[0]); //Record_ID
			else
				return null;

			selectQuery.append(", ");
			
			selectQuery.append("to_tsvector(");
			selectQuery.append("'" + getTSConfig() + "', "); //Language Parameter config		
		}

		//Columns that want to be indexed
		MColumn column = null;
		//TableName, List of validations after the ON clause
		ArrayList<String> joinClauses = null;
		for (int i = 0; i < columnIDs.length; i++) {
			int AD_Column_ID = columnIDs[i];
			column = MColumn.get(Env.getCtx(), AD_Column_ID);
			String foreignTableName = column.getReferenceTableName();

			if (foreignTableName != null) {
				String foreignAlias = "a" + i;
				MTable foreignTable = MTable.get(Env.getCtx(), foreignTableName);

				if (joinClauses == null)
					joinClauses = new ArrayList<>();

				joinClauses.add(getJoinClause(foreignTable, mainTableAlias, foreignAlias, column));
				selectQuery.append(getForeignValues(foreignTable, foreignAlias));

			} else {
				selectQuery.append("COALESCE(");
				selectQuery.append(mainTableAlias);
				selectQuery.append(".");
				selectQuery.append(column.getColumnName());
			}

			if (i < columnIDs.length -1)
				selectQuery.append(",'') || ' ' || "); //space between words
			else
				selectQuery.append(",'') ");
		}

		if (isSearch)
			selectQuery.append(" AS body, q ");
		else
			selectQuery.append(") ");

		selectQuery.append(" FROM ");
		selectQuery.append(table.getTableName());
		selectQuery.append(" " + mainTableAlias);

		if (joinClauses != null && joinClauses.size() > 0) {
			for (String joinClause : joinClauses)
				selectQuery.append(joinClause);
		}

		if (isSearch) {
	    	selectQuery.append(",");
	    	selectQuery.append("to_tsquery(?) q");
	    	selectQuery.append(" WHERE " + mainTableAlias + ".AD_Client_ID = ?");
	    	selectQuery.append(" AND ");
	    	selectQuery.append(mainTableAlias + ".");
			selectQuery.append(table.getKeyColumns()[0]); //Record_ID
			selectQuery.append(" = ? ) AS foo WHERE body @@ q");
		} else {
			String whereClause = null;
			//If System -> All clients
			if (Env.getAD_Client_ID(Env.getCtx()) != 0) {
				whereClause = " WHERE " + mainTableAlias + ".AD_Client_ID = ? ";
			}
			if (isSingleRecord) {
				if (whereClause != null)
					whereClause = whereClause + " AND ";
				else
					whereClause = " WHERE ";

				whereClause = whereClause + mainTableAlias + "." 
						+ table.getKeyColumns()[0] //Record_ID
								+ " = ? ";
			}
			if (whereClause != null)
				selectQuery.append(whereClause);
		}

		return selectQuery.toString();
	}
}