package com.cloudempiere.searchindex.util;

/**
 * Interface for search results
 */
public interface ISearchResult {
	
	int getAD_Table_ID();
	
	void setAD_Table_ID(int AD_Table_ID);
	
	int getRecord_ID();
	
	void setRecord_ID(int Record_ID);
	
	String getHtmlHeadline();
	
	void setHtmlHeadline(String htmlHeadline);
	
	String getLabel();
}
