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
package com.cloudempiere.omnisearch.ui.dashboard;

import java.util.Properties;

import org.adempiere.webui.dashboard.DashboardPanel;
import org.compiere.util.Env;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import com.cloudempiere.omnisearch.indexprovider.ISearchIndexProvider;
import com.cloudempiere.omnisearch.ui.ZkOmnisearchUI;
import com.cloudempiere.omnisearch.ui.searchresult.DefaultSearchResultRenderer;
import com.cloudempiere.omnisearch.util.SearchIndexUtils;

public class DPOmnisearchPanel extends DashboardPanel implements EventListener<Event> {

	private static final long serialVersionUID = -8116512057982561129L;

	private Properties ctx;
	private ISearchIndexProvider searchIndexProvider;
	private ZkOmnisearchUI omnisearchUI;

	public DPOmnisearchPanel() {
		super();
		ctx = Env.getCtx();
		searchIndexProvider = SearchIndexUtils.getSearchIndexProvider(ctx, 1000004, null); // FIXME hardcoded PGTextSearchIndexProvider - need to get from the search index definition

		omnisearchUI = new ZkOmnisearchUI(ctx, searchIndexProvider, new DefaultSearchResultRenderer());
		omnisearchUI.setParent(this);
	}

	@Override
	public void onEvent(Event e) throws Exception {
		omnisearchUI.onEvent(e);
	}
}
