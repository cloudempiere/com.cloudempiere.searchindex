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
import java.util.Properties;
import java.util.Set;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.model.GenericPO;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;

import com.cloudempiere.searchindex.event.pojo.SearchIndexTableConfigSimple;
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
	private Set<SearchIndexTableConfigSimple> indexedTables = null;
	
	@Override
	protected void initialize() {

		indexedTables = SearchIndexUtils.getIndexedTableNames(trxName, -1); // gets data from all clients
		Set<String> tablesToRegister = new HashSet<>();

		for (SearchIndexTableConfigSimple indexTable : indexedTables) {
			String tableName = indexTable.getTableName();
			tablesToRegister.add(tableName);
			//Index the FK tables
			for (String fkTableName : indexTable.getFKTableNames()) {
				tablesToRegister.add(fkTableName);
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
		PO[] mainPOArr = getMainPOs(eventPO, indexedTables);
		MSearchIndex[] searchIndexArr;
		
		for (PO po : mainPOArr) {
			// Find existing search indexes for the record
			if (type.equals(IEventTopics.PO_AFTER_NEW)) {
				searchIndexArr = MSearchIndex.getForTable(ctx, po, indexedTables, trxName);
			} else {
				searchIndexArr = MSearchIndex.getForRecord(ctx, po, indexedTables, trxName);
			}
			
			for (MSearchIndex searchIndex : searchIndexArr) {
				ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(ctx, searchIndex.getAD_SearchIndexProvider_ID(), null, trxName);
				int tableId = po.get_Table_ID();
				int recordId = po.get_ID() > 0 ? po.get_ID() : po.get_IDOld();
				
				// Initialise builder
				SearchIndexConfigBuilder builder = new SearchIndexConfigBuilder()
						.setCtx(ctx)
						.setTrxName(trxName)
						.setSearchIndexId(searchIndex.getAD_SearchIndex_ID())
						.setRecord(tableId, recordId);
				
				// TODO handle changes in search index configuration: invalidate the configuration
				if (po instanceof MSearchIndexColumn) {
					if (type.equals(IEventTopics.PO_AFTER_NEW) ||
							type.equals(IEventTopics.PO_AFTER_DELETE) ||
							(type.equals(IEventTopics.PO_AFTER_CHANGE) &&
							po.is_ValueChanged(MSearchIndexColumn.COLUMNNAME_AD_Column_ID) || 
							po.is_ValueChanged(MSearchIndexColumn.COLUMNNAME_AD_Table_ID))) {
						
						// If the Search Index configuration has changed -> register/unregister the modified table
						IEventManager tempManager = eventManager;
						unbindEventManager(eventManager);
						bindEventManager(tempManager);
					}
				} else if (type.equals(IEventTopics.PO_AFTER_DELETE)) {
					if (provider != null) {
						StringBuilder whereClause = new StringBuilder();
						whereClause.append(" AD_Client_ID=? AND AD_Table_ID=? AND Record_ID=?");
						Object[] params = new Object[] { po.getAD_Client_ID(), tableId, recordId };
						provider.deleteIndexByQuery(ctx, null, whereClause.toString(), params, trxName);
					}
				} else if (type.equals(IEventTopics.PO_AFTER_CHANGE)) {
					if (provider != null) {
						provider.updateIndex(ctx, builder.build().getData(false), trxName);
					}
				} else if (type.equals(IEventTopics.PO_AFTER_NEW)) {
					if (provider != null) {
						provider.createIndex(ctx, builder.build().getData(false), trxName);
					}
				} else {
					log.warning("Unsupportet event type");
				}
			}
		}
	}
	
	private PO[] getMainPOs(PO po, Set<SearchIndexTableConfigSimple> tableConfigs) {
		Set<PO> mainPOSet = new HashSet<>();
		for (SearchIndexTableConfigSimple tableConfig : tableConfigs) {
			// record of an index table
			if (po.get_TableName().equals(tableConfig.getTableName())) {
				mainPOSet.add(po);
			} else { // related to an index table - find the main record
				for (String fkTableName : tableConfig.getFKTableNames()) {
					if (po.get_TableName().equals(fkTableName)) {
						PO[] mainPOsOfTable = getMainPOsOfTable(po, tableConfig.getTableName());
						for (PO mainPO : mainPOsOfTable) {
							mainPOSet.add(mainPO);
						}
					}
				}
			}
		}
		return mainPOSet.toArray(new PO[0]);
	}
	
	private PO[] getMainPOsOfTable(PO po, String mainTableName) {
		Set<PO> mainPOSet = new HashSet<>();
		// one to many
		MTable fkTable = MTable.get(ctx, mainTableName, mainTableName);
		for (String keyCol : po.get_KeyColumns()) {
			if (fkTable.columnExistsInDictionary(keyCol)) {
				for (int recordId : PO.getAllIDs(mainTableName, keyCol+"="+po.get_ID(), trxName)) {
					mainPOSet.add(new GenericPO(mainTableName, ctx, recordId, trxName));
				}
			}
		}
		// one to one
		if (mainPOSet.isEmpty()) {
			for (String keyCol : fkTable.getKeyColumns()) {
				if (po.columnExists(keyCol)) {
//					for (int recordId : PO.getAllIDs(mainTableName, keyCol+"="+po.get_ValueAsInt(keyCol), trxName)) {
					int recordId = po.get_ValueAsInt(keyCol);
					mainPOSet.add(new GenericPO(mainTableName, ctx, recordId, trxName));
				}
			}
		}
		return mainPOSet.toArray(new PO[0]);
	}

}
