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
package com.cloudempiere.searchindex.process;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Msg;

import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider;
import com.cloudempiere.searchindex.util.SearchIndexConfigBuilder;
import com.cloudempiere.searchindex.util.SearchIndexUtils;
import com.cloudempiere.searchindex.util.pojo.SearchIndexRecord;

public class CreateSearchIndex extends SvrProcess {
	
	protected int p_AD_SearchIndexProvider_ID = 0;
	
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_SearchIndexProvider_ID"))
				p_AD_SearchIndexProvider_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		
		if(p_AD_SearchIndexProvider_ID <= 0)
			throw new FillMandatoryException("AD_SearchIndexProvider_ID");
		
		// Get provider
		ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(getCtx(), p_AD_SearchIndexProvider_ID, processUI, get_TrxName());
		if(provider == null)
			throw new AdempiereException(Msg.getMsg(getCtx(), "SearchIndexProviderNotFound"));
		
		// Load data
		if (processUI != null) {
			processUI.statusUpdate("Collecting data...");  // TODO translate
		}
		SearchIndexConfigBuilder builder = new SearchIndexConfigBuilder()
				.setCtx(getCtx())
				.setTrxName(get_TrxName())
				.build();
		Map<Integer, Set<SearchIndexRecord>> indexRecordsMap = builder.getData(false); // key is AD_SearchIndex_ID
		if(indexRecordsMap.size() <= 0)
	    	return Msg.getMsg(getCtx(), "NoRecordsFound");
	    
		// Create index
	    provider.reCreateIndex(getCtx(), indexRecordsMap, get_TrxName());
		
		return Msg.getMsg(getCtx(), "Success"); // FIXME no error message
	}
}
