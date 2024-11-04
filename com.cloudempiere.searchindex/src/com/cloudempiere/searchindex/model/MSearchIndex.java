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
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;

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
}
