# Documentation Governance

**Purpose:** Rules and standards for organizing documentation in the CloudEmpiere REST API project

**Last Updated:** 2025-12-19

---

## 1. Naming Conventions

### File Naming Standard

**Rule:** All documentation files MUST use `lowercase-kebab-case.md`

**Examples:**
- ✅ `auth-api-reference.md`
- ✅ `quickstart.md`
- ✅ `metadata-specification.md`
- ❌ `API_REFERENCE.md`
- ❌ `QuickStart.md`
- ❌ `metadataSpec.md`

### Descriptive Suffixes

Use clear suffixes to indicate document type:

| Suffix | Purpose | Example |
|--------|---------|---------|
| `-reference` | API endpoint reference | `auth-api-reference.md` |
| `-architecture` | Technical architecture docs | `auth-architecture.md` |
| `-specification` | Technical specifications | `metadata-specification.md` |
| `-overview` | High-level overviews | `oauth2-plugin-overview.md` |
| `-guide` | How-to guides | `frontend-validation-guide.md` |
| `-template` | Reusable templates | `strategic-review-template.md` |

### Special Files

- `README.md` - Directory index (UPPERCASE by convention)
- `CHANGELOG.md` - Project changelog (UPPERCASE by convention)
- `CLAUDE.md` - AI assistant guidance (UPPERCASE by convention)

---

## 2. Directory Structure

### Core Structure

```
docs/
├── README.md                    # Main documentation index
├── quickstart.md               # Quick start guide
├── {topic}-{type}.md           # Core documentation files
│
├── guides/                     # User guides and how-tos
│   ├── README.md
│   └── {topic}-guide.md
│
├── adr/                        # Architecture Decision Records
│   ├── README.md
│   ├── 000-template.md
│   └── NNNN-{decision-title}.md
│
├── templates/                  # Reusable templates
│   ├── README.md
│   └── {name}-template.md
│
└── reports/                    # Historical reports
    ├── README.md
    ├── audits/
    └── code-reviews/
```

### Categorization Rules

**Root `docs/` - Core Documentation Only**
- Essential technical documentation
- High-level architecture
- Main API references
- Quick start guides

**`docs/guides/` - User Guides**
- Step-by-step instructions
- How-to guides
- Technical specifications for features
- Integration guides
- Maintenance documentation

**`docs/adr/` - Architecture Decisions**
- Follow ADR template format
- Use sequential numbering (0001, 0002, etc.)
- Document significant architectural choices

**`docs/templates/` - Templates**
- Reusable planning templates
- Use `-template` suffix
- Keep empty/example templates

**`docs/reports/` - Historical Reports**
- Audit reports (in `audits/`)
- Code review findings (in `code-reviews/`)
- Use date prefix: `YYYY-MM-DD-{topic}.md`

---

## 3. Document Lifecycle

### When to Create New Documentation

1. **Core Documentation** - New major feature or API endpoint
2. **Guide** - User-facing how-to or integration instructions
3. **ADR** - Significant architectural decision
4. **Report** - Audit or review findings

### When to Update Existing Documentation

- Feature changes affect documented behavior
- New examples or use cases discovered
- Errors or unclear sections identified
- Cross-references need updating

### When to Archive Documentation

- Deprecated features (move to `reports/`)
- Superseded decisions (mark in ADR, create new ADR)
- Historical audits (keep in `reports/`)

### When to Delete Documentation

- Duplicate content (merge into canonical location)
- Temporary analysis files (after incorporation)
- Obsolete content with no historical value

---

## 4. Content Quality Standards

### Required Elements

All documentation MUST include:
- **Title** (H1 header)
- **Purpose/Overview** (first paragraph)
- **Table of Contents** (if >3 sections)
- **Last Updated** date (at bottom)

### Optional Elements

Consider adding:
- **Target Audience**
- **Prerequisites**
- **Related Documents** (cross-references)
- **Examples** (code snippets, curl commands)
- **Diagrams** (architecture, flows)

### Writing Style

- **Be concise** - Short sentences, clear language
- **Use examples** - Code snippets over abstract explanations
- **Link related docs** - Create cross-references
- **Keep updated** - Remove outdated content
- **Use active voice** - "Create a token" not "A token is created"

