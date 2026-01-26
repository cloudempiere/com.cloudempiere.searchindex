# Eclipse Maven Refresh After Release

The release process updated version numbers in all POMs. Eclipse needs to refresh its project configuration.

## Option 1: Eclipse UI (Recommended)

1. **Select all projects** in Project Explorer:
   - com.cloudempiere.searchindex
   - com.cloudempiere.searchindex.ui
   - com.cloudempiere.searchindex.test
   - com.cloudempiere.searchindex.feature
   - com.cloudempiere.searchindex.p2
   - com.cloudempiere.searchindex.parent

2. **Right-click** → **Maven** → **Update Project...**

3. In the dialog:
   - ✅ Check "Select All"
   - ✅ Check "Force Update of Snapshots/Releases"
   - ✅ Check "Update project configuration from pom.xml"
   - Click **OK**

4. Wait for Eclipse to rebuild (check progress in bottom-right corner)

## Option 2: Command Line

If Eclipse UI doesn't work, run from terminal:

```bash
cd /Users/developer/GitHub/com.cloudempiere.searchindex

# Clean and rebuild
mvn clean install -DskipTests

# Refresh Eclipse projects
mvn eclipse:eclipse
```

Then in Eclipse:
- **File** → **Refresh** (F5) on all projects

## Option 3: Nuclear Option (If Still Broken)

```bash
# Remove Eclipse metadata
find . -name ".project" -delete
find . -name ".classpath" -delete
find . -name ".settings" -type d -exec rm -rf {} +

# Rebuild Maven projects
mvn clean install -DskipTests
mvn eclipse:eclipse

# Re-import in Eclipse:
# File → Import → Existing Maven Projects
# Select: /Users/developer/GitHub/com.cloudempiere.searchindex
# Import all 6 modules
```

## Verification

After refresh, these errors should disappear:
- ❌ "Could not read maven project"
- ❌ "Project configuration is not up-to-date with pom.xml"

All modules should show version **10.1.0** in:
- MANIFEST.MF (Bundle-Version: 10.1.0.qualifier)
- pom.xml (version: 10.1.0)
- feature.xml (version: 10.1.0)

---

**Note**: This is normal after version updates. Eclipse's Maven integration needs to re-read the POM files.
