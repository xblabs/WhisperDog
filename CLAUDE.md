---
title: Claude Desktop Instructions - Polymorphic Template
environment: claude-desktop
mcp_enabled: true
generated: 2025-12-31T03:34:40.420Z
---

# ScaffoldX Configuration for Claude Desktop
# ============================================
# IMPORTANT: This file is generated from templates - DO NOT EDIT DIRECTLY
# Edit: .scaffoldx/xcore/templates/core_instructions/claude-desktop.template.md

SCAFFOLDX_REPO_ROOT=C:\__dev\_projects\whisperdog
REPO_ROOT=C:\__dev\_projects\whisperdog
PROJECT_NAME=ScaffoldX
USER_NAME=User

# ======================
# CLAUDE DESKTOP SPECIFIC SETUP
# ======================

## MCP (Model Context Protocol) Configuration

Claude Desktop uses MCP for file system operations through Desktop Commander.
This provides powerful file manipulation capabilities while maintaining security.

### MCP Hierarchy - STRICT PRIORITY

ONLY use the Desktop Commander MCP for file operations:
- ✅ read_file, write_file, edit_block, list_directory, create_directory
- ✅ execute_command for script delegation when specified
- ❌ DO NOT use MySQL, Trello, REPL, or other MCP tools even if available
- ❌ DO NOT create chat artifacts - use file operations instead

### Token-Efficient Operations

**ALWAYS prioritize edit_block over write_file:**
- edit_block: Shows only the diff (minimal tokens)
- write_file: Shows entire file content (maximum tokens)

# ======================
# CONTEXT LOADING CONFIGURATION
# ======================

<!-- Context Loading: Based on .scaffoldx/xconfig/context.yaml -->
<!-- nlp: minimal | layer2: SUMMARY | commands: pareto -->

# ======================
# LAYER 1: CORE FRAMEWORK RULES
# ======================

<!-- Layer 1 Core: Always loaded (bootstrap + config-dependent modules) -->
# ScaffoldX Minimal Bootstrap - claude_desktop

**Environment**: claude_desktop
**Repo**: C:\__dev\_projects\whisperdog
**User**: User

---

## Core Framework Identity

**ScaffoldX**: AI-assisted development scaffold for project management and execution.

**Markdown-First**: .md files ARE executable specifications, NOT documentation.

**Two-Layer Architecture**:
- **LLM Layer**: Reasoning, content generation, decisions
- **Script Layer**: Deterministic operations (100x+ speed)

---

## Core Philosophy (Evolution, Not Revolution)

**CRITICAL**: These principles have ALWAYS been ScaffoldX's foundation. We're making them explicit, not introducing them.

### Contextual Expansion
Load information on-demand, when you need it, not upfront.

**How it works**:
- Start with minimal context (this bootstrap)
- When you encounter expansion triggers, load specific files
- Progressive disclosure: summary → details → deep dive
- Natural references guide you to additional context

**Why it matters**: Keeps token usage minimal while maintaining full access to knowledge.

### Traversability
Navigate context naturally through semantic understanding, not algorithmic path calculation.

**How it works**:
- Read expansion triggers as natural language
- Decide what to load based on comprehension (not keyword matching)
- Follow file references when semantically relevant
- Explore tangential context when it makes sense

**Why it matters**: Your understanding determines what's relevant, not pre-computed scores.

### The MCP Analogy (AI-GOV-013)

**Think of yourself as an MCP HOST, delegated executors as MCP SERVERS**

ScaffoldX follows the Model Context Protocol pattern:

| Component | MCP Term | Your Role |
|-----------|----------|-----------|
| **Command Definitions (.md)** | Protocol Specs | YOU read and interpret |
| **Delegated Executors (.js)** | Servers | Return raw resources |
| **You (AI)** | Host | Decide everything, interpret all data |
| **Payloads** | Requests | YOU craft with all parameters |
| **JSON Responses** | Resources | YOU interpret and present |

**Delegated Executors Should Do** (Deterministic, like MCP servers):
- Scan filesystems → return raw file lists
- Read JSON files → return raw content
- Count tokens (math: chars / 4)
- Generate sequential IDs
- Create directory structures
- Output raw data as JSON (no interpretation)

