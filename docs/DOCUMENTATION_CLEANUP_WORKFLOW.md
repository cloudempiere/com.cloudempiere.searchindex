# Documentation Cleanup Workflow

**Purpose:** Step-by-step process for organizing complex folders with generated/messy markdown files

**Based on:** Proven pattern from docs/ reorganization (2025-12-18 to 2025-12-19)

---

## Overview

This workflow helps you systematically clean up documentation chaos using governance rules, categorization, and iterative refinement.

**Process:**
1. **Governance** - Establish naming and organization rules
2. **Discovery** - Find and analyze all files
3. **Categorize** - Sort files by purpose
4. **Rename** - Apply naming conventions
5. **Relocate** - Move to proper directories
6. **Refine** - Shrink, merge, iterate
7. **Verify** - Update references and indexes

---

## Phase 1: Governance (15 minutes)

### 1.1 Review Governance Rules

Read and understand: `docs/DOCUMENTATION_GOVERNANCE.md`

**Key Rules:**
- ✅ Use `lowercase-kebab-case.md` naming
- ✅ Use descriptive suffixes (`-reference`, `-architecture`, `-guide`, `-template`)
- ✅ Organize into clear directories
- ✅ Update all cross-references

### 1.2 Define Target Structure

Decide on directory structure for your cleanup target:

**Example:**
```
target-folder/
├── README.md                    # Index
├── core/                        # Core documentation
├── guides/                      # User guides
├── reference/                   # API references
├── templates/                   # Templates
└── archive/                     # Historical/deprecated
```

### 1.3 Create Structure

```bash
# Navigate to target folder
cd path/to/messy-folder

# Create directories
mkdir -p core guides reference templates archive
```

---

## Phase 2: Discovery (30 minutes)

### 2.1 Inventory Files

```bash
# Count files
find . -name "*.md" -type f | wc -l

# List all markdown files
find . -name "*.md" -type f ! -path "./.git/*" | sort > /tmp/md-files.txt

# Show file sizes
find . -name "*.md" -type f -exec wc -l {} + | sort -rn > /tmp/md-sizes.txt
```

### 2.2 Analyze Each File

For each file, capture:

```bash
# Preview first 20 lines
for file in *.md; do
  echo "=== $file ==="
  head -20 "$file" | grep -E "^#|Purpose:|Document Type:|Target Audience:"
  echo ""
done
```

**Document in analysis file:**
```markdown
# File Analysis

| File | Lines | Type | Purpose | Action |
|------|-------|------|---------|--------|
| API_DOC.md | 450 | Reference | API endpoints | Rename → api-reference.md, move → reference/ |
| auth.md | 800 | Architecture | Auth flows | Rename → auth-architecture.md, move → core/ |
| GUIDE.md | 300 | Guide | How-to | Rename → feature-guide.md, move → guides/ |
```

### 2.3 Create Analysis Document

```bash
# Create analysis file
cat > CLEANUP_ANALYSIS.md <<'EOF'
# Documentation Cleanup Analysis

**Date:** YYYY-MM-DD
**Target:** path/to/messy-folder
**Total Files:** XX

## Files Inventory

[Paste table from 2.2]

## Categorization

### Core Documentation (X files)
- file1.md → new-name.md (action)
- file2.md → new-name.md (action)

### Guides (X files)
- ...

### Reference (X files)
- ...

### Templates (X files)
- ...

### To Archive (X files)
- ...

### To Delete (X files)
- Duplicates
- Temporary analysis files
EOF
```

---

## Phase 3: Categorize (20 minutes)

### 3.1 Group by Purpose

Using your analysis, categorize each file:

**Categories:**
1. **Core** - Essential technical docs, architecture, specifications
2. **Guides** - How-to, tutorials, integration guides
3. **Reference** - API docs, endpoint lists, configuration
4. **Templates** - Reusable templates
5. **Archive** - Historical reports, deprecated docs
6. **Delete** - Duplicates, temporary files, obsolete content

### 3.2 Identify Actions

