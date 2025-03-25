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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import org.adempiere.util.IProcessUI;
import org.compiere.model.MClient;
import org.compiere.model.MRole;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider;
import com.cloudempiere.searchindex.model.MSearchIndexProvider;
import com.cloudempiere.searchindex.util.ISearchResult;
import com.cloudempiere.searchindex.util.pojo.SearchIndexColumnData;
import com.cloudempiere.searchindex.util.pojo.SearchIndexTableData;

/**
 * 
 * Search Index Provider for PostgreSQL Text Search
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class PGTextSearchIndexProvider implements ISearchIndexProvider {

	/* Weights thresholds in percents */
    private static final NavigableMap<BigDecimal, String> WEIGHT_THRESHOLDS = new TreeMap<>();
    static {
        WEIGHT_THRESHOLDS.put(new BigDecimal("75"), "A");
        WEIGHT_THRESHOLDS.put(new BigDecimal("50"), "B");
        WEIGHT_THRESHOLDS.put(new BigDecimal("25"), "C");
        WEIGHT_THRESHOLDS.put(BigDecimal.ZERO, "D");
    }
    
    /* Valid tsquery operators for to_tsquery function */
    private static final String OPERATOR_AND = "&";
    private static final String OPERATOR_OR = "|";
    private static final String OPERATOR_NOT = "!";
    private static final String OPERATOR_FOLLOWED_BY = "<->";

	private HashMap<Integer, String> indexQuery = new HashMap<>();
	private MSearchIndexProvider searchIndexProvider;
	private IProcessUI processUI;
	
    @Override
    public void init(MSearchIndexProvider searchIndexProvider, IProcessUI processUI) {
    	this.searchIndexProvider = searchIndexProvider;
    	this.processUI = processUI;
    }
    
    @Override
	public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName) {
	    if (indexRecordsMap == null) {
	        return;
	    }
	    String tsConfig = getTSConfig(ctx, trxName);
	    for (Map.Entry<Integer, Set<SearchIndexTableData>> searchIndexRecordSet : indexRecordsMap.entrySet()) {
            for (SearchIndexTableData searchIndexRecord : searchIndexRecordSet.getValue()) {
                String tableName = searchIndexRecord.getSearchIndexName();

                int i = 0;
                for (Map<String, SearchIndexColumnData> tableDataSet : searchIndexRecord.getColumnData()) {
                    if (tableDataSet.get("Record_ID") == null)
                        continue;

                    updateProcessUIStatus("Preparing " + tableName + "(" + i + "/" + searchIndexRecord.getColumnData().size() + ")"); // TODO translate

                    List<Object> params = new ArrayList<>();
                    params.add(Env.getAD_Client_ID(ctx));
                    params.add(searchIndexRecord.getTableId());
                    params.add(Integer.parseInt(tableDataSet.get("Record_ID").getValue().toString()));
                    String documentContent = documentContentToTsvector(tableDataSet, tsConfig, params);
                    StringBuilder upsertQuery = new StringBuilder();
                    upsertQuery.append("INSERT INTO ").append(tableName).append(" ")
                               .append("(ad_client_id, ad_table_id, record_id, idx_tsvector) VALUES (?, ?, ?, ")
                               .append(documentContent).append(") ")
                               .append("ON CONFLICT (ad_table_id, record_id) DO UPDATE SET idx_tsvector = EXCLUDED.idx_tsvector");
                    DB.executeUpdateEx(upsertQuery.toString(), params.toArray(), trxName);
                    i++;
                }
            }
        }

        updateProcessUIStatus("Inserting data..."); // TODO translate
	}
	
	@Override
	public void updateIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName) {
		createIndex(ctx, indexRecordsMap, trxName); // uses upsert
	}

	@Override
	public void deleteIndex(Properties ctx, String searchIndexName, String trxName) {
		deleteIndex(ctx, searchIndexName, null, null, trxName);
	}

    @Override
    public void deleteIndex(Properties ctx, String searchIndexName, String dynWhere, Object[] dynParams, String trxName) {
    	List<Object> params = null;
    	if (Util.isEmpty(searchIndexName)) {
    		updateProcessUIStatus("Preparing data to be deleted..."); // TODO translate
    		List<String> tables = getAllSearchIndexTables(ctx, trxName);
    		String sql;
    		int i = 0;
    		for (String tableName : tables) {
    			sql = "DELETE FROM " + tableName + " WHERE AD_Client_ID IN (0,?)";
    			params = new ArrayList<>();
    			params.add(Env.getAD_Client_ID(ctx));
    			if (!Util.isEmpty(dynWhere)) {
    				if (!dynWhere.trim().toUpperCase().startsWith("AND")) {
    					dynWhere = " AND " + dynWhere;
    				}
    				sql += dynWhere;
    				if (dynParams != null) {
    					for (Object param : dynParams) {
    						params.add(param);
    					}
    				}
    			}
    			DB.executeUpdateEx(sql, params.toArray(), trxName);
    			updateProcessUIStatus("Deleted " + i + "/" + tables.size()); // TODO translate
    			i++;
    		}
    	} else {
    		String sql = "DELETE FROM " + searchIndexName + " WHERE AD_Client_ID IN (0,?)";
    		params = new ArrayList<>();
    		params.add(Env.getAD_Client_ID(ctx));
    		if (!Util.isEmpty(dynWhere)) {
    			if (!dynWhere.trim().toUpperCase().startsWith("AND")) {
    				dynWhere = " AND " + dynWhere;
    			}
    			sql += dynWhere;
    			if (dynParams != null) {
    				for (Object param : dynParams) {
    					params.add(param);
    				}
    			}					
    		}
    		DB.executeUpdateEx(sql, params.toArray(), trxName);
    	}
    }
	
	@Override
	public void reCreateIndex(Properties ctx, Map<Integer, Set<SearchIndexTableData>> indexRecordsMap, String trxName) {
		Set<String> searchIndexNames = new HashSet<>();
		for (Map.Entry<Integer, Set<SearchIndexTableData>> searchIndexRecordSet : indexRecordsMap.entrySet()) {
			for (SearchIndexTableData searchIndexRecord : searchIndexRecordSet.getValue()) {
				searchIndexNames.add(searchIndexRecord.getSearchIndexName());
			}
		}
		for (String searchIndexName : searchIndexNames) {
			deleteIndex(ctx, searchIndexName, trxName);
		}
		createIndex(ctx, indexRecordsMap, trxName);
	}

    @Override
    public List<ISearchResult> getSearchResults(Properties ctx, String searchIndexName, String query, boolean isAdvanced, SearchType searchType, String trxName) {
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
            sql.append("SELECT DISTINCT ad_table_id, record_id, ")
            	.append(getRank(query, isAdvanced, searchType, params, tsConfig))
	            .append(" as rank FROM ")
	            .append(tableName)
            	.append(" WHERE idx_tsvector @@ ");
            
            params.add(tsConfig);
            if (isAdvanced) {
                sql.append("to_tsquery(?::regconfig, ?::text) ");
                params.add(convertQueryString(query));
            } else {
                sql.append("plainto_tsquery(?::regconfig, ?::text) ");
                params.add(query);
            }

            sql.append("AND AD_CLIENT_ID IN (0,?) ");
            params.add(clientId);

            sql.append("ORDER BY rank ");
            switch (searchType) {
				case TS_RANK:
					sql.append("DESC ");
					break;
				case POSITION:
					sql.append("ASC ");
					break;
				default:
					break;
			}

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
//                int AD_Window_ID = Env.getZoomWindowID(AD_Table_ID, recordID);
//                
//                if (AD_Window_ID > 0 && role.getWindowAccess(AD_Window_ID) == null)
//                    continue;
//                if (AD_Window_ID > 0 && !role.isRecordAccess(AD_Table_ID, recordID, true))
//                    continue;

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
			pstmt.setString(1, convertQueryString(query)); // TODO add IsAdvanced check?
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
     * Extracts the document content from the table data set 
     * converts each part to tsvector with added weight, 
     * finally concatenates the tsvectors.
     * 
     * E.g. setweight(to_tsvector(value1::regconfig,tsConfig::text),'A') || setweight(to_tsvector(value2::regconfig,tsConfig::text),'B')
     *
     * @param tableDataSet the table data set
     * @return the document content
     */
    private String documentContentToTsvector(Map<String, SearchIndexColumnData> tableDataSet, String tsConfig, List<Object> params) {
        StringBuilder documentContent = new StringBuilder();
        for (Map.Entry<String, SearchIndexColumnData> entry : tableDataSet.entrySet()) {
        	SearchIndexColumnData columnData = entry.getValue();
            if (columnData != null) {
            	String tsWeight = getTSWeight(columnData.getSearchWeight(), columnData.getMaxSearchWeight());
                documentContent.append("setweight(")
                	.append("to_tsvector('")
                	.append(tsConfig).append("'::regconfig,")
                	.append("?::text)")
                	.append(",").append("'").append(tsWeight).append("')")
                	.append(" || ");
                params.add(Objects.toString(columnData.getValue(), ""));
            }
        }
        documentContent.setLength(documentContent.length() - 4); // Remove the last " || "
        return documentContent.toString();
    }
    
	/**
     * Gets the weight for the given search weight.
     * @param searchWeight the search weight
     * @return weight
     */
    private String getTSWeight(BigDecimal searchWeight, BigDecimal maxSearchWeight) {
    	// For negative values return the lowest weight
        if (searchWeight.compareTo(BigDecimal.ZERO) < 0) {
            return WEIGHT_THRESHOLDS.get(BigDecimal.ZERO);
        }
        // Calculate the weight based on the search weight and the maximum search weight
        BigDecimal weight = searchWeight.divide(maxSearchWeight, 2, RoundingMode.HALF_UP).multiply(Env.ONEHUNDRED);
        return WEIGHT_THRESHOLDS.floorEntry(weight).getValue();
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
     * Converts a String to a valid to_tsquery String. <br>
     * Only AND (&) operator is supported <br>
     * Prefix search (:*) and weighted search (:ABCD) is supported <br>
     * @see <a href="https://www.postgresql.org/docs/current/textsearch-controls.html">PostgreSQL Text Search Controls</a>
     * @param queryString
     * @return
     */
	private String convertQueryString(String queryString) {
	    queryString = queryString.trim();
	    
	    // Remove all operators
	    queryString = queryString.replace(OPERATOR_AND, "")
	                             .replace(OPERATOR_OR, "")
	                             .replace(OPERATOR_NOT, "")
	                             .replace(OPERATOR_FOLLOWED_BY, "");
	    
	    // Remove abandoned controls and handle duplicate controls
	    queryString = queryString.replaceAll("\\s+:\\*", "")
	    		.replaceAll("\\s+:[A-D]", "")
	    		.replaceAll("(:\\*|:[A-D]){2,}", "");
	    
	    // Replace all spaces with OPERATOR_AND
	    queryString = queryString.trim();
	    queryString = queryString.replace(" ", " "+OPERATOR_AND+" ");
	    
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
	
	/**
	 * Gets the rank SQL based on the search type.
	 * @param query the query
	 * @param isAdvanced the advanced search flag
	 * @param searchType the search type
	 * @param params the parameters
	 * @param tsConfig the text search configuration
	 * @return the rank SQL
	 */
	private String getRank(String query, boolean isAdvanced, SearchType searchType, List<Object> params, String tsConfig) {
	    StringBuilder rankSql = new StringBuilder();
	    String[] searchTerms = query.split(" ");
	
	    switch (searchType) {
	        case TS_RANK:
	            rankSql.append("ts_rank(idx_tsvector, to_tsquery(?::regconfig, ?::text)) ");
	            params.add(tsConfig);
	            params.add(isAdvanced ? convertQueryString(query) : query);
	            break;
	        case POSITION:
	            rankSql.append("(");
	            for (int i = 0; i < searchTerms.length; i++) {
	                String term = searchTerms[i];
	        	    // Remove valid (currently supported) operators for advanced search
	                String regexQuery = isAdvanced ? escapeSpecialCharacters(term.replace("&", "").replace(":*", "")) : escapeSpecialCharacters(term);
	                if (i > 0) {
	                    rankSql.append(" + ");
	                }
	                // Match full and partial matches and consider weights
	                rankSql.append("COALESCE(")
	                       .append("(SELECT (regexp_match(idx_tsvector::text, '").append(regexQuery).append("[^'']*'':(\\d+)([A])'))[1]::int), ")
	                       .append("(SELECT (regexp_match(idx_tsvector::text, '").append(regexQuery).append("[^'']*'':(\\d+)([B])'))[1]::int), ")
	                       .append("(SELECT (regexp_match(idx_tsvector::text, '").append(regexQuery).append("[^'']*'':(\\d+)([C])'))[1]::int), ")
	                       .append("1000)"); // a large number to deprioritize non-matches
	            }
	            rankSql.append(") ");
	            break;
	        default:
	            break;
	    }
	    return rankSql.toString();
	}

	/**
	 * Escapes special characters in a string for use in a PostgreSQL regular expression.
	 * @param term the term to escape
	 * @return the escaped term
	 */
	private String escapeSpecialCharacters(String term) {
	    StringBuilder escapedTerm = new StringBuilder();
	    for (char c : term.toCharArray()) {
	        if ("\\.*+?^${}()|[]".indexOf(c) != -1) {
	            escapedTerm.append("\\");
	        }
	        escapedTerm.append(c);
	    }
	    return escapedTerm.toString();
	}
}