**YOU (AI/LLM) Should Do** (Semantic, like MCP host):
- Read command definitions (.md) to understand what's possible
- Decide what's relevant and significant
- Recognize patterns in meaning
- Write natural prose
- Decide what context to load
- Craft payloads for executors
- Interpret raw responses from executors
- Present results to user

**CRITICAL - AI-GOV-013 Violations**:
- ❌ Executors deciding what's "relevant", "critical", or "important"
- ❌ Executors filtering/presenting data for you
- ❌ Executors making semantic judgments
- ❌ YOU accepting pre-interpreted data without question

**Think**: If an MCP server wouldn't do it, a delegated executor shouldn't either.

**See**: `.scaffoldx/xcore/xcontext/architecture/mcp-analogy-mental-model.md` for complete explanation.

---

## Command Processing Basics

### x-Command Execution - NON-NEGOTIABLE

**NO EXCEPTIONS. NO ANALYSIS. NO QUESTIONS.**

When you see: `x-<command>`
IMMEDIATELY:
1. Check if command is in Pareto set (Pattern Mappings section)
   - If YES: Execute from memory (zero file I/O)
   - If NO: Proceed to step 2
2. READ (not execute) `.scaffoldx/xcustom/commands/x-<command>.md` (fallback: `.scaffoldx/xcore/commands/`)
   → This is a SPECIFICATION for you to interpret, like an MCP protocol spec
   - **IF FILE NOT FOUND** → Proceed to step 3
3. **COMMAND NOT FOUND PROTOCOL** (AI-GOV-024):
   - ❌ DO NOT invent behavior
   - ❌ DO NOT create directories or files speculatively
   - ❌ DO NOT guess what the command should do
   - ✅ STOP and ASK: "Command `x-<name>` not found. Should I create it, or did you mean something else?"
   - Only proceed after user confirms
4. DECIDE based on spec: Does it delegate to an executor?
5. IF delegated: Extract executor path from `**Script Delegation**`
6. CRAFT payload with all parameters
7. INVOKE executor: `node {executor}` with payload
8. INTERPRET raw JSON response
9. PRESENT result to user

**This is not optional. This is not a suggestion. This happens BEFORE any analysis, explanation, or context consideration.**

**Remember**: .md files are specifications YOU read. .js executors are MCP-server-like tools YOU invoke.

---

## Critical Governance (Always Enforced)

### Checklist Discipline (AI-GOV-012)
✅ **Read checklist FIRST** when resuming task - `.scaffoldx/xtasks/<task-id>/03_checklist.md`
✅ **Update checklist BEFORE commit** - Check [x] all completed items
✅ **Work sequentially** - Resume from first unchecked [ ] item

### File System Protection
❌ **NEVER modify .env files** - READ-ONLY with redaction
❌ **NEVER create task directories directly** - Use `x-task-create` command
❌ **NEVER modify archived content** - Archives are read-only
❌ **NEVER edit framework files** - Use x-commands for modifications

### Security Rules
✅ Always redact API keys (patterns: `sk-*`, `pk-*`, `AIza*`)
✅ Never expose passwords or tokens in responses
✅ Scan for credentials before displaying any content
✅ Protected files: `.env`, `credentials.json`, SSH keys

### Token Efficiency
✅ Use `edit_block` over `write_file` (99% token savings)
✅ Use `read_file` with offset/length for targeted reads
✅ Delegate deterministic operations to executors (when command definition specifies)

### Command Usage Logging (AI-GOV-007)
✅ **After successful x-* command execution** - Log silently for optimization
✅ **Fire-and-forget** - Don't announce, don't wait, don't fail on log errors
✅ **Privacy-safe** - Log command names only, not arguments or content
✅ **Enables adaptation** - Feeds `x-pareto-optimize` for personalized system

**Quick log**:
```bash
node .scaffoldx/xcore/scripts/log-command-usage.js log '{"command":"x-task-create","resolution_method":"EXPLICIT","confidence":1.0}'
```

---

## Sentinel System - Domain Detection

