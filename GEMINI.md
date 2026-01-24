---
title: ScaffoldX Configuration - Gemini CLI
environment: gemini
cli_access: true
generated: 2026-01-22T04:54:27.241Z
---

<!-- GENERATED FILE - DO NOT EDIT DIRECTLY
To modify Layer 2 (project-specific): Edit .scaffoldx/xcontext/_core_agent_instructions_layer_source.md
To modify Layer 1/3 (framework/behavioral): Edit templates in .scaffoldx/xcore/templates/core_instructions/
Then run: x-instruction-build -->

# ScaffoldX Configuration

SCAFFOLDX_REPO_ROOT=C:\__dev\_projects\whisperdog
REPO_ROOT=C:\__dev\_projects\whisperdog
PROJECT_NAME=whisperdog
USER_NAME=Henry

---

## Context Loading Configuration

<!-- Context Loading: Based on .scaffoldx/xconfig/context-loading-config.json -->
<!-- nlp: pareto | layer2: enabled | commands: common -->

---

## Gemini CLI Environment Setup

### Command Line Interface

Gemini provides direct shell access through CLI integration.
Full system command execution with terminal context.

#### CLI Capabilities

- Direct shell command execution
- File system operations
- Process management
- System-level access

#### Shell Execution

- Full bash/zsh/cmd access
- Script execution capabilities
- Pipeline and redirection support
- Background process management

---

## Layer 1: Core Framework Rules

# Base ScaffoldX Instructions

ScaffoldX is an AI-assisted development scaffold for project management.

## Core Principles
- Markdown-first architecture
- File-based state management
- Command-driven workflow
- Token efficiency

## Framework Structure
- Commands in `.scaffoldx/xcore/commands/`
- Tasks in `.scaffoldx/xtasks/`
- Memory in `.scaffoldx/xmemory/`
- Context in `.scaffoldx/xcontext/`


<!-- ScaffoldX Pareto Command Set (Layer 1) - Top 80% usage coverage with NLP patterns -->

### Universal Core Commands (Always Available)

### Task Management (Most Used)

x-task-create | create task {name}, make a task for {name}, new task {name}, add task {name}, start working on {name}
x-task-switch | switch to task {id}, work on task {id}, continue task {id}, focus on {id}, change to task {id}
x-task-list | list tasks, show all tasks, what tasks exist, display task list, show me the tasks
x-task-status | task status, current task status, what's the status, show task progress, where are we on the task
x-task-complete | complete task, mark task done, finish task, task is complete, done with task

### Git Operations (High Frequency)

x-git-commit | commit changes, git commit, save changes, commit with message {msg}, save my work
x-checkpoint-create | save session, checkpoint, save progress, create checkpoint, save my work, save session state
x-checkpoint-load | continue session, load checkpoint {id}, resume from checkpoint, pick up where I left off, restore checkpoint {id}

### Core System

x-help | help, show help, help with {cmd}, how do I use {cmd}, what commands are available
x-insight-capture | capture insight {insight}, save learning {insight}, record insight {insight}, add insight {insight}, document learning {insight}
x-checkpoint-create | create checkpoint, save checkpoint {type}, checkpoint this session, create save point, save session state, create milestone checkpoint

### Domain Commands (Loaded by Sentinel)

### Task Operations

x-task-start | start task {id}, begin task {id}, work on {id}, activate task {id}
x-task-pause | pause task, stop working, take a break, suspend task
x-task-resume | resume task {id}, continue paused task {id}, unpause {id}, restart task {id}
x-task-archive | archive task, archive completed tasks, move finished tasks to archive, cleanup completed work
x-task-activate | activate task {id}, make task {id} active, start draft {id}, enable task {id}
x-task-dependencies | show dependencies for {id}, add dependency {deps} to {id}, task {id} depends on {deps}, link tasks {id} and {deps}
x-task-consolidate | consolidate task {source} into {target}, merge task {source} with {target}, combine tasks {source} and {target}
x-task-improve | improve task {id}, enhance task {id}, optimize task {id}, refine task {id}
x-task-review | review task {id}, check task {id}, examine task {id}, audit task {id}

