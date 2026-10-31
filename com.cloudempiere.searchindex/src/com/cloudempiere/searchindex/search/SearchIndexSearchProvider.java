/******************************************************************************
 * Copyright (C) 2026 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 *****************************************************************************/
package com.cloudempiere.searchindex.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.search.ISearchProvider;
import org.adempiere.base.search.SearchResult;
import org.compiere.model.MSearchDefinition;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;

import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider;
import com.cloudempiere.searchindex.indexprovider.ISearchIndexProvider.SearchType;
import com.cloudempiere.searchindex.model.MSearchIndex;
import com.cloudempiere.searchindex.util.ISearchResult;
import com.cloudempiere.searchindex.util.SearchIndexUtils;

/**
 * Answers the global document search (header search box) from a full-text search index.
 * <p>
 * Mechanism: upstream {@link ISearchProvider} (IDEMPIERE-6810) — the document search asks every registered provider
 * whether it {@link #accept(MSearchDefinition) accepts} a search definition. This provider accepts definitions of
 * search type {@value #SEARCHTYPE_SearchIndex} (Search Index, entity MM02); upstream's {@code DefaultSQLSearchProvider}
 * accepts only {@code Q} and {@code T}, so existing definitions are not affected.<br>
 * The definition's transaction code selects the tenant's {@link MSearchIndex} (case-insensitive: the document search
 * upper-cases the code, indexes are often lower case). Hits keep the index's rank as relevance score, so upstream
 * sorts them; an {@code AD_Message} / {@code AD_Style} on the definition formats them like any card search
 * (values: {@code {0}} record ID, {@code {1}} label, {@code {2}} HTML headline).
 * <p>
 * Scope ui-global-search, decision 3 option A (iDempiereCLDE {@code docs/divergence/scopes/ui-global-search.md}).
 */
@Component(service = ISearchProvider.class, property = { "service.ranking:Integer=100" })
public class SearchIndexSearchProvider implements ISearchProvider {

	/** AD_SearchDefinition.SearchType value of a search-index definition (reference 53291, entity MM02) */
	public static final String SEARCHTYPE_SearchIndex = "I";
	/** Value-map key of the HTML headline (matched words highlighted) */
	public static final String VALUE_HEADLINE = "Headline";

	private static final CLogger log = CLogger.getCLogger(SearchIndexSearchProvider.class);

	@Override
	public boolean accept(MSearchDefinition def) {
		return def != null && SEARCHTYPE_SearchIndex.equals(def.getSearchType());
	}

	@Override
	public List<SearchResult> search(MSearchDefinition def, String query, int pageSize, int pageNo) {
		List<SearchResult> list = new ArrayList<>();
		if (Util.isEmpty(query, true) || pageSize <= 0)
			return list;
		Properties ctx = Env.getCtx();
		MSearchIndex index = getIndex(ctx, def.getTransactionCode());
		if (index == null)
			return list;
		try {
			ISearchIndexProvider provider = SearchIndexUtils.getSearchIndexProvider(ctx, index.getAD_SearchIndexProvider_ID(), null, null);
			if (provider == null)
				return list;
			List<ISearchResult> hits = provider.getSearchResults(ctx, index.getSearchIndexName(), query, false, SearchType.TS_RANK, null);
			int from = Math.max(pageNo, 0) * pageSize;
			for (int i = from; hits != null && i < hits.size() && list.size() < pageSize; i++)
				list.add(toSearchResult(def, hits.get(i)));
		} catch (Exception e) {
			log.log(Level.WARNING, "Search index " + index.getSearchIndexName() + ": " + e.getLocalizedMessage(), e);
			SearchResult error = new SearchResult();
			error.setRecordId(-1);
			error.setLabel(Msg.getMsg(ctx, "DBExecuteError"));
			list.add(error);
		}
		return list;
	}

	/**
	 * @param ctx context
	 * @param transactionCode code of the search definition
	 * @return active index of the login tenant (or System) with that code, case-insensitive; null if none
	 */
	static MSearchIndex getIndex(Properties ctx, String transactionCode) {
		if (Util.isEmpty(transactionCode, true))
			return null;
		return new Query(ctx, MSearchIndex.Table_Name,
				"UPPER(" + MSearchIndex.COLUMNNAME_TransactionCode + ")=UPPER(?) AND " + MSearchIndex.COLUMNNAME_AD_Client_ID + " IN (?,0)", null)
				.setParameters(transactionCode.trim(), Env.getAD_Client_ID(ctx))
				.setOnlyActiveRecords(true)
				.setOrderBy(MSearchIndex.COLUMNNAME_AD_Client_ID + " DESC")
				.first();
	}

	/**
	 * Map an index hit to upstream's result, the way {@code DefaultSQLSearchProvider} fills it.
	 * @param def search definition
	 * @param hit index hit
	 * @return search result
	 */
	static SearchResult toSearchResult(MSearchDefinition def, ISearchResult hit) {
		SearchResult result = new SearchResult();
		MTable table = MTable.get(Env.getCtx(), hit.getAD_Table_ID());
		int windowId = Env.getZoomWindowID(hit.getAD_Table_ID(), hit.getRecord_ID());
		result.setRecordId(hit.getRecord_ID());
		result.setTableName(table != null ? table.getTableName() : null);
		result.setWindowId(windowId);
		if (windowId > 0)
			result.setWindowName(MWindow.get(Env.getCtx(), windowId).get_Translation(MWindow.COLUMNNAME_Name));
		result.setLabel(hit.getLabel());
		result.setRelevanceScore(hit.getRank());

		Map<String, Object> valueMap = new HashMap<>();
		if (table != null)
			valueMap.put(table.getTableName() + "_ID", hit.getRecord_ID());
		valueMap.put(VALUE_HEADLINE, hit.getHtmlHeadline() != null ? hit.getHtmlHeadline() : "");
		result.setValueMap(valueMap);
		result.setValues(new Object[] { hit.getRecord_ID(), result.getLabel(), valueMap.get(VALUE_HEADLINE) });

		if (def.getAD_Message_ID() > 0) {
			result.setAD_Message_ID(def.getAD_Message_ID());
			if (def.getAD_Style_ID() > 0)
				result.setAD_Style_ID(def.getAD_Style_ID());
		}
		return result;
	}
}
