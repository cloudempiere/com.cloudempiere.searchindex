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
package com.cloudempiere.searchindex.model;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

import com.cloudempiere.searchindex.event.pojo.IndexedTable;
import com.cloudempiere.searchindex.util.SearchIndexUtils;

/**
 * 
 * Model class for AD_SearchIndex
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class MSearchIndex extends X_AD_SearchIndex implements ImmutablePOSupport {

	/** Generated serial version ID */
	private static final long serialVersionUID = 5448449186132862379L;
	/**	Cache */
	private static ImmutableIntPOCache<Integer,MSearchIndex> s_cache = new ImmutableIntPOCache<Integer,MSearchIndex>(Table_Name, 20);

	/**
	 * @param ctx
	 * @param AD_SearchIndex_ID
	 * @param trxName
	 */
	public MSearchIndex(Properties ctx, int AD_SearchIndex_ID, String trxName) {
		super(ctx, AD_SearchIndex_ID, trxName);
	}

	/**
	 * @param ctx
	 * @param AD_SearchIndex_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MSearchIndex(Properties ctx, int AD_SearchIndex_ID, String trxName, String... virtualColumns) {
		super(ctx, AD_SearchIndex_ID, trxName, virtualColumns);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MSearchIndex(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get search index from cache
	 * @param ctx
	 * @param AD_SearchIndex_ID
	 * @param trxName
	 * @return
	 */
	public static MSearchIndex get(Properties ctx, int AD_SearchIndex_ID, String trxName) {
		MSearchIndex searchIndex = s_cache.get(AD_SearchIndex_ID);
		if (searchIndex != null)
			return searchIndex;
		
		searchIndex = new MSearchIndex(ctx, AD_SearchIndex_ID, trxName);
		s_cache.put(AD_SearchIndex_ID, searchIndex);
		return searchIndex;
	}
	
	/**
	 * Get search index by Transaction Code
	 * @param ctx
	 * @param transactionCode
	 * @param trxName
	 * @return
	 */
	public static MSearchIndex get(Properties ctx, String transactionCode, String trxName) {
		if (transactionCode == null)
			return null;
		return new Query(ctx, Table_Name, COLUMNNAME_TransactionCode+"=? AND "+COLUMNNAME_AD_Client_ID+" IN (?,0)", trxName)
				.setParameters(transactionCode, Env.getAD_Client_ID(ctx))
				.setOnlyActiveRecords(true)
				.first(); // TransactionCode should be unique
	}

	/**
	 * Get search index by client
	 * @param ctx
	 * @param clientId
	 * @param trxName
	 * @return
	 */
	public static MSearchIndex[] getByClient(Properties ctx, int clientId, String trxName) {
		List<MSearchIndex> list = new Query(ctx, Table_Name, COLUMNNAME_AD_Client_ID + "=?", trxName)
				.setParameters(clientId)
				.list();
		return list.toArray(new MSearchIndex[list.size()]);
	}
	
	public static MSearchIndex[] getForTable(Properties ctx, PO po, PO eventPO, Set<IndexedTable> indexedTables, String trxName) {
		return get(ctx, po, eventPO, false, indexedTables, trxName);
	}
	
	public static MSearchIndex[] getForRecord(Properties ctx, PO po, PO eventPO, Set<IndexedTable> indexedTables, String trxName) {
		return get(ctx, po, eventPO, true, indexedTables, trxName);
	}
	
	/**
	 * Get AD_SearchIndex for a PO record
	 * @param ctx
	 * @param po
	 * @param indexedTables
	 * @param trxName
	 * @return
	 */
	private static MSearchIndex[] get(Properties ctx, PO po, PO eventPO, boolean isRecordLevel, Set<IndexedTable> indexedTables, String trxName) {
		if (po instanceof MSearchIndex) {
			return new MSearchIndex[] { (MSearchIndex) po };
			
		} else if (po instanceof MSearchIndexTable) {			
			MSearchIndexTable searchIndexTable = (MSearchIndexTable) po;
			return new MSearchIndex[] { MSearchIndex.get(ctx, searchIndexTable.getAD_SearchIndex_ID(), trxName) };
		
		} else if (po instanceof MSearchIndexColumn) {			
			MSearchIndexColumn searchIndexColumn = (MSearchIndexColumn) po;
			return new MSearchIndex[] { searchIndexColumn.getSearchIndex() };
		
		} else {			
			if (indexedTables == null || indexedTables.isEmpty()) {
				Map<Integer, Set<IndexedTable>> indexedTablesByClient = SearchIndexUtils.getSearchIndexConfigs(trxName, Env.getAD_Client_ID(ctx));
				indexedTables = indexedTablesByClient.get(Env.getAD_Client_ID(ctx));
				Set<IndexedTable> indexedTables0 = indexedTablesByClient.get(0);
				if (indexedTables == null)
					indexedTables = indexedTables0;
				else {
					if (indexedTables0 != null)
						indexedTables.addAll(indexedTables0);
				}
			}
			if (indexedTables == null)
				return null;
			
			Set<MSearchIndex> searchIndexSet = new HashSet<>();
			for (IndexedTable searchIndexConfig : indexedTables) {
				if (isRecordLevel) {
					int recordId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();
					if (containsRecord(ctx, po.getAD_Client_ID(), po.get_Table_ID(), recordId, searchIndexConfig.getSearchIndexName(), trxName))
						searchIndexSet.add(MSearchIndex.get(ctx, searchIndexConfig.getSearchIndexId(), trxName));
				} else {
					if (containsTable(ctx, po.getAD_Client_ID(), po.get_Table_ID(), searchIndexConfig.getSearchIndexName(), trxName))
						searchIndexSet.add(MSearchIndex.get(ctx, searchIndexConfig.getSearchIndexId(), trxName));
				}
			}
			return searchIndexSet.toArray(new MSearchIndex[0]);
		}
	}
	
	/**
	 * Check if the given index table contains the given record
	 * @param ctx
	 * @param tableId
	 * @param recordId
	 * @param indexTableName
	 * @param trxName
	 * @return true if the given index table contains the given record
	 */
	public static boolean containsRecord(Properties ctx, int clientId, int tableId, int recordId, String indexTableName, String trxName) {
		StringBuilder sql = new StringBuilder("SELECT 1 FROM ").append(indexTableName)
				.append(" WHERE AD_Client_ID=? AND AD_Table_ID=? AND Record_ID=?");
		return DB.getSQLValue(trxName, sql.toString(), clientId, tableId, recordId) > 0;
	}

	/**
	 * Check if the given index table contains the given table
	 * @param ctx
	 * @param tableId
	 * @param indexTableName
	 * @param trxName
	 * @return true if the given index table contains the given table
	 */
	public static boolean containsTable(Properties ctx, int clientId, int tableId, String indexTableName, String trxName) {
		StringBuilder sql = new StringBuilder("SELECT 1 FROM ").append(indexTableName)
				.append(" WHERE AD_Client_ID=? AND AD_Table_ID=?");
		return DB.getSQLValue(trxName, sql.toString(), clientId, tableId) > 0;
	}
	
	/**
	 * Get AD_SearchIndexProvider_ID by transaction code
	 * @param ctx
	 * @param searchIndexName
	 * @param trxName
	 * @return
	 */
	public static int getAD_SearchIndexProvider_ID(Properties ctx, String searchIndexName, String trxName) {
		String sql = "SELECT AD_SearchIndexProvider_ID FROM AD_SearchIndex WHERE SearchIndexName=? AND IsActive='Y' AND AD_Client_ID = IN(0, ?)";
		return DB.getSQLValue(trxName, sql, searchIndexName, Env.getAD_Client_ID(ctx));
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}
}