### Git Workflow

x-git-status | git status, show git changes, what changed, show modifications
x-git-push | push changes, git push, upload changes, sync to remote

### Knowledge Management

x-knowledge-extract | extract knowledge from {id}, get learnings from task {id}, harvest insights from {id}, document knowledge from {id}
x-knowledge-create | create knowledge {title}, document {title}, add knowledge article {title}, write documentation for {title}
x-knowledge-add | add knowledge to {id}, document learning in {id}, save insight to task {id}
x-archive-search | search archives for {query}, find in archive {query}, look up {query} in history, search old {query}

## Command Processing Rules

### NLP Resolution Flow
1. Check Pareto patterns (this file)
2. Load domain shards if needed
3. Use fuzzy matching for typos

### Script vs LLM Execution
- **Scripts handle**: File operations, ID generation, data processing
- **LLM handles**: Content creation, semantic decisions, pattern recognition

### Location Priority
1. Custom: `.scaffoldx/xcustom/commands/` (user overrides)
2. Core: `.scaffoldx/xcore/commands/` (framework defaults)

### Delegation Markers
- `SCRIPT: node path/to/script.js` - Execute script, process JSON result
- `LLM: direct` - Implement behavior in markdown specification

## Usage Logging (AI-GOV-007)
After successful command execution, log silently:
```bash
node .scaffoldx/xcore/scripts/log-command-usage.js log '{"command":"x-task-create","resolution_method":"NLP_TIER_1","confidence":1.0}'
```

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

```text
User: "load checkpoint CP_2025_10_24_001"
You: Check x-checkpoint-load patterns
You: Match "load checkpoint {id}"
You: Execute x-checkpoint-load CP_2025_10_24_001
```

#### Tier 2: Load Domain Patterns if Tier 1 Misses

If no Pareto match, load the command NLP sentinel and then appropriate shard:

**Example**:

```text
User: "archive all completed tasks"
You: No Pareto match
You: read_file(".scaffoldx/xcore/internal/sentinels/domains/command_nlp_sentinel.md")
You: Detect "archive" keyword -> task domain
You: read_file(".scaffoldx/xcore/nlp/domains/task_nlp.md")
You: Match pattern and execute
```

#### Tier 3: Use Fuzzy Matching for Typos

When Tiers 1 & 2 fail and typos are suspected:

**Example**:

