package com.cloudempiere.omnisearch.ui.searchresult;

import java.util.List;

import org.zkoss.zul.Listbox;

import com.cloudempiere.omnisearch.ui.OmnisearchItemRenderer;
import com.cloudempiere.omnisearch.util.ISearchResult;

public class DefaultSearchResultRenderer implements ISearchResultRenderer {

	@Override
	public void renderResults(List<ISearchResult> results, Listbox listbox) {
		listbox.setItemRenderer(new OmnisearchItemRenderer());
	}
}
