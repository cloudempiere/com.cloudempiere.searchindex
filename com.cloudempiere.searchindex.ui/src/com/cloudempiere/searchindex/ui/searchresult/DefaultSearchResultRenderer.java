package com.cloudempiere.searchindex.ui.searchresult;

import java.util.List;

import org.zkoss.zul.Listbox;

import com.cloudempiere.searchindex.ui.SearchIndexItemRenderer;
import com.cloudempiere.searchindex.util.ISearchResult;

public class DefaultSearchResultRenderer implements ISearchResultRenderer {

	@Override
	public void renderResults(List<ISearchResult> results, Listbox listbox) {
		listbox.setItemRenderer(new SearchIndexItemRenderer());
	}
}