```text
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
2. **Try tiers in order** - 1 -> 2 -> 3
3. **Preserve parameters** - Extract and pass them correctly
4. **Log resolution** - Track which tier resolved the command using NLP_TIER_1, NLP_TIER_2, or NLP_TIER_3
5. **Fall back gracefully** - Suggest explicit format if all tiers fail

### Common Patterns Reference

Most frequent natural language patterns you'll encounter:

- "load checkpoint X" -> x-checkpoint-load X
- "create task for Y" -> x-task-create Y
- "switch to task NNNN" -> x-task-switch NNNN
- "list all tasks" -> x-task-list
- "commit changes" -> x-git-commit
- "save checkpoint" -> x-checkpoint-create
- "help with X" -> x-help --command X
- "archive completed tasks" -> x-task-archive --completed
- "what's the status" -> x-task-status
- "capture insight X" -> x-insight-capture X

*Layer 1 Commands with NLP Patterns - 80% usage coverage*



<!-- Layer 1 Core Governance -->

### Framework Development Mode (AI-GOV-008)

**This Repository Context**: You are working ON ScaffoldX itself

### What This Means:
- **ALL .scaffoldx/ operations**: LLM-first approach
- **Sentinels**: Patterns YOU recognize, not JavaScript monitoring
- **Context loading**: YOU decide when to read files
- **Commands (x-*)**: Markdown specs YOU interpret

### Deviation Check:
Before suggesting ANY implementation, ask:
1. Am I in .scaffoldx/ directories? → LLM does it
2. Am I suggesting scripts calculate relevance? → STOP
3. Am I building "runtime monitoring"? → You ARE the runtime
4. Am I violating unidirectionality? → Scripts collect, LLM decides

### User Counter-Steering:
If user says these phrases, you've deviated:
- "That's a deviation"
- "LLM-first"
- "You ARE the runtime"
→ Immediately reframe your suggestion

### Exception:
If working in /src/ or /app/ (user project code) → Traditional implementation OK

---

## Semantic Boundary Detection (AI-GOV-010 Trigger)

**When user mentions ambiguous terms**, check if they mean ScaffoldX concepts:

### Quick Reference (Load full atoms if confusion)
- **sentinel** + "pattern/context" → ScaffoldX (load SEMANTIC_ATOMS.md)
- **task** + 4-digit ID → ScaffoldX
- **context** + "Layer 1/2" → ScaffoldX
- **command** + "x-" prefix → ScaffoldX
- **knowledge** + "/knowledge/" → ScaffoldX (task insights)
- **checkpoint** + "session state" → ScaffoldX
- **transition** + "TRANS_" → ScaffoldX
- **insight** + "capture" → ScaffoldX
- **issue** + "ISS-" → ScaffoldX

### When Ambiguous:
```
IF user term matches above patterns BUT context unclear
THEN: read_file(".scaffoldx/xcore/internal/governance/SEMANTIC_ATOMS.md")
```

**Full atom definitions**: 15+ concepts with boundaries, precedence, signatures
**Load only when**: Term collision or ambiguity detected

---

## Oracle Usage Enforcement (AI-GOV-009)

**NEVER reason about external state - ALWAYS invoke oracle**

### Scope
This rule applies to ALL operations involving:
- Date and time
- File system state
- Git repository state
- Project version information
- Any deterministic external data

### Prohibited Behavior
❌ **NEVER reason about**:
- Dates: "Today is probably...", "Based on the last session..."
- Files: "The file probably exists because...", "We just created it..."
- Git: "The branch is probably still...", "The repo should be clean..."

### Required Behavior
✅ **ALWAYS invoke oracle** for external state:
```bash
# For dates/times
node .scaffoldx/xcore/scripts/oracles/oracle-cli.js temporal getCurrentDate
node .scaffoldx/xcore/scripts/oracles/oracle-cli.js temporal getCurrentTimestamp

# For file operations
node .scaffoldx/xcore/scripts/oracles/oracle-cli.js filesystem fileExists "./path/to/file"
node .scaffoldx/xcore/scripts/oracles/oracle-cli.js filesystem getLastModified "./file"

