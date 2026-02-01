# Implementation Plan: LLM-Driven Changelog Sync

## Overview

Implement two ScaffoldX commands for automated changelog maintenance:
1. **x-changelog-sync** - Analyze commits and update CHANGELOG.md Unreleased section
2. **x-release-prep** - Prepare release (version bump, migrate changelog sections)

Both commands follow AI-GOV-013 (LLM-first): scripts handle deterministic data extraction, LLM handles semantic decisions.

---

## Cluster 1: Git History Extraction (Oracle Layer)

Build deterministic oracle functions for git data extraction.

### 1.1 Last Version Tag Detection

**Oracle Command**:
```bash
# Get last semantic version tag
git describe --tags --abbrev=0 --match "v*"
```

**Edge Cases**:
| Scenario | Resolution |
|----------|------------|
| No tags exist | Return null, command prompts for `--since` date |
| Multiple tag formats | Filter to `v*` pattern only |
| Pre-release tags (v2.0.0-beta) | Include in search, newest wins |

### 1.2 Commit Extraction

**Oracle Command**:
```bash
# Get commits since tag with structured output
git log v2.2.0..HEAD --pretty=format:"%h|%ad|%s|%b|||" --date=short
```

**Output Fields**:
- `%h` - Short hash (for deduplication)
- `%ad` - Author date (for ordering)
- `%s` - Subject line (conventional commit)
- `%b` - Body (detailed context)

**Parsing**:
- Split on `|||` delimiter (handles multiline bodies)
- Parse subject for conventional commit type: `feat:`, `fix:`, `refactor:`

### 1.3 Tag Date Extraction

**Oracle Command**:
```bash
git log -1 --format=%ad --date=short v2.2.0
```

**Purpose**: Determine date boundary for README Version History entry.

---

## Cluster 2: Command Specification - x-changelog-sync

Define the command specification for ScaffoldX.

### 2.1 Command File

**Path**: `.scaffoldx/xcore/commands/x-changelog-sync.md`

**Structure**:
```markdown
# x-changelog-sync

## Purpose
Synchronize git commits to CHANGELOG.md Unreleased section using LLM semantic analysis.

## Syntax
x-changelog-sync [--since=<tag|date>] [--include-readme] [--dry-run] [--filter=<types>]

## Parameters
| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| --since | No | Last v* tag | Start point for commit extraction |
| --include-readme | No | false | Also update README Version History |
| --dry-run | No | false | Preview changes without writing |
| --filter | No | chore,test,ci | Commit types to exclude |

## Process
1. ORACLE: Extract last version tag
2. ORACLE: Extract commits since tag
3. LLM: Parse conventional commit types
4. LLM: Group related commits by feature/area
5. LLM: Synthesize changelog entries matching existing style
6. LLM: Deduplicate with existing Unreleased entries (by commit hash)
7. WRITE: Update CHANGELOG.md Unreleased section
8. OPTIONAL: Update README Version History if --include-readme

## Deduplication Logic
- Extract commit hashes from existing Unreleased entries (if inline)
- Skip commits already represented
- Use semantic matching as fallback: same feature name = same entry

## Output
- Modified CHANGELOG.md with new entries in Unreleased section
- Console summary: "Added X entries (Y commits grouped)"
```

### 2.2 Changelog Style Analysis

**Before Writing**: LLM reads existing CHANGELOG.md to match:
- Bullet style (`-` vs `*`)
- Bold patterns (`**Feature Name**` vs plain)
- Indentation depth for sub-items
- Section ordering (Added/Changed/Fixed/Removed)

### 2.3 Entry Synthesis Rules

**Input**: Commits with same task ID or same file path prefix

**Output**: Single coherent entry

**Example Transformation**:
```
# Input: 3 commits
c91f875|2026-01-25|feat(TASK-0006): add device labels|...
42d3725|2026-01-25|feat(TASK-0006): add word-level timestamps|...
a8b2c3d|2026-01-26|fix(TASK-0006): fix device detection on Linux|...

# Output: 2 entries
### Added
- **System Audio Capture (TASK-0006)** - Dual-source recording with [User]/[System] attribution
  - Audio device labels showing current mic and system audio devices
  - Word-level timestamp attribution using OpenAI verbose_json

### Fixed
- Device detection on Linux for system audio capture
```

---

## Cluster 3: Command Specification - x-release-prep

Define the release preparation command.

### 3.1 Command File

**Path**: `.scaffoldx/xcore/commands/x-release-prep.md`

