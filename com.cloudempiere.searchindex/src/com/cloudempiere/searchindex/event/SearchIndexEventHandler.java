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
package com.cloudempiere.searchindex.event;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.model.GenericPO;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;

import com.cloudempiere.searchindex.event.pojo.IndexedTable;
import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider;
import com.cloudempiere.searchindex.model.MSearchIndex;
import com.cloudempiere.searchindex.model.MSearchIndexColumn;
import com.cloudempiere.searchindex.model.MSearchIndexTable;
import com.cloudempiere.searchindex.util.SearchIndexConfigBuilder;
import com.cloudempiere.searchindex.util.SearchIndexUtils;

@Component( reference = @Reference( name = "IEventManager", bind = "bindEventManager", unbind="unbindEventManager", 
policy = ReferencePolicy.STATIC, cardinality =ReferenceCardinality.MANDATORY, service = IEventManager.class))
public class SearchIndexEventHandler extends AbstractEventHandler {

	/**	Logger */
	private static CLogger log = CLogger.getCLogger(SearchIndexEventHandler.class);
	
	/** Context */
	private Properties ctx = null;
	/** Transaction name */
	private String trxName = null;
	/** Indexed tables set (key: AD_SearchIndex_ID, name: TableName) */
	private Map<Integer, Set<IndexedTable>> indexedTablesByClient = null; // key is AD_Client_ID
	
