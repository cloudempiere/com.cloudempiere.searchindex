/**
 * 
 */
package com.cloudempiere.searchindex.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.Msg;

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
	 * 	Get Parent
	 *	@return parent
	 */
	public MSearchIndexTable getParent()
	{
		if (m_parent == null)
			m_parent = new MSearchIndexTable(getCtx(), getAD_SearchIndexTable_ID(), get_TrxName());
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

}
