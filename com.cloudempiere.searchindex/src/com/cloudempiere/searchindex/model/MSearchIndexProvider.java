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
public class MSearchIndexProvider extends X_AD_SearchIndexProvider {

	/** Generated serial version ID */
	private static final long serialVersionUID = -5327411696537583461L;

	/**
	 * @param ctx
	 * @param AD_SearchIndexProvider_ID
	 * @param trxName
	 */
	public MSearchIndexProvider(Properties ctx, int AD_SearchIndexProvider_ID, String trxName) {
		super(ctx, AD_SearchIndexProvider_ID, trxName);
	}

	/**
	 * @param ctx
	 * @param AD_SearchIndexProvider_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MSearchIndexProvider(Properties ctx, int AD_SearchIndexProvider_ID, String trxName, String... virtualColumns) {
		super(ctx, AD_SearchIndexProvider_ID, trxName, virtualColumns);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MSearchIndexProvider(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get Search Index Providers by Client
	 * @param ctx
	 * @param clientId - AD_Client_ID
	 * @param trxName
	 * @return
	 */
	public static MSearchIndexProvider[] getByClient(Properties ctx, int clientId, String trxName) {
		List<MSearchIndexProvider> list = new Query(ctx, Table_Name, COLUMNNAME_AD_Client_ID + "=?", trxName)
				.setParameters(clientId)
				.list();
		return list.toArray(new MSearchIndexProvider[list.size()]);
	}
}
