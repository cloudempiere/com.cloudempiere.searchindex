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
package com.cloudempiere.searchindex.indexprovider;

import org.compiere.util.Util;

import com.cloudempiere.searchindex.indexprovider.elasticsearch.ElasticSearchIndexProvider;
import com.cloudempiere.searchindex.indexprovider.pgtextsearch.PGTextSearchIndexProvider;

/**
 * 
 * Search Index Provider factory
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public class SearchIndexProviderFactory {

	public ISearchIndexProvider getSearchIndexProvider(String classname) {
		if(Util.isEmpty(classname))
			return null;
		else
			classname = classname.trim();
		if(classname.equals("com.cloudempiere.searchindex.elasticsearch.ElasticSearchIndexProvider"))
			return new ElasticSearchIndexProvider();
		if(classname.equals("com.cloudempiere.searchindex.pgtextsearch.PGTextSearchIndexProvider"))
			return new PGTextSearchIndexProvider();
		return null;
	}

}
