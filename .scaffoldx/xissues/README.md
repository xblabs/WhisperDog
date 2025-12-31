# WhisperDog Issue Tracking

Issue tracking system for WhisperDog, following ScaffoldX conventions.

## Structure

```
.scaffoldx/xissues/
├── active/
│   └── 0001-0300/          # ID range bucket for active issues
│       └── ISS_XXXXX_*.md  # Individual issue files
├── archive/
│   └── 0001-0300/          # Archived/resolved issues
├── resolutions/            # Detailed resolution documentation
├── meta.json               # ID tracking and metadata
└── README.md               # This file
```

## Quick Commands

- `x-issue-list` - List all active issues
- `x-issue-add <title>` - Create a new issue
- `x-issue-start [id]` - Start working on an issue
- `x-issue-resolve <id>` - Mark issue as resolved
- `x-issue-details <id>` - View issue details

## Issue Statuses

| Status | Symbol | Description |
|--------|--------|-------------|
| open | 🔵 | New/unassigned issue |
| in-progress | 🟡 | Being actively worked on |
| blocked | 🔴 | Cannot proceed due to dependencies |
| resolved | ✅ | Successfully completed |
| deprecated | ⛔ | Abandoned/no longer relevant |

## Issue File Format

Issues use YAML frontmatter + markdown body:

```markdown
---
issue_id: ISS_XXXXX
title: Issue title
type: bug|enhancement
priority: critical|high|normal|low
status: open|in-progress|resolved|deprecated
created: YYYY-MM-DD
tags: [tag1, tag2]
---

# ISS_XXXXX: Issue title

## Problem Description
...
```

## Priority Levels

- **critical**: System-breaking, needs immediate attention
- **high**: Important bugs or features
- **normal**: Standard priority (default)
- **low**: Nice-to-have, can wait