### Root Sentinel Registry

**Task Domain** (`task_sentinel.md`):
- Triggers: `x-task-*` commands, task ID (4 digits), "task" + action verb
- Loads: Task lifecycle, governance, creation rules

**Security Domain** (`security_sentinel.md`) - **IMMEDIATE PRIORITY**:
- Triggers: `.env` mention, "API key", "password", "secret", "token"
- Loads: Enhanced security monitoring, credential detection patterns

**File Operations Domain** (`file_ops_sentinel.md`):
- Triggers: File paths mentioned, file verbs (read/write/edit/delete)
- Loads: File operation governance, MCP best practices

**Git Domain** (`git_sentinel.md`):
- Triggers: `x-git-*` commands, "commit", "push", "branch"
- Loads: Git workflows, commit message conventions

**Insight Domain** (`insight_sentinel.md`):
- Triggers: "weird", "unexpected", "strange", surprise expressions
- Loads: Learning system, insight capture templates

### How Sentinels Work

```
1. User input arrives
2. Root sentinel scans for domain patterns
3. Match found → Load domain sentinel file
4. Domain sentinel activates specific governance
5. Additional context loaded on-demand within domain
```

### CRITICAL: Sentinel Loading Instructions

**IMMEDIATE ACTION REQUIRED** - When patterns detected, immediately load sentinels:

1. **Task Pattern Detected** → IMMEDIATELY read `.scaffoldx/xcore/internal/sentinels/domains/task_sentinel.md`
   - Triggers: `x-task-*`, 4-digit task IDs, "task" + action verb

2. **Security Pattern Detected** → IMMEDIATELY read `.scaffoldx/xcore/internal/sentinels/domains/security_sentinel.md`
   - Triggers: `.env`, "API key", "password", "secret", "token"
   - **PRIORITY**: Always load first when detected

3. **File Operation Pattern** → IMMEDIATELY read `.scaffoldx/xcore/internal/sentinels/domains/file_ops_sentinel.md`
   - Triggers: file paths, read/write/edit/delete operations

4. **Git Pattern Detected** → IMMEDIATELY read `.scaffoldx/xcore/internal/sentinels/domains/git_sentinel.md`
   - Triggers: `x-git-*`, "commit", "push", "branch"

5. **Insight Pattern Detected** → IMMEDIATELY read `.scaffoldx/xcore/internal/sentinels/domains/insight_sentinel.md`
   - Triggers: "weird", "unexpected", "strange", surprise expressions

**Example Flow**:
```
User: "x-task-create New Feature"
    ↓
Root detects: "x-task-create" pattern
    ↓
AI: read_file(".scaffoldx/xcore/internal/sentinels/domains/task_sentinel.md")
    ↓
Task sentinel provides: TASK-GOV-001 rules + creation patterns
    ↓
Execute with full context
```

---

## Session Initialization & Layer 2 Context

### SessionStart Behavior

**IMMEDIATE ACTIONS** when starting a new session:

1. **Generate Layer 2 Context**:
   ```
   node .scaffoldx/xcore/scripts/x-context-reindex.js
   ```
   - Creates `.scaffoldx/xcontext/current_project_context.md`
   - Loads current task, recent session summaries, active domains

2. **Load Generated Context**:
   ```
   read_file(".scaffoldx/xcontext/current_project_context.md")
   ```
   - Provides current work awareness
   - Shows relevant commands and project knowledge

3. **Monitor for Sentinel Triggers**:
   - Watch user input for domain patterns
   - Load appropriate sentinels when triggered
   - Update Layer 2 when domains activate

### Task Switch Updates

When user switches tasks (`x-task-switch`):
1. Re-run Layer 2 generation script
2. Load updated context
3. Adjust sentinel sensitivity to new domain

---

## Environment Capabilities

**File Operations**: MCP Desktop Commander
**MCP Enabled**: true
**Native Operations**: Limited shell via execute_command

### Environment-Specific Notes
- Use Desktop Commander for all file operations
- MCP provides secure file system access
- Shell commands via execute_command only

---

*Minimal Bootstrap v2.0 - Dynamic loading via sentinels*
*Size: ~190 lines | <6KB | 88% reduction from v1.0*


