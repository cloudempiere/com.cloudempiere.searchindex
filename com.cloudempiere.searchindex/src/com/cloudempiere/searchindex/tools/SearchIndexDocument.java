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
* - Diego Ruiz, BX Service GmbH                                       *
* - Peter Takacs, Cloudempiere                                        *
**********************************************************************/
package com.cloudempiere.searchindex.tools;

import java.util.ArrayList;

import org.compiere.model.PO;

import com.cloudempiere.searchindex.pgtextsearch.PGTextSearchResult;

public interface SearchIndexDocument {
	
	//Document setup
	void buildDocument(String trxName);
	void updateDocument(PO po, boolean isNew, String trxName);
	void updateParent(PO po);
	void deleteDocument(String trxName);
	void recreateDocument(String trxName);
	void insertIntoDocument(String trxName, int AD_Table_ID, int Record_ID, ArrayList<Integer> columns);
	void insertIntoDocument(String trxName, int AD_Table_ID, ArrayList<Integer> columns);
	void deleteFromDocument(PO po);
	boolean isValidDocument();
	
	//Queries
	ArrayList<PGTextSearchResult> performQuery(String query, boolean isAdvanced);
	void setHeadline(PGTextSearchResult result, String query);
}
