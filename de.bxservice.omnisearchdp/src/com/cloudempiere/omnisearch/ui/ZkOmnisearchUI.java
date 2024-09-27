package com.cloudempiere.omnisearch.ui;

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
import com.cloudempiere.omnisearch.ui.searchresult.ISearchResultRenderer;
import com.cloudempiere.omnisearch.util.ISearchResult;
import com.cloudempiere.omnisearch.util.SearchIndexUtils;


public class ZkOmnisearchUI extends Div implements EventListener<Event> {

	/* Generated serial version ID */
	private static final long serialVersionUID = 565544798736411334L;
	
	private Properties ctx;
	private ISearchIndexProvider searchIndexProvider;
	private List<ISearchResult> results;
	private ISearchResultRenderer resultRenderer;
	private Combobox searchCombobox = new Combobox();
	private Checkbox cbAdvancedSearch = new Checkbox();
	private Listbox resultListbox = new Listbox();
	private Div div = new Div();
	private Vlayout layout = new Vlayout();
	private WNoData noRecordsWidget = new WNoData("FindZeroRecords", "WidgetNoData.png");
	private WNoData noIndexWidget = new WNoData("BXS_NoIndex", "WidgetError.png");
	private Map<String, String> transactionCodeMap;

	public ZkOmnisearchUI(Properties ctx, ISearchIndexProvider searchIndexProvider, ISearchResultRenderer resultRenderer) {
		this.ctx = ctx;
		this.searchIndexProvider = searchIndexProvider;
		this.resultRenderer = resultRenderer;
		this.transactionCodeMap = SearchIndexUtils.getTransactionCodesByClient(ctx, Env.getAD_Client_ID(ctx), 1000004, null); // FIXME hardcoded PGTextSearchIndexProvider - need to get from the search index definition
		
		
		this.setSclass("dashboard-widget-max");
		this.setHeight("500px");
		
		initComponent();
		initLayout();
	}

	private void initComponent() {
		// TODO fix hardcoded options (load from search index)
		Comboitem itemO = new Comboitem("/order");
		Comboitem itemP = new Comboitem("/product");
		Comboitem itemS = new Comboitem("/crm");
		searchCombobox.appendChild(itemO);
		searchCombobox.appendChild(itemP);
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

		// ActionListener
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
//		div.setHeight("100%");
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

			if (searchText.startsWith("/")) {
				String searchIndexKey = searchText.substring(1, searchText.indexOf(" ")); // FIXME error for empty string: begin 1, end -1, length 6
				searchIndexName = transactionCodeMap.get(searchIndexKey);
				searchText = searchText.substring(searchText.indexOf(" ") + 1);
			}

			results = searchIndexProvider.searchIndexDocument(searchIndexName, searchText, cbAdvancedSearch.isChecked()); // FIXME hardcoded

			if (results != null && results.size() > 0) {
				showResults(true, null);
				setModel(results);
				resultRenderer.renderResults(results, resultListbox);
			} else {
				showResults(false, ErrorLabel.NO_RESULTS);
			}
		} else if ("onPaging".equals(e.getName()) && (e.getTarget() instanceof Listbox)) {
			PagingEvent ee = (PagingEvent) e;
			int pgno = ee.getActivePage();

			if (pgno != 0 && results != null) {
				int start = pgno * 10;
				int end = (pgno * 10) + 10;

				if (end > results.size())
					end = results.size();

				for (int i = start; i < end; i++)
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

	private void setModel(List<ISearchResult> data) {
		resultListbox.setModel(new ListModelArray<>(data));
	}

	private enum ErrorLabel {
		NO_RESULTS, NO_INDEX
	}
}