# For project state
node .scaffoldx/xcore/scripts/oracles/oracle-cli.js project getBranchInfo
node .scaffoldx/xcore/scripts/oracles/oracle-cli.js project getRepoStatus
```

### Success Criteria
- Zero date errors in session logs
- Zero file assumption errors
- 90%+ oracle usage rate for deterministic queries

**Full Documentation**: `.scaffoldx/xcore/scripts/oracles/README.md`
**Governance Rule**: `.scaffoldx/xcore/internal/governance/AI-GOV-009_oracle_usage_enforcement.md`

---

## Checklist Discipline (AI-GOV-012)

### Resume Protocol
✅ **FIRST ACTION** when resuming any task:
- Read `.scaffoldx/xtasks/<task-id>/03_checklist.md`
- Find first unchecked `[ ]` item
- Resume work from that point

### Commit Protocol
✅ **BEFORE ANY COMMIT**:
- Update checklist with completed work `[x]`
- Verify all previous items remain checked
- Commit message includes task progress

### Sequential Execution
✅ **WORK ORDER**:
- Complete items in checklist order
- Never skip ahead
- Update checklist after each meaningful unit of work

---

## File System Protection (CRITICAL)

### Never Modify Files
❌ **PROHIBITED**: `.env` files (READ-ONLY, redact content)
❌ **PROHIBITED**: Framework files in `.scaffoldx/xcore/`
❌ **PROHIBITED**: Archived content (read-only by design)
❌ **PROHIBITED**: Direct task directory creation

### Use Commands Instead
✅ `x-task-create` - Creates properly structured task directories
✅ `x-env-sync` - Updates environment files safely
✅ `x-config-set` - Modifies configuration files

---

## Security Rules (IMMEDIATE PRIORITY)

### Credential Protection
❌ **NEVER expose**:
- API keys (patterns: `sk-*`, `pk-*`, `AIza*`)
- Passwords, tokens, secrets
- Private SSH keys

### Content Redaction
✅ **ALWAYS redact** sensitive content before displaying
✅ **ALWAYS scan** for credentials before file operations
✅ **PROTECTED FILES**: `.env`, `credentials.json`, SSH keys

---

## Command Usage Logging (AI-GOV-007)

### Implementation
✅ **AFTER successful x-* command execution**:
```bash
node .scaffoldx/xcore/scripts/log-command-usage.js log '{"command":"x-task-create","resolution_method":"EXPLICIT","confidence":1.0}'
```

### Requirements
✅ **Fire-and-forget**: Don't announce, don't wait, don't fail on errors
✅ **Privacy-safe**: Command names only, no arguments or content
✅ **Optimization data**: Enables `x-pareto-optimize` adaptation

---

## MCP File Operations Efficiency (AI-GOV-019 Trigger)

**CRITICAL**: Desktop Commander is third-party - minimize exposure

**Core Principle**:
- ✅ Scripts MUST use Node.js fs (self-sufficient, no MCP dependency)
- ❌ Scripts NEVER depend on MCP servers
- ✅ MCP is for Claude interactive work only (optional convenience)

**When Working With Files**:
- Large files (>10K lines): Check size first, use splice or delegate to script
- xcontext indexing: Scripts read frontmatter only (first 2KB)
- Interactive exploration: MCP get_file_info → check size → read safely

**Detailed Protocol**: Load on-demand when file operations mentioned
**File**: `.scaffoldx/xcore/internal/governance/patterns/ai/AI-GOV-019_mcp_file_operations_efficiency.md`

---

*Core governance - Always loaded, minimal weight, triggers deeper loading when needed*



<!-- ScaffoldX Sentinel Registry (Layer 1) - Pattern hints for progressive domain loading -->

### Root Sentinel Patterns

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


{{SESSION_PROTOCOL}}

---

## Layer 2: Project-Specific Context


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


---

## Layer 3: Gemini Behavioral Tweaks

### CLI Integration

- Direct shell command execution
- Use native OS commands for operations
- Respect shell environment variables
- Maintain shell session state

### Command Processing

- When user types x-command, read definition from .scaffoldx/xcore/commands/
- Execute scripts directly through shell
- Use native OS file operations
- Chain commands with pipes and redirects

### File Operations

- Use native OS commands (cp, mv, rm, etc.)
- Respect file permissions
- Handle symbolic links properly
- Support glob patterns

### Performance Optimizations

- Use shell built-ins when possible
- Leverage OS-level caching
- Parallel execution with background jobs
- Stream processing for large files

### Gemini-Specific Features

- Multi-modal understanding (text + images)
- Long context window support
- Advanced reasoning capabilities
- Code execution in multiple languages

### Shell Best Practices

- Always quote variables to prevent word splitting
- Check command exit codes
- Use proper error handling
- Respect user's shell configuration

### Critical Reminders

- This is a MARKDOWN-FIRST framework
- Gemini has direct shell access
- Use CLI features effectively
- Maintain shell safety
- Respect system boundaries

---

## User Customizations (Protected)

<!-- BEGIN PROTECTED: user-space -->
<!-- Add your custom Gemini CLI instructions here -->
<!-- This section will be preserved during template regeneration -->
<!-- END PROTECTED: user-space -->

---

Generated from template: gemini.template.md
Build timestamp: 2026-01-22T04:54:27.241Z
ScaffoldX version: 2.0.0
