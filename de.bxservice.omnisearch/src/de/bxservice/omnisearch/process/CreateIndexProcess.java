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
* - Diego Ruiz - BX Service GmbH                                      *
**********************************************************************/
package de.bxservice.omnisearch.process;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Msg;

import com.cloudempiere.omnisearch.indexprovider.ISearchIndexProvider;
import com.cloudempiere.omnisearch.util.SearchIndexConfig;
import com.cloudempiere.omnisearch.util.SearchIndexRecord;
import com.cloudempiere.omnisearch.util.SearchIndexUtils;

public class CreateIndexProcess extends SvrProcess {
	
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
		
		ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(getCtx(), p_AD_SearchIndexProvider_ID, processUI, get_TrxName());
		if(provider == null)
			throw new AdempiereException(Msg.getMsg(getCtx(), "SearchIndexProviderNotFound"));
		
		if (processUI != null) {
			processUI.statusUpdate("Collecting data...");  // TODO translate
		}
	    List<SearchIndexConfig> searchIndexConfigs = SearchIndexUtils.loadSearchIndexConfig(getCtx(), get_TrxName());
	    if(searchIndexConfigs.size() <= 0)
	    	throw new AdempiereException(Msg.getMsg(getCtx(), "SearchIndexConfigNotFound"));
	    
	    Map<Integer, Set<SearchIndexRecord>> indexRecordsMap = SearchIndexUtils.loadSearchIndexDataWithJoins(getCtx(), searchIndexConfigs, MSG_InvalidArguments);
		if(indexRecordsMap.size() <= 0)
	    	return Msg.getMsg(getCtx(), "NoRecordsFound");
	    
	    provider.reCreateIndex(getCtx(), indexRecordsMap, get_TrxName());
		
		return Msg.getMsg(getCtx(), "Success"); // FIXME no error message
	}
}
