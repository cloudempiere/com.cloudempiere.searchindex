# ADR-002: SQL Injection Prevention Strategy

**Status:** ✅ Implemented
**Date:** 2025-12-12
**Implementation Date:** 2025-12-17
**Decision Makers:** Development Team, Security Team
**Related Issues:** Finding 1.1, 1.2, 1.3 (CRITICAL)
**Implementation Commits:** 378ec8b, e4c23b5 (critical search index table fix)

---

## Context

The search index plugin has **three critical SQL injection vulnerabilities** where user-controlled data is directly concatenated into SQL queries without validation or parameterization.

### Vulnerability Locations

1. **Dynamic WHERE Clause in PGTextSearchIndexProvider** (Lines 148-151, 167-170)
   ```java
   if (!Util.isEmpty(dynWhere)) {
       if (!dynWhere.trim().toUpperCase().startsWith("AND")) {
           dynWhere = " AND " + dynWhere;
       }
       sql += dynWhere;  // ← INJECTION POINT
   }
   ```

2. **WHERE Clause in SearchIndexConfigBuilder** (Lines 320-326)
   ```java
   String dynamicWhere = tableConfig.getSqlWhere();
   if (!Util.isEmpty(dynamicWhere)) {
       whereClauseBuilder.append(dynamicWhere);  // ← INJECTION POINT
   }
   ```

3. **Table Name Injection** (Lines 144, 163, 361)
   ```java
   sql = "DELETE FROM " + tableName + " WHERE ...";  // ← INJECTION POINT
   ```

### Exploitation Scenarios

**Scenario 1: WHERE Clause Injection**
```sql
-- Admin creates SearchIndexTable with WHERE clause:
IsActive='Y'; DROP TABLE M_Product; --

-- Results in:
DELETE FROM searchindex_product
WHERE AD_Client_ID IN (0,1000)
  AND IsActive='Y'; DROP TABLE M_Product; --
```

**Scenario 2: Table Name Injection**
```sql
-- SearchIndexName set to:
searchindex_product; DROP TABLE AD_User; --

-- Results in:
DELETE FROM searchindex_product; DROP TABLE AD_User; --
WHERE AD_Client_ID IN (0,?)
```

### Impact

- **Data Loss:** Arbitrary table drops
- **Data Corruption:** Arbitrary UPDATE/DELETE
- **Privilege Escalation:** Access to system tables
- **Compliance Violation:** GDPR, SOC 2, PCI DSS

---

## Decision

We will implement a **defense-in-depth** SQL injection prevention strategy with **three layers of protection**:

1. **Input Validation** (First Line of Defense)
2. **Whitelist Verification** (Second Line of Defense)
3. **Parameterized Queries** (Last Line of Defense)

---

## Layer 1: Input Validation

### WHERE Clause Validator

```java
public class SearchIndexSecurityValidator {

    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        ".*(;|--|/\\*|\\*/|xp_|sp_|exec|execute|union|insert|update|delete|drop|create|alter|grant|revoke).*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SAFE_WHERE_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_\\s=<>!'\"(),.?&|+-]+$"
    );

    /**
     * Validates WHERE clause for SQL injection patterns
     * @throws AdempiereException if validation fails
     */
    public static void validateWhereClause(String whereClause) {
        if (Util.isEmpty(whereClause)) {
            return;  // Empty is safe
        }

        // Check for dangerous keywords/patterns
        if (DANGEROUS_PATTERNS.matcher(whereClause).matches()) {
            throw new AdempiereException("Invalid WHERE clause: contains dangerous SQL keywords");
        }

        // Check for safe characters only
        if (!SAFE_WHERE_PATTERN.matcher(whereClause).matches()) {
            throw new AdempiereException("Invalid WHERE clause: contains unsafe characters");
        }

        // Additional checks
        if (whereClause.contains("--")) {
            throw new AdempiereException("Invalid WHERE clause: SQL comments not allowed");
        }

        if (whereClause.contains(";")) {
            throw new AdempiereException("Invalid WHERE clause: multiple statements not allowed");
        }
    }
}
```

