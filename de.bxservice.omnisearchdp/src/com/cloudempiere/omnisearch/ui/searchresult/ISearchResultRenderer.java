package com.cloudempiere.omnisearch.ui.searchresult;

import java.util.List;

import org.zkoss.zul.Listbox;

import com.cloudempiere.omnisearch.util.ISearchResult;

public interface ISearchResultRenderer {
	void renderResults(List<ISearchResult> results, Listbox listbox);
}
