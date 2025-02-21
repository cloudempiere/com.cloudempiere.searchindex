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
* - Peter Takacs, Cloudempiere                                        *
**********************************************************************/
package com.cloudempiere.searchindex.util.pojo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Search Index Data
 */
public class SearchIndexTableData {

    /* AD_Table_ID */
    private int tableId;
    /* Key Column Name */
    private String keyColName;
    /* Index Table Name */
    private String searchIndexName;
    /* Data, key is ColumnName */
    private Set<Map<String, SearchIndexColumnData>> columnData;

    public SearchIndexTableData(int tableId, String keyColName, String searchIndexName) {
        this.tableId = tableId;
        this.keyColName = keyColName;
        this.searchIndexName = searchIndexName;
        this.columnData = new HashSet<>();
    }

    public int getTableId() {
        return tableId;
    }

    public String getKeyColName() {
        return keyColName;
    }

    public Set<Map<String, SearchIndexColumnData>> getColumnData() {
        return columnData;
    }

    public void addColumnData(Map<String, SearchIndexColumnData> data) {
        this.columnData.add(data);
    }

    public String getSearchIndexName() {
        return searchIndexName;
    }

    @Override
    public String toString() {
        return "SearchIndexRecord{" +
                "tableId='" + tableId + '\'' +
                ", columnData=" + columnData +
                '}';
    }
}