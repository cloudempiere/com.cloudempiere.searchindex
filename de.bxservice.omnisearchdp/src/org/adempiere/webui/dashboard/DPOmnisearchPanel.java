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
package org.adempiere.webui.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.webui.apps.graph.WNoData;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Combobox;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.event.PagingEvent;

import com.cloudempiere.omnisearch.indexprovider.ISearchIndexProvider;
import com.cloudempiere.omnisearch.util.ISearchResult;
import com.cloudempiere.omnisearch.util.SearchIndexUtils;


public class DPOmnisearchPanel extends DashboardPanel implements EventListener<Event> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8116512057982561129L;

	private Properties ctx;
	private ISearchIndexProvider searchIndexProvider;
	private List<ISearchResult> results;
	private Map<String, String> transactionCodeMap;
	
	private OmnisearchItemRenderer renderer;
	private Vlayout layout = new Vlayout();
	private Div div = new Div();
	private Combobox searchCombobox = new Combobox();
	private Checkbox cbAdvancedSearch = new Checkbox();
	private Listbox resultListbox = null;
	private WNoData noRecordsWidget = new WNoData("FindZeroRecords", "WidgetNoData.png");
	private WNoData noIndexWidget = new WNoData("BXS_NoIndex", "WidgetError.png");
		
	public DPOmnisearchPanel()
	{
		super();
		ctx = Env.getCtx();
		searchIndexProvider = SearchIndexUtils.getSearchIndexProvider(ctx, 1000004, null); // FIXME hardcoded PGTextSearchIndexProvider - need to get from the search index definition
		transactionCodeMap = SearchIndexUtils.getTransactionCodesByClient(ctx, Env.getAD_Client_ID(ctx), 1000004, null); // FIXME hardcoded PGTextSearchIndexProvider - need to get from the search index definition
		
		this.setSclass("dashboard-widget-max");
		this.setHeight("500px");
		
		initLayout();
		initComponent();

	}
	
	void setModel(List<ISearchResult> data) {
		resultListbox.setModel(new ListModelArray<>(data));
	}

	private void initComponent() {
		// Add predefined options to the Combobox
		Comboitem itemO = new Comboitem("/order");
		Comboitem itemS = new Comboitem("/crm");
		searchCombobox.appendChild(itemO);
		searchCombobox.appendChild(itemS);
		
		searchCombobox.addEventListener(Events.ON_OK, this);
		searchCombobox.setHflex("1");
		searchCombobox.setSclass("z-combobox");
		searchCombobox.setAutodrop(true);
		searchCombobox.setButtonVisible(false);
		
		// Add input event listener for filtering
		searchCombobox.addEventListener(Events.ON_CHANGING, event -> {
			InputEvent inputEvent = (InputEvent) event;
			String value = inputEvent.getValue();
			filterComboboxItems(value);
		});
		
		cbAdvancedSearch.setLabel(Msg.getMsg(ctx, "BXS_AdvancedQuery"));
		
		resultListbox = new Listbox();
		resultListbox.setMold("paging");
		resultListbox.setPageSize(10);
		resultListbox.setVflex("1");
		resultListbox.setHflex("1");
		resultListbox.addEventListener("onPaging", this);
	
		if (!searchIndexProvider.isIndexPopulated("IDX_SalesOrder")) { // FIXME hardcoded
			showResults(false, ErrorLabel.NO_INDEX);
		} else {
			showResults(true, null);
		}
	
		Vbox box = new Vbox();
		box.setVflex("1");
		box.setHflex("1");
		box.appendChild(searchCombobox);
		box.appendChild(cbAdvancedSearch);
		box.appendChild(resultListbox);
		box.appendChild(noRecordsWidget);
		box.appendChild(noIndexWidget);
		div.appendChild(box);
	
		//  ActionListener
		cbAdvancedSearch.setChecked(false);
	}

	private void initLayout() {
		layout.setParent(this);
		layout.setSclass("omnisearchpanel-layout");
		layout.setSpacing("0px");
		layout.setStyle("height: 100%; width: 100%;");
		
		div.setParent(layout);
		div.setVflex("1");
		div.setHflex("1");
		div.setStyle("overflow: auto;");
	}
	
	public void showResults(boolean show, ErrorLabel error) {
		if (resultListbox != null)
			resultListbox.setVisible(show);
		if (noRecordsWidget != null)
			noRecordsWidget.setVisible(!show && error == ErrorLabel.NO_RESULTS);
		if (noIndexWidget != null)
			noIndexWidget.setVisible(!show && error == ErrorLabel.NO_INDEX);
	}

	@Override
	public void onEvent(Event e) throws Exception {
		if (Events.ON_OK.equals(e.getName()) && e.getTarget() instanceof Combobox) {
			Combobox combobox = (Combobox) e.getTarget();
			String searchText = combobox.getValue();
			String searchIndexName = "";
			
			if (resultListbox.getItems() != null) {
				setModel(new ArrayList<ISearchResult>());
			}
			
			if(searchText.startsWith("/")) {
				String searchIndexKey = searchText.substring(1, searchText.indexOf(" "));
				searchIndexName = transactionCodeMap.get(searchIndexKey);
				searchText = searchText.substring(searchText.indexOf(" ") + 1);
			}
			
			results = searchIndexProvider.searchIndexDocument(searchIndexName, searchText, cbAdvancedSearch.isChecked()); // FIXME hardcoded

			if (results != null && results.size() > 0) {
				
				showResults(true, null);
				setModel(results);
				renderer = new OmnisearchItemRenderer();
				resultListbox.setItemRenderer(renderer);
		
			} else {
				showResults(false, ErrorLabel.NO_RESULTS);
			}
		} else if ("onPaging".equals(e.getName()) && (e.getTarget() instanceof Listbox)) {
			PagingEvent ee = (PagingEvent) e;
			int pgno = ee.getActivePage();
			
			if (pgno != 0 && results != null) {
				int start = pgno * 10;
				int end = (pgno*10) + 10;
				
				if (end > results.size()) 
					end = results.size();
				
				for(int i = start; i < end; i++)
					searchIndexProvider.setHeadline(results.get(i), searchCombobox.getValue());
				
				setModel(results);
			}	
		}
	}

	private void filterComboboxItems(String value) {
		if (value == null || value.isEmpty()) {
			searchCombobox.open();
			return;
		}
		
		for (Comboitem item : searchCombobox.getItems()) {
			if (item.getLabel().toLowerCase().contains(value.toLowerCase())) {
				item.setVisible(true);
			} else {
				item.setVisible(false);
			}
		}
		searchCombobox.open();
	}

	private enum ErrorLabel {
		NO_RESULTS, NO_INDEX
	}

}
