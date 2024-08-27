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
package de.bxservice.omnisearch.validator;

import java.util.List;
import java.util.Set;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.util.CLogger;
import org.osgi.service.event.Event;

import com.cloudempiere.omnisearch.model.MSearchIndexColumn;

import de.bxservice.omnisearch.tools.OmnisearchHelper;

public class TSearchIndexEventHandler extends AbstractEventHandler {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(TSearchIndexEventHandler.class);
	
	private String       trxName      = null;
	private Set<String>  fkTableNames = null;

	@Override
	protected void initialize() {
		log.warning("");

		List<String> indexedTables = OmnisearchHelper.getIndexedTableNames(trxName, -1); // CLDE this gets data from all clients

		for (String tableName : indexedTables) {
			registerTableEvent(IEventTopics.PO_AFTER_NEW, tableName);
			registerTableEvent(IEventTopics.PO_AFTER_CHANGE, tableName);
			registerTableEvent(IEventTopics.PO_AFTER_DELETE, tableName);
		}

		fkTableNames = OmnisearchHelper.getForeignTableNames(trxName, -1); // CLDE - this gets data from all clients
		//Index the FK tables
		for (String tableName : fkTableNames) {
			//Don't duplicate the Event for the same table
			if (!indexedTables.contains(tableName))
				registerTableEvent(IEventTopics.PO_AFTER_CHANGE, tableName);
		}

		// CLDE -Handle the changes in the Omnisearch Config to update the index
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MSearchIndexColumn.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MSearchIndexColumn.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_DELETE, MSearchIndexColumn.Table_Name);
	}

	@Override
	protected void doHandleEvent(Event event) {
// TODO implement the event handler for AD_IndexColumn 
		
//		String type = event.getTopic();
//		PO po = getPO(event);
//		trxName = po.get_TrxName();
//		
//		// CLDE
//		if (po instanceof MSearchIndexColumn) {
//			if (type.equals(IEventTopics.PO_AFTER_NEW) ||
//					type.equals(IEventTopics.PO_AFTER_DELETE) ||
//					(type.equals(IEventTopics.PO_AFTER_CHANGE) &&
//					po.is_ValueChanged(MSearchIndexColumn.COLUMNNAME_AD_Column_ID) || 
//					po.is_ValueChanged(MSearchIndexColumn.COLUMNNAME_AD_Table_ID))) {
//				//If the Omnisearch configuration has changed -> register/unregister the modified table
//				IEventManager tempManager = eventManager;
//				unbindEventManager(eventManager);
//				bindEventManager(tempManager);
//			}
//		} else if (type.equals(IEventTopics.PO_AFTER_DELETE))
//			OmnisearchHelper.deleteFromDocument(OmnisearchAbstractFactory.TEXTSEARCH_INDEX, po);
//		else 
//			OmnisearchHelper.updateDocument(OmnisearchAbstractFactory.TEXTSEARCH_INDEX, po, 
//					type.equals(IEventTopics.PO_AFTER_NEW));
//		
//		if (fkTableNames.contains(po.get_TableName())) {
//			OmnisearchHelper.updateParent(OmnisearchAbstractFactory.TEXTSEARCH_INDEX, po);
//		}
	}

}
