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
import java.util.logging.Level;

import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Msg;

import com.cloudempiere.omnisearch.indexprovider.ISearchIndexProvider;
import com.cloudempiere.omnisearch.indexprovider.SearchIndexProviderFactory;
import com.cloudempiere.omnisearch.model.MSearchIndexProvider;
import com.cloudempiere.omnisearch.util.SearchIndexConfig;
import com.cloudempiere.omnisearch.util.SearchIndexUtils;

public class CreateIndexProcess extends SvrProcess {
	
	protected int p_AD_SearchIndexProvider_ID = 0;
	
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
//		for (int i = 0; i < para.length; i++)
//		{
//			String name = para[i].getParameterName();
//			if (para[i].getParameter() == null)
//				;
//			else if (name.equals("BXS_IndexType"))
//				indexType = (String)para[i].getParameter();
//			else
//				log.log(Level.SEVERE, "Unknown Parameter: " + name);
//		}
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
//		//Set default in case of null to avoid NPE
//		if (indexType == null)
//			indexType = OmnisearchAbstractFactory.TEXTSEARCH_INDEX;
//
//		//First populate the vector then create the index for faster performance
//		//Creates the document
//		log.log(Level.INFO, "Creating the document");
//		OmnisearchHelper.recreateDocument(indexType, get_TrxName());
//		
//		//Creates the index
//		log.log(Level.INFO, "Creating the index");
//		OmnisearchHelper.getIndex(indexType).recreateIndex(get_TrxName());
//		
//        return "@OK@";
		
		if(p_AD_SearchIndexProvider_ID <= 0)
			throw new FillMandatoryException("AD_SearchIndexProvider_ID");
		
		MSearchIndexProvider providerDef = new MSearchIndexProvider(getCtx(), p_AD_SearchIndexProvider_ID, get_TrxName());		
		SearchIndexProviderFactory factory = new SearchIndexProviderFactory();
		ISearchIndexProvider provider = factory.getSearchIndexProvider(providerDef.getClassname());
	    List<SearchIndexConfig> searchIndexConfigs = SearchIndexUtils.loadSearchIndexConfig(getCtx(), get_TrxName());
	    System.out.println(SearchIndexUtils.loadSearchIndexDataWithJoins(getCtx(), searchIndexConfigs, MSG_InvalidArguments));
		
		// TODO call the search provider
		
		return Msg.getMsg(getCtx(), "Success");
	}
}
