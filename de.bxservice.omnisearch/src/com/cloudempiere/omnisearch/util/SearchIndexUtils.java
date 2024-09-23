package com.cloudempiere.omnisearch.util;

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

import org.cloudempiere.util.WebFormUtil;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.cloudempiere.omnisearch.indexprovider.ISearchIndexProvider;
import com.cloudempiere.omnisearch.indexprovider.SearchIndexProviderFactory;
import com.cloudempiere.omnisearch.model.MSearchIndexProvider;

/**
 * 
 * Search Index utility methods
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class SearchIndexUtils {

	/** Logger */
	private static final CLogger log = CLogger.getCLogger(WebFormUtil.class);
	
	/**
	 * Get the indexed tables and columns
	 * @param ctx
	 * @param trxName
	 * @return
	 * @throws SQLException
	 */
	public static List<SearchIndexConfig> loadSearchIndexConfig(Properties ctx, String trxName) throws SQLException {
		List<SearchIndexConfig> searchIndexConfigs = new ArrayList<>();

        String combinedQuery = 
        		"SELECT "
        		+ "si.AD_SearchIndex_ID, "
        		+ "mainT.AD_Table_ID as AD_Table_ID_main, "
        		+ "mainT.TableName as TableName_main, "
        		+ "mainKeyCol.ColumnName as ColumnName_mainkey , "
        		+ "tbl.AD_Table_ID, "
        		+ "tbl.TableName, "
        		+ "col.AD_Column_ID, "
        		+ "col.ColumnName,"
        		+ "sic.AD_Reference_ID, "
        		+ "parentCol.AD_Column_ID as AD_Column_ID_parent, "
        		+ "parentCol.ColumnName as ColumnName_parent "
				+ "FROM AD_SearchIndex si "
				+ "JOIN AD_SearchIndexTable sit ON (si.AD_SearchIndex_ID = sit.AD_SearchIndex_ID) "
				+ "JOIN AD_Table mainT ON (sit.AD_Table_ID = mainT.AD_Table_ID) "
				+ "LEFT JOIN AD_Column mainKeyCol ON (mainT.AD_Table_ID = mainKeyCol.AD_Table_ID AND mainKeyCol.IsKey = 'Y') "
				+ "JOIN AD_SearchIndexColumn sic ON (sit.AD_SearchIndexTable_ID = sic.AD_SearchIndexTable_ID) "
				+ "JOIN AD_Table tbl ON (sic.AD_Table_ID = tbl.AD_Table_ID) "
				+ "JOIN AD_Column col ON (sic.AD_Column_ID = col.AD_Column_ID) "
				+ "LEFT JOIN AD_Column parentCol ON (sic.Parent_Column_ID = parentCol.AD_Column_ID) ";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(combinedQuery, trxName);
            rs = pstmt.executeQuery();

            while (rs.next()) {
            	int searchIndexId = rs.getInt("AD_SearchIndex_ID");
				int mainTableId = rs.getInt("AD_Table_ID_main");
				String mainTableName = rs.getString("TableName_main");
				String mainKeyColumnName = rs.getString("ColumnName_mainkey");
				int tableId = rs.getInt("AD_Table_ID");
				String tableName = rs.getString("TableName");
				int columnId = rs.getInt("AD_Column_ID");
				String columnName = rs.getString("ColumnName");
				int parentColId = rs.getInt("AD_Column_ID_parent");
				String parentColName = rs.getString("ColumnName_parent");
				int referenceId = rs.getInt("AD_Reference_ID");
				
				if(Util.isEmpty(mainKeyColumnName)) {
					log.severe("No Key Column found for table: " + tableName);
					return null;
				}
				// TODO also need to check for composite key tables - only single key column is allowed

                SearchIndexConfig searchIndexConfig = searchIndexConfigs.stream()
                    .filter(config -> config.getSearchIndexId() == searchIndexId)
                    .findFirst()
                    .orElseGet(() -> {
                        SearchIndexConfig newConfig = new SearchIndexConfig(searchIndexId);
                        searchIndexConfigs.add(newConfig);
                        return newConfig;
                    });

                SearchIndexTableConfig tableConfig = searchIndexConfig.getTableConfigs().stream()
                    .filter(config -> config.getTableId() == mainTableId)
                    .findFirst()
                    .orElseGet(() -> {
                    	SearchIndexTableConfig newConfig = new SearchIndexTableConfig(mainTableName, mainTableId, mainKeyColumnName);
                        searchIndexConfig.addTableConfig(newConfig);
                        return newConfig;
                    });

                if(referenceId <= 0) {
                	tableConfig.addColumn(new SearchIndexColumnConfig(tableId, tableName, columnId, columnName, parentColId, parentColName));
                } else {
                	MLookup lookup = MLookupFactory.get(ctx, 0, 0, columnId, referenceId);
                	MLookupInfo lookupInfo = lookup.getLookupInfo();
                	for(String lookupDisplayColumnName : lookupInfo.lookupDisplayColumns) {
                		// FIXME - lookupInfo.KeyColumn is like tableName.ColumnName
                		tableConfig.addColumn(new SearchIndexColumnConfig(tableId, lookupInfo.TableName, -1, lookupDisplayColumnName, -1, lookupInfo.KeyColumn));
                	}
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, combinedQuery, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }

        return searchIndexConfigs;
    }
	
	/**
     * Load search index data based on the configuration with join clauses
     * @param ctx
     * @param searchIndexConfig
     * @param trxName
     * @return
     * @throws SQLException
     */
	public static Map<Integer, Set<SearchIndexRecord>> loadSearchIndexDataWithJoins(Properties ctx, List<SearchIndexConfig> searchIndexConfigs, String trxName) throws SQLException {
		Map<Integer, Set<SearchIndexRecord>> indexDataMap = new HashMap<>();
		SearchIndexRecord searchIndexRecord = null;

	    for (SearchIndexConfig searchIndexConfig : searchIndexConfigs) {
	    	Set<SearchIndexRecord> indexTableDataSet = new HashSet<>();
	    	for (SearchIndexTableConfig tableConfig : searchIndexConfig.getTableConfigs()) {
	
		        // FROM clause with joins
		        StringBuilder fromClauseBuilder = new StringBuilder();
		        fromClauseBuilder.append(" FROM ").append(tableConfig.getTableName()).append(" main");
		        // SELECT clause
		        StringBuilder selectClauseBuilder = new StringBuilder();
		        selectClauseBuilder.append("SELECT ").append("main.").append(tableConfig.getKeyColName()).append(" as Record_ID, ");
	
		        int joinIndex = 1;
		        for (SearchIndexColumnConfig columnConfig : tableConfig.getColumns()) {
		            if (columnConfig.getTableId() == tableConfig.getTableId()) {
		            	selectClauseBuilder.append("main.").append(columnConfig.getColumnName());
		            } else {
		            	selectClauseBuilder.append("t").append(joinIndex).append(".").append(columnConfig.getColumnName());
		            	fromClauseBuilder.append(" LEFT JOIN ").append(columnConfig.getTableName()).append(" t").append(joinIndex);
			            fromClauseBuilder.append(" ON main.").append(columnConfig.getParentColumnName()).append(" = t").append(joinIndex).append(".").append(columnConfig.getParentColumnName());
			            joinIndex++;
		            }
		            selectClauseBuilder.append(", ");
		        }
		        // Remove the last comma from the SELECT clause
		        selectClauseBuilder.deleteCharAt(selectClauseBuilder.length() - 2);
		        
		        // WHERE clause
		        StringBuilder whereClauseBuilder = new StringBuilder();
		        whereClauseBuilder.append(" WHERE main.AD_Client_ID = ? AND main.IsActive = 'Y' ");
	
		        // Combine SELECT and FROM clauses
		        String query = selectClauseBuilder.toString() + fromClauseBuilder.toString() + whereClauseBuilder.toString();
		        
		        PreparedStatement pstmt = null;
		        ResultSet rs = null;
	
		        try {
		            pstmt = DB.prepareStatement(query, trxName);
		            pstmt.setInt(1, Env.getAD_Client_ID(ctx));
		            rs = pstmt.executeQuery();
		            searchIndexRecord = new SearchIndexRecord(tableConfig.getTableId(), tableConfig.getKeyColName());

		            while (rs.next()) {
		            	Map<String, Object> data = new HashMap<>();
		            	data.put("Record_ID", rs.getObject("Record_ID"));
	                    for (SearchIndexColumnConfig columnConfig : tableConfig.getColumns()) {
	                        data.put(columnConfig.getColumnName(), rs.getObject(columnConfig.getColumnName()));
	                    }
	                    searchIndexRecord.addTableData(data);
		            }
		        } finally {
		            DB.close(rs, pstmt);
		        }
		        if(searchIndexRecord != null)
		        	indexTableDataSet.add(searchIndexRecord);
	    	} // for getTableConfigs
	    	indexDataMap.put(searchIndexConfig.getSearchIndexId(), indexTableDataSet);
	    } // for searchIndexConfigs

	    return indexDataMap;
	}
	
	/**
	 * Get Search Index Provider
	 * @param searchIndexProviderId - AD_SearchIndexProvider_ID
	 * @return
	 */
	public static ISearchIndexProvider getSearchIndexProvider(Properties ctx, int searchIndexProviderId, String trxName) {
		MSearchIndexProvider providerDef = new MSearchIndexProvider(ctx, searchIndexProviderId, trxName);		
		SearchIndexProviderFactory factory = new SearchIndexProviderFactory();
		return factory.getSearchIndexProvider(providerDef.getClassname());
	}
}

