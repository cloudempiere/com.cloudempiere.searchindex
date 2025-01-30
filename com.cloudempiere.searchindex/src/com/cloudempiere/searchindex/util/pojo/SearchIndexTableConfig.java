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
package com.cloudempiere.searchindex.util.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified class for AD_SearchIndexTable
 */
public class SearchIndexTableConfig {

	/* AD_Table_ID */
	private int tableId;
	/* TableName */
	private String tableName;
	/* Key Column Name */
	private String keyColName;
	/* WhereClause */
	private String sqlWhere;
	/* Search Index Columns */
    private List<SearchIndexColumnConfig> columns;

    public SearchIndexTableConfig(String tableName, int tableId, String keyColName, String sqlWhere) {
    	this.tableId = tableId;
        this.tableName = tableName;
        this.keyColName = keyColName;
        this.sqlWhere = sqlWhere;
        this.columns = new ArrayList<>();
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<SearchIndexColumnConfig> getColumns() {
        return columns;
    }

    public void addColumn(SearchIndexColumnConfig column) {
        this.columns.add(column);
    }

	public int getTableId() {
		return tableId;
	}

	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	public String getKeyColName() {
		return keyColName;
	}

	public void setKeyColName(String keyColName) {
		this.keyColName = keyColName;
	}

	public String getSqlWhere() {
		return sqlWhere;
	}

	public void setSqlWhere(String sqlWhere) {
		this.sqlWhere = sqlWhere;
	}

}
