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

/** Generated Model for AD_SearchIndexProvider
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="AD_SearchIndexProvider")
public class X_AD_SearchIndexProvider extends PO implements I_AD_SearchIndexProvider, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250210L;

    /** Standard Constructor */
    public X_AD_SearchIndexProvider (Properties ctx, int AD_SearchIndexProvider_ID, String trxName)
    {
      super (ctx, AD_SearchIndexProvider_ID, trxName);
      /** if (AD_SearchIndexProvider_ID == 0)
        {
			setAD_SearchIndexProvider_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_SearchIndexProvider (Properties ctx, int AD_SearchIndexProvider_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_SearchIndexProvider_ID, trxName, virtualColumns);
      /** if (AD_SearchIndexProvider_ID == 0)
        {
			setAD_SearchIndexProvider_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_AD_SearchIndexProvider (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_AD_SearchIndexProvider[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
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

	/** Set AD_SearchIndexProvider_UU.
		@param AD_SearchIndexProvider_UU AD_SearchIndexProvider_UU
	*/
	public void setAD_SearchIndexProvider_UU (String AD_SearchIndexProvider_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AD_SearchIndexProvider_UU, AD_SearchIndexProvider_UU);
	}

	/** Get AD_SearchIndexProvider_UU.
		@return AD_SearchIndexProvider_UU	  */
	public String getAD_SearchIndexProvider_UU()
	{
		return (String)get_Value(COLUMNNAME_AD_SearchIndexProvider_UU);
	}

	/** Set Classname.
		@param Classname Java Classname
	*/
	public void setClassname (String Classname)
	{
		set_Value (COLUMNNAME_Classname, Classname);
	}

	/** Get Classname.
		@return Java Classname
	  */
	public String getClassname()
	{
		return (String)get_Value(COLUMNNAME_Classname);
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

	/** Set URL.
		@param URL URL
	*/
	public void setURL (String URL)
	{
		set_Value (COLUMNNAME_URL, URL);
	}

	/** Get URL.
		@return URL
	  */
	public String getURL()
	{
		return (String)get_Value(COLUMNNAME_URL);
	}
}