package com.cloudempiere.searchindex.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.cloudempiere.searchindex.util.pojo.SearchIndexColumnConfig;
import com.cloudempiere.searchindex.util.pojo.SearchIndexTableConfig;

/**
 * Unit tests for the data query SearchIndexConfigBuilder generates.
 *
 * The configuration mirrors idx_product_ts: M_Product with M_Product_PO.M_Product_ID as a
 * Search reference column (AD_Reference_Value_ID "M_Product_PO VendorProductNo").
 * Since IDEMPIERE-6163 the lookup returns its display column as NVL(M_Product_PO.VendorProductNo,'-1');
 * used as a column name it produced "M_Product_PO.NVL(...) as M_Product_PO_NVL(...)" and the whole
 * index update failed with a syntax error.
 */
@Tag("unit")
public class SearchIndexConfigBuilderSqlTest {

	private static final Pattern ALIAS = Pattern.compile(" as (\\S+?)(?:,|\\s|$)");
	private static final int M_PRODUCT_TABLE_ID = 208;
	private static final int M_PRODUCT_PO_TABLE_ID = 210;

	private SearchIndexTableConfig productConfig(String lookupDisplayColumn) {
		SearchIndexTableConfig tableConfig = new SearchIndexTableConfig("M_Product", M_PRODUCT_TABLE_ID, "M_Product_ID", null, BigDecimal.TEN);
		tableConfig.addColumn(new SearchIndexColumnConfig(M_PRODUCT_TABLE_ID, "M_Product", 1, "Name", -1, null, null, BigDecimal.TEN));
		tableConfig.addColumn(new SearchIndexColumnConfig(M_PRODUCT_PO_TABLE_ID, "M_Product_PO", -1, "M_Product_ID", 2, "M_Product_ID", null, BigDecimal.ONE));
		tableConfig.addColumn(new SearchIndexColumnConfig(-1, "M_Product_PO", -1, lookupDisplayColumn, -1, "M_Product_ID", "M_Product_PO", BigDecimal.ONE));
		return tableConfig;
	}

	@Test
	public void testReferenceColumnSelectsPlainColumn() {
		String sql = SearchIndexConfigBuilder.getSelectFromClause(productConfig("VendorProductNo"));

		assertThat(sql).isEqualTo("SELECT M_Product.M_Product_ID as Record_ID, "
				+ "M_Product.Name as M_Product_Name, "
				+ "M_Product_PO.M_Product_ID as M_Product_PO_M_Product_ID, "
				+ "M_Product_PO.VendorProductNo as M_Product_PO_VendorProductNo "
				+ " FROM M_Product LEFT JOIN M_Product_PO ON M_Product.M_Product_ID = M_Product_PO.M_Product_ID");
		assertAliasesAreIdentifiers(sql);
	}

	@Test
	public void testExpressionColumnNameIsSkipped() {
		String sql = SearchIndexConfigBuilder.getSelectFromClause(productConfig("NVL(M_Product_PO.VendorProductNo,'-1')"));

		assertThat(sql).doesNotContain("NVL(");
		assertThat(sql).contains("M_Product_PO.M_Product_ID as M_Product_PO_M_Product_ID");
		assertAliasesAreIdentifiers(sql);
	}

	@Test
	public void testIsSqlIdentifier() {
		assertThat(SearchIndexConfigBuilder.isSqlIdentifier("VendorProductNo")).isTrue();
		assertThat(SearchIndexConfigBuilder.isSqlIdentifier("C_BPartner_ID")).isTrue();
		assertThat(SearchIndexConfigBuilder.isSqlIdentifier("NVL(M_Product_PO.VendorProductNo,'-1')")).isFalse();
		assertThat(SearchIndexConfigBuilder.isSqlIdentifier("AD_User.Name")).isFalse();
		assertThat(SearchIndexConfigBuilder.isSqlIdentifier("")).isFalse();
		assertThat(SearchIndexConfigBuilder.isSqlIdentifier(null)).isFalse();
	}

	private static void assertAliasesAreIdentifiers(String sql) {
		Matcher matcher = ALIAS.matcher(sql);
		int count = 0;
		while (matcher.find()) {
			assertThat(SearchIndexConfigBuilder.isSqlIdentifier(matcher.group(1))).as("alias %s in %s", matcher.group(1), sql).isTrue();
			count++;
		}
		assertThat(count).isPositive();
	}
}
