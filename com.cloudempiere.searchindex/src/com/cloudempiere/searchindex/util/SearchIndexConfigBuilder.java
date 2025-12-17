package com.cloudempiere.searchindex.util;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.cloudempiere.searchindex.util.pojo.SearchIndexColumnConfig;
import com.cloudempiere.searchindex.util.pojo.SearchIndexColumnData;
import com.cloudempiere.searchindex.util.pojo.SearchIndexConfig;
import com.cloudempiere.searchindex.util.pojo.SearchIndexTableConfig;
import com.cloudempiere.searchindex.util.pojo.SearchIndexTableData;

public class SearchIndexConfigBuilder {
	
	/** Logger */
	private static final CLogger log = CLogger.getCLogger(SearchIndexConfigBuilder.class);
	/** Search Index Config Cache */
	private static final CCache<Integer, List<SearchIndexConfig>> searchIndexConfigCache = new CCache<>("SearchIndexConfig", 50);

	/** Context */
	private Properties ctx = null;
	/** Transaction Name */
	private String trxName = null;
	/** AD_SearchIndexProvider_ID */
	private int searchIndexProviderId = -1;
	/** AD_SearchIndex_ID */
	private int searchIndexId = -1;
	/** AD_Table_ID */
	private int tableId = -1;
	/** Record_ID */
	private int recordId = -1;
	/** Search Index Configs */
	private List<SearchIndexConfig> searchIndexConfigs = new ArrayList<>();
	/** Search Index Data - key is AD_SearchIndex_ID */
	private Map<Integer, Set<SearchIndexTableData>> searchIndexData = new HashMap<>();

	/**
	 * Set Context
	 * @param ctx
	 * @return
	 */
	public SearchIndexConfigBuilder setCtx(Properties ctx) {
		this.ctx = ctx;
		return this;
	}

	/**
	 * Set Transaction Name
	 * @param trxName
	 * @return
	 */
	public SearchIndexConfigBuilder setTrxName(String trxName) {
		this.trxName = trxName;
		return this;
	}

	/**
	 * Set Search Index Provider ID
	 * @param searchIndexId - AD_SearchIndexProvider_ID
	 * @return
	 */
	public SearchIndexConfigBuilder setAD_SearchIndexProvider_ID(int searchIndexProviderId) {
		this.searchIndexProviderId = searchIndexProviderId;
		return this;
	}
	
	/**
	 * Set Search Index ID
	 * @param searchIndexId - AD_SearchIndex_ID
	 * @return
	 */
	public SearchIndexConfigBuilder setAD_SearchIndex_ID(int searchIndexId) {
		this.searchIndexId = searchIndexId;
		return this;
	}

	/**
	 * Set Table ID and Record ID
	 * @param tableId - AD_Table_ID
	 * @param recordId - Record_ID
	 * @return
	 */
	public SearchIndexConfigBuilder setRecord(int tableId, int recordId) {
		this.tableId = tableId;
		this.recordId = recordId;
		return this;
	}

	/**
	 * Build Search Index Config
	 * @return
	 */
	public SearchIndexConfigBuilder build() {
		try {
			loadSearchIndexConfig();
			loadSearchIndexData();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Error loading Search Index Config", e);
		}
		return this;
	}

	/**
	 * Get Search Index Configs
	 * @return
	 */
	public Map<Integer, Set<SearchIndexTableData>> getData(boolean forceReload) {
		if (forceReload) {
			try {
				loadSearchIndexData();
			} catch (SQLException e) {
				log.log(Level.SEVERE, "Error loading Search Index Data", e);
			}
		}
		return searchIndexData;
	}
	