For each file, decide:
- **Keep** (needs rename/move)
- **Merge** (combine with another file)
- **Split** (break into multiple files)
- **Archive** (move to archive/)
- **Delete** (remove entirely)

### 3.3 Update Analysis Document

Add action column to your table:

```markdown
| File | Category | New Name | Action |
|------|----------|----------|--------|
| API_DOC.md | Reference | api-reference.md | Rename + Move to reference/ |
| AUTH.md | Core | auth-architecture.md | Rename + Keep in core/ |
| TEMP.md | Delete | - | Delete (temporary analysis) |
```

---

## Phase 4: Rename (15 minutes)

### 4.1 Apply Naming Conventions

**Pattern:** `{topic}-{type}.md`

**Types:**
- `-reference` - API/endpoint references
- `-architecture` - Technical architecture
- `-specification` - Technical specs
- `-overview` - High-level overviews
- `-guide` - How-to guides
- `-template` - Templates

**Example renaming:**
```bash
# Batch rename (dry run first!)
mv API_DOC.md api-reference.md
mv AUTH.md auth-architecture.md
mv GUIDE.md feature-guide.md
mv TEMPLATE.md planning-template.md
```

### 4.2 Verify Naming

```bash
# Check for non-compliant names
find . -name "*.md" -type f | grep -v "^[a-z0-9-]*\.md$"

# Should return empty if all comply
```

---

## Phase 5: Relocate (20 minutes)

### 5.1 Move Files to Categories

```bash
# Move files to target directories
mv api-reference.md reference/
mv auth-architecture.md core/
mv feature-guide.md guides/
mv planning-template.md templates/
mv old-report.md archive/
```

### 5.2 Create Directory READMEs

For each directory:

```bash
# Create index
cat > reference/README.md <<'EOF'
# Reference Documentation

API references and configuration documentation.

## Available References

- [API Reference](api-reference.md) - Complete API endpoints
- ...

---

**Last Updated:** YYYY-MM-DD
EOF
```

---

## Phase 6: Refine (30-60 minutes)

### 6.1 Shrink Content

For verbose files:
1. Remove redundant sections
2. Consolidate similar content
3. Move examples to separate files
4. Extract long code blocks

### 6.2 Merge Duplicates

Find overlapping content:

```bash
# Compare files
diff file1.md file2.md

# If similar, merge:
# 1. Read both files
# 2. Identify unique content
# 3. Merge into single file
# 4. Delete duplicate
```

### 6.3 Split Large Files

For files >1000 lines:
1. Identify logical sections
2. Create separate files
3. Link with cross-references
4. Update TOC

### 6.4 Iterate

Review each file:
- Clear title?
- Concise content?
- Good examples?
- Cross-references correct?
- Up-to-date?

---

## Phase 7: Verify (20 minutes)

### 7.1 Update Cross-References

Find all references to renamed/moved files:

```bash
# Search for old file names
grep -r "old-filename\.md" . --include="*.md"

# Update each reference
# From: [Link](old-filename.md)
# To:   [Link](../reference/new-filename.md)
```

### 7.2 Update Indexes

Update all README.md files:
1. Root README - main index
2. Category READMEs - section indexes
3. Cross-references between categories

### 7.3 Test Links

```bash
# Check for broken links (manual)
# 1. Open each README in viewer
# 2. Click all links
# 3. Verify they resolve

# Or use automated link checker
find . -name "*.md" -exec grep -l "](.*\.md)" {} \;
```

### 7.4 Cleanup Temporary Files

```bash
# Delete analysis files
rm CLEANUP_ANALYSIS.md

# Delete backups
rm *.bak

# Verify structure
tree -L 2
```

---

## Phase 8: Document & Commit (15 minutes)

### 8.1 Create Summary

```markdown
# Documentation Cleanup Summary

**Date:** YYYY-MM-DD
**Target:** path/to/folder

## Changes Made

### Files Renamed (X files)
1. OLD_NAME.md → new-name.md

### Files Relocated (X files)
1. file.md → category/file.md

### Files Merged (X pairs)
1. file1.md + file2.md → merged-file.md

### Files Deleted (X files)
1. duplicate.md (merged into X)
2. temp.md (temporary analysis)

### Final Structure

[Tree output or directory listing]

## Benefits
- ✅ Consistent naming
- ✅ Clear organization
- ✅ Updated cross-references
- ✅ X% reduction in file count
```

