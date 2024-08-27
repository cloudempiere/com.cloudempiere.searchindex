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
package com.cloudempiere.omnisearch.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for AD_SearchIndexColumn
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AD_SearchIndexColumn")
public class X_AD_SearchIndexColumn extends PO implements I_AD_SearchIndexColumn, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20240826L;

    /** Standard Constructor */
    public X_AD_SearchIndexColumn (Properties ctx, int AD_SearchIndexColumn_ID, String trxName)
    {
      super (ctx, AD_SearchIndexColumn_ID, trxName);
      /** if (AD_SearchIndexColumn_ID == 0)
        {
			setAD_Column_ID (0);
			setAD_SearchIndexColumn_ID (0);
			setAD_SearchIndexTable_ID (0);
// @AD_SearchIndexTable_ID@
			setAD_Table_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_AD_SearchIndexColumn (Properties ctx, int AD_SearchIndexColumn_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_SearchIndexColumn_ID, trxName, virtualColumns);
      /** if (AD_SearchIndexColumn_ID == 0)
        {
			setAD_Column_ID (0);
			setAD_SearchIndexColumn_ID (0);
			setAD_SearchIndexTable_ID (0);
// @AD_SearchIndexTable_ID@
			setAD_Table_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AD_SearchIndexColumn (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AD_SearchIndexColumn[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Column)MTable.get(getCtx(), org.compiere.model.I_AD_Column.Table_ID)
			.getPO(getAD_Column_ID(), get_TrxName());
	}

	/** Set Column.
		@param AD_Column_ID Column in the table
	*/
	public void setAD_Column_ID (int AD_Column_ID)
	{
		if (AD_Column_ID < 1)
			set_Value (COLUMNNAME_AD_Column_ID, null);
		else
			set_Value (COLUMNNAME_AD_Column_ID, Integer.valueOf(AD_Column_ID));
	}

	/** Get Column.
		@return Column in the table
	  */
	public int getAD_Column_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Column_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Omnisearch Config Line.
		@param AD_SearchIndexColumn_ID Omnisearch Config Line
	*/
	public void setAD_SearchIndexColumn_ID (int AD_SearchIndexColumn_ID)
	{
		if (AD_SearchIndexColumn_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndexColumn_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndexColumn_ID, Integer.valueOf(AD_SearchIndexColumn_ID));
	}

	/** Get Omnisearch Config Line.
		@return Omnisearch Config Line
	  */
	public int getAD_SearchIndexColumn_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_SearchIndexColumn_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AD_OmnSearchConfigLine_UU.
		@param AD_SearchIndexColumn_UU AD_OmnSearchConfigLine_UU
	*/
	public void setAD_SearchIndexColumn_UU (String AD_SearchIndexColumn_UU)
	{
		set_Value (COLUMNNAME_AD_SearchIndexColumn_UU, AD_SearchIndexColumn_UU);
	}

	/** Get AD_OmnSearchConfigLine_UU.
		@return AD_OmnSearchConfigLine_UU	  */
	public String getAD_SearchIndexColumn_UU()
	{
		return (String)get_Value(COLUMNNAME_AD_SearchIndexColumn_UU);
	}

	public I_AD_SearchIndexTable getAD_SearchIndexTable() throws RuntimeException
	{
		return (I_AD_SearchIndexTable)MTable.get(getCtx(), I_AD_SearchIndexTable.Table_ID)
			.getPO(getAD_SearchIndexTable_ID(), get_TrxName());
	}

	/** Set Search Index Table.
		@param AD_SearchIndexTable_ID Search Index Table definition.
	*/
	public void setAD_SearchIndexTable_ID (int AD_SearchIndexTable_ID)
	{
		if (AD_SearchIndexTable_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndexTable_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndexTable_ID, Integer.valueOf(AD_SearchIndexTable_ID));
	}

	/** Get Search Index Table.
		@return Search Index Table definition.
	  */
	public int getAD_SearchIndexTable_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_SearchIndexTable_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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
			set_Value (COLUMNNAME_AD_Table_ID, null);
		else
			set_Value (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
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

	public org.compiere.model.I_AD_Column getParent_Column() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Column)MTable.get(getCtx(), org.compiere.model.I_AD_Column.Table_ID)
			.getPO(getParent_Column_ID(), get_TrxName());
	}

	/** Set Parent Column.
		@param Parent_Column_ID The link column on the parent tab.
	*/
	public void setParent_Column_ID (int Parent_Column_ID)
	{
		if (Parent_Column_ID < 1)
			set_Value (COLUMNNAME_Parent_Column_ID, null);
		else
			set_Value (COLUMNNAME_Parent_Column_ID, Integer.valueOf(Parent_Column_ID));
	}

	/** Get Parent Column.
		@return The link column on the parent tab.
	  */
	public int getParent_Column_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Parent_Column_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}