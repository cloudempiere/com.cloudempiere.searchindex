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
package com.cloudempiere.searchindex.tools;

import java.util.logging.Level;

import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

public class SearchIndexHelper {
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(SearchIndexHelper.class);
	public static final String SEARCH_INDEX_INDEX = "SEARCH_INDEX_INDEX";
	
	@Deprecated
	public static void recreateIndex(String indexType) {
		
		Thread recreateIndexThread = new Thread(() -> {
			int AD_Process_ID = MProcess.getProcess_ID("CreateSearchIndex", null);
			MProcess process = new MProcess(Env.getCtx(), AD_Process_ID, null);
			ProcessInfo m_pi = new ProcessInfo ("", AD_Process_ID);
			if (indexType != null) {
				MPInstance instance = new MPInstance(Env.getCtx(), AD_Process_ID, 0);
				if (!instance.save()) {
					log.log(Level.SEVERE, Msg.getMsg(Env.getCtx(), "ProcessNoInstance"));
					return;
				}
				m_pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());
				MPInstancePara para = new MPInstancePara(instance, 0);
				para.setParameter("BXS_IndexType", indexType);
			}

			String newTrxName = Trx.createTrxName("OmniIndex");
			Trx trx = Trx.get(newTrxName, true);
			if (!process.processIt(m_pi, trx) && m_pi.getClassName() != null) {
				String msg = Msg.getMsg(Env.getCtx(), "ProcessFailed") + " : (" + m_pi.getClassName() + ") " + m_pi.getSummary();
				log.log(Level.SEVERE, msg);
			}
		});
		recreateIndexThread.setDaemon(true);
		recreateIndexThread.start();
	}
	
	@Deprecated
	public static void recreateDocument(String documentType, String trxName) {
		if (documentType != null)
			getDocument(documentType).recreateDocument(trxName);
	}

	@Deprecated
	//Check if the index column exists to avoid NPE in the validator the first time the plug-in runs
	public static boolean indexExist(String indexType, String trxName) {
		return DB.getSQLValue(trxName, "SELECT 1 FROM ad_column WHERE columnname =?", indexType) > 0;
	}
	
	@Deprecated
	public static void updateDocument(String documentType, PO po, boolean isNew) {
		getDocument(documentType).updateDocument(po, isNew, po.get_TrxName());
	}
	
	@Deprecated
	public static void deleteFromDocument(String documentType, PO po) {
		getDocument(documentType).deleteFromDocument(po);
	}
	
	@Deprecated
	public static void updateParent(String documentType, PO po) {
		getDocument(documentType).updateParent(po);
	}

	@Deprecated
	public static SearchIndexDocument getDocument() {
		return getDocument(MSysConfig.getValue(SEARCH_INDEX_INDEX, SearchIndexAbstractFactory.TEXTSEARCH_INDEX));
	}
	
	@Deprecated
	public static SearchIndexDocument getDocument(String documentType) {
		SearchIndexAbstractFactory searchIndexFactory = SearchIndexFactoryProducer.getFactory(SearchIndexFactoryProducer.DOCUMENT_FACTORY);
		return searchIndexFactory.getDocument(documentType);
	}
	
	@Deprecated
	public static SearchIndexIndex getIndex(String indexType) {
		SearchIndexAbstractFactory searchIndexFactory = SearchIndexFactoryProducer.getFactory(SearchIndexFactoryProducer.INDEX_FACTORY);
		return searchIndexFactory.getIndex(indexType);
	}
	
}