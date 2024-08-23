/**
 * 
 */
package com.cloudempiere.omnisearch.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * 
 * Model class for AD_SearchIndexColumn
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class MSearchIndexColumn extends X_AD_SearchIndexColumn {

	/** Generated serial version ID */
	private static final long serialVersionUID = 2900699941170299022L;

	/**
	 * @param ctx
	 * @param AD_SearchIndexColumn_ID
	 * @param trxName
	 */
	public MSearchIndexColumn(Properties ctx, int AD_SearchIndexColumn_ID, String trxName) {
		super(ctx, AD_SearchIndexColumn_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param ctx
	 * @param AD_SearchIndexColumn_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MSearchIndexColumn(Properties ctx, int AD_SearchIndexColumn_ID, String trxName, String... virtualColumns) {
		super(ctx, AD_SearchIndexColumn_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MSearchIndexColumn(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

}