<!-- Config-dependent Layer 1 modules -->

<!-- Layer 1 Commands: On-Demand Loading (minimal strategy) -->

**Command Discovery**: When you need to execute a ScaffoldX command:
1. Check .scaffoldx/xcore/internal/command_system/patterns/pareto_patterns.md for common patterns
2. Use `./sx.ps1 help` or `./sx.sh help` to see available commands
3. Load specific command docs from .scaffoldx/xcore/internal/commands/ as needed


<!-- Layer 1 Governance: Essential Rules -->

## File Operation Governance

# File Operations Governance

## CRITICAL: Path Access Validation - MANDATORY BLOCKING CHECK

**This MUST be checked BEFORE any other file operation rules.**

### Pre-Operation Path Validation

**BEFORE ANY file operation**:
1. **VERIFY** the target path is within the configured REPO_ROOT
2. **CHECK** MCP access to the target directory
3. **IF MCP cannot access REPO_ROOT or target path**:
   - **STOP IMMEDIATELY** - This is a BLOCKING condition
   - **DO NOT** fall back to any other path (especially not ScaffoldX_Dev)
   - **DO NOT** use alternative locations as "workaround"
   - **WARN** the user with explicit message:
     ```
     ❌ CRITICAL: Cannot access project path: [path]
        MCP restrictions prevent operations outside: [allowed_paths]
        NO file operations will be performed.
     ```

**See**: `.scaffoldx/xcore/internal/governance/patterns/critical/PATH-ACCESS-001.md` for full pattern

## Critical Rules

1. **NEVER modify .env files** - Read only with redaction
2. **Use x-task-create for task directories** - Never create directly
3. **Archive files are read-only** - No modifications allowed
4. **Framework files need permission** - Direct edits require explicit approval

## Token Efficiency

- Always use `edit_block` for modifications (saves 90% tokens)
- Use `read_file` with offset/length for targeted reads
- Check for scripts before bulk operations
- Never rewrite entire files for small changes

## Decision Flow

```
Need to modify file?
├─ File exists?
│  ├─ YES → Use edit_block
│  └─ NO → Use write_file
└─ Need to read file?
   ├─ Know location? → read_file with offset
   └─ Don't know? → search_code first
```


## Additional Governance

For detailed governance rules, load on-demand from:
- .scaffoldx/xcore/internal/governance/ (full governance rules)
- Critical rules: file_operations.md, command_discovery_guard.md


<!-- Layer 1 Sentinels: Progressive Loading Triggers -->

# ScaffoldX Sentinel Registry (Layer 1)
# Pattern hints for progressive domain loading

## Root Sentinel Patterns

### Task Domain Triggers
**Patterns**: `x-task-*`, 4-digit task IDs, "task" + action verb
**Sentinel**: `task_sentinel.md`
**Loads**: Task lifecycle, governance, creation rules
**Priority**: HIGH

**Example Triggers**:
- `x-task-create New Feature`
- `Task 0308 is complete`
- `Let's switch to task 0280`

### Security Domain Triggers
**Patterns**: `.env`, "API key", "password", "secret", "token"
**Sentinel**: `security_sentinel.md`
**Loads**: Enhanced security monitoring, credential detection
**Priority**: IMMEDIATE

**Example Triggers**:
- `I need to update my .env file`
- `What's my API key for...`
- `The password should be...`

### File Operations Domain Triggers
**Patterns**: File paths mentioned, file verbs (read/write/edit/delete)
**Sentinel**: `file_ops_sentinel.md`
**Loads**: File operation governance, MCP best practices
**Priority**: HIGH

**Example Triggers**:
- `Edit the file at /path/to/file.md`
- `Read the configuration from config.json`
- `Delete the old backup files`

### Git Domain Triggers
**Patterns**: `x-git-*`, "commit", "push", "branch", merge
**Sentinel**: `git_sentinel.md`
**Loads**: Git workflows, commit message conventions
**Priority**: MEDIUM

**Example Triggers**:
- `x-git-commit "Fixed bug"`
- `I need to push these changes`
- `Create a new branch for feature-x`

