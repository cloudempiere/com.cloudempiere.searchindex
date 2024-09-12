package com.cloudempiere.omnisearch.indexprovider;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.cloudempiere.omnisearch.model.MSearchIndexProvider;
import com.cloudempiere.omnisearch.util.SearchIndexRecord;

import de.bxservice.omnisearch.tools.TextSearchResult;

public class PGTextSearchIndexProvider implements ISearchIndexProvider {

    @Override
    public void init(MSearchIndexProvider searchIndexProvider, String searchIndexName) {
        // Initialize any necessary configurations or connections
    }

    @Override
    public void deleteAllIndex() {
        String sql = "DELETE FROM BXS_omnSearch ";
        DB.executeUpdate(sql, null);
    }

    @Override
    public void deleteIndexByQuery(String query) {
        String sql = "DELETE FROM BXS_omnSearch WHERE " + query;
        DB.executeUpdate(sql, null);
    }

    @Override
    public Object searchIndexNoRestriction(String queryString) {
        ArrayList<TextSearchResult> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ad_table_id, record_id ");
        sql.append("FROM BXS_omnSearch ");
        sql.append(" WHERE to_tsvector('english', document) @@ to_tsquery('english', ?)");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql.toString(), null);
            pstmt.setString(1, queryString);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                TextSearchResult result = new TextSearchResult();
                result.setAD_Table_ID(rs.getInt("ad_table_id"));
                result.setRecord_ID(rs.getInt("record_id"));
                results.add(result);
            }
        } catch (Exception e) {
            throw new AdempiereException(e);
        } finally {
            DB.close(rs, pstmt);
        }
        return results;
    }

    @Override
    public List<Object> searchIndexDocument(String queryString) {
        return searchIndexDocument(queryString, 0);
    }

    @Override
    public List<Object> searchIndexDocument(String queryString, int maxRow) {
        ArrayList<Object> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM BXS_omnSearch ");
        sql.append(" WHERE to_tsvector('english', document) @@ to_tsquery('english', ?)");
        if (maxRow > 0) {
            sql.append(" LIMIT " + maxRow);
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql.toString(), null);
            pstmt.setString(1, queryString);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                // Add the result to the list
                results.add(rs.getObject(1)); // Adjust as per your requirement
            }
        } catch (Exception e) {
            throw new AdempiereException(e);
        } finally {
            DB.close(rs, pstmt);
        }
        return results;
    }

    @Override
    public void createIndex(Properties ctx, Map<Integer, Set<SearchIndexRecord>> indexRecordsMap, String trxName) {
        if (indexRecordsMap == null) {
            return;
        }

        for (Map.Entry<Integer, Set<SearchIndexRecord>> searchIndexRecordSet : indexRecordsMap.entrySet()) {
            for (SearchIndexRecord searchIndexRecord : searchIndexRecordSet.getValue()) {
                for (Map<String, Object> tableDataSet : searchIndexRecord.getTableData()) {
                    if (tableDataSet.get("Record_ID") == null)
                        continue;

                    // Extract document content
                    String documentContent = extractDocumentContent(tableDataSet);

                    // Build the upsert query
                    StringBuilder upsertQuery = new StringBuilder();
                    upsertQuery.append("INSERT INTO adempiere.bxs_omnsearch ");
                    upsertQuery.append("(ad_client_id, ad_table_id, record_id, bxs_omntsvector) VALUES (?, ?, ?, to_tsvector(");
                    upsertQuery.append(getTSConfig());
                    upsertQuery.append(", ?)) ");
                    upsertQuery.append("ON CONFLICT (ad_table_id, record_id) DO UPDATE SET bxs_omntsvector = EXCLUDED.bxs_omntsvector");

                    // Execute the query
                    PreparedStatement pstmt = null;
                    try {
                        pstmt = DB.prepareStatement(upsertQuery.toString(), trxName);
                        pstmt.setInt(1, Env.getAD_Client_ID(ctx));
                        pstmt.setInt(2, searchIndexRecord.getTableId());
                        pstmt.setInt(3, Integer.parseInt(tableDataSet.get("Record_ID").toString()));
                        pstmt.setString(4, documentContent);
                        pstmt.executeUpdate();
                    } catch (Exception e) {
                        throw new AdempiereException(e);
                    } finally {
                        DB.close(pstmt);
                    }
                }
            }
        }
    }

    /**
     * Extracts the document content from the table data set.
     *
     * @param tableDataSet the table data set
     * @return the document content
     */
    private String extractDocumentContent(Map<String, Object> tableDataSet) {
        StringBuilder documentContent = new StringBuilder();
        for (Map.Entry<String, Object> entry : tableDataSet.entrySet()) {
            if (entry.getValue() != null) {
                documentContent.append(entry.getValue().toString()).append(" ");
            }
        }
        return documentContent.toString().trim();
    }
    
    /**
	 * Gets the text search configuration to use. 
	 * @return the text search configuration
	 */
    private String getTSConfig() {
		// Check if the specified text search configuration exists
        String tsConfig = MClient.get(Env.getCtx()).getLanguage().getLocale().getDisplayLanguage(Locale.ENGLISH);
        String fallbackConfig = "simple";
        String checkConfigQuery = "SELECT COUNT(*) FROM pg_ts_config WHERE cfgname = ?";
        int configCount = DB.getSQLValue(null, checkConfigQuery, tsConfig);

        if (configCount == 0) {
            log.log(Level.INFO, "Text search configuration '" + tsConfig + "' does not exist. Falling back to '" + fallbackConfig + "'.");
            tsConfig = fallbackConfig;
        }
		return tsConfig;
	}
}