	/**
	 * Load Search Index Config
	 * @throws SQLException
	 */
	protected void loadSearchIndexConfig() throws SQLException {

		if (searchIndexConfigCache.containsKey(searchIndexId)) {
			searchIndexConfigs = searchIndexConfigCache.get(searchIndexId);
			return;
		}

	    StringBuilder sql = new StringBuilder();
	    sql.append("SELECT ")
	       .append("si.AD_SearchIndex_ID, ")
	       .append("mainT.AD_Table_ID as AD_Table_ID_main, ")
	       .append("mainT.TableName as TableName_main, ")
	       .append("mainKeyCol.ColumnName as ColumnName_mainkey, ")
	       .append("tbl.AD_Table_ID, ")
	       .append("tbl.TableName, ")
	       .append("col.AD_Column_ID, ")
	       .append("col.ColumnName, ")
	       .append("col.IsParent, ")
	       .append("sic.AD_Reference_ID, ")
	       .append("sic.AD_Reference_Value_ID, ")
	       .append("sic.SearchWeight, ")
	       .append("parentCol.AD_Column_ID as AD_Column_ID_parent, ")
	       .append("parentCol.ColumnName as ColumnName_parent, ")
	       .append("si.SearchIndexName, ")
	       .append("sit.WhereClause ")
	       .append("FROM AD_SearchIndex si ")
	       .append("JOIN AD_SearchIndexTable sit ON (si.AD_SearchIndex_ID = sit.AD_SearchIndex_ID AND sit.IsActive = 'Y') ")
	       .append("JOIN AD_Table mainT ON (sit.AD_Table_ID = mainT.AD_Table_ID) ")
	       .append("LEFT JOIN AD_Column mainKeyCol ON (mainT.AD_Table_ID = mainKeyCol.AD_Table_ID AND mainKeyCol.IsKey = 'Y') ")
	       .append("JOIN AD_SearchIndexColumn sic ON (sit.AD_SearchIndexTable_ID = sic.AD_SearchIndexTable_ID AND sic.IsActive = 'Y') ")
	       .append("JOIN AD_Table tbl ON (sic.AD_Table_ID = tbl.AD_Table_ID) ")
	       .append("JOIN AD_Column col ON (sic.AD_Column_ID = col.AD_Column_ID) ")
	       .append("LEFT JOIN AD_Column parentCol ON (sic.Parent_Column_ID = parentCol.AD_Column_ID) ")
	       .append("WHERE si.IsActive = 'Y' AND si.AD_Client_ID IN (?,0) ");
	    
	    List<Object> params = new ArrayList<>();
	    params.add(Env.getAD_Client_ID(ctx));
	    
	    if (searchIndexId > 0) {
	    	sql.append(" AND si.AD_SearchIndex_ID = ? ");
	    	params.add(searchIndexId);
	    }
	    if (searchIndexProviderId > 0) {
	    	sql.append(" AND si.AD_SearchIndexProvider_ID = ? ");
	    	params.add(searchIndexProviderId);
	    }
	    
	    sql.append(" ORDER BY sic.SearchWeight DESC ");
	
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	
	    try {
	        pstmt = DB.prepareStatement(sql.toString(), trxName);
	        for (int i = 0; i < params.size(); i++) {
	            pstmt.setObject(i + 1, params.get(i));
	        }
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
	            boolean isParent = rs.getBoolean("IsParent");
	            int parentColId = rs.getInt("AD_Column_ID_parent");
	            String parentColName = rs.getString("ColumnName_parent");
	            int referenceId = rs.getInt("AD_Reference_ID");
	            int referenceValueId = rs.getInt("AD_Reference_Value_ID");
	            String searchIndexName = rs.getString("SearchIndexName");
	            String whereClause = rs.getString("WhereClause");
	            BigDecimal searchWeight = rs.getBigDecimal("SearchWeight");
	
	            if (Util.isEmpty(mainKeyColumnName)) {
	                log.severe("No Key Column found for table: " + tableName);
	                return;
	            }
	
	            SearchIndexConfig searchIndexConfig = searchIndexConfigs.stream()
	                .filter(config -> config.getSearchIndexId() == searchIndexId)
	                .findFirst()
	                .orElseGet(() -> {
	                    SearchIndexConfig newConfig = new SearchIndexConfig(searchIndexId, searchIndexName);
	                    searchIndexConfigs.add(newConfig);
	                    return newConfig;
	                });
	
	            SearchIndexTableConfig tableConfig = searchIndexConfig.getTableConfigs().stream()
	                .filter(config -> config.getTableId() == mainTableId)
	                .findFirst()
	                .orElseGet(() -> {
	                    SearchIndexTableConfig newConfig = new SearchIndexTableConfig(mainTableName, mainTableId, mainKeyColumnName, whereClause, searchWeight);
	                    searchIndexConfig.addTableConfig(newConfig);
	                    return newConfig;
	                });
	            
	            tableConfig.updateMaxSearchWeight(searchWeight);
	
	            if (referenceId <= 0) {
	                tableConfig.addColumn(new SearchIndexColumnConfig(tableId, tableName, columnId, columnName, parentColId, parentColName, null, searchWeight));
	            } else {
	                tableConfig.addColumn(new SearchIndexColumnConfig(tableId, tableName, -1, columnName, parentColId, parentColName, null, searchWeight));
	                MLookup lookup = MLookupFactory.get(ctx, 0, columnId, referenceId, Env.getLanguage(ctx), columnName, referenceValueId, isParent, null);
	                MLookupInfo lookupInfo = lookup.getLookupInfo();
	                for (String lookupDisplayColumnName : lookupInfo.lookupDisplayColumns) {
	                    tableConfig.addColumn(new SearchIndexColumnConfig(-1, lookupInfo.TableName, -1, lookupDisplayColumnName, -1, getLookupKeyColumnName(lookupInfo.KeyColumn), tableName, searchWeight));
	                }
	            }
	        }
	        searchIndexConfigCache.put(searchIndexId, searchIndexConfigs);
	    } catch (Exception e) {
	        log.log(Level.SEVERE, sql.toString(), e);
	    } finally {
	        DB.close(rs, pstmt);
	    }
	}

	/**
	 * Load Search Index Data with Joins
	 * @throws SQLException
	 */
    protected void loadSearchIndexData() throws SQLException {
    	if(searchIndexConfigs == null || searchIndexConfigs.size() <= 0)
	    	throw new AdempiereException(Msg.getMsg(ctx, "SearchIndexConfigNotFound"));
    	
    	SearchIndexTableData searchIndexTableData = null;

        for (SearchIndexConfig searchIndexConfig : searchIndexConfigs) {
            Set<SearchIndexTableData> indexTableDataSet = new HashSet<>();
            for (SearchIndexTableConfig tableConfig : searchIndexConfig.getTableConfigs()) {

            	if (tableId > 0 && tableId != tableConfig.getTableId())
            		continue;
            	
                StringBuilder fromClauseBuilder = new StringBuilder();
                fromClauseBuilder.append(" FROM ").append(tableConfig.getTableName());
                StringBuilder selectClauseBuilder = new StringBuilder();
                selectClauseBuilder.append("SELECT ").append(tableConfig.getTableName()).append(".").append(tableConfig.getKeyColName()).append(" as Record_ID, ");

                Set<String> joinedTables = new HashSet<>();

                for (SearchIndexColumnConfig columnConfig : tableConfig.getColumns()) {
                    String columnAlias = columnConfig.getTableName() + "_" + columnConfig.getColumnName();
                    if (columnConfig.getTableId() == tableConfig.getTableId()) {
                        if (!Util.isEmpty(columnConfig.getColumnName())) {
                            selectClauseBuilder.append(tableConfig.getTableName()).append(".").append(columnConfig.getColumnName()).append(" as ").append(columnAlias);
                        }
                    } else {
                        if (!Util.isEmpty(columnConfig.getTableName()) && !Util.isEmpty(columnConfig.getColumnName())) {
                            selectClauseBuilder.append(columnConfig.getTableName()).append(".").append(columnConfig.getColumnName()).append(" as ").append(columnAlias);
                        }
                        if (!joinedTables.contains(columnConfig.getTableName())) {
                            fromClauseBuilder.append(" LEFT JOIN ").append(columnConfig.getTableName()).append(" ON ");
                            if (!Util.isEmpty(columnConfig.getParentTableName()) && !tableConfig.getTableName().equals(columnConfig.getParentTableName())) {
                                fromClauseBuilder.append(columnConfig.getParentTableName());
                            } else {
                                fromClauseBuilder.append(tableConfig.getTableName());
                            }
                            fromClauseBuilder.append(".").append(columnConfig.getParentColumnName());
                            fromClauseBuilder.append(" = ").append(columnConfig.getTableName()).append(".").append(columnConfig.getColumnName());
                            joinedTables.add(columnConfig.getTableName());
                        }
                    }
                    if (selectClauseBuilder.charAt(selectClauseBuilder.length() - 2) != ',') {
                        selectClauseBuilder.append(", ");
                    }
                }
                selectClauseBuilder.deleteCharAt(selectClauseBuilder.length() - 2);

                StringBuilder whereClauseBuilder = new StringBuilder();
                List<Object> params = new ArrayList<>();
                whereClauseBuilder.append(" WHERE ").append(tableConfig.getTableName()).append(".AD_Client_ID = ? AND ").append(tableConfig.getTableName()).append(".IsActive = 'Y' ");
                params.add(Env.getAD_Client_ID(ctx));
                String dynamicWhere = tableConfig.getSqlWhere();
                if (!Util.isEmpty(dynamicWhere)) {
                    // Validate WHERE clause to prevent SQL injection
                    SearchIndexSecurityValidator.validateWhereClause(dynamicWhere);
                    if (!dynamicWhere.trim().toUpperCase().startsWith("AND")) {
                        whereClauseBuilder.append("AND ");
                    }
                    whereClauseBuilder.append(dynamicWhere);
                    whereClauseBuilder.append(" ");
                }
                if (recordId > 0) {
                	whereClauseBuilder.append(" AND ").append(tableConfig.getTableName()).append(".").append(tableConfig.getKeyColName()).append(" = ? ");
                	params.add(recordId);
                }

                String query = selectClauseBuilder.toString() + fromClauseBuilder.toString() + whereClauseBuilder.toString();

                PreparedStatement pstmt = null;
                ResultSet rs = null;

                try {
                    pstmt = DB.prepareStatement(query, trxName);
                    for (int i = 0; i < params.size(); i++) {
        	            pstmt.setObject(i + 1, params.get(i));
        	        }
                    rs = pstmt.executeQuery();
                    searchIndexTableData = new SearchIndexTableData(tableConfig.getTableId(), tableConfig.getKeyColName(), searchIndexConfig.getSearchIndexName());

                    while (rs.next()) {
                        Map<String, SearchIndexColumnData> data = new LinkedHashMap<>();
                        data.put("Record_ID", new SearchIndexColumnData("Record_ID", rs.getObject("Record_ID"), Env.ONE, tableConfig.getMaxSearchWeight()));
                        for (SearchIndexColumnConfig columnConfig : tableConfig.getColumns()) {
                            if (!Util.isEmpty(columnConfig.getColumnName())) {
                                String key = columnConfig.getTableName() + "." + columnConfig.getColumnName();
                                String columnAlias = columnConfig.getTableName() + "_" + columnConfig.getColumnName();
                                data.put(key, new SearchIndexColumnData(key, rs.getObject(columnAlias), columnConfig.getSearchWeight(), tableConfig.getMaxSearchWeight()));
                            }
                        }
                        searchIndexTableData.addColumnData(data);
                    }
                } catch (Exception e) {
                    log.log(Level.SEVERE, query, e);
                    throw new AdempiereException(e.getMessage());
                } finally {
                    DB.close(rs, pstmt);
                }
                if (searchIndexTableData != null)
                    indexTableDataSet.add(searchIndexTableData);
            }
            searchIndexData.put(searchIndexConfig.getSearchIndexId(), indexTableDataSet);
        }
    }
	
	/**
	 * Get the lookup key column name
	 * @param keyColumn
	 * @return
	 */
    protected static String getLookupKeyColumnName(String keyColumn) {
        if (keyColumn == null || !keyColumn.contains(".")) {
            return keyColumn;
        }
        String[] parts = keyColumn.split("\\.");
        if (parts.length != 2) {
        	return keyColumn;
        }
        return parts[1];
    }
}