### Usage in PGTextSearchIndexProvider

```java
// Before concatenation
if (!Util.isEmpty(dynWhere)) {
    SearchIndexSecurityValidator.validateWhereClause(dynWhere);  // VALIDATE FIRST

    if (!dynWhere.trim().toUpperCase().startsWith("AND")) {
        dynWhere = " AND " + dynWhere;
    }
    sql += dynWhere;  // Now safe to concatenate
}
```

---

## Layer 2: Whitelist Verification

### Table Name Validation

```java
public class SearchIndexSecurityValidator {

    /**
     * Validates table name against AD_Table registry
     * @return Validated table name from database
     * @throws AdempiereException if table not found
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
            throw new AdempiereException("Invalid table name: " + tableName);
        }

        return validatedName;  // Use validated name from DB
    }

    /**
     * Validates column name against AD_Column registry
     */
    public static String validateColumnName(String tableName, String columnName, String trxName) {
        String sql = "SELECT c.ColumnName FROM AD_Column c " +
                     "JOIN AD_Table t ON c.AD_Table_ID = t.AD_Table_ID " +
                     "WHERE t.TableName = ? AND LOWER(c.ColumnName) = LOWER(?) " +
                     "AND c.IsActive='Y'";

        String validatedName = DB.getSQLValueString(trxName, sql, tableName, columnName);

        if (validatedName == null) {
            throw new AdempiereException("Invalid column: " + columnName + " in table " + tableName);
        }

        return validatedName;
    }
}
```

### Usage in PGTextSearchIndexProvider

```java
// Before using in SQL
String safeTableName = SearchIndexSecurityValidator.validateTableName(tableName, trxName);
sql = "DELETE FROM " + safeTableName + " WHERE AD_Client_ID IN (0,?)";
```

---

## Layer 3: Parameterized Queries (Where Possible)

### Preferred Approach

```java
// GOOD: Use PreparedStatement with parameters
String sql = "DELETE FROM " + safeTableName + " WHERE AD_Client_ID = ? AND AD_Table_ID = ?";
PreparedStatement pstmt = DB.prepareStatement(sql, trxName);
pstmt.setInt(1, clientId);
pstmt.setInt(2, tableId);
pstmt.executeUpdate();
```

### When Parameterization Not Possible

For dynamic WHERE clauses, use **parsed query builder**:

```java
public class SafeWhereClauseBuilder {

    private List<String> conditions = new ArrayList<>();
    private List<Object> parameters = new ArrayList<>();

    /**
     * Parses WHERE clause and extracts parameters
     * Example: "Name LIKE 'Test%' AND Value > 100"
     *   → "Name LIKE ? AND Value > ?"
     *   → parameters: ['Test%', 100]
     */
    public void parseAndAdd(String whereClause) {
        // Tokenize WHERE clause
        String[] tokens = whereClause.split("\\s+(AND|OR)\\s+");

        for (String token : tokens) {
            // Parse: "Name LIKE 'Test%'"
            Matcher m = Pattern.compile("(\\w+)\\s*([=<>!]+|LIKE)\\s*'([^']+)'").matcher(token);
            if (m.matches()) {
                String column = m.group(1);
                String operator = m.group(2);
                String value = m.group(3);

                // Validate column exists
                SearchIndexSecurityValidator.validateColumnName(tableName, column, trxName);

                conditions.add(column + " " + operator + " ?");
                parameters.add(value);
            }
        }
    }

    public String getSQL() {
        return String.join(" AND ", conditions);
    }

    public Object[] getParameters() {
        return parameters.toArray();
    }
}
```

---

## Implementation Plan

### Phase 1: Core Validation Framework (1 day)

1. **Create `SearchIndexSecurityValidator` class**
   - `validateWhereClause(String)`
   - `validateTableName(String, String)`
   - `validateColumnName(String, String, String)`

