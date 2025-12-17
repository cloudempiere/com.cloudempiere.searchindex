# Claude Code Configuration

This directory contains Claude Code agents and commands symlinked from the cloudempiere-workspace shared configuration.

## Structure

```
.claude/
├── agents/          → ../../cloudempiere-workspace/.claude/agents/
├── commands/        → ../../cloudempiere-workspace/.claude/commands/
├── settings.local.json  (project-specific settings)
└── README.md        (this file)
```

## Available Agents

### Symlinked from cloudempiere-workspace:

**Angular:**
- Located in `agents/angular/`
- Angular-specific development agents

**DevOps:**
- Located in `agents/devops/`
- Deployment, CI/CD, infrastructure agents

**Fullstack:**
- Located in `agents/fullstack/`
- Full-stack development agents

**iDempiere:**
- Located in `agents/idempiere/`
- iDempiere ERP-specific agents
- Includes: plugin development, OSGi, database, etc.

**Quality:**
- Located in `agents/quality/`
- Code review, testing, quality assurance agents

**Utilities:**
- Located in `agents/utilities/`
- General-purpose utility agents

## Available Commands (Slash Commands)

### Symlinked from cloudempiere-workspace:

Available slash commands:
- `/analyze-history` - Analyze git history and patterns
- `/angular-service` - Generate Angular service
- `/create-adr` - Create Architecture Decision Record
- `/create-doc` - Create documentation
- `/graphql-type` - Generate GraphQL type
- `/ingest-archive` - Ingest archive/backup files
- `/init-repo` - Initialize repository structure
- `/known-issues` - Document known issues
- `/linear` - Linear issue management
- `/project-status` - Generate project status report
- `/recap` - Session recap (decisions, flow, code)
- `/release` - Execute release workflow
- `/validate-adr` - Validate ADR compliance
- `/validate-docs` - Validate documentation

## Project-Specific Commands

This project has the following custom commands:

### `/recap`
- **Purpose:** Session recap with decisions, flow, vibe, and code
- **Location:** `.claude/commands/recap.md` (symlinked)
- **Usage:** `/recap`

### `/release`
- **Purpose:** Execute release workflow using release-manager agent
- **Location:** `.claude/commands/release.md` (symlinked)
- **Usage:** `/release`

## Settings

**Local Settings:** `.claude/settings.local.json`
- Project-specific Claude Code settings
- Not version controlled
- Overrides workspace settings for this project

## Maintenance

### Updating Symlinks

If the cloudempiere-workspace location changes, update symlinks:

```bash
# Remove old symlinks
rm .claude/agents .claude/commands

# Create new symlinks
ln -s ../../cloudempiere-workspace/.claude/agents .claude/agents
ln -s ../../cloudempiere-workspace/.claude/commands .claude/commands
```

### Adding Project-Specific Agents/Commands

**Option 1:** Add to cloudempiere-workspace (shared across all projects)
```bash
cd ../cloudempiere-workspace/.claude/agents/idempiere/
# Add your agent here
```

**Option 2:** Add locally to this project (project-specific only)
```bash
# Create local override directory
mkdir -p .claude/local-agents
mkdir -p .claude/local-commands

# Add your agent/command
# Note: Update .gitignore if needed
```

## Verification

Test that symlinks are working:

```bash
# List agents
ls -la .claude/agents/

# List commands
ls -la .claude/commands/

# Test a command
# (in Claude Code CLI)
/recap
```

## Shared Configuration Benefits

**Advantages of symlinking from cloudempiere-workspace:**

1. **Consistency** - Same agents/commands across all cloudempiere projects
2. **Maintenance** - Update once, applies everywhere
3. **Efficiency** - No duplication of configuration
4. **Version Control** - Centralized agent/command version management

**When to use local configs:**
- Project has unique requirements
- Testing experimental agents
- Temporary overrides

## Related Documentation

- Main project docs: `docs/`
- Architectural analysis: `docs/ARCHITECTURAL-ANALYSIS-2025.md`
- Implementation roadmap: `docs/implementation-plan/IMPLEMENTATION-ROADMAP-2025.md`
- AI strategy: `docs/AI-ENHANCED-SEARCH-STRATEGY-2025.md`

---

**Status:** ✅ Symlinks active and working
**Workspace:** cloudempiere-workspace
**Last Verified:** 2025-12-12
