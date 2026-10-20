package com.cloudempiere.searchindex.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MTable;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.cloudempiere.searchindex.model.MSearchIndex;
import com.cloudempiere.searchindex.model.MSearchIndexColumn;
import com.cloudempiere.searchindex.model.MSearchIndexProvider;
import com.cloudempiere.searchindex.model.MSearchIndexTable;
import com.cloudempiere.searchindex.util.pojo.SearchIndexColumnData;
import com.cloudempiere.searchindex.util.pojo.SearchIndexTableData;

/**
 * Integration tests for search index columns with a Table / Search reference.
 *
 * Since IDEMPIERE-6163 MLookupInfo.lookupDisplayColumns (and lookupDisplayColumnNames for a
 * reference that displays a non-key column) holds SQL such as NVL(M_Warehouse.Name,'-1').
 * The builder used it as a column name and the data query failed with a syntax error,
 * so idx_product_ts and idx_bpartner_ts were no longer updated.
 * Configurations use the production shape: a child table FK column whose reference points back
 * at the child table, e.g. M_Product_PO.M_Product_ID with "M_Product_PO VendorProductNo".
 */
@Tag("integration")
public class SearchIndexConfigBuilderLookupTest extends AbstractTestCase {

	/** M_Warehouse, key M_Warehouse_ID, display Name (not translated) */
	private static final int REFERENCE_WAREHOUSE_NAME = 200194;
	/** AD_User, display AD_User_ID - shows the identifiers */
	private static final int REFERENCE_AD_USER = 110;
	/** AD_Table, display Name (translated) */
	private static final int REFERENCE_AD_TABLE_NAME = 200232;

	@Test
	public void testReferenceWithDisplayColumnBuildsValidQuery() {
		buildAndAssertColumn("AD_Org", "M_Warehouse", "AD_Org_ID", REFERENCE_WAREHOUSE_NAME, "M_Warehouse.Name");
	}

	@Test
	public void testReferenceShowingIdentifiersBuildsValidQuery() {
		buildAndAssertColumn("C_BPartner", "AD_User", "C_BPartner_ID", REFERENCE_AD_USER, "AD_User.Name");
	}

	@Test
	public void testTranslatedDisplayColumnResolvesToBaseColumn() throws Exception {
		MColumn column = MColumn.get(Env.getCtx(), "AD_Column", "AD_Table_ID");
		MLookup lookup = MLookupFactory.get(Env.getCtx(), 0, column.getAD_Column_ID(), DisplayType.Search,
				Language.getLanguage("sk_SK"), column.getColumnName(), REFERENCE_AD_TABLE_NAME, false, null);
		MLookupInfo lookupInfo = lookup.getLookupInfo();

		List<String> columnNames = new SearchIndexConfigBuilder().setCtx(Env.getCtx())
				.getLookupDisplayColumnNames(lookupInfo, REFERENCE_AD_TABLE_NAME);

		assertThat(columnNames).containsExactly("Name");
	}

	/**
	 * Create a search index on mainTable with childTable.childColumn as a Search reference column,
	 * build it and assert the data query returns a value for the expected display column key.
	 */
	private void buildAndAssertColumn(String mainTable, String childTable, String childColumn,
			int referenceValueId, String expectedKey) {
		MSearchIndexProvider provider = new MSearchIndexProvider(Env.getCtx(), 0, getTrxName());
		provider.setName("Test Provider");
		provider.setClassname("com.cloudempiere.searchindex.pgtextsearch.PGTextSearchIndexProvider");
		provider.saveEx();

		MSearchIndex index = new MSearchIndex(Env.getCtx(), 0, getTrxName());
		index.setName("Test " + childTable);
		index.setSearchIndexName("idx_test_ts");
		index.setAD_SearchIndexProvider_ID(provider.getAD_SearchIndexProvider_ID());
		index.saveEx();

		MTable main = MTable.get(Env.getCtx(), mainTable);
		MSearchIndexTable indexTable = new MSearchIndexTable(Env.getCtx(), 0, getTrxName());
		indexTable.setAD_SearchIndex_ID(index.getAD_SearchIndex_ID());
		indexTable.setAD_Table_ID(main.getAD_Table_ID());
		indexTable.setName(mainTable);
		indexTable.saveEx();

		MColumn column = MColumn.get(Env.getCtx(), childTable, childColumn);
		MSearchIndexColumn indexColumn = new MSearchIndexColumn(Env.getCtx(), 0, getTrxName());
		indexColumn.setAD_SearchIndexTable_ID(indexTable.getAD_SearchIndexTable_ID());
		indexColumn.setAD_Table_ID(column.getAD_Table_ID());
		indexColumn.setAD_Column_ID(column.getAD_Column_ID());
		indexColumn.setParent_Column_ID(MColumn.get(Env.getCtx(), mainTable, main.getKeyColumns()[0]).getAD_Column_ID());
		indexColumn.setAD_Reference_ID(DisplayType.Search);
		indexColumn.setAD_Reference_Value_ID(referenceValueId);
		indexColumn.setSearchWeight(BigDecimal.ONE);
		indexColumn.saveEx();

		Map<Integer, Set<SearchIndexTableData>> data = new SearchIndexConfigBuilder()
				.setCtx(Env.getCtx())
				.setTrxName(getTrxName())
				.setAD_SearchIndex_ID(index.getAD_SearchIndex_ID())
				.build()
				.getData(false);

		Set<SearchIndexTableData> tableData = data.get(index.getAD_SearchIndex_ID());
		assertThat(tableData).as("data of %s", mainTable).hasSize(1);
		Set<Map<String, SearchIndexColumnData>> rows = tableData.iterator().next().getColumnData();
		assertThat(rows).isNotEmpty();
		assertThat(rows.iterator().next().keySet()).contains(expectedKey)
				.allSatisfy(key -> assertThat(key).doesNotContain("("));
		assertThat(rows).anySatisfy(row -> assertThat(row.get(expectedKey).getValue()).isNotNull());
	}
}
