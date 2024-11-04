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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

public class SearchIndexHelper {
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(SearchIndexHelper.class);
	public static final String SEARCH_INDEX_INDEX = "SEARCH_INDEX_INDEX";
	
	public static void recreateIndex(String indexType) {
		
		Thread recreateIndexThread = new Thread(() -> {
			int AD_Process_ID = MProcess.getProcess_ID("CreateIndexProcess", null);
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
	
	public static void recreateDocument(String documentType, String trxName) {
		if (documentType != null)
			getDocument(documentType).recreateDocument(trxName);
	}

	//Check if the index column exists to avoid NPE in the validator the first time the plug-in runs
	public static boolean indexExist(String indexType, String trxName) {
		return DB.getSQLValue(trxName, "SELECT 1 FROM ad_column WHERE columnname =?", indexType) > 0;
	}
	
	public static void updateDocument(String documentType, PO po, boolean isNew) {
		getDocument(documentType).updateDocument(po, isNew, po.get_TrxName());
	}
	
	public static void deleteFromDocument(String documentType, PO po) {
		getDocument(documentType).deleteFromDocument(po);
	}
	
	public static void updateParent(String documentType, PO po) {
		getDocument(documentType).updateParent(po);
	}
	
	public static SearchIndexDocument getDocument() {
		return getDocument(MSysConfig.getValue(SEARCH_INDEX_INDEX, SearchIndexAbstractFactory.TEXTSEARCH_INDEX));
	}
	
	public static SearchIndexDocument getDocument(String documentType) {
		SearchIndexAbstractFactory searchIndexFactory = SearchIndexFactoryProducer.getFactory(SearchIndexFactoryProducer.DOCUMENT_FACTORY);
		return searchIndexFactory.getDocument(documentType);
	}
	
	public static SearchIndexIndex getIndex(String indexType) {
		SearchIndexAbstractFactory searchIndexFactory = SearchIndexFactoryProducer.getFactory(SearchIndexFactoryProducer.INDEX_FACTORY);
		return searchIndexFactory.getIndex(indexType);
	}
	
	/**
	 * CLDE
	 * @param trxName
	 * @param clientId
	 * @return
	 */
	public static List<String> getIndexedTableNames(String trxName, int clientId) {
	    List<String> tableNames = new ArrayList<>();

	    StringBuilder sql = new StringBuilder("SELECT DISTINCT t.tablename ")
	        .append("FROM AD_Table t ")
	        .append("JOIN AD_OmnSearchConfigLine oscl ON t.AD_Table_ID = oscl.AD_Table_ID ")
	        .append("JOIN AD_OmnSearchConfig osc ON oscl.AD_OmnSearchConfig_ID = osc.AD_OmnSearchConfig_ID ")
	        .append("WHERE osc.IsActive = 'Y' AND oscl.IsActive = 'Y'");
	    if(clientId >= 0)
	    	sql.append(" AND osc.AD_Client_ID IN (0, ?)");

	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
	        pstmt = DB.prepareStatement(sql.toString(), trxName);
	        if(clientId >= 0)
	        	pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
	        rs = pstmt.executeQuery();

	        while (!Thread.currentThread().isInterrupted() && rs.next()) {
	            tableNames.add(rs.getString(1));
	        }
	    } catch (Exception e) {
	        log.log(Level.SEVERE, sql.toString(), e);
	    } finally {
	        DB.close(rs, pstmt);
	        rs = null;
	        pstmt = null;
	    }

	    return tableNames;
	}

	/**
	 * CLDE
	 * @param trxName
	 * @param clientId
	 * @return
	 */
	public static Set<String> getForeignTableNames(String trxName, int clientId) {
	    Set<String> tableNames = new HashSet<>();

	    String sql = "SELECT DISTINCT t.tablename "
	        + "FROM AD_Table t "
	        + "JOIN AD_Column c ON t.AD_Table_ID = c.AD_Table_ID "
	        + "JOIN AD_OmnSearchConfigLine oscl ON c.AD_Column_ID = oscl.AD_Column_ID "
	        + "JOIN AD_OmnSearchConfig osc ON oscl.AD_OmnSearchConfig_ID = osc.AD_OmnSearchConfig_ID "
	        + "WHERE osc.IsActive = 'Y' AND oscl.IsActive = 'Y' ";
	    if(clientId >= 0)
	    	sql += "AND osc.AD_Client_ID IN (0, ?) ";
	    sql += "AND c.AD_Reference_ID IN (?,?,?,?,?,?,?,?,?,?,?)";

	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
	        pstmt = DB.prepareStatement(sql.toString(), trxName);
	        int idx = 1;
	        if(clientId >= 0) pstmt.setInt(idx++, Env.getAD_Client_ID(Env.getCtx()));
	        pstmt.setInt(idx++, DisplayType.TableDir);
	        pstmt.setInt(idx++, DisplayType.Search);
	        pstmt.setInt(idx++, DisplayType.Table);
	        pstmt.setInt(idx++, DisplayType.List);
	        pstmt.setInt(idx++, DisplayType.Payment);
	        pstmt.setInt(idx++, DisplayType.Location);
	        pstmt.setInt(idx++, DisplayType.Account);
	        pstmt.setInt(idx++, DisplayType.Locator);
	        pstmt.setInt(idx++, DisplayType.PAttribute);
	        pstmt.setInt(idx++, DisplayType.Assignment);
	        pstmt.setInt(idx++, DisplayType.RadiogroupList);
	        rs = pstmt.executeQuery();

	        while (!Thread.currentThread().isInterrupted() && rs.next()) {
	            tableNames.add(rs.getString(1));
	        }
	    } catch (Exception e) {
	        log.log(Level.SEVERE, sql.toString(), e);
	    } finally {
	        DB.close(rs, pstmt);
	        rs = null;
	        pstmt = null;
	    }

	    return tableNames;
	}
}