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
import org.compiere.util.Trx;
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

	/** Indexed tables set (key: AD_SearchIndex_ID, name: TableName) */
	private Map<Integer, Set<IndexedTable>> indexedTablesByClient = null; // key is AD_Client_ID

	@Override
	protected void initialize() {

		indexedTablesByClient = SearchIndexUtils.getSearchIndexConfigs(null, -1); // gets data from all clients
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
		// Fix ADR-001: Use local variables instead of instance variables
		Properties ctx = Env.getCtx();
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

			handleSearchIndexConfigChange(eventPO, ctx);
			return;
		}
		
		if (!MSysConfig.getBooleanValue(MSysConfig.ALLOW_SEARCH_INDEX_EVENT, false, Env.getAD_Client_ID(ctx)))
			return;
		if (indexedTables == null)
			return;
		
		// Check if changed column is indexed or if IsActive changed
		if (type.equals(IEventTopics.PO_AFTER_CHANGE)) {
			MTable mTableEvt = MTable.get(ctx, eventPO.get_Table_ID());
			boolean updateIndex = false;
			boolean isActiveChanged = false;
			Set<Integer> changedColumnIDs = new HashSet<>();
			
			// Check if IsActive changed
			if (eventPO.is_ValueChanged("IsActive")) {
				isActiveChanged = true;
				updateIndex = true;
			}
			
			// Continue with normal column change check if IsActive didn't change
			if (!isActiveChanged) {
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
			}
			if (!updateIndex)
				return;
		}
		
		// Fix ADR-001: Get main POs using null transaction (read-only query)
		PO[] mainPOArr = getMainPOs(eventPO, indexedTables, ctx, null);
		MSearchIndex[] searchIndexArr;

		for (PO po : mainPOArr) {
			// Find existing search indexes for the record (read-only query, no transaction needed)
			if (type.equals(IEventTopics.PO_AFTER_NEW)
					|| (type.equals(IEventTopics.PO_AFTER_CHANGE) && eventPO.is_ValueChanged("IsActive"))) {
				searchIndexArr = MSearchIndex.getForTable(ctx, po, eventPO, indexedTables, null);
			} else {
				searchIndexArr = MSearchIndex.getForRecord(ctx, po, eventPO, indexedTables, null);
			}

			if (searchIndexArr == null)
				continue;

			for (MSearchIndex searchIndex : searchIndexArr) {
				int tableId = po.get_Table_ID();
				int recordId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();

				// Handle IsActive changes specially
				if (type.equals(IEventTopics.PO_AFTER_CHANGE) && po.is_ValueChanged("IsActive")) {
					boolean isActive = po.get_ValueAsBoolean("IsActive");
					// Fix ADR-001: Use separate transaction for index operations
					executeIndexUpdateWithSeparateTransaction((indexTrxName) -> {
						ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(ctx, searchIndex.getAD_SearchIndexProvider_ID(), null, indexTrxName);
						if (provider != null) {
							SearchIndexConfigBuilder builder = new SearchIndexConfigBuilder()
									.setCtx(ctx)
									.setTrxName(indexTrxName)
									.setAD_SearchIndex_ID(searchIndex.getAD_SearchIndex_ID())
									.setRecord(tableId, recordId);

							if (isActive) {
								// Record activated - create index
								provider.createIndex(ctx, builder.build().getData(false), indexTrxName);
							} else {
								// Record deactivated - delete index
								StringBuilder whereClause = new StringBuilder();
								whereClause.append(" AD_Client_ID=? AND AD_Table_ID=? AND Record_ID=?");
								Object[] params = new Object[] { po.getAD_Client_ID(), tableId, recordId };
								provider.deleteIndex(ctx, searchIndex.getSearchIndexName(), whereClause.toString(), params, indexTrxName);
							}
						}
					});
					continue;
				}

				// Handle changes
				if (type.equals(IEventTopics.PO_AFTER_DELETE) && po.equals(eventPO)) {
					// Fix ADR-001: Use separate transaction for index operations
					executeIndexUpdateWithSeparateTransaction((indexTrxName) -> {
						ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(ctx, searchIndex.getAD_SearchIndexProvider_ID(), null, indexTrxName);
						if (provider != null) {
							StringBuilder whereClause = new StringBuilder();
							whereClause.append(" AD_Client_ID=? AND AD_Table_ID=? AND Record_ID=?");
							Object[] params = new Object[] { po.getAD_Client_ID(), tableId, recordId };
							provider.deleteIndex(ctx, searchIndex.getSearchIndexName(), whereClause.toString(), params, indexTrxName);
						}
					});
				} else if (type.equals(IEventTopics.PO_AFTER_CHANGE)
						|| type.equals(IEventTopics.PO_AFTER_DELETE) && !po.equals(eventPO)
						|| type.equals(IEventTopics.PO_AFTER_NEW) && !po.equals(eventPO)) {
					// Fix ADR-001: Use separate transaction for index operations
					executeIndexUpdateWithSeparateTransaction((indexTrxName) -> {
						ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(ctx, searchIndex.getAD_SearchIndexProvider_ID(), null, indexTrxName);
						if (provider != null) {
							SearchIndexConfigBuilder builder = new SearchIndexConfigBuilder()
									.setCtx(ctx)
									.setTrxName(indexTrxName)
									.setAD_SearchIndex_ID(searchIndex.getAD_SearchIndex_ID())
									.setRecord(tableId, recordId);
							provider.updateIndex(ctx, builder.build().getData(false), indexTrxName);
						}
					});
				} else if (type.equals(IEventTopics.PO_AFTER_NEW) && po.equals(eventPO)) {
					// Fix ADR-001: Use separate transaction for index operations
					executeIndexUpdateWithSeparateTransaction((indexTrxName) -> {
						ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(ctx, searchIndex.getAD_SearchIndexProvider_ID(), null, indexTrxName);
						if (provider != null) {
							SearchIndexConfigBuilder builder = new SearchIndexConfigBuilder()
									.setCtx(ctx)
									.setTrxName(indexTrxName)
									.setAD_SearchIndex_ID(searchIndex.getAD_SearchIndex_ID())
									.setRecord(tableId, recordId);
							provider.createIndex(ctx, builder.build().getData(false), indexTrxName);
						}
					});
				} else {
					log.warning("Unsupportet event type");
				}
			}
		}
	}

	/**
	 * Execute index update with separate transaction (ADR-001: Transaction Isolation)
	 *
	 * This method wraps index operations in a dedicated transaction to prevent:
	 * - Index failures from rolling back business transactions
	 * - Long-running index updates from holding business transaction locks
	 * - Tight coupling between business logic and index maintenance
	 *
	 * @param operation Lambda/Consumer that performs the index operation
	 */
	private void executeIndexUpdateWithSeparateTransaction(java.util.function.Consumer<String> operation) {
		// Create separate transaction for index operations
		Trx indexTrx = Trx.get(Trx.createTrxName("SearchIdx"), true);
		try {
			String indexTrxName = indexTrx.getTrxName();

			// Perform index operation with separate transaction
			operation.accept(indexTrxName);

			// Commit the index transaction
			indexTrx.commit();
		} catch (Exception e) {
			// Rollback on error
			indexTrx.rollback();
			// Log the error but don't throw - index failure shouldn't fail business transaction
			log.severe("Failed to update search index: " + e.getMessage());
			if (log.isLoggable(java.util.logging.Level.FINE))
				log.log(java.util.logging.Level.FINE, "Search index update failed", e);
		} finally {
			// Always close the transaction
			indexTrx.close();
		}
	}

	private PO[] getMainPOs(PO po, Set<IndexedTable> tableConfigs, Properties ctx, String trxName) {
		Set<PO> mainPOSet = new HashSet<>();
		for (IndexedTable tableConfig : tableConfigs) {
			// record of an index table
			if (po.get_TableName().equals(tableConfig.getTableName())) {
				if (applyWhereClause(po, tableConfig.getWhereClause(), ctx, trxName))
					mainPOSet.add(po);
			} else { // related to an index table - find the main record
				for (String fkTableName : tableConfig.getFKTableNames()) {
					if (po.get_TableName().equals(fkTableName)) {
						PO[] mainPOsOfTable = getMainPOsOfTable(po, tableConfig.getTableName(), tableConfig.getWhereClause(), ctx, trxName);
						for (PO mainPO : mainPOsOfTable) {
							mainPOSet.add(mainPO);
						}
					}
				}
			}
		}
		return mainPOSet.toArray(new PO[0]);
	}

	private PO[] getMainPOsOfTable(PO po, String mainTableName, String whereClause, Properties ctx, String trxName) {
		
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
					if (recordId > 0)
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
	 * @param ctx
	 * @param trxName
	 * @return true if po passed the filter
	 */
	private boolean applyWhereClause(PO po, String whereClause, Properties ctx, String trxName) {

		if (Util.isEmpty(whereClause))
			return true;
		else
			whereClause = " AND " + whereClause;

		MTable table = MTable.get(ctx, po.get_TableName(), trxName);
		for (String keyCol : po.get_KeyColumns()) {
			if (table.columnExistsInDictionary(keyCol)) {
				// FIXME has problem with aliases in whereClause: ERROR: missing FROM-clause entry for table "main
				int poId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();
				if (new Query(ctx, po.get_TableName(), keyCol+"="+poId+whereClause, trxName).match())
					return true;
			}
		}
		return false;
	}
	
	private void handleSearchIndexConfigChange(PO po, Properties ctx) {

		int searchIndexId;
		// Get the Search Index
		if (po instanceof MSearchIndex) {
			searchIndexId = po.get_ValueAsInt(MSearchIndex.COLUMNNAME_AD_SearchIndex_ID);
		} else if (po instanceof MSearchIndexTable) {
			MSearchIndexTable indexTable = (MSearchIndexTable) po;
			searchIndexId = indexTable.getAD_SearchIndex_ID();
		} else if (po instanceof MSearchIndexColumn) {
			MSearchIndexColumn indexColumn = (MSearchIndexColumn) po;
			// Fix ADR-001: Use null transaction for read-only query
			MSearchIndexTable indexTable = MSearchIndexTable.get(ctx, indexColumn.getAD_SearchIndexTable_ID(), null);
			searchIndexId = indexTable.getAD_SearchIndex_ID();
		} else {
			return;
		}

		// Invalidate the Search Index (config metadata update, use PO's transaction)
		String sql = "UPDATE AD_SearchIndex SET IsValid='N' WHERE AD_SearchIndex_ID=?";
		DB.executeUpdateEx(sql, new Object[] {searchIndexId}, po.get_TrxName());
		
		// Register/unregister the modified table
		IEventManager tempManager = eventManager;
		unbindEventManager(eventManager);
		bindEventManager(tempManager);
	}

}