---

## 5. Cross-Reference Management

### Internal Links

Always use **relative paths** from the linking document:

```markdown
<!-- From docs/README.md -->
[Quickstart](quickstart.md)
[ADR-0001](adr/0001-odata-hierarchy-filter-functions.md)
[URL Token Auth](guides/url-token-authentication.md)

<!-- From docs/adr/0001-*.md -->
[Metadata Specification](../metadata-specification.md)
[OData Guide](../guides/odata-hierarchy-filters-guide.md)

<!-- From docs/guides/{file}.md -->
[Auth Architecture](../auth-architecture.md)
[ADR-0008](../adr/0008-card-authentication-extension-grant.md)
```

### When Renaming Files

1. Update the file name
2. Search for references: `grep -r "old-name.md" docs/`
3. Update all cross-references
4. Test links locally
5. Commit with descriptive message

---

## 6. Review Process

### Before Committing Documentation

- [ ] File uses `lowercase-kebab-case.md` naming
- [ ] File is in correct directory
- [ ] Cross-references use correct relative paths
- [ ] Document has title and purpose
- [ ] Last updated date is current
- [ ] No broken links (test locally)
- [ ] README index updated (if new file)

### Periodic Reviews

**Quarterly:**
- Review documentation structure
- Identify outdated content
- Check for broken links
- Update cross-references

**When Adding Features:**
- Update relevant documentation
- Create ADR if architectural change
- Update API reference
- Add examples to guides

---

## 7. Common Patterns

### Creating a New Guide

```bash
# 1. Create file with proper naming
touch docs/guides/new-feature-guide.md

# 2. Use standard structure
cat > docs/guides/new-feature-guide.md <<'EOF'
# New Feature Guide

Brief description of what this guide covers.

## Table of Contents

## Overview

## Prerequisites

## Step-by-Step Instructions

## Examples

## Related Documentation

---

**Last Updated:** YYYY-MM-DD
EOF

# 3. Update guides/README.md
# 4. Update docs/README.md if needed
```

### Creating a New ADR

```bash
# 1. Copy template
cp docs/adr/000-template.md docs/adr/00XX-decision-title.md

# 2. Fill in template sections
# 3. Update docs/adr/README.md index
# 4. Update docs/README.md ADR list
```

### Archiving a Report

```bash
# Move audit report to reports/audits/
mv REPORT.md docs/reports/audits/YYYY-MM-DD-topic.md

# Update docs/reports/README.md
```

---

## 8. Enforcement

### Automated Checks (Future)

Consider adding pre-commit hooks to check:
- File naming conventions
- Broken internal links
- Required document elements
- Cross-reference validity

### Manual Review

When reviewing documentation PRs, check:
- Naming conventions followed
- Files in correct directories
- Cross-references updated
- README indexes updated
- No broken links

---

## 9. Migration Process

When finding non-compliant documentation:

1. **Analyze** - Read file, determine purpose and category
2. **Categorize** - Decide: core, guide, ADR, template, or report?
3. **Rename** - Apply naming conventions with descriptive suffix
4. **Relocate** - Move to correct directory
5. **Update References** - Fix all cross-references
6. **Update Indexes** - Update README files
7. **Verify** - Test links, check structure

---

## 10. Examples of Good Documentation

### Well-Organized

✅ `docs/quickstart.md` - Clear name, right location
✅ `docs/auth-api-reference.md` - Descriptive suffix
✅ `docs/guides/url-token-authentication.md` - Proper categorization
✅ `docs/adr/0008-card-authentication-extension-grant.md` - ADR format

### Needs Improvement

❌ `OAUTH2_IMPROVEMENTS.md` - UPPERCASE, unclear location
❌ `authentication.md` - Needs suffix (auth-architecture.md)
❌ `docs/CODE_REVIEW.md` - Should be in reports/code-reviews/

---

## Governance Evolution

This governance document should evolve with the project. When proposing changes:

1. Create ADR for significant governance changes
2. Update this document
3. Notify team of changes
4. Update tooling/automation if needed

---

**Maintained by:** CloudEmpiere Team
**Last Updated:** 2025-12-19
**Next Review:** 2026-03-19 (Quarterly)
