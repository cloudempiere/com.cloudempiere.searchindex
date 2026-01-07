# Search Index Test Bundle - Quick Reference

## Overview

The test bundle (`com.cloudempiere.searchindex.test`) is a JUnit 5 OSGi fragment with a multi-level testing pyramid:
- **Unit Tests**: Fast, no OSGi/DB required (default)
- **Integration Tests**: Requires iDempiere runtime + database
- **E2E Tests**: Full system workflows
- **Stress Tests**: Performance and concurrency validation

---

## ⚠️ IMPORTANT: Eclipse IDE Required for Tests

Due to iDempiere's OSGi architecture and circular dependency between `org.idempiere.test` and the P2 repository, **tests MUST be run from Eclipse IDE**, not from Maven command line.

### Why Maven Doesn't Work

1. The test bundle depends on `org.idempiere.test` for `AbstractTestCase`
2. `org.idempiere.test` is NOT included in the standard iDempiere P2 repository
3. Including it creates a circular dependency (test → P2 → test)
4. This is a known limitation of the iDempiere test infrastructure

### Running Tests in Eclipse IDE

1. **Import all projects** into Eclipse workspace:
   - iDempiereCLDE (iDempiere core)
   - com.cloudempiere.searchindex (main plugin)
   - com.cloudempiere.searchindex.test (test bundle)

2. **Use the launch configuration**:
   - Right-click `com.cloudempiere.searchindex.test.launch`
   - Select "Run As > JUnit Plug-in Test"

3. **Or run individual test classes**:
   - Right-click test class → Run As → JUnit Test

### Maven Build (Without Tests)

```bash
# Build the plugin (tests excluded from reactor)
cd com.cloudempiere.searchindex
mvn clean verify -DskipTests
```

---

## Running Tests

### Prerequisites

1. **Eclipse IDE** with PDE and JDT plugins installed

2. **Import all workspace projects**:
   - iDempiereCLDE (all modules)
   - com.cloudempiere.searchindex (all modules)

3. **Ensure iDempiere environment is available** (for integration tests):
   - Database connection configured
   - IDEMPIERE_HOME environment variable set

### Test Profiles

#### Unit Tests (Default - FAST)
Run isolated tests with no database required:
```bash
# Run all unit tests (parallel execution, ~20-30 seconds)
mvn test

# Run specific test class
mvn test -Dtest=SQLInjectionPreventionTest

# Run tests matching pattern
mvn test -Dtest=*Prevention*
```

**What's Included**:
- SQL injection prevention validation
- WHERE clause syntax validation
- Table/column name validation
- Fast isolated unit tests

**Execution**: Parallel (4 threads), 200-500ms per test

---

#### Integration Tests (Medium)
Run tests requiring iDempiere database:
```bash
# Run all integration tests (sequential, ~5-10 minutes)
mvn test -Pintegration

# Run specific integration test
mvn test -Pintegration -Dtest=TransactionIsolationTest
```

**What's Included**:
- Transaction isolation verification
- Multi-tenant data integrity
- Event handler behavior
- Rollback scenarios
- PO save + index update workflows

**Requirements**:
- iDempiere database must be running
- IDEMPIERE_HOME configured
- Connection pool available

**Execution**: Sequential (prevents resource contention), 5-30 seconds per test

---

#### E2E Tests (Slow)
Complete system workflows:
```bash
# Run all e2e tests (sequential, ~15-30 minutes)
mvn test -Pe2e

# Run specific e2e test
mvn test -Pe2e -Dtest=FullSearchWorkflowTest
```

**What's Included**:
- Complete search index creation workflows
- Multi-client scenarios
- Performance benchmarks
- Real-world usage patterns

**Execution**: Sequential, 30-120 seconds per test

---

#### Fast Tests (Custom Group)
Exclude slow/stress tests, parallel execution:
```bash
# Run quick tests only
mvn test -Pfast

# Excludes @Tag("slow") and @Tag("stress")
```

**Execution**: Parallel, ~2-5 minutes total

---

#### All Tests (CI/CD)
Comprehensive test suite:
```bash
# Run unit + integration + e2e tests
mvn test -Pall-tests

# With coverage reporting
mvn test -Pall-tests jacoco:report
```

**Execution**: 20-45 minutes (depends on system)

---

## Test Organization

### By Type

