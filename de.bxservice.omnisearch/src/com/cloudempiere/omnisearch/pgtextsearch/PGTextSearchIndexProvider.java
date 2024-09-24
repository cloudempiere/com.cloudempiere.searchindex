package com.cloudempiere.omnisearch.pgtextsearch;

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
import org.compiere.model.MClient;
import org.compiere.model.MRole;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.cloudempiere.omnisearch.indexprovider.ISearchIndexProvider;
import com.cloudempiere.omnisearch.model.MSearchIndexProvider;
import com.cloudempiere.omnisearch.util.ISearchResult;
import com.cloudempiere.omnisearch.util.SearchIndexRecord;

public class PGTextSearchIndexProvider implements ISearchIndexProvider {

	private HashMap<Integer, String> indexQuery = new HashMap<>();
	
    @Override
    public void init(MSearchIndexProvider searchIndexProvider, String searchIndexName) {
        // Initialize any necessary configurations or connections
    }

    @Override
    public void deleteAllIndex(String searchIndexName) {
        String sql = "DELETE FROM " + searchIndexName;
        DB.executeUpdate(sql, null);
    }

    @Override
    public void deleteIndexByQuery(String searchIndexName, String query) {
        String sql = "DELETE FROM " + searchIndexName + " WHERE " + query;
        DB.executeUpdate(sql, null);
    }

    @Override
    public Object searchIndexNoRestriction(String searchIndexName, String queryString) {
        ArrayList<TextSearchResult> results = new ArrayList<>();
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
                TextSearchResult result = new TextSearchResult();
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
		sql.append("SELECT DISTINCT ad_table_id, record_id ");
		sql.append("FROM ");
		sql.append(searchIndexName);
		sql.append(" WHERE idx_tsvector @@ ");

		if (isAdvanced) 
			sql.append("to_tsquery('" + convertQueryString(query) + "') ");
		else
			sql.append("plainto_tsquery('" + query + "') ");

		sql.append("AND AD_CLIENT_ID IN (0,?) ");
		
		MRole role = MRole.getDefault(Env.getCtx(), false);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			rs = pstmt.executeQuery();

			TextSearchResult result = null;
			int i = 0;
			while (rs.next()) {
				int AD_Table_ID = rs.getInt(1);
				int recordID = rs.getInt(2);
				
				int AD_Window_ID = Env.getZoomWindowID(AD_Table_ID, recordID);
				
				if (AD_Window_ID > 0 && role.getWindowAccess(AD_Window_ID) == null)
					continue;
				if (AD_Window_ID > 0 && !role.isRecordAccess(AD_Table_ID, recordID, true))
					continue;
				
				result = new TextSearchResult();
				result.setAD_Table_ID(AD_Table_ID);
				result.setRecord_ID(recordID);
				results.add(result);

				if (i < 10) {
					setHeadline(result, query);
				}
				i++;
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

		return results;
    }
    
    @Override
	public void setHeadline(ISearchResult result, String query) {
		
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
	
	                for (Map<String, Object> tableDataSet : searchIndexRecord.getTableData()) {
	                    if (tableDataSet.get("Record_ID") == null)
	                        continue;
	
	                    String documentContent = extractDocumentContent(tableDataSet);
	
	                    int idx = 1;
	                    pstmt.setInt(idx++, Env.getAD_Client_ID(ctx));
	                    pstmt.setInt(idx++, searchIndexRecord.getTableId());
	                    pstmt.setInt(idx++, Integer.parseInt(tableDataSet.get("Record_ID").toString()));
	                    pstmt.setString(idx++, tsConfig);
	                    pstmt.setString(idx++, documentContent);
	                    pstmt.addBatch();
	                }
	            }
	        }
	
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
}