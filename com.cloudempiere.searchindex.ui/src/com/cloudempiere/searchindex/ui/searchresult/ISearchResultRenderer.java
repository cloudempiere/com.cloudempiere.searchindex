package com.cloudempiere.searchindex.ui.searchresult;

import java.util.List;

import org.zkoss.zul.Listbox;

import com.cloudempiere.searchindex.util.ISearchResult;

public interface ISearchResultRenderer {
	void renderResults(List<ISearchResult> results, Listbox listbox);
}
