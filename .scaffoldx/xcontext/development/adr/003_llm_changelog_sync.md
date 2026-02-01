---
title: ADR-003 LLM-Driven Changelog Synchronization
status: proposed
created: 2026-01-25
domain: development
type: adr
---

# ADR-003: LLM-Driven Changelog Synchronization

## Context

README, CHANGELOG, and git history frequently drift out of sync. Manual synchronization is error-prone and often forgotten. A deterministic script-based approach (pattern matching `feat:` → Added) produces mechanical, repetitive entries that lack context.

## Decision

Implement an **LLM-driven changelog synchronization** system that:

1. **Reads** git commit history since last release/tag
2. **Synthesizes** related commits into coherent changelog entries
3. **Contextualizes** changes (understands what a fix actually fixes)
4. **Groups** logically by Added/Fixed/Changed categories
5. **Updates** CHANGELOG.md Unreleased section

## Workflow

### Trigger Points

| Trigger | Action |
| ------- | ------ |
| `x-changelog-sync` | Sync commits since last tag to Unreleased |
| `x-release-prep <version>` | Move Unreleased to versioned section, update version refs |
| Pre-commit (optional) | Validate commit follows conventional format |

### LLM Process (Not Deterministic)

```
Input:
- Git commits since last version tag
- Existing CHANGELOG.md structure
- README version history section

LLM Tasks:
1. Group related commits (e.g., multiple fixes for same feature)
2. Synthesize into human-readable entries
3. Determine correct category (Added/Fixed/Changed)
4. Deduplicate with existing entries
5. Maintain consistent voice/style with existing changelog

Output:
- Updated CHANGELOG.md Unreleased section
- (Optional) Suggested README Version History updates
```

### Why LLM-Driven (AI-GOV-013)

| Deterministic Script | LLM-Driven |
| -------------------- | ---------- |
| `feat: add X` → "Added: add X" | Understands X is part of larger feature Y |
| Each commit = one entry | Groups 5 related commits into 1 coherent entry |
| Misses context | Reads commit body, understands impact |
| Mechanical voice | Matches existing changelog tone |

## Implementation Notes

### For ScaffoldX Core

This feature belongs in ScaffoldX core as:
- `x-changelog-sync` - Command to sync git history to changelog
- `x-release-prep` - Command to prepare release (version bump + changelog finalization)
- Sentinel pattern for detecting "changelog", "release", "version" keywords

### Immediate WhisperDog Approach

Until ScaffoldX core implements this:
1. User triggers manually: "sync changelog from git history"
2. LLM reads: `git log --since=<last-version-date>`
3. LLM updates: CHANGELOG.md and README.md Version History
4. User reviews and commits

## Governance Integration

- **AI-GOV-013**: LLM makes semantic decisions, not scripts
- **Oracle Usage**: Use git commands for deterministic data (commits, tags, dates)
- **Checkpoint**: Save state before/after changelog sync for rollback

## Success Criteria

- Zero manual changelog entries required
- Changelog entries are contextual and grouped logically
- README and CHANGELOG stay synchronized
- Version numbers consistent across pom.xml, README badge, CHANGELOG

## Related

- AI-GOV-013: LLM-First Processing
- x-git-commit: Existing commit command
- Conventional Commits: Input format standard