### 8.2 Commit Changes

```bash
# Stage changes
git add .

# Commit with descriptive message
git commit -m "docs: reorganize folder following governance rules

- Renamed X files to lowercase-kebab-case
- Moved files to category directories (core, guides, reference)
- Merged X duplicate files
- Updated all cross-references
- Created category README indexes

Ref: DOCUMENTATION_GOVERNANCE.md"
```

---

## Workflow Checklist

Use this checklist for each cleanup:

### Pre-Cleanup
- [ ] Read DOCUMENTATION_GOVERNANCE.md
- [ ] Define target directory structure
- [ ] Create directories

### Discovery
- [ ] Inventory all .md files
- [ ] Analyze file content and purpose
- [ ] Create CLEANUP_ANALYSIS.md

### Categorization
- [ ] Categorize each file
- [ ] Identify merge/split/delete candidates
- [ ] Plan renaming strategy

### Execution
- [ ] Rename files (lowercase-kebab-case)
- [ ] Relocate to correct directories
- [ ] Create directory READMEs
- [ ] Shrink verbose content
- [ ] Merge duplicates
- [ ] Split large files

### Verification
- [ ] Update all cross-references
- [ ] Update all README indexes
- [ ] Test all links
- [ ] Delete temporary files

### Documentation
- [ ] Create cleanup summary
- [ ] Commit with descriptive message
- [ ] Update governance if needed

---

## Time Estimates

| Phase | Time | Notes |
|-------|------|-------|
| Governance | 15 min | One-time setup |
| Discovery | 30 min | Depends on file count |
| Categorize | 20 min | Review each file |
| Rename | 15 min | Batch operations |
| Relocate | 20 min | Move and create READMEs |
| Refine | 30-60 min | Merge, split, iterate |
| Verify | 20 min | Update references, test links |
| Document | 15 min | Summary and commit |
| **Total** | **2.5-3 hours** | For ~20-30 files |

---

## Examples

### Successful Cleanups

**CloudEmpiere REST docs/ (2025-12-18 to 2025-12-19)**
- **Before:** 24 mixed files, inconsistent naming, scattered locations
- **After:** 32 organized files in 5 categories (core, guides, adr, templates, reports)
- **Actions:** Created 7 ADRs, renamed 14 files, created guides/ directory
- **Time:** ~3 hours total
- **Result:** Professional, discoverable structure

**OAuth2 Plugin docs/ (2025-12-18)**
- **Before:** 12 UPPERCASE files in plugin directory
- **After:** Integrated into main docs structure
- **Actions:** Created 2 ADRs, archived 2 reports, renamed 6 guides
- **Time:** ~2 hours
- **Result:** Complete OAuth2 documentation suite

---

## Tips for Success

1. **Start Small** - Clean one folder at a time
2. **Use Templates** - Follow DOCUMENTATION_GOVERNANCE.md patterns
3. **Iterate** - Multiple passes are OK
4. **Document** - Keep CLEANUP_ANALYSIS.md during process
5. **Test Links** - Broken references cause frustration
6. **Commit Often** - Save progress incrementally
7. **Ask for Help** - Review with team before major changes

---

## Maintenance

After cleanup, maintain quality:

**Weekly:**
- Check for new files in wrong locations
- Verify new files follow naming conventions

**Monthly:**
- Review cross-references for broken links
- Update outdated content

**Quarterly:**
- Full documentation review
- Update governance rules if needed
- Archive old reports

---

**Created:** 2025-12-19
**Last Used:** 2025-12-19
**Success Rate:** 100% (2 successful cleanups)

**Related:**
- [DOCUMENTATION_GOVERNANCE.md](DOCUMENTATION_GOVERNANCE.md) - Rules and standards
- [adr/README.md](adr/README.md) - ADR process
- [templates/](templates/) - Reusable templates
