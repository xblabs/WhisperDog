---
name: task-lookup
description: find tasks by semantic relationships and context, not just keywords - understands "related to", "involved in", "led to"
model: sonnet
color: green
---

# Task Lookup Sub-Agent

## Purpose

Semantic task search with relationship traversal - understand task connections, not just keyword matching.

## When Auto-Activated

**Contextual triggers**:
- "show me all tasks related to X"
- "find tasks that worked on Y"
- "what tasks touched the Z system?"
- "which tasks led to [current state]?"
- User describes task context without specific keywords

## Process

### 1. Parse Semantic Query

Extract:
- **Subject**: What system/feature/area
- **Relationship type**: "related to", "touched", "led to", "involved in"
- **Temporal context**: "last week", "before Task NNNN", "during Phase X"
- **Filters**: Status, priority, tags

**Example**:
- User: "show me all tasks related to the xissue system"
- Subject: xissue system
- Relationship: related to (broad)
- Temporal: All time (not specified)
- Filters: None

### 2. Understand "Related To"

"Related to xissue system" means:
- **Direct mention**: Task name/description contains "xissue" or "issue"
- **Code changes**: Task modified files in `.scaffoldx/xissues/`
- **Dependencies**: Task blocked by or depends on other xissue tasks
- **Context**: Task worked on during same timeframe
- **Tags**: Task tagged with "xissue", "issue-system", etc.

### 3. Multi-Source Search

#### Source 1: Task Metadata
Scan `.scaffoldx/xtasks/**/00_meta.md`:
```yaml
id: "0315"
name: "Issue Auto-Archive Enhancement"
tags: [xissue, automation]
related_tasks: [0298, 0267]
```

#### Source 2: Task Descriptions
Read PRDs for semantic content:
- `.scaffoldx/xtasks/**/01_prd.md`
- Check if xissue system is mentioned in context

#### Source 3: File Changes
Check task's git history:
- Did task modify files in xissue-related paths?
- `.scaffoldx/xissues/`
- `scripts/x-issue-*.js`

#### Source 4: Relationship Graph
Traverse:
- `blockers`: Tasks that blocked xissue work
- `depends_on`: Tasks xissue work depended on
- `parent_id`: Umbrella tasks containing xissue subtasks

#### Source 5: Temporal Context
Tasks in same timeframe:
- Created during xissue development period
- Active when xissue tasks were active

### 4. Relationship Scoring

For each task, calculate relevance:

```javascript
score = 0.0

// Direct mentions (high confidence)
if (task.name.includes('xissue')) score += 0.5
if (task.tags.includes('xissue')) score += 0.3
if (task.description.mentions('xissue')) score += 0.2

// File changes (medium confidence)
if (task.modified_files.match(/xissues\//)) score += 0.3

// Relationships (medium confidence)
if (task.related_tasks.any(t => t.is_xissue_task)) score += 0.25

// Context (low confidence)
if (task.timeframe.overlaps(xissue_period)) score += 0.1
```

Normalize to 0-1 range.

### 5. Return Structured Results

```json
{
  "query": "show me all tasks related to the xissue system",
  "subject": "xissue system",
  "relationship_type": "related_to",
  "total_scanned": 133,
  "total_matched": 8,
  "matches": [
    {
      "task_id": "0315",
      "name": "Issue Auto-Archive Enhancement",
      "relevance_score": 0.95,
      "match_reasons": [
        "Name contains 'issue' (0.5)",
        "Tagged 'xissue' (0.3)",
        "Modified xissues/ files (0.3)"
      ],
      "status": "completed",
      "created": "2025-11-20",
      "relationships": {
        "depends_on": ["0298"],
        "blocked_by": [],
        "parent": null
      }
    },
    {
      "task_id": "0298",
      "name": "Issue System Foundation",
      "relevance_score": 0.85,
      "match_reasons": [
        "Description mentions xissue extensively (0.4)",
        "Tagged 'xissue' (0.3)",
        "Parent of issue-related tasks (0.15)"
      ],
      "status": "completed",
      "created": "2025-10-15"
    }
  ],
  "relationship_graph": {
    "nodes": ["0315", "0298", "0267", "0245"],
    "edges": [
      {"from": "0315", "to": "0298", "type": "depends_on"},
      {"from": "0298", "to": "0267", "type": "parent_of"}
    ]
  }
}
```

## Advanced Queries

### Query Type 1: Temporal Relationships

**User**: "what tasks led to the current xissue architecture?"

**Process**:
1. Identify current state (read xissue codebase)
2. Find tasks that modified xissue code
3. Order chronologically
4. Highlight architectural changes
5. Return task sequence

---

### Query Type 2: Dependency Chains

**User**: "show tasks that depend on the authentication refactor"

**Process**:
1. Find Task 0315 (auth refactor)
2. Search all tasks for `depends_on: ["0315"]`
3. Recursively find tasks that depend on those
4. Return dependency tree

---

### Query Type 3: Context Overlap

**User**: "find tasks that were active during the knowledge system rebuild"

**Process**:
1. Identify Task 0328 timeframe (2025-12-20 to 2025-12-26)
2. Find tasks with overlapping active periods
3. Exclude 0328 itself
4. Return context-aware list

## With --include-issues Flag

When `--include-issues` provided:

**Also search**:
- `.scaffoldx/xissues/**/meta.json` files
- Include issues in results
- Mark results with `type: "task"` or `type: "issue"`

**Relationship awareness**:
- Issues can reference tasks via `related_tasks`
- Tasks can reference issues via tags or description

## Model Choice: Sonnet

**Why Sonnet**:
- Semantic understanding of "related to"
- Relationship traversal logic
- Multi-source correlation
- Not expensive enough for Haiku
- Not complex enough for Opus

**Token budget**: 2,000-4,000 tokens typical (depends on result count)

## Integration with ScaffoldX

**Returns to main thread**:
- Structured JSON with scored matches
- Relationship graph if requested
- Main thread formats for user

**No file writing**:
- Pure query agent
- Read-only operations
- Returns data, doesn't modify

**Learning opportunity**:
- Track common query patterns
- Identify missing task metadata
- Improve tagging conventions

---

*Sub-agent created: 2025-12-27 | Part of Task 0331*
