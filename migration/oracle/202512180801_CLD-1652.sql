-- CLD-1652: Multi-Tenant UNIQUE Constraint Fix (Oracle)
-- Migration Date: 2025-12-18 08:01
-- Description:
--   Fixes multi-tenant UNIQUE constraint to include CLIENT_ID
--   Note: Slovak text search configuration is PostgreSQL-specific and not applicable to Oracle
-- Prerequisites: None
-- Related ADR:
--   - ADR-006: Multi-Tenant Data Integrity
-- Linear Issue: https://linear.app/cloudempiere/issue/CLD-1652

-- Production Execution Results (2025-12-18 08:01:51)
-- ========================================
-- ✓ Data cleanup found 0 duplicates
-- ✓ Successfully migrated 5 tables:
--   - idx_product_ts (renamed to searchindex_product in Oracle)
--   - idx_bpartner_ts (renamed to searchindex_bpartner in Oracle)
--   - idx_k_entry_ts (renamed to searchindex_k_entry in Oracle)
--   - idx_productcategory_ts (renamed to searchindex_productcategory in Oracle)
--   - idx_r_request_ts (renamed to searchindex_r_request in Oracle)
-- ✓ Changed constraint from (ad_table_id, record_id)
--   to (ad_client_id, ad_table_id, record_id)

