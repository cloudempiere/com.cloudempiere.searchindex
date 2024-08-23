/**
 * 
 */
package com.cloudempiere.omnisearch.model;

import java.sql.ResultSet;
import java.util.Properties;

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

}
