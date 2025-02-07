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
 * Simplified class for AD_SearchIndex
 */
public class SearchIndexConfig {

	/* AD_SearchIndex_ID */
	private int searchIndexId;
	/* SearchIndexName */
	private String searchIndexName;
	/* List of Search Index Tables */
    private List<SearchIndexTableConfig> tableConfigs;

    public SearchIndexConfig(int searchIndexId, String searchIndexName) {
    	this.searchIndexId = searchIndexId;
    	this.searchIndexName = searchIndexName;
        this.tableConfigs = new ArrayList<>();
    }

    public List<SearchIndexTableConfig> getTableConfigs() {
        return tableConfigs;
    }

    public void addTableConfig(SearchIndexTableConfig tableConfig) {
        this.tableConfigs.add(tableConfig);
    }

	public int getSearchIndexId() {
		return searchIndexId;
	}

	public void setSearchIndexId(int searchIndexId) {
		this.searchIndexId = searchIndexId;
	}

	public String getSearchIndexName() {
		return searchIndexName;
	}

	public void setSearchIndexName(String searchIndexName) {
		this.searchIndexName = searchIndexName;
	}

}
