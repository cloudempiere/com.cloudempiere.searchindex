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
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.PO;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * 
 * Model class for AD_SearchIndexColumn
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class MSearchIndexColumn extends X_AD_SearchIndexColumn implements ImmutablePOSupport {

	/** Generated serial version ID */
	private static final long serialVersionUID = 2900699941170299022L;
	/**	Cache */
	private static ImmutableIntPOCache<Integer,MSearchIndexColumn> s_cache = new ImmutableIntPOCache<Integer,MSearchIndexColumn>(Table_Name, 20);

	/** Parent */
	protected MSearchIndexTable m_parent = null;

	/**
	 * @param ctx
	 * @param AD_SearchIndexColumn_ID
	 * @param trxName
	 */
	public MSearchIndexColumn(Properties ctx, int AD_SearchIndexColumn_ID, String trxName) {
		super(ctx, AD_SearchIndexColumn_ID, trxName);
	}

	/**
	 * @param ctx
	 * @param AD_SearchIndexColumn_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MSearchIndexColumn(Properties ctx, int AD_SearchIndexColumn_ID, String trxName, String... virtualColumns) {
		super(ctx, AD_SearchIndexColumn_ID, trxName, virtualColumns);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MSearchIndexColumn(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get search index column from cache
	 * @param ctx
	 * @param AD_SearchIndexColumn_ID
	 * @param trxName
	 * @return
	 */
	public static MSearchIndexColumn get(Properties ctx, int AD_SearchIndexColumn_ID, String trxName) {
		MSearchIndexColumn searchIndexColumn = s_cache.get(AD_SearchIndexColumn_ID);
		if (searchIndexColumn != null)
			return searchIndexColumn;
		
		searchIndexColumn = new MSearchIndexColumn(ctx, AD_SearchIndexColumn_ID, trxName);
		s_cache.put(AD_SearchIndexColumn_ID, searchIndexColumn);
		return searchIndexColumn;
	}
	
	/**
	 * 	Get Parent
	 *	@return parent
	 */
	public MSearchIndexTable getParent()
	{
		if (m_parent == null)
			m_parent = MSearchIndexTable.get(getCtx(), getAD_SearchIndexTable_ID(), get_TrxName());
		return m_parent;
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		// Check Parent Link Column
		if(getParent().getAD_Table_ID() != getAD_Table_ID() && getParent_Column_ID() <= 0) {
			log.saveError("Error", Msg.getMsg(getCtx(), "ParentLinkError"));
			return false;
		}
			
		return true;
	}
	
	/**
	 * Get AD_SearchIndex
	 * @return
	 */
	public MSearchIndex getSearchIndex() {
		MSearchIndex searchIndex = null;
		StringBuilder sql = new StringBuilder("SELECT si.* ")
			.append("FROM ").append(MSearchIndexTable.Table_Name).append(" sit ")
			.append("JOIN ").append(MSearchIndex.Table_Name).append(" si ")
				.append("ON sit.").append(MSearchIndexTable.COLUMNNAME_AD_SearchIndex_ID)
				.append(" = si.").append(MSearchIndex.COLUMNNAME_AD_SearchIndex_ID)
			.append("WHERE sit.").append(MSearchIndexTable.COLUMNNAME_AD_SearchIndexTable_ID).append(" = ?");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setInt(1, getAD_SearchIndexTable_ID());
			rs = pstmt.executeQuery();
			if(rs.next()) {
				searchIndex = new MSearchIndex(getCtx(), rs, get_TrxName());
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Error while getting Search Index", e);
		} finally {
			DB.close(rs, pstmt);
		}
		return searchIndex;
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}

}