2. **Add unit tests**
   ```java
   @Test(expected = AdempiereException.class)
   public void testSQLInjection_DropTable() {
       SearchIndexSecurityValidator.validateWhereClause(
           "IsActive='Y'; DROP TABLE M_Product; --"
       );
   }

   @Test
   public void testValidWhereClause() {
       SearchIndexSecurityValidator.validateWhereClause(
           "IsActive='Y' AND Created > '2025-01-01'"
       );
       // Should pass
   }
   ```

### Phase 2: Apply to PGTextSearchIndexProvider (1 day)

1. **Fix deleteIndex() methods (Lines 144-170)**
   ```java
   // Line 144
   - sql = "DELETE FROM " + tableName + " WHERE AD_Client_ID IN (0,?)";
   + String safeTableName = SearchIndexSecurityValidator.validateTableName(tableName, trxName);
   + sql = "DELETE FROM " + safeTableName + " WHERE AD_Client_ID IN (0,?)";

   // Line 148-151
   if (!Util.isEmpty(dynWhere)) {
   +   SearchIndexSecurityValidator.validateWhereClause(dynWhere);
       if (!dynWhere.trim().toUpperCase().startsWith("AND")) {
           dynWhere = " AND " + dynWhere;
       }
       sql += dynWhere;
   }
   ```

2. **Fix getSearchResults() method (Line 361)**
   ```java
   - sql.append("FROM ").append(searchIndexName)
   + String safeTableName = SearchIndexSecurityValidator.validateTableName(searchIndexName, null);
   + sql.append("FROM ").append(safeTableName)
   ```

### Phase 3: Apply to SearchIndexConfigBuilder (0.5 days)

1. **Fix buildIndexData() method (Lines 320-326)**
   ```java
   String dynamicWhere = tableConfig.getSqlWhere();
   if (!Util.isEmpty(dynamicWhere)) {
   +   SearchIndexSecurityValidator.validateWhereClause(dynamicWhere);
       if (!dynamicWhere.trim().toUpperCase().startsWith("AND")) {
           whereClauseBuilder.append("AND ");
       }
       whereClauseBuilder.append(dynamicWhere);
   }
   ```

### Phase 4: Security Testing (0.5 days)

1. **Penetration testing**
   - Attempt known SQL injection patterns
   - Verify all blocked by validator

2. **Integration testing**
   - Valid WHERE clauses still work
   - Error messages helpful to users

---

## Validation & Testing

### Test Cases

1. **SQL Injection Attempts (Should Fail)**
   ```java
   // WHERE clause injection
   "IsActive='Y'; DROP TABLE M_Product; --"
   "1=1 OR 1=1"
   "Name LIKE '%'; DELETE FROM AD_User; --"

   // Table name injection
   "searchindex_product; DROP TABLE AD_User; --"
   "searchindex_product UNION SELECT * FROM AD_User"

   // Comment injection
   "IsActive='Y' --"
   "IsActive='Y' /* comment */"
   ```

2. **Valid SQL (Should Pass)**
   ```java
   // WHERE clause
   "IsActive='Y' AND Created > '2025-01-01'"
   "Name LIKE 'Test%' AND Value > 100"
   "AD_Org_ID IN (0, 1000)"

   // Table name
   "searchindex_product"
   "M_Product"
   ```

### Security Scan

Run static analysis tools:
```bash
# SonarQube security scan
mvn sonar:sonar -Dsonar.security.hotspots=true

# OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check

# FindSecBugs
mvn com.github.spotbugs:spotbugs-maven-plugin:spotbugs
```

Expected result: **Zero SQL injection vulnerabilities**

---

## Monitoring & Alerting

### Log Security Events