### Insight Domain Triggers
**Patterns**: "weird", "unexpected", "strange", surprise expressions
**Sentinel**: `insight_sentinel.md`
**Loads**: Learning system, insight capture templates
**Priority**: MEDIUM

**Example Triggers**:
- `That's weird, it should work`
- `Unexpected behavior here`
- `This is strange, let me capture this insight`

## Sentinel Loading Instructions

### Immediate Actions Required
When patterns detected in user input:

1. **SECURITY** → Load `.scaffoldx/xcore/internal/sentinels/domains/security_sentinel.md`
2. **TASK** → Load `.scaffoldx/xcore/internal/sentinels/domains/task_sentinel.md`
3. **FILE OPS** → Load `.scaffoldx/xcore/internal/sentinels/domains/file_ops_sentinel.md`
4. **GIT** → Load `.scaffoldx/xcore/internal/sentinels/domains/git_sentinel.md`
5. **INSIGHT** → Load `.scaffoldx/xcore/internal/sentinels/domains/insight_sentinel.md`

### Loading Protocol
```
User Input → Pattern Match → Load Sentinel → Apply Governance → Execute with Context
```

### Example Flow
```
User: "x-task-create New Feature"
     ↓
Pattern: "x-task-create" matches TASK domain
     ↓
AI: read_file(".scaffoldx/xcore/internal/sentinels/domains/task_sentinel.md")
     ↓
Apply: TASK-GOV-001 rules + creation patterns
     ↓
Execute: Create task with full context awareness
```

## Progressive Loading Benefits

### Token Efficiency
- **Upfront**: Only essential patterns (~2KB)
- **On-demand**: Domain-specific context when needed
- **Total**: 94% reduction from full context loading

### Performance
- **Fast startup**: Minimal context loaded initially
- **Smart expansion**: Load relevant domains automatically
- **Contextual awareness**: Right information at right time

## Configuration Integration

### Loading Strategies
- **minimal**: Sentinel registry only (this file)
- **pareto**: + top commands + governance
- **sentinel**: + domain sentinels on trigger
- **full**: All patterns upfront (legacy)

### Config Location
- **YAML**: `.scaffoldx/xconfig/context.yaml`
- **JSON**: `.scaffoldx/xconfig/context-loading-config.json`

*Sentinel Registry - Smart pattern detection for efficient context loading*


# ======================
# LAYER 2: PROJECT-SPECIFIC CONTEXT
# ======================

<!-- Layer 2: Project context (loaded based on layer2 config setting) -->
---
title: Layer 2 Context - BOOTSTRAP MODE
generated: 2025-12-26T02:16:08.495Z
level: BOOTSTRAP
description: Minimal context for new project initialization
---

# Layer 2 Project Context (BOOTSTRAP)

**⚠️ BOOTSTRAP MODE ACTIVE**

This is a minimal context generated automatically because no prose context exists yet.
After this session, run `node .scaffoldx/xcore/scripts/x-instruction-build.js --full`
to generate complete project context with LLM assistance.

## Project Information

- **Name**: whisperdog
- **Path**: C:\__dev\_projects\whisperdog
- **Git Enabled**: Yes
- **Package.json**: Yes
- **Tasks**: 0 task(s)

## Quick Start Commands

Once ScaffoldX is initialized, you can use these commands:

- `x-task-create <name>` - Create a new task
- `x-task-list` - List all tasks
- `x-session-save` - Save current session
- `x-help` - Get help with commands

## Next Steps

1. **Initialize Context**: Ask the AI to run `x-context-reindex`
2. **Generate Prose Context**: AI will create proper Layer 2 context
3. **Rebuild Instructions**: Run `x-instruction-build --full`

---

*Bootstrap context generated by ScaffoldX initialization*


# ======================
# LAYER 3: CLAUDE BEHAVIORAL TWEAKS
# ======================

## Claude Desktop Specific Behaviors

### Session Management
- Load most recent session summary at start
- Save session summaries to .scaffoldx/xmemory/session_summaries/
- Use Desktop Commander for all file operations

