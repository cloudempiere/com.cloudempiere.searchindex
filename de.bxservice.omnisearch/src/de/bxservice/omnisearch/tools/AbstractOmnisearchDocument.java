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
package de.bxservice.omnisearch.tools;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.compiere.model.MColumn;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;

public abstract class AbstractOmnisearchDocument implements OmnisearchDocument {

	/**	Logger							*/
	protected static CLogger log = CLogger.getCLogger (AbstractOmnisearchDocument.class);
	
	protected HashMap<Integer, ArrayList<Integer>> indexedTables;
	protected HashMap<String, ArrayList<MColumn>> foreignTables;
	
	/**
	 * CLDE
	 * @param reQuery
	 * @param trxName
	 * @return
	 */
	public HashMap<Integer, ArrayList<Integer>> getIndexedTables(boolean reQuery, String trxName) {
	    if (indexedTables == null || reQuery)
	    	getIndexedTables(trxName);
	    
	    return indexedTables;
	}

	/**
	 * CLDE
	 * Fetch a hashmap with tables and their indexed columns
	 * @param trxName
	 */
	private void getIndexedTables(String trxName) {
	    StringBuilder sql = new StringBuilder("SELECT oscl.AD_Table_ID, oscl.AD_Column_ID ")
	        .append("FROM AD_OmnSearchConfig osc ")
	        .append("JOIN AD_OmnSearchConfigLine oscl ON oscl.AD_OmnSearchConfig_ID = osc.AD_OmnSearchConfig_ID ")
	        .append("WHERE osc.IsActive = 'Y' AND oscl.IsActive = 'Y' AND osc.AD_Client_ID IN (0, ?)");

	    // Bring the table ids that are indexed
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
	        pstmt = DB.prepareStatement(sql.toString(), trxName);
	        pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
	        rs = pstmt.executeQuery();
	        
	        indexedTables = new HashMap<>();
	        int tableID = -1;
	        int columnID = -1;
	        while (!Thread.currentThread().isInterrupted() && rs.next()) {
	            tableID = rs.getInt(1);
	            columnID = rs.getInt(2);
	            
	            if (!indexedTables.containsKey(tableID)) {
	                indexedTables.put(tableID, new ArrayList<Integer>());
	            }
	            indexedTables.get(tableID).add(columnID);
	        }
	    } catch (Exception e) {
	        log.log(Level.SEVERE, sql.toString(), e);
	    } finally {
	        DB.close(rs, pstmt);
	        rs = null;
	        pstmt = null;
	    }
	}
	
	public ArrayList<Integer> getIndexedColumns(int AD_Table_ID) {
	    if (indexedTables == null) {
	        getIndexedTables(null);
	    }

	    return indexedTables.get(AD_Table_ID);
	}

	public ArrayList<String> getIndexedColumnNames(int AD_Table_ID) {
	    ArrayList<String> columnNames = null;

	    String sql = "SELECT c.columnname FROM AD_Column c "
	               + "JOIN AD_OmnSearchConfigLine oscl ON c.AD_Column_ID = oscl.AD_Column_ID "
	               + "JOIN AD_OmnSearchConfig osc ON oscl.AD_OmnSearchConfig_ID = osc.AD_OmnSearchConfig_ID "
	               + "WHERE osc.IsActive = 'Y' AND oscl.IsActive = 'Y' AND osc.AD_Client_ID IN (0, ?) "
	               + "AND c.AD_Table_ID = ?";

	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
	        pstmt = DB.prepareStatement(sql, null);
	        pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
	        pstmt.setInt(2, AD_Table_ID);
	        rs = pstmt.executeQuery();

	        columnNames = new ArrayList<>();
	        while (!Thread.currentThread().isInterrupted() && rs.next()) {
	            columnNames.add(rs.getString(1));
	        }
	    } catch (Exception e) {
	        log.log(Level.SEVERE, sql.toString(), e);
	    } finally {
	        DB.close(rs, pstmt);
	        rs = null;
	        pstmt = null;
	    }

	    return columnNames;
	}
	
	protected ArrayList<MColumn> getReferencedColumns(String tableName) {
		if(foreignTables == null)
			getFKParentTables();
		
		return foreignTables.get(tableName);
	}
	
	protected HashMap<String, ArrayList<MColumn>> getFKParentTables() {
	    foreignTables = new HashMap<>();

	    String sql = "SELECT c.AD_Column_ID, t.TableName FROM AD_Column c "
	               + "JOIN AD_OmnSearchConfigLine oscl ON c.AD_Column_ID = oscl.AD_Column_ID "
	               + "JOIN AD_OmnSearchConfig osc ON oscl.AD_OmnSearchConfig_ID = osc.AD_OmnSearchConfig_ID "
	               + "JOIN AD_Table t ON c.AD_Table_ID = t.AD_Table_ID "
	               + "WHERE osc.IsActive = 'Y' AND oscl.IsActive = 'Y' AND osc.AD_Client_ID IN (0, ?) "
	               + "AND c.AD_Reference_ID IN (?,?,?,?,?,?,?,?,?,?,?)";

	    // Bring the column ids from the FK
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
	        pstmt = DB.prepareStatement(sql.toString(), null);
	        pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
	        pstmt.setInt(2, DisplayType.TableDir);
	        pstmt.setInt(3, DisplayType.Search);
	        pstmt.setInt(4, DisplayType.Table);
	        pstmt.setInt(5, DisplayType.List);
	        pstmt.setInt(6, DisplayType.Payment);
	        pstmt.setInt(7, DisplayType.Location);
	        pstmt.setInt(8, DisplayType.Account);
	        pstmt.setInt(9, DisplayType.Locator);
	        pstmt.setInt(10, DisplayType.PAttribute);
	        pstmt.setInt(11, DisplayType.Assignment);
	        pstmt.setInt(12, DisplayType.RadiogroupList);
	        rs = pstmt.executeQuery();

	        MColumn column = null;
	        while (!Thread.currentThread().isInterrupted() && rs.next()) {
	            column = MColumn.get(Env.getCtx(), rs.getInt(1));

	            if (column != null) {
	                String tableName = rs.getString(2);
	                if (tableName != null) {
	                    if (!foreignTables.containsKey(tableName))
	                        foreignTables.put(tableName, new ArrayList<MColumn>());

	                    foreignTables.get(tableName).add(column);
	                }
	            }
	        }
	    } catch (Exception e) {
	        log.log(Level.SEVERE, sql.toString(), e);
	    } finally {
	        DB.close(rs, pstmt);
	        rs = null;
	        pstmt = null;
	    }

	    return foreignTables;
	}
}
