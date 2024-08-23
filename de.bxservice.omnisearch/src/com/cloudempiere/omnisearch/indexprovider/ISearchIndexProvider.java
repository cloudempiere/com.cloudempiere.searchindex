/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package com.cloudempiere.omnisearch.indexprovider;

import java.util.List;

import com.cloudempiere.omnisearch.model.MSearchIndexProvider;

/**
 * 
 * Search Index Provider interface
 * 
 * @author Peter Takacs, Cloudempiere
 *
 */
public interface ISearchIndexProvider
{
	public void init(MSearchIndexProvider searchIndexProvider, String searchIndexName);
	
	public void deleteAllIndex();

	public void deleteIndexByQuery(String query);

	public Object searchIndexNoRestriction(String queryString);

	public List<Object> searchIndexDocument(String queryString);

	public List<Object> searchIndexDocument(String queryString, int maxRow);

	public void createIndex(int ad_table_id, int record_id, String ...values);
	
}
