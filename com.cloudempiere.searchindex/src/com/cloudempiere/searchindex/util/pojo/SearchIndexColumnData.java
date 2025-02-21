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

import java.math.BigDecimal;
import java.util.Objects;

public class SearchIndexColumnData {
    private String columnName;
    private Object value;
    private BigDecimal searchWeight;
		private BigDecimal maxSearchWeight;

    public SearchIndexColumnData(String columnName, Object value, BigDecimal weight, BigDecimal maxSearchWeight) {
        this.columnName = columnName;
        this.value = value;
        this.searchWeight = weight;
				this.maxSearchWeight = maxSearchWeight;
    }

    public String getColumnName() {
        return columnName;
    }

    public Object getValue() {
        return value;
    }

    public BigDecimal getSearchWeight() {
        return searchWeight;
    }

	public BigDecimal getMaxSearchWeight() {
		return maxSearchWeight;
	}

	public void setMaxSearchWeight(BigDecimal maxSearchWeight) {
		this.maxSearchWeight = maxSearchWeight;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchIndexColumnData that = (SearchIndexColumnData) o;
        return searchWeight == that.searchWeight &&
                Objects.equals(columnName, that.columnName) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, value, searchWeight, maxSearchWeight);
    }

    @Override
    public String toString() {
        return "SearchIndexColumnData{" +
                "columnName='" + columnName + '\'' +
                ", value=" + value +
                ", weight=" + searchWeight +
				", maxSearchWeight=" + maxSearchWeight +
                '}';
    }
}