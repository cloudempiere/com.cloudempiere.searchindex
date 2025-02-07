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

/**
 * Simplified class for AD_SearchIndexColumn
 */
public class SearchIndexColumnConfig {

	/* AD_Table_ID */
	private int tableId;
	/* TableName */
	private String tableName;
	/* AD_Column_ID */
	private int columnId;
	/* ColumnName */
	private String columnName;
	/* ParentColumn_ID */
	private int parentColumnId;
	/* Parent Column Name */
	private String parentColumnName;
	/* Parent Column Name for foreign key */
	private String parentTableName;

    public SearchIndexColumnConfig(int tableId, String tableName, int columnId, String columnName, int parentColumnId, String parentColumnName, String parentTableName) {
		this.tableId = tableId;
		this.tableName = tableName;
		this.columnId = columnId;
		this.columnName = columnName;
		this.parentColumnId = parentColumnId;
		this.parentColumnName = parentColumnName;
		this.parentTableName = parentTableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

	public String getParentColumnName() {
		return parentColumnName;
	}

	public void setParentColumnName(String parentColumnName) {
		this.parentColumnName = parentColumnName;
	}

	public int getColumnId() {
		return columnId;
	}

	public void setColumnId(int columnId) {
		this.columnId = columnId;
	}

	public int getParentColumnId() {
		return parentColumnId;
	}

	public void setParentColumnId(int parentColumnId) {
		this.parentColumnId = parentColumnId;
	}

	public int getTableId() {
		return tableId;
	}

	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getParentTableName() {
		return parentTableName;
	}

	public void setParentTableName(String parentTableName) {
		this.parentTableName = parentTableName;
	}

}
