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
package com.cloudempiere.omnisearch.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for AD_SearchIndexColumn
 *  @author iDempiere (generated) 
 *  @version Release 10
 */
@SuppressWarnings("all")
public interface I_AD_SearchIndexColumn 
{

    /** TableName=AD_SearchIndexColumn */
    public static final String Table_Name = "AD_SearchIndexColumn";

    /** AD_Table_ID=1001836 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 6 - System - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(6);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Column_ID */
    public static final String COLUMNNAME_AD_Column_ID = "AD_Column_ID";

	/** Set Column.
	  * Column in the table
	  */
	public void setAD_Column_ID (int AD_Column_ID);

	/** Get Column.
	  * Column in the table
	  */
	public int getAD_Column_ID();

	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException;

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name AD_SearchIndexColumn_ID */
    public static final String COLUMNNAME_AD_SearchIndexColumn_ID = "AD_SearchIndexColumn_ID";

	/** Set Omnisearch Config Line.
	  * Omnisearch Config Line
	  */
	public void setAD_SearchIndexColumn_ID (int AD_SearchIndexColumn_ID);

	/** Get Omnisearch Config Line.
	  * Omnisearch Config Line
	  */
	public int getAD_SearchIndexColumn_ID();

    /** Column name AD_SearchIndexColumn_UU */
    public static final String COLUMNNAME_AD_SearchIndexColumn_UU = "AD_SearchIndexColumn_UU";

	/** Set AD_OmnSearchConfigLine_UU	  */
	public void setAD_SearchIndexColumn_UU (String AD_SearchIndexColumn_UU);

	/** Get AD_OmnSearchConfigLine_UU	  */
	public String getAD_SearchIndexColumn_UU();

    /** Column name AD_SearchIndexTable_ID */
    public static final String COLUMNNAME_AD_SearchIndexTable_ID = "AD_SearchIndexTable_ID";

	/** Set Search Index Table.
	  * Search Index Table definition.
	  */
	public void setAD_SearchIndexTable_ID (int AD_SearchIndexTable_ID);

	/** Get Search Index Table.
	  * Search Index Table definition.
	  */
	public int getAD_SearchIndexTable_ID();

	public I_AD_SearchIndexTable getAD_SearchIndexTable() throws RuntimeException;

    /** Column name AD_Table_ID */
    public static final String COLUMNNAME_AD_Table_ID = "AD_Table_ID";

	/** Set Table.
	  * Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID);

	/** Get Table.
	  * Database Table information
	  */
	public int getAD_Table_ID();

	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException;

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name Help */
    public static final String COLUMNNAME_Help = "Help";

	/** Set Comment.
	  * Comment or Hint
	  */
	public void setHelp (String Help);

	/** Get Comment.
	  * Comment or Hint
	  */
	public String getHelp();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name Parent_Column_ID */
    public static final String COLUMNNAME_Parent_Column_ID = "Parent_Column_ID";

	/** Set Parent Column.
	  * The link column on the parent tab.
	  */
	public void setParent_Column_ID (int Parent_Column_ID);

	/** Get Parent Column.
	  * The link column on the parent tab.
	  */
	public int getParent_Column_ID();

	public org.compiere.model.I_AD_Column getParent_Column() throws RuntimeException;

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();
}
