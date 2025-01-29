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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

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
	
	public int[] getColumnIdList() {
		List<Integer> colIdList = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT ").append(MSearchIndexColumn.COLUMNNAME_AD_Column_ID)
				.append(" FROM ").append(MSearchIndexColumn.Table_Name).append(" sic ")
				.append(" JOIN ").append(MSearchIndexTable.Table_Name).append(" sit ON sic.")
				.append(MSearchIndexColumn.COLUMNNAME_AD_SearchIndexTable_ID).append("=sit.")
				.append(MSearchIndexTable.COLUMNNAME_AD_SearchIndexTable_ID)
				.append(" WHERE sit.").append(MSearchIndexTable.COLUMNNAME_AD_SearchIndex_ID).append("=?");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setInt(1, getAD_SearchIndex_ID());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				colIdList.add(rs.getInt(1));
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Error while getting column id list", e);
		} finally {
			DB.close(rs, pstmt);
		}
		return colIdList.stream().mapToInt(i -> i).toArray();
	}
	
	public static MSearchIndex[] getForTable(Properties ctx, PO po, Set<KeyNamePair> indexedTableNames, String trxName) {
		return get(ctx, po, false, indexedTableNames, trxName);
	}
	
	public static MSearchIndex[] getForRecord(Properties ctx, PO po, Set<KeyNamePair> indexedTableNames, String trxName) {
		return get(ctx, po, true, indexedTableNames, trxName);
	}
	
	/**
	 * Get AD_SearchIndex for a PO record
	 * @param ctx
	 * @param po
	 * @param indexedTableNames
	 * @param trxName
	 * @return
	 */
	private static MSearchIndex[] get(Properties ctx, PO po, boolean isRecordLevel, Set<KeyNamePair> indexedTableNames, String trxName) {
		if (po instanceof MSearchIndex) {
			
			return new MSearchIndex[] { (MSearchIndex) po };
			
		} else if (po instanceof MSearchIndexTable) {
			
			MSearchIndexTable searchIndexTable = (MSearchIndexTable) po;
			return new MSearchIndex[] { new MSearchIndex(ctx, searchIndexTable.getAD_SearchIndex_ID(), trxName) };
		
		} else if (po instanceof MSearchIndexColumn) {
			
			MSearchIndexColumn searchIndexColumn = (MSearchIndexColumn) po;
			return new MSearchIndex[] { searchIndexColumn.getSearchIndex() };
		
		} else {
			
			if (indexedTableNames == null || indexedTableNames.isEmpty())
				indexedTableNames = SearchIndexUtils.getIndexedTableNames(trxName, Env.getAD_Client_ID(ctx));
			
			Set<MSearchIndex> searchIndexSet = new HashSet<>();
			for (KeyNamePair indexTable : indexedTableNames) {
				int recordId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();
				int searchIndexId = indexTable.getKey();
				MSearchIndex searchIndex = new MSearchIndex(ctx, searchIndexId, trxName);
				String searchIdxTableName = searchIndex.getSearchIndexName();
				if (isRecordLevel) {
					if (containsRecord(ctx, po.getAD_Client_ID(), po.get_Table_ID(), recordId, searchIdxTableName, trxName))
						searchIndexSet.add(searchIndex);
				} else {
					if (containsTable(ctx, po.getAD_Client_ID(), po.get_Table_ID(), searchIdxTableName, trxName))
						searchIndexSet.add(searchIndex);
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