	@Override
	protected void initialize() {

		indexedTablesByClient = SearchIndexUtils.getSearchIndexConfigs(trxName, -1); // gets data from all clients
		Set<String> tablesToRegister = new HashSet<>();

		for (Map.Entry<Integer, Set<IndexedTable>> entry : indexedTablesByClient.entrySet()) {
			Set<IndexedTable> indexedTables = entry.getValue();
			for (IndexedTable indexTable : indexedTables) {
				String tableName = indexTable.getTableName();
				tablesToRegister.add(tableName);
				//Index the FK tables
				for (String fkTableName : indexTable.getFKTableNames()) {
					tablesToRegister.add(fkTableName);
				}
			}
		}
		
		for (String tableName : tablesToRegister) {
			registerTableEvent(IEventTopics.PO_AFTER_NEW, tableName);
			registerTableEvent(IEventTopics.PO_AFTER_CHANGE, tableName);
			registerTableEvent(IEventTopics.PO_AFTER_DELETE, tableName);
		}

		// Handle the changes in the Search Index definition to update the index
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MSearchIndexColumn.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MSearchIndexColumn.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_DELETE, MSearchIndexColumn.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MSearchIndexTable.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MSearchIndex.Table_Name);
	}

	@Override
	protected void doHandleEvent(Event event) {
		
		String type = event.getTopic();
		PO eventPO = getPO(event);
		ctx = Env.getCtx();
		trxName = eventPO.get_TrxName();
		Set<IndexedTable> indexedTables = indexedTablesByClient.get(Env.getAD_Client_ID(ctx));
		Set<IndexedTable> indexedTables0 = indexedTablesByClient.get(0);
		if (indexedTables == null)
			indexedTables = indexedTables0;
		else {
			if (indexedTables0 != null)
				indexedTables.addAll(indexedTables0);
		}
		
		if (eventPO instanceof MSearchIndex
				|| eventPO instanceof MSearchIndexTable
				|| eventPO instanceof MSearchIndexColumn) {
			
			handleSearchIndexConfigChange(eventPO);
			return;
		}
		
		if (!MSysConfig.getBooleanValue(MSysConfig.ALLOW_SEARCH_INDEX_EVENT, false, Env.getAD_Client_ID(ctx)))
			return;
		if (indexedTables == null)
			return;
		
		// Check if changed column is indexed
		if (type.equals(IEventTopics.PO_AFTER_CHANGE)) {
			MTable mTableEvt = MTable.get(ctx, eventPO.get_Table_ID(), trxName);
			boolean updateIndex = false;
			Set<Integer> changedColumnIDs = new HashSet<>();
			for (int columnId : mTableEvt.getColumnIDs(false)) {
				if (eventPO.is_ValueChanged_byId(columnId))
					changedColumnIDs.add(columnId);
			}
			for (IndexedTable searchIndexConfig : indexedTables) {
				for (int changedColId : changedColumnIDs) {
					if (searchIndexConfig.getColumnIDs().contains(Integer.valueOf(changedColId))) {
						updateIndex = true;
						break;
					}
				}
				if (updateIndex)
					break;
			}
			if (!updateIndex)
				return;
		}
		
		PO[] mainPOArr = getMainPOs(eventPO, indexedTables);
		MSearchIndex[] searchIndexArr;
		
		for (PO po : mainPOArr) {
			// Find existing search indexes for the record
			if (type.equals(IEventTopics.PO_AFTER_NEW)) {
				searchIndexArr = MSearchIndex.getForTable(ctx, po, eventPO, indexedTables, trxName);
			} else {
				searchIndexArr = MSearchIndex.getForRecord(ctx, po, eventPO, indexedTables, trxName);
			}
			
			if (searchIndexArr == null)
				continue;
			
			for (MSearchIndex searchIndex : searchIndexArr) {
				ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(ctx, searchIndex.getAD_SearchIndexProvider_ID(), null, trxName);
				int tableId = po.get_Table_ID();
				int recordId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();
				
				// Initialise builder
				SearchIndexConfigBuilder builder = new SearchIndexConfigBuilder()
						.setCtx(ctx)
						.setTrxName(trxName)
						.setAD_SearchIndex_ID(searchIndex.getAD_SearchIndex_ID())
						.setRecord(tableId, recordId);
				
				if (type.equals(IEventTopics.PO_AFTER_DELETE) && po.equals(eventPO)) {
					if (provider != null) {
						StringBuilder whereClause = new StringBuilder();
						whereClause.append(" AD_Client_ID=? AND AD_Table_ID=? AND Record_ID=?");
						Object[] params = new Object[] { po.getAD_Client_ID(), tableId, recordId };
						provider.deleteIndex(ctx, searchIndex.getSearchIndexName(), whereClause.toString(), params, trxName);
					}
				} else if (type.equals(IEventTopics.PO_AFTER_CHANGE)
						|| type.equals(IEventTopics.PO_AFTER_DELETE) && !po.equals(eventPO)
						|| type.equals(IEventTopics.PO_AFTER_NEW) && !po.equals(eventPO)) {
					if (provider != null) {
						provider.updateIndex(ctx, builder.build().getData(false), trxName);
					}
				} else if (type.equals(IEventTopics.PO_AFTER_NEW) && po.equals(eventPO)) {
					if (provider != null) {
						provider.createIndex(ctx, builder.build().getData(false), trxName);
					}
				} else {
					log.warning("Unsupportet event type");
				}
			}
		}
	}
	
	private PO[] getMainPOs(PO po, Set<IndexedTable> tableConfigs) {
		Set<PO> mainPOSet = new HashSet<>();
		for (IndexedTable tableConfig : tableConfigs) {
			// record of an index table
			if (po.get_TableName().equals(tableConfig.getTableName())) {
				po = applyWhereClause(po, tableConfig.getWhereClause());
				if (po != null)
					mainPOSet.add(po);
			} else { // related to an index table - find the main record
				for (String fkTableName : tableConfig.getFKTableNames()) {
					if (po.get_TableName().equals(fkTableName)) {
						PO[] mainPOsOfTable = getMainPOsOfTable(po, tableConfig.getTableName(), tableConfig.getWhereClause());
						for (PO mainPO : mainPOsOfTable) {
							mainPOSet.add(mainPO);
						}
					}
				}
			}
		}
		return mainPOSet.toArray(new PO[0]);
	}
	
	private PO[] getMainPOsOfTable(PO po, String mainTableName, String whereClause) {
		
		if (!Util.isEmpty(whereClause))
			whereClause = " AND " + whereClause;
		else if (whereClause == null)
			whereClause = "";
		
		Set<PO> mainPOSet = new HashSet<>();
		
		// one to many
		MTable fkTable = MTable.get(ctx, mainTableName, trxName);
		for (String keyCol : po.get_KeyColumns()) {
			if (fkTable.columnExistsInDictionary(keyCol)) {
				// FIXME has problem with aliases in whereClause: ERROR: missing FROM-clause entry for table "main
				int poId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();
				for (int recordId : PO.getAllIDs(mainTableName, keyCol+"="+poId+whereClause, trxName)) {
					mainPOSet.add(new GenericPO(mainTableName, ctx, recordId, trxName));
				}
			}
		}
		// one to one
		if (mainPOSet.isEmpty()) {
			for (String keyCol : fkTable.getKeyColumns()) {
				if (po.columnExists(keyCol)) {
					//  TODO apply where clause here too
					int recordId = po.get_ValueAsInt(keyCol);
					mainPOSet.add(new GenericPO(mainTableName, ctx, recordId, trxName));
				}
			}
		}
		return mainPOSet.toArray(new PO[0]);
	}
	
	/**
	 * Check if PO passes the where clause filter
	 * @param po
	 * @param whereClause
	 * @return
	 */
	private PO applyWhereClause(PO po, String whereClause) {
		
		if (Util.isEmpty(whereClause))
			return po;
		else
			whereClause = " AND " + whereClause;
		
		MTable table = MTable.get(ctx, po.get_TableName(), trxName);
		for (String keyCol : po.get_KeyColumns()) {
			if (table.columnExistsInDictionary(keyCol)) {
				// FIXME has problem with aliases in whereClause: ERROR: missing FROM-clause entry for table "main
				int poId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();
				if (new Query(ctx, po.get_TableName(), keyCol+"="+poId+whereClause, trxName).match())
					return po;
			}
		}
		return null;
	}
	
	private void handleSearchIndexConfigChange(PO po) {
		
		int searchIndexId;
		// Get the Search Index
		if (po instanceof MSearchIndex) {
			searchIndexId = po.get_ValueAsInt(MSearchIndex.COLUMNNAME_AD_SearchIndex_ID);
		} else if (po instanceof MSearchIndexTable) {
			MSearchIndexTable indexTable = (MSearchIndexTable) po;
			searchIndexId = indexTable.getAD_SearchIndex_ID();
		} else if (po instanceof MSearchIndexColumn) {
			MSearchIndexColumn indexColumn = (MSearchIndexColumn) po;
			MSearchIndexTable indexTable = MSearchIndexTable.get(ctx, indexColumn.getAD_SearchIndexTable_ID(), trxName);
			searchIndexId = indexTable.getAD_SearchIndex_ID();
		} else {
			return;
		}
		
		// Invalidate the Search Index
		String sql = "UPDATE AD_SearchIndex SET IsValid='N' WHERE AD_SearchIndex_ID=?";
		DB.executeUpdateEx(sql, new Object[] {searchIndexId}, trxName);
		
		// Register/unregister the modified table
		IEventManager tempManager = eventManager;
		unbindEventManager(eventManager);
		bindEventManager(tempManager);
	}

}
