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
import java.util.Properties;

/**
 * 
 * Model class for AD_SearchIndexTable
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class MSearchIndexTable extends X_AD_SearchIndexTable {

	/** Generated serial version ID */
	private static final long serialVersionUID = -2412775728559620890L;

	/**
	 * @param ctx
	 * @param AD_SearchIndexTable_ID
	 * @param trxName
	 */
	public MSearchIndexTable(Properties ctx, int AD_SearchIndexTable_ID, String trxName) {
		super(ctx, AD_SearchIndexTable_ID, trxName);
	}

	/**
	 * @param ctx
	 * @param AD_SearchIndexTable_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MSearchIndexTable(Properties ctx, int AD_SearchIndexTable_ID, String trxName, String... virtualColumns) {
		super(ctx, AD_SearchIndexTable_ID, trxName, virtualColumns);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MSearchIndexTable(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

}