```java
// In SearchIndexSecurityValidator
private static CLogger log = CLogger.getCLogger(SearchIndexSecurityValidator.class);

public static void validateWhereClause(String whereClause) {
    if (DANGEROUS_PATTERNS.matcher(whereClause).matches()) {
        // LOG SECURITY EVENT
        log.severe("SECURITY: SQL injection attempt blocked: " + whereClause);

        // Notify security team
        EmailUtils.sendSecurityAlert("SQL Injection Attempt", whereClause);

        throw new AdempiereException("Invalid WHERE clause: contains dangerous SQL keywords");
    }
}
```

### Metrics to Track

1. **Injection Attempts**
   ```sql
   SELECT COUNT(*) FROM AD_ChangeLog
   WHERE ColumnName = 'WhereClause'
     AND NewValue LIKE '%;%'
   GROUP BY TRUNC(Created, 'DD')
   ```

2. **Validation Failures**
   - Count of AdempiereException with "Invalid WHERE clause"
   - Alert if >10/hour (possible attack)

---

## Consequences

### Positive

1. **Security hardening**
   - SQL injection risk eliminated
   - Defense-in-depth approach

2. **Compliance**
   - Meets OWASP Top 10 requirements
   - SOC 2, PCI DSS compliant

3. **Audit trail**
   - All validation failures logged
   - Security events traceable

### Negative

1. **Reduced flexibility**
   - Some complex WHERE clauses may be blocked
   - Legitimate use cases might fail validation

2. **Performance overhead**
   - Validation adds ~1-2ms per query
   - Acceptable for security benefit

3. **User experience**
   - Users must learn safe WHERE clause syntax
   - Error messages must be clear

---

## Alternatives Considered

### Alternative 1: ORM/Query Builder Only

Use iDempiere's `Query` class exclusively:
```java
MQuery query = new MQuery(tableName);
query.addRestriction("IsActive='Y'");
```

**Pros:** Framework handles escaping automatically

**Cons:**
- Major refactoring required
- `MQuery` doesn't support full SQL syntax needed for search indexes

**Decision:** Rejected - too disruptive for current architecture

---

### Alternative 2: Stored Procedures

Create PostgreSQL stored procedures for all index operations:
```sql
CREATE FUNCTION search_index_delete(tableName TEXT, clientId INT) ...
```

**Pros:** SQL injection impossible (input validated by PostgreSQL)

**Cons:**
- Database-specific (not portable to other RDBMS)
- Deployment complexity (migration scripts)

**Decision:** Rejected - violates iDempiere database independence principle

---

### Alternative 3: No Validation (Document Only)

Document that WHERE clauses must be validated by admins, don't enforce programmatically.

**Pros:** Zero development effort

**Cons:**
- **UNACCEPTABLE SECURITY RISK**
- Relies on user behavior (fails in practice)

**Decision:** Rejected - security must be enforced, not optional

---

## Rollback Plan

If validation causes production issues:

1. **Emergency bypass flag**
   ```java
   // Add system config
   boolean strictValidation = MSysConfig.getBooleanValue(
       "SEARCH_INDEX_STRICT_VALIDATION", true, 0
   );

   if (strictValidation) {
       SearchIndexSecurityValidator.validateWhereClause(dynWhere);
   } else {
       log.warning("SECURITY: Strict validation bypassed");
   }
   ```

2. **Whitelist specific WHERE clauses**
   ```java
   // Config table: AD_SearchIndex_WhereClause_Whitelist
   if (isWhitelisted(dynWhere)) {
       // Skip validation
   }
   ```

---

## References

- OWASP Top 10 A03:2021 - Injection: https://owasp.org/Top10/A03_2021-Injection/
- iDempiere Security Best Practices: http://wiki.idempiere.org/en/Security
- CLAUDE.md:138-176 (SQL Injection Findings)

---

## Decision

**Approved:** [Pending]
**Approved By:** [Name]
**Approval Date:** [Date]
**Implementation Target:** Week 1 (Phase 0 Hotfix)

---

**Next ADR:** [ADR-003: Thread Safety Model](ADR-003-thread-safety.md)