### Command Processing
- When user types x-command, read definition from .scaffoldx/xcore/commands/
- Check for Script Delegation in command definition
- If present, execute script via execute_command()
- Otherwise, implement using MCP operations

### File Operations
- NEVER modify .env files directly
- Use edit_block for modifications (99% token savings)
- Use write_file only for new files
- Always check governance rules before operations

### Performance Optimizations
- Use scripts for deterministic operations (100x+ speed)
- Batch file operations when possible
- Cache frequently accessed data in memory
- Use targeted reads with offset/length

### Security Rules
- Redact sensitive information when displaying
- Never expose API keys or credentials
- Check for .env in .gitignore
- Suggest environment variables over hardcoded values

## Natural Language Command Resolution (NLP)

### Overview
You have a three-tier system for resolving natural language to x-commands:
- **Tier 1 (80%)**: Patterns in Pareto command set (instant, already loaded)
- **Tier 2 (15%)**: Domain-specific patterns (load on-demand)
- **Tier 3 (5%)**: Fuzzy matching for typos (call script)

### How to Use the Three Tiers

#### Tier 1: Check Pareto Patterns First
Look at the natural language patterns listed with each command in the Pareto set in CLAUDE.md.

**Example**:
```
User: "continue from transition TRANS_2025_10_24_001"
You: Check x-session-continue patterns
You: Match "continue from transition {id}"
You: Execute x-session-continue --transition TRANS_2025_10_24_001
```

#### Tier 2: Load Domain Patterns if Tier 1 Misses
If no Pareto match, load the command NLP sentinel and then appropriate shard:

**Example**:
```
User: "archive all completed tasks"
You: No Pareto match
You: read_file(".scaffoldx/xcore/internal/sentinels/domains/command_nlp_sentinel.md")
You: Detect "archive" keyword → task domain
You: read_file(".scaffoldx/xcore/nlp/domains/task_nlp.md")
You: Match pattern and execute
```

#### Tier 3: Use Fuzzy Matching for Typos
When Tiers 1 & 2 fail and typos are suspected:

**Example**:
```
User: "creat a taks for auth"
You: No match in Tiers 1 & 2, obvious typos
You: Bash("node .scaffoldx/xcore/scripts/x-nlp-resolve.js 'creat a taks for auth'")
Result: {
  "success": true,
  "command": "x-task-create",
  "parameters": { "name": "auth" },
  "corrections": { "creat": "create", "taks": "task" }
}
You: Execute x-task-create auth
```

### Important NLP Rules

1. **You make ALL semantic decisions** - Scripts only match text
2. **Try tiers in order** - 1 → 2 → 3
3. **Preserve parameters** - Extract and pass them correctly
4. **Log resolution** - Track which tier resolved the command using NLP_TIER_1, NLP_TIER_2, or NLP_TIER_3
5. **Fall back gracefully** - Suggest explicit format if all tiers fail

### Common Patterns Reference

Most frequent natural language patterns you'll encounter:

- "continue from transition X" → x-session-continue --transition X
- "create task for Y" → x-task-create Y
- "switch to task NNNN" → x-task-switch NNNN
- "list all tasks" → x-task-list
- "commit changes" → x-git-commit
- "save session" → x-session-save
- "help with X" → x-help --command X
- "archive completed tasks" → x-task-archive --completed
- "what's the status" → x-task-status
- "capture insight X" → x-insight-capture X

## Critical Reminders
- This is a MARKDOWN-FIRST framework
- Commands are defined in markdown, not code
- Use MCP for all file operations
- Never create artifacts in chat
- Always save important information to files

# ======================
# USER CUSTOMIZATIONS (PROTECTED)
# ======================

<!-- BEGIN PROTECTED: user-space -->
<!-- Add your custom Claude Desktop instructions here -->
<!-- This section will be preserved during template regeneration -->

<!-- Example customizations:
- Project-specific workflows
- Preferred tool usage patterns
- Custom security rules
- Environment-specific configurations
-->

<!-- END PROTECTED: user-space -->

---
Generated from template: claude-desktop.template.md
Build timestamp: 2025-12-31T03:34:40.420Z
ScaffoldX version: 1.0.0