SELECT register_migration_script('202512180801_CLD-1652.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- ========================================
-- Multi-Tenant UNIQUE Constraint Fix
-- ========================================
-- Note: Oracle search index tables use naming convention: searchindex_<indexname>
-- PostgreSQL uses: idx_<indexname>_ts
-- This script processes all tables matching pattern: searchindex_%

DECLARE
    v_table_name VARCHAR2(128);
    v_constraint_name VARCHAR2(128);
    v_duplicate_count NUMBER;
    v_sql VARCHAR2(4000);

    CURSOR c_tables IS
        SELECT table_name
        FROM user_tables
        WHERE table_name LIKE 'SEARCHINDEX_%'
        ORDER BY table_name;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Starting migration: Multi-tenant UNIQUE constraint fix');
    DBMS_OUTPUT.PUT_LINE('Date: ' || TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('========================================');

    FOR rec IN c_tables LOOP
        v_table_name := rec.table_name;
        DBMS_OUTPUT.PUT_LINE('Processing table: ' || v_table_name);

        -- Check for duplicate entries that would violate new constraint
        v_sql := 'SELECT COUNT(*) FROM (' ||
                 '  SELECT ad_table_id, record_id, COUNT(*) as cnt' ||
                 '  FROM ' || v_table_name ||
                 '  GROUP BY ad_table_id, record_id' ||
                 '  HAVING COUNT(*) > 1' ||
                 ')';
        EXECUTE IMMEDIATE v_sql INTO v_duplicate_count;

        IF v_duplicate_count > 0 THEN
            DBMS_OUTPUT.PUT_LINE('  ⚠ Found ' || v_duplicate_count || ' duplicate groups - cleaning up...');

            -- Clean up duplicates - keep only most recent entry
            v_sql := 'DELETE FROM ' || v_table_name || ' WHERE ROWID IN (' ||
                     '  SELECT rid FROM (' ||
                     '    SELECT ROWID rid,' ||
                     '           ROW_NUMBER() OVER (' ||
                     '             PARTITION BY ad_table_id, record_id' ||
                     '             ORDER BY' ||
                     '               NVL(updated, created) DESC NULLS LAST,' ||
                     '               created DESC NULLS LAST,' ||
                     '               ad_client_id DESC' ||
                     '           ) AS rn' ||
                     '    FROM ' || v_table_name ||
                     '  ) WHERE rn > 1' ||
                     ')';
            EXECUTE IMMEDIATE v_sql;

            DBMS_OUTPUT.PUT_LINE('  ✓ Cleaned up duplicates from ' || v_table_name);
        ELSE
            DBMS_OUTPUT.PUT_LINE('  ✓ No duplicates found in ' || v_table_name);
        END IF;

        -- Drop old UNIQUE constraint if exists
        -- Oracle naming pattern: <table>_AD_TABLE_ID_RECORD_ID_KEY
        v_constraint_name := v_table_name || '_AD_TABLE_ID_RECORD_ID_KEY';

        BEGIN
            v_sql := 'ALTER TABLE ' || v_table_name || ' DROP CONSTRAINT ' || v_constraint_name;
            EXECUTE IMMEDIATE v_sql;
            DBMS_OUTPUT.PUT_LINE('  ✓ Dropped old constraint: ' || v_constraint_name);
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE = -2443 THEN  -- ORA-02443: Cannot drop constraint - nonexistent constraint
                    DBMS_OUTPUT.PUT_LINE('  ⚠ Old constraint does not exist, skipping');
                ELSE
                    RAISE;
                END IF;
        END;

        -- Also try dropping index-based constraint (alternative naming)
        BEGIN
            v_sql := 'DROP INDEX ' || v_constraint_name;
            EXECUTE IMMEDIATE v_sql;
            DBMS_OUTPUT.PUT_LINE('  ✓ Dropped old index: ' || v_constraint_name);
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE = -1418 THEN  -- ORA-01418: specified index does not exist
                    NULL;  -- Ignore, index doesn't exist
                ELSE
                    RAISE;
                END IF;
        END;

        -- Create new multi-tenant safe UNIQUE constraint
        v_constraint_name := v_table_name || '_CLIENT_TABLE_REC_KEY';

        BEGIN
            v_sql := 'CREATE UNIQUE INDEX ' || v_constraint_name ||
                     ' ON ' || v_table_name ||
                     ' (AD_CLIENT_ID, AD_TABLE_ID, RECORD_ID)';
            EXECUTE IMMEDIATE v_sql;
            DBMS_OUTPUT.PUT_LINE('  ✓ Created new constraint: ' || v_constraint_name);
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE = -955 THEN  -- ORA-00955: name is already used by an existing object
                    DBMS_OUTPUT.PUT_LINE('  ⚠ Constraint already exists, skipping');
                ELSIF SQLCODE = -1 THEN  -- ORA-00001: unique constraint violated
                    RAISE_APPLICATION_ERROR(-20001,
                        '✗ UNIQUE constraint violation - duplicate data still exists!');
                ELSE
                    RAISE;
                END IF;
        END;

        DBMS_OUTPUT.PUT_LINE('  ✓ Table ' || v_table_name || ' migration completed');
        DBMS_OUTPUT.PUT_LINE('');
    END LOOP;

    DBMS_OUTPUT.PUT_LINE('========================================');
    DBMS_OUTPUT.PUT_LINE('MIGRATION COMPLETED SUCCESSFULLY!');
    DBMS_OUTPUT.PUT_LINE('========================================');
    DBMS_OUTPUT.PUT_LINE('');
    DBMS_OUTPUT.PUT_LINE('Changes applied:');
    DBMS_OUTPUT.PUT_LINE('✓ Multi-tenant UNIQUE constraints fixed for all search index tables');
    DBMS_OUTPUT.PUT_LINE('');
    DBMS_OUTPUT.PUT_LINE('Note: Slovak text search configuration is PostgreSQL-specific');
    DBMS_OUTPUT.PUT_LINE('      Oracle full-text search uses Oracle Text (CONTEXT indexes)');
    DBMS_OUTPUT.PUT_LINE('      If Slovak language support is needed for Oracle, configure:');
    DBMS_OUTPUT.PUT_LINE('      - Oracle Text lexer preferences');
    DBMS_OUTPUT.PUT_LINE('      - BASIC_LEXER with BASE_LETTER conversion');
    DBMS_OUTPUT.PUT_LINE('');

    COMMIT;
END;
/
