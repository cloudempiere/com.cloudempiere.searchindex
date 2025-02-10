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

/** Generated Model for AD_SearchIndex
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AD_SearchIndex")
public class X_AD_SearchIndex extends PO implements I_AD_SearchIndex, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250210L;

    /** Standard Constructor */
    public X_AD_SearchIndex (Properties ctx, int AD_SearchIndex_ID, String trxName)
    {
      super (ctx, AD_SearchIndex_ID, trxName);
      /** if (AD_SearchIndex_ID == 0)
        {
			setAD_SearchIndex_ID (0);
			setAD_SearchIndexProvider_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_SearchIndex (Properties ctx, int AD_SearchIndex_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_SearchIndex_ID, trxName, virtualColumns);
      /** if (AD_SearchIndex_ID == 0)
        {
			setAD_SearchIndex_ID (0);
			setAD_SearchIndexProvider_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_AD_SearchIndex (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AD_SearchIndex[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
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

	public I_AD_SearchIndexProvider getAD_SearchIndexProvider() throws RuntimeException
	{
		return (I_AD_SearchIndexProvider)MTable.get(getCtx(), I_AD_SearchIndexProvider.Table_ID)
			.getPO(getAD_SearchIndexProvider_ID(), get_TrxName());
	}

	/** Set Search Index Provider.
		@param AD_SearchIndexProvider_ID Search Index Provider
	*/
	public void setAD_SearchIndexProvider_ID (int AD_SearchIndexProvider_ID)
	{
		if (AD_SearchIndexProvider_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndexProvider_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_SearchIndexProvider_ID, Integer.valueOf(AD_SearchIndexProvider_ID));
	}

	/** Get Search Index Provider.
		@return Search Index Provider	  */
	public int getAD_SearchIndexProvider_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_SearchIndexProvider_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AD_SearchIndex_UU.
		@param AD_SearchIndex_UU AD_SearchIndex_UU
	*/
	public void setAD_SearchIndex_UU (String AD_SearchIndex_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AD_SearchIndex_UU, AD_SearchIndex_UU);
	}

	/** Get AD_SearchIndex_UU.
		@return AD_SearchIndex_UU	  */
	public String getAD_SearchIndex_UU()
	{
		return (String)get_Value(COLUMNNAME_AD_SearchIndex_UU);
	}

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Valid.
		@param IsValid Element is valid
	*/
	public void setIsValid (boolean IsValid)
	{
		set_Value (COLUMNNAME_IsValid, Boolean.valueOf(IsValid));
	}

	/** Get Valid.
		@return Element is valid
	  */
	public boolean isValid()
	{
		Object oo = get_Value(COLUMNNAME_IsValid);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
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

	/** Set Process Now.
		@param Processing Process Now
	*/
	public void setProcessing (boolean Processing)
	{
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));
	}

	/** Get Process Now.
		@return Process Now	  */
	public boolean isProcessing()
	{
		Object oo = get_Value(COLUMNNAME_Processing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Search Index Name.
		@param SearchIndexName Search Index Name
	*/
	public void setSearchIndexName (String SearchIndexName)
	{
		set_Value (COLUMNNAME_SearchIndexName, SearchIndexName);
	}

	/** Get Search Index Name.
		@return Search Index Name	  */
	public String getSearchIndexName()
	{
		return (String)get_Value(COLUMNNAME_SearchIndexName);
	}

	/** Set Transaction Code.
		@param TransactionCode The transaction code represents the search definition
	*/
	public void setTransactionCode (String TransactionCode)
	{
		set_Value (COLUMNNAME_TransactionCode, TransactionCode);
	}

	/** Get Transaction Code.
		@return The transaction code represents the search definition
	  */
	public String getTransactionCode()
	{
		return (String)get_Value(COLUMNNAME_TransactionCode);
	}
}