**Structure**:
```markdown
# x-release-prep

## Purpose
Prepare release: version bump, migrate Unreleased to versioned section, update all version references.

## Syntax
x-release-prep <version> [--no-readme] [--title=<string>]

## Parameters
| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| version | Yes | - | Semantic version (e.g., "2.3.0") |
| --no-readme | No | false | Skip README Version History update |
| --title | No | Auto-generated | Release title (e.g., "Enhanced Audio") |

## Validation
- Version must match semver pattern: X.Y.Z
- Version must be greater than current (no downgrades)
- Unreleased section must have content

## Process
1. VALIDATE: Semver format, version increment
2. READ: Current Unreleased content from CHANGELOG.md
3. LLM: Generate release title if not provided (from features)
4. WRITE: Replace Unreleased with `## [version] - date - title`
5. WRITE: Create new empty Unreleased section
6. ORACLE: Update pom.xml <version> tag
7. ORACLE: Update README badge version
8. OPTIONAL: Update README Version History if not --no-readme

## Files Modified
| File | Change |
|------|--------|
| CHANGELOG.md | Migrate Unreleased → versioned section |
| pom.xml | Update `<version>X.Y.Z</version>` |
| README.md | Update badge, Version History |
```

### 3.2 Version History Entry Format

**README Version History Template**:
```markdown
- **vX.Y.Z** (YYYY-MM-DD) - {title}: {1-2 sentence summary of key features}
```

---

## Cluster 4: Sentinel Integration

Create changelog domain sentinel for NLP triggering.

### 4.1 Sentinel File

**Path**: `.scaffoldx/xcore/internal/sentinels/domains/changelog_sentinel.md`

**Trigger Patterns**:
- `changelog` + action verb (sync, update, add)
- `release` + version number
- `prepare release`
- `bump version`
- `version X.Y.Z`

### 4.2 NLP Pattern Registration

**Add to Pareto Command Set** (CLAUDE.md):
```
x-changelog-sync | sync changelog, update changelog, add to changelog, synchronize changelog
x-release-prep | prepare release {version}, release {version}, bump version to {version}, cut release {version}
```

---

## Cluster 5: Edge Case Handling

Define deterministic resolutions for all edge cases.

### 5.1 Git State Edge Cases

| Case | Detection | Resolution |
|------|-----------|------------|
| No commits since tag | Empty git log | Message: "No new commits since vX.Y.Z" |
| No version tags | `git describe` fails | Require `--since` parameter |
| Merge commits | Commit has 2+ parents | Exclude (parent commits have detail) |
| Revert commits | Subject starts "Revert" | Show as "Reverted: {original}" or exclude |
| Non-conventional | No type prefix | Categorize as "Changed" |

### 5.2 File State Edge Cases

| Case | Detection | Resolution |
|------|-----------|------------|
| CHANGELOG.md missing | File not found | Create with template header |
| No Unreleased section | Section not found | Create section after header |
| Malformed sections | Parse error | Error with line number |

### 5.3 Deduplication Edge Cases

| Case | Detection | Resolution |
|------|-----------|------------|
| Commit hash in entry | Pattern `([a-f0-9]{7})` | Skip if hash matches |
| Same feature different words | Semantic overlap | LLM judgment: merge or keep both |
| Partial overlap | Some commits new | Add only new commits to existing entry |

---

## Cluster 6: Testing & Validation

### 6.1 WhisperDog Test Cases

| Test | Input | Expected Output |
|------|-------|-----------------|
| Normal sync | Commits since v2.2.0 | Grouped entries in Unreleased |
| Empty history | No commits since tag | "No new commits" message |
| Idempotency | Run twice | Same result, no duplicates |
| With README | `--include-readme` | Both files updated |
| Dry run | `--dry-run` | Preview only, no writes |

### 6.2 Release Prep Test Cases

| Test | Input | Expected Output |
|------|-------|-----------------|
| Normal release | `x-release-prep 2.3.0` | All files updated |
| No unreleased content | Empty section | Error: "Nothing to release" |
| Invalid version | `x-release-prep abc` | Error: "Invalid semver" |
| Downgrade attempt | Lower version | Error: "Version must increment" |

---

## Dependencies & Risks

### Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| Conventional commits | Input format | Assumed present |
| Git CLI | Oracle | Available |
| ScaffoldX command framework | Infrastructure | Exists |

### Risks

| Risk | Mitigation |
|------|------------|
| LLM grouping inconsistent | Provide examples in command spec |
| Style drift | Always read existing style first |
| Merge conflicts | Dry-run by default in PRs |

---

## Implementation Sequence

1. **Cluster 1**: Git oracle functions (testable independently)
2. **Cluster 5**: Edge case handling (design before implementation)
3. **Cluster 2**: x-changelog-sync command spec
4. **Cluster 3**: x-release-prep command spec
5. **Cluster 4**: Sentinel and NLP integration
6. **Cluster 6**: Validation on WhisperDog

---

*Plan generated per TASK-0009 PRD, audited by Scrutinizer role*
