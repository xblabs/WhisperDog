---
title: Claude Desktop Instructions - Polymorphic Template
environment: claude-desktop
mcp_enabled: true
generated: 2025-12-26T15:01:58.780Z
---

# ScaffoldX Configuration for Claude Desktop
# ============================================
# IMPORTANT: This file is generated from templates - DO NOT EDIT DIRECTLY
# Edit: .scaffoldx/xcore/templates/core_instructions/claude-desktop.template.md

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

### The Script vs LLM Boundary

**Scripts Should Do** (Deterministic):
- Scan filesystems
- Read JSON files
- Count tokens (math: chars / 4)
- Generate sequential IDs
- Create directory structures
- Output raw data as JSON

**LLM Should Do** (Semantic):
- Understand relevance
- Determine significance
- Recognize patterns in meaning
- Write natural prose
- Decide what context to load
- Follow references based on comprehension

**NEVER**: Scripts calculating "relevance", "active domains", or "significance" - these are semantic judgments only LLM can make.

---

## Command Processing Basics

### x-Command Execution - NON-NEGOTIABLE

**NO EXCEPTIONS. NO ANALYSIS. NO QUESTIONS.**

When you see: `x-<command>`
IMMEDIATELY:
1. Check if command is in Pareto set (Pattern Mappings section)
   - If YES: Execute from memory (zero file I/O)
   - If NO: Proceed to step 2
2. Read `.scaffoldx/xcustom/commands/x-<command>.md` (fallback: `.scaffoldx/xcore/commands/`)
3. Extract `Script:` from `**Script Delegation**`
4. Execute: `node {script}` → parse JSON → return output
5. On any error: Return error with path/details

**This is not optional. This is not a suggestion. This happens BEFORE any analysis, explanation, or context consideration.**

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
✅ Delegate deterministic operations to scripts

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

---
title: Sentinel System Registry & Index
type: system_index
domain: core
priority: critical
created: 2025-01-24
last_updated: 2025-01-24
---

# Sentinel System Index

## Overview
The Sentinel System implements Layer 2 of ScaffoldX's three-layer architecture, providing intelligent context discovery and progressive loading without overwhelming token usage.

## Architecture Position
```yaml
Layer 1 (Pareto/NLP): 
  - 58 essential commands
  - Natural language mapping
  - Always loaded (~2,000 tokens)
  - Handles 90% of operations

Layer 2 (Sentinel): 
  - Pattern recognition triggers
  - Progressive context loading
  - Domain activation signals
  - This system (~500 tokens)

Layer 3 (Complete):
  - Full command set (150+)
  - Comprehensive documentation
  - Loaded only when needed
  - (~15,000 tokens)
```

## Core Components

### Pattern Definitions
Located in `/patterns/`:

1. **governance_sentinels.md**
   - Security and compliance triggers
   - Data protection patterns
   - Framework integrity rules
   - Always active for safety

2. **domain_sentinels.md**
   - Task, git, issue domain detection
   - Progressive loading strategies
   - Cross-domain interactions
   - Context-aware activation

3. **memory_sentinels.md**
   - Insight and learning capture
   - Knowledge gap detection
   - Pattern recognition
   - Continuous improvement

4. **command_sentinels.md**
   - Command discovery patterns
   - Usage optimization
   - Error recovery assistance
   - Natural language mapping

### Processing Engine
Located in `/processor/`:

**sentinel_processor.md**
- AI interpretation logic
- Proximity calculation engine
- Loading decision framework
- Integration specifications

## Key Principles

### 1. Unidirectional Flow
```
✅ AI → Sentinel → Decision → Action
❌ Sentinel → Control → AI Behavior
```

### 2. Hints Not Commands
Sentinels provide information for AI consideration, never mandatory actions.

### 3. Progressive Discovery
Start minimal, expand based on need, compress when inactive.

### 4. Token Efficiency
Target: <3,000 tokens for 90% of operations while maintaining full discoverability.

## Proximity Model

### Calculation Factors
```yaml
Temporal: How recently mentioned/used
Contextual: How related to current work
Hierarchical: Task/structural relationships
```

