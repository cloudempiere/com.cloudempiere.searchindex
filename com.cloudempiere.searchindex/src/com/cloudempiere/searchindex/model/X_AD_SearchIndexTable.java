/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package com.cloudempiere.searchindex.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for AD_SearchIndexTable
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AD_SearchIndexTable")
public class X_AD_SearchIndexTable extends PO implements I_AD_SearchIndexTable, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250325L;

    /** Standard Constructor */
    public X_AD_SearchIndexTable (Properties ctx, int AD_SearchIndexTable_ID, String trxName)
    {
      super (ctx, AD_SearchIndexTable_ID, trxName);
      /** if (AD_SearchIndexTable_ID == 0)
        {
			setAD_SearchIndex_ID (0);
// @AD_SearchIndex_ID@
			setAD_SearchIndexTable_ID (0);
			setAD_Table_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_SearchIndexTable (Properties ctx, int AD_SearchIndexTable_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_SearchIndexTable_ID, trxName, virtualColumns);
      /** if (AD_SearchIndexTable_ID == 0)
        {
			setAD_SearchIndex_ID (0);
// @AD_SearchIndex_ID@
			setAD_SearchIndexTable_ID (0);
			setAD_Table_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_AD_SearchIndexTable (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_AD_SearchIndexTable[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	public I_AD_SearchIndex getAD_SearchIndex() throws RuntimeException
	{
		return (I_AD_SearchIndex)MTable.get(getCtx(), I_AD_SearchIndex.Table_ID)
			.getPO(getAD_SearchIndex_ID(), get_TrxName());
	}

	/** Set Search Index.
		@param AD_SearchIndex_ID Search Index
	*/
	public void setAD_SearchIndex_ID (int AD_SearchIndex_ID)
	{
		if (AD_SearchIndex_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndex_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndex_ID, Integer.valueOf(AD_SearchIndex_ID));
	}

	/** Get Search Index.
		@return Search Index	  */
	public int getAD_SearchIndex_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_SearchIndex_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Search Index Table.
		@param AD_SearchIndexTable_ID Search Index Table
	*/
	public void setAD_SearchIndexTable_ID (int AD_SearchIndexTable_ID)
	{
		if (AD_SearchIndexTable_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndexTable_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndexTable_ID, Integer.valueOf(AD_SearchIndexTable_ID));
	}

	/** Get Search Index Table.
		@return Search Index Table	  */
	public int getAD_SearchIndexTable_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_SearchIndexTable_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AD_SearchIndexTable_UU.
		@param AD_SearchIndexTable_UU AD_SearchIndexTable_UU
	*/
	public void setAD_SearchIndexTable_UU (String AD_SearchIndexTable_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AD_SearchIndexTable_UU, AD_SearchIndexTable_UU);
	}

	/** Get AD_SearchIndexTable_UU.
		@return AD_SearchIndexTable_UU	  */
	public String getAD_SearchIndexTable_UU()
	{
		return (String)get_Value(COLUMNNAME_AD_SearchIndexTable_UU);
	}

	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Table)MTable.get(getCtx(), org.compiere.model.I_AD_Table.Table_ID)
			.getPO(getAD_Table_ID(), get_TrxName());
	}

	/** Set Table.
		@param AD_Table_ID Database Table information
	*/
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_Table_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get Table.
		@return Database Table information
	  */
	public int getAD_Table_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Comment.
		@param Help Comment or Hint
	*/
	public void setHelp (String Help)
	{
		set_Value (COLUMNNAME_Help, Help);
	}

	/** Get Comment.
		@return Comment or Hint
	  */
	public String getHelp()
	{
		return (String)get_Value(COLUMNNAME_Help);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Sql WHERE.
		@param WhereClause Fully qualified SQL WHERE clause
	*/
	public void setWhereClause (String WhereClause)
	{
		set_Value (COLUMNNAME_WhereClause, WhereClause);
	}

	/** Get Sql WHERE.
		@return Fully qualified SQL WHERE clause
	  */
	public String getWhereClause()
	{
		return (String)get_Value(COLUMNNAME_WhereClause);
	}
}