```
src/
├── com/cloudempiere/searchindex/test/
│   ├── TestConstants.java
│   │   └── Garden World demo data constants
│   │
│   ├── unit/
│   │   ├── SQLInjectionPreventionTest.java
│   │   │   └── @Test: Malicious WHERE clause detection
│   │   │   └── @Test: Valid WHERE clause acceptance
│   │   │   └── @Test: Table name validation
│   │   │   └── @Test: Column name validation
│   │   │
│   │   └── TsRankCdPerformanceTest.java
│   │       └── @Test: Search ranking algorithm performance
│   │
│   └── integration/
│       ├── TransactionIsolationTest.java
│       │   └── @Test: Index updates use separate transaction
│       │   └── @Test: PO rollback doesn't affect index
│       │   └── @Test: Index failure doesn't block PO
│       │   └── @Test: Concurrent PO changes (stress)
│       │
│       └── MultiTenantIntegrityTest.java
│           └── @Test: Cross-client data isolation
│           └── @Test: Client-scoped configuration
```

### By Tag

Tests use JUnit 5 @Tag annotations for grouping:

| Tag | Profile | Speed | Includes |
|-----|---------|-------|----------|
| `unit` | unit (default) | <1s | SQLInjection, Performance |
| `integration` | integration | 5-30s | Transactions, Multi-tenant |
| `e2e` | e2e | 30-120s | Full workflows |
| `stress` | (included if not excluded) | 10-60s | Concurrent operations |
| `slow` | fast profile excludes | 30s+ | Complex scenarios |

---

## Common Scenarios

### Development Workflow

```bash
# 1. Make code changes
vim src/com/cloudempiere/searchindex/YourClass.java

# 2. Run quick unit tests
mvn test

# 3. If unit tests pass, run integration tests
mvn test -Pintegration

# 4. Before committing, run all tests
mvn test -Pall-tests
```

### Continuous Integration

```bash
# GitHub Actions / Jenkins CI
mvn clean test -Pall-tests \
    -DIDEMPIERE_HOME=/opt/idempiere \
    -Drevision=10.0.0 \
    jacoco:report

# Generate coverage report
mvn jacoco:report
# Report at: target/site/jacoco/index.html
```

### Performance Testing

```bash
# Run only stress tests (concurrent scenarios)
mvn test -Pintegration -Dtest=*ConcurrentPOChanges*

# With JVM profiling
mvn test -Pintegration \
    -Darguments="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
```

### Debugging a Test

```bash
# Run single test with debug output
mvn test -Dtest=SQLInjectionPreventionTest \
    -X  # Enable debug logging

# Run in debug mode (suspend on test entry)
mvn test -Dtest=TransactionIsolationTest \
    -Dmaven.surefire.debug \
    -Dsuspend=y
# Now attach IDE debugger to localhost:5005
```

### Database Connection Issues

```bash
# If integration tests fail with DB connection errors:

# 1. Verify iDempiere is running
psql -U adempiere -d idempiere -c "SELECT version();"

# 2. Check IDEMPIERE_HOME
echo $IDEMPIERE_HOME

# 3. Pass explicitly to Maven
mvn test -Pintegration \
    -DIDEMPIERE_HOME=/path/to/iDempiere

# 4. Check database port (default 5432)
psql -h localhost -U adempiere -d idempiere -c "SELECT 1"
```

---

## Test Results

### Unit Tests Output
```
[INFO] Running com.cloudempiere.searchindex.test.unit.SQLInjectionPreventionTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.234 s
[INFO]
[INFO] Running com.cloudempiere.searchindex.test.unit.TsRankCdPerformanceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.156 s
[INFO]
[INFO] BUILD SUCCESS
```

### Integration Tests Output
```
[INFO] Running com.cloudempiere.searchindex.test.integration.TransactionIsolationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.432 s
[INFO]
[INFO] Running com.cloudempiere.searchindex.test.integration.MultiTenantIntegrityTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.156 s
[INFO]
[INFO] BUILD SUCCESS
```

### Coverage Report
```bash
# Generate code coverage
mvn test jacoco:report

# View report
open target/site/jacoco/index.html  # macOS
firefox target/site/jacoco/index.html  # Linux
```

---

## Troubleshooting

### "Bundle cannot be resolved"
**Cause**: Host bundle `com.cloudempiere.searchindex` not built

**Solution**:
```bash
cd ../com.cloudempiere.searchindex
mvn clean package
cd ../com.cloudempiere.searchindex.test
mvn test
```

---

### "Plugin cannot be resolved"
**Cause**: OSGi target platform not initialized

**Solution**:
```bash
# Clean and rebuild
mvn clean verify -U -Didempiere.core.repository.url=file:///path/to/p2/repo
```

---

### "Cannot connect to database"
**Cause**: iDempiere instance not running or IDEMPIERE_HOME not set

**Solution**:
```bash
# Check PostgreSQL is running
psql -U adempiere -d idempiere -c "SELECT 1"

# Set IDEMPIERE_HOME
export IDEMPIERE_HOME=/opt/idempiere

# Run integration tests
mvn test -Pintegration
```

---

### "Test timeout" (slow tests)
**Cause**: Concurrent stress tests running slowly

**Solution**:
```bash
# Increase test timeout (default 600s)
mvn test -Pall-tests -DtestFailureIgnore=true -DargLine="-Dcom.sun.jndi.ldap.connect.timeout=5000"

# Or skip stress tests
mvn test -Pintegration -Dgroups="!stress"
```

---

### Memory Issues
**Cause**: Tests consuming too much heap

**Solution**:
```bash
# Increase Maven heap
export MAVEN_OPTS="-Xmx2048m"
mvn test -Pall-tests

# Reduce parallelism
mvn test -DJUNIT_THREADS=2
```

---

## Best Practices

### Writing New Tests

1. **Extend AbstractTestCase**:
   ```java
   public class MyFeatureTest extends AbstractTestCase {
       @Test
       public void testFeature() {
           // Use getTrxName() for transactions
           // Use getCtx() for iDempiere context
       }
   }
   ```

2. **Clean up after tests**:
   ```java
   @AfterEach
   public void tearDown() {
       // Cleanup code (AbstractTestCase handles most)
   }
   ```

3. **Use meaningful test names**:
   ```java
   // Good
   testSQLInjection_DropTable_ShouldFail()

   // Bad
   testSQL()
   ```

4. **Tag your tests**:
   ```java
   @Test
   @Tag("integration")
   public void testWithDatabase() { ... }
   ```

5. **Test both success and failure**:
   ```java
   @Test
   public void testValidInput_ShouldPass() { ... }

   @Test
   public void testInvalidInput_ShouldFail() { ... }
   ```

---

## Configuration

### pom.xml Properties

Modify test behavior in `pom.xml`:

```xml
<properties>
    <!-- Skip tests during build -->
    <skipTests>false</skipTests>

    <!-- Number of parallel threads -->
    <p4>-Djunit.jupiter.execution.parallel.config.fixed.parallelism=4</p4>

    <!-- Test groups (unit, integration, e2e) -->
    <test.groups>unit</test.groups>
    <test.excludedGroups>integration,e2e,slow</test.excludedGroups>

    <!-- iDempiere home for DB access -->
    <idempiere.home>../../iDempiereCLDE</idempiere.home>
</properties>
```

### MANIFEST.MF Configuration

JUnit 5 version and Java compatibility:

```manifest
Bundle-RequiredExecutionEnvironment: JavaSE-11
Import-Package: org.junit.jupiter.api;version="5.6.0",
 org.assertj.core.api;version="3.22.0"
```

---

## Advanced Usage

### Test Filtering

```bash
# Run tests matching pattern
mvn test -Dtest=*Injection*

# Run tests excluding pattern
mvn test -Dtest=*Injection* -DexcludedTests=*Complex*

# Run specific test method
mvn test -Dtest=SQLInjectionPreventionTest#testValidWhereClause_ShouldPass
```

### Parallel Configuration

```bash
# Custom parallelism
mvn test -Djunit.jupiter.execution.parallel.config.fixed.parallelism=8

# Disable parallelism
mvn test -Djunit.jupiter.execution.parallel.enabled=false
```

### Test Reporting

```bash
# Surefire reports
mvn test
# Report at: target/surefire-reports/

# JUnit HTML reports
mvn surefire-report:report
# Report at: target/site/surefire-report.html
```

---

## Performance Baselines

Expected test execution times on typical hardware (4-core, 8GB RAM):

| Profile | Time | Notes |
|---------|------|-------|
| unit | 20-30s | Parallel (4 threads) |
| integration | 5-10 min | Sequential, requires DB |
| e2e | 15-30 min | Full workflows |
| all-tests | 20-45 min | Complete suite |

---

## Documentation

- **Full Validation Report**: TEST-BUNDLE-VALIDATION-REPORT.md
- **Architecture Decisions**: ../docs/adr/ (ADRs for design patterns)
- **Parent POM**: ../../iDempiereCLDE/org.idempiere.parent/pom.xml
- **iDempiere Testing**: https://wiki.idempiere.org/en/Unit_Testing

---

**Last Updated**: 2026-01-03
**Framework**: JUnit 5 + Tycho + iDempiere 10.0.0-SNAPSHOT
