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
 * - Cloudempiere                                                      *
 **********************************************************************/
package com.cloudempiere.searchindex.util;

import java.util.regex.Pattern;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Util;

/**
 * Security validator for search index operations to prevent SQL injection.
 *
 * Implements defense-in-depth strategy with three layers:
 * 1. Input validation (dangerous patterns and safe characters)
 * 2. Whitelist verification (table/column names against AD_Table/AD_Column)
 * 3. Parameterized queries (where possible)
 *
 * @see ADR-002: SQL Injection Prevention Strategy
 * @author Cloudempiere
 */
public class SearchIndexSecurityValidator {

    /** Logger */
    private static final CLogger log = CLogger.getCLogger(SearchIndexSecurityValidator.class);

    /**
     * Pattern to detect dangerous SQL keywords and patterns that could lead to injection
     */
    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        ".*(;|--|/\\*|\\*/|xp_|sp_|exec|execute|union|insert|update|delete|drop|create|alter|grant|revoke).*",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern for safe WHERE clause characters (alphanumeric, common operators, whitespace)
     */
    private static final Pattern SAFE_WHERE_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_\\s=<>!'\"(),.?&|+-]+$"
    );

    /**
     * Validates WHERE clause for SQL injection patterns.
     *
     * <p>Checks for:
     * <ul>
     * <li>Dangerous SQL keywords (DROP, DELETE, INSERT, etc.)</li>
     * <li>SQL comments (double dash and slash-star)</li>
     * <li>Statement terminators (semicolon)</li>
     * <li>Unsafe characters</li>
     * </ul>
     *
     * @param whereClause WHERE clause to validate (without WHERE keyword)
     * @throws AdempiereException if validation fails
     */
    public static void validateWhereClause(String whereClause) {
        if (Util.isEmpty(whereClause)) {
            return;  // Empty is safe
        }

        // Check for dangerous keywords/patterns
        if (DANGEROUS_PATTERNS.matcher(whereClause).matches()) {
            log.severe("SECURITY: SQL injection attempt blocked in WHERE clause: " + whereClause);
            throw new AdempiereException("Invalid WHERE clause: contains dangerous SQL keywords");
        }

        // Check for safe characters only
        if (!SAFE_WHERE_PATTERN.matcher(whereClause).matches()) {
            log.severe("SECURITY: SQL injection attempt blocked (unsafe characters): " + whereClause);
            throw new AdempiereException("Invalid WHERE clause: contains unsafe characters");
        }

        // Additional specific checks
        if (whereClause.contains("--")) {
            log.severe("SECURITY: SQL injection attempt blocked (SQL comment): " + whereClause);
            throw new AdempiereException("Invalid WHERE clause: SQL comments not allowed");
        }

        if (whereClause.contains(";")) {
            log.severe("SECURITY: SQL injection attempt blocked (statement terminator): " + whereClause);
            throw new AdempiereException("Invalid WHERE clause: multiple statements not allowed");
        }
    }

    /**
     * Validates table name against AD_Table registry.
     *
     * This ensures that only existing, active tables can be used in queries,
     * preventing SQL injection through table name manipulation.
     *
     * @param tableName Table name to validate
     * @param trxName Transaction name
     * @return Validated table name from database (canonical form)
     * @throws AdempiereException if table not found or invalid
     */
    public static String validateTableName(String tableName, String trxName) {
        if (Util.isEmpty(tableName)) {
            throw new AdempiereException("Table name cannot be empty");
        }

        // Query AD_Table to verify table exists
        String sql = "SELECT TableName FROM AD_Table " +
                     "WHERE LOWER(TableName) = LOWER(?) " +
                     "AND IsView='N' AND IsActive='Y'";

        String validatedName = DB.getSQLValueString(trxName, sql, tableName);

        if (validatedName == null) {
            log.severe("SECURITY: Invalid table name rejected: " + tableName);
            throw new AdempiereException("Invalid table name: " + tableName);
        }

        return validatedName;  // Use validated name from DB
    }

    /**
     * Validates column name against AD_Column registry.
     *
     * Ensures column exists in specified table and is active.
     *
     * @param tableName Table name (must be validated first)
     * @param columnName Column name to validate
     * @param trxName Transaction name
     * @return Validated column name from database (canonical form)
     * @throws AdempiereException if column not found or invalid
     */
    public static String validateColumnName(String tableName, String columnName, String trxName) {
        if (Util.isEmpty(columnName)) {
            throw new AdempiereException("Column name cannot be empty");
        }

        String sql = "SELECT c.ColumnName FROM AD_Column c " +
                     "JOIN AD_Table t ON c.AD_Table_ID = t.AD_Table_ID " +
                     "WHERE t.TableName = ? AND LOWER(c.ColumnName) = LOWER(?) " +
                     "AND c.IsActive='Y'";

        String validatedName = DB.getSQLValueString(trxName, sql, tableName, columnName);

        if (validatedName == null) {
            log.severe("SECURITY: Invalid column rejected: " + columnName + " in table " + tableName);
            throw new AdempiereException("Invalid column: " + columnName + " in table " + tableName);
        }

        return validatedName;
    }
}
