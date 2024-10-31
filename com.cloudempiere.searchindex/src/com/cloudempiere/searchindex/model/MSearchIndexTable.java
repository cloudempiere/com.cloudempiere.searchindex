/**
 * 
 */
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
