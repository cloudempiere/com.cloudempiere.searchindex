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
import java.util.Properties;
import java.util.Set;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.cloudempiere.searchindex.event.pojo.IndexedTable;
import com.cloudempiere.searchindex.util.SearchIndexUtils;

/**
 * 
 * Model class for AD_SearchIndex
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class MSearchIndex extends X_AD_SearchIndex {

	/** Generated serial version ID */
	private static final long serialVersionUID = 5448449186132862379L;

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
		return getIfIndexedColumnChanged(ctx, po, eventPO, false, indexedTables, trxName);
	}
	
	public static MSearchIndex[] getForRecord(Properties ctx, PO po, PO eventPO, Set<IndexedTable> indexedTables, String trxName) {
		return getIfIndexedColumnChanged(ctx, po, eventPO, true, indexedTables, trxName);
	}
	
	/**
	 * Get AD_SearchIndex for a PO record, if an indexed column changed
	 * @param ctx
	 * @param po
	 * @param indexedTables
	 * @param trxName
	 * @return
	 */
	private static MSearchIndex[] getIfIndexedColumnChanged(Properties ctx, PO po, PO eventPO, boolean isRecordLevel, Set<IndexedTable> indexedTables, String trxName) {
		if (po instanceof MSearchIndex) {
			return new MSearchIndex[] { (MSearchIndex) po };
			
		} else if (po instanceof MSearchIndexTable) {			
			MSearchIndexTable searchIndexTable = (MSearchIndexTable) po;
			return new MSearchIndex[] { new MSearchIndex(ctx, searchIndexTable.getAD_SearchIndex_ID(), trxName) };
		
		} else if (po instanceof MSearchIndexColumn) {			
			MSearchIndexColumn searchIndexColumn = (MSearchIndexColumn) po;
			return new MSearchIndex[] { searchIndexColumn.getSearchIndex() };
		
		} else {			
			if (indexedTables == null || indexedTables.isEmpty())
				indexedTables = SearchIndexUtils.getSearchIndexConfigs(trxName, Env.getAD_Client_ID(ctx));
			
			MTable mTableEvt = MTable.get(ctx, eventPO.get_Table_ID(), trxName);
			Set<Integer> changedColumnIDs = new HashSet<>();
			
			for (int columnId : mTableEvt.getColumnIDs(false)) {
				if (eventPO.is_ValueChanged_byId(columnId))
					changedColumnIDs.add(columnId);
			}
			
			Set<MSearchIndex> searchIndexSet = new HashSet<>();
			for (IndexedTable searchIndexConfig : indexedTables) {
				boolean containsChangedColumn = false;
				for (int changedColId : changedColumnIDs) {
					if (searchIndexConfig.getColumnIDs().contains(Integer.valueOf(changedColId))) {
						containsChangedColumn = true;
						break;
					}
				}
				if (!containsChangedColumn)
					continue;
				
				if (isRecordLevel) {
					int recordId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();
					if (containsRecord(ctx, po.getAD_Client_ID(), po.get_Table_ID(), recordId, searchIndexConfig.getSearchIndexName(), trxName))
						searchIndexSet.add(new MSearchIndex(ctx, searchIndexConfig.getSearchIndexId(), trxName));
				} else {
					if (containsTable(ctx, po.getAD_Client_ID(), po.get_Table_ID(), searchIndexConfig.getSearchIndexName(), trxName))
						searchIndexSet.add(new MSearchIndex(ctx, searchIndexConfig.getSearchIndexId(), trxName));
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
}