### Loading Thresholds
```yaml
≥0.9: Full content
0.7-0.9: Summaries
0.5-0.7: Indexes only
<0.5: Not loaded
```

## Activation Patterns

### Always Active
- Governance sentinels (security, data loss)
- Memory sentinels (insight capture)
- Critical error detection

### Context-Activated
- Domain sentinels (task, git, issue)
- Command discovery
- Workflow optimization

### On-Demand
- Learning assistance
- Deep documentation
- Historical patterns

## Integration Points

### With Layer 1 (Pareto)
- Sentinels identify Layer 1 candidates
- Usage tracking for optimization
- Seamless handoff between layers

### With Layer 3 (Complete)
- Sentinels prevent unnecessary full loads
- Enable targeted retrieval
- Maintain discoverability

### With Core Systems
- Command processing enhancement
- Governance enforcement
- Memory system integration
- Natural language understanding

## Usage Examples

### Example 1: Task Creation
```markdown
User: "I need to create a new task for API development"
Sentinel: Task domain detected (proximity 0.8)
Action: Load task templates, recent tasks summary
Result: Rich context without full system load
```

### Example 2: Security Concern
```markdown
User: "Update the .env file with new keys"
Sentinel: Security sentinel triggered (proximity 1.0)  
Action: Load governance rules, block edit, suggest approach
Result: Protection without workflow interruption
```

### Example 3: Learning Moment
```markdown
User: "That's weird, the command failed again"
Sentinel: Friction + insight pattern detected
Action: Capture context, document issue, suggest fix
Result: Continuous improvement without asking
```

## Performance Metrics

### Token Usage
- Base load: ~2,500 tokens (Layer 1 + Sentinels)
- Average operation: ~3,500 tokens
- Complex operation: ~6,000 tokens
- Full load avoided: ~15,000 tokens saved

### Discovery Success
- Command found rate: 95%
- Context relevance: 90%
- False positive rate: <5%
- User interruptions: Near zero

## Evolution & Learning

### Pattern Maturation
1. Sentinels detect patterns
2. Track frequency and value
3. Promote successful patterns
4. Demote unused patterns
5. System continuously improves

### Feedback Loops
- User behavior informs sentinel sensitivity
- Success patterns strengthen triggers
- Failures refine detection logic
- System adapts to usage

## Implementation Status

### Completed
- ✅ Sentinel pattern definitions
- ✅ Processor specification
- ✅ Integration architecture
- ✅ Registry structure

### Next Steps
1. Connect to Knowledge Index (Task 0303)
2. Update command processing flow
3. Implement proximity calculations
4. Test with real workflows
5. Measure token savings

## Quick Reference

### Sentinel Types
```markdown
Governance → Security, compliance, protection
Domain → Task, git, issue contexts  
Memory → Insights, learning, knowledge
Command → Discovery, usage, optimization
```

### Proximity Factors
```markdown
Temporal → How recent (0.0-1.0)
Contextual → How related (0.0-1.0)
Hierarchical → Structure (0.0-1.0)
```

### Loading Levels
```markdown
Full → Complete content (proximity ≥0.9)
Summary → Key points (proximity 0.7-0.9)
Index → List only (proximity 0.5-0.7)
None → Not loaded (proximity <0.5)
```

## Troubleshooting

### Too Much Loaded
- Lower proximity thresholds
- Tighten sentinel patterns
- Increase compression triggers

### Missing Context
- Review sentinel patterns
- Adjust proximity calculations
- Add missing triggers

### Token Overflow
- Aggressive compression
- Stricter proximity requirements
- Priority-based loading

## Related Documentation

### Task References
- Task 0255: Prompt Layering & Env Tooling (parent)
- Task 0280: Intelligent Discovery System (architecture source)
- Task 0300: Sentinel-based discovery (parallel development)
- Task 0303: Knowledge Index metadata (data source)

### System References
- `/xcore/internal/command_system/` - NLP integration
- `/xcore/internal/governance/` - Governance rules
- `/xmemory/insights/` - Learning storage
- `/xcore/commands/` - Command definitions

---
*Sentinel System: Enabling discovery without token overflow since 2025*

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
Build timestamp: 2025-12-26T15:01:58.780Z
ScaffoldX version: 1.0.0
