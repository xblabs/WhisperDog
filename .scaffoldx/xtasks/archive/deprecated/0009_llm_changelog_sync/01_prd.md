# PRD: LLM-Driven Changelog Synchronization

## 1. Problem Statement

### 1.1 Current Behavior

README.md, CHANGELOG.md, and git history routinely drift out of sync. The WhisperDog project audit (2026-01-25) revealed:

| Document | v2.0.0 | v2.1.0 | v2.2.0 |
|----------|--------|--------|--------|
| README.md | Present | Present | Incomplete |
| CHANGELOG.md | Present | **MISSING** | In "Unreleased" |
| Git history | Commits exist | Commits exist | Commits exist |

**Root Cause**: Manual synchronization is error-prone and often forgotten. Version bumps don't trigger documentation updates.

### 1.2 Why Deterministic Scripts Fail

A naive solution (git hook with pattern matching) produces:

```
feat: add X → Added: add X
fix: resolve Y → Fixed: resolve Y
```

**Problems:**
- Mechanical, repetitive entries lacking context
- No grouping of related commits
- Misses commit body information
- Cannot synthesize "5 fixes for system audio" into one coherent entry
- Doesn't understand semantic relationships

### 1.3 Impact

| Metric | Current State |
|--------|---------------|
| Documentation debt | Accumulates between releases |
| Onboarding friction | New contributors confused by incomplete history |
| Release overhead | Manual changelog writing before each release |
| User trust | Incomplete changelog reduces perceived quality |

## 2. Solution: LLM-Driven Sync

### 2.1 Core Principle (AI-GOV-013)

**LLM makes semantic decisions, not scripts.**

| Deterministic Script | LLM-Driven |
|---------------------|------------|
| `feat: add X` → "Added: add X" | Understands X is part of larger feature Y |
| Each commit = one entry | Groups 5 related commits into 1 coherent entry |
| Misses context | Reads commit body, understands impact |
| Mechanical voice | Matches existing changelog tone |

### 2.2 Workflow

```
Trigger: "x-changelog-sync" or "sync changelog"
                ↓
Oracle: git log --since=<last-version-tag>
                ↓
LLM: Analyze commits, synthesize entries
                ↓
LLM: Update CHANGELOG.md Unreleased section
                ↓
LLM: Optionally update README Version History
```

### 2.3 Command Specifications

#### x-changelog-sync

**Purpose**: Synchronize git commits to CHANGELOG.md Unreleased section

**Inputs**:
- `--since=<tag|date>` - Start point (default: last version tag)
- `--include-readme` - Also update README Version History
- `--dry-run` - Preview changes without writing

**Process**:
1. Get commits since specified point
2. Filter: exclude `chore:`, `test:`, `docs:` (configurable)
3. Synthesize into Added/Fixed/Changed entries
4. Deduplicate with existing Unreleased entries
5. Write to CHANGELOG.md

#### x-release-prep

**Purpose**: Prepare release (move Unreleased to versioned, update version refs)

**Inputs**:
- `<version>` - New version number (e.g., "2.3.0")
- `--no-readme` - Skip README updates

**Process**:
1. Move Unreleased → `## [<version>] - <date> - <title>`
2. Update pom.xml version
3. Update README badge version
4. Update README Version History
5. Create empty Unreleased section

## 3. Technical Specifications

### 3.1 Git History Analysis

**Oracle Invocation** (deterministic):
```bash
# Get commits since last tag
git log v2.2.0..HEAD --pretty=format:"%h|%ad|%s|%b" --date=short

# Get last version tag
git describe --tags --abbrev=0

# Get tag date
git log -1 --format=%ai v2.2.0
```

**LLM Analysis** (semantic):
- Group related commits (same feature, same fix area)
- Extract meaningful information from commit bodies
- Determine appropriate changelog category
- Write human-readable entry matching existing style

### 3.2 Changelog Entry Synthesis

**Input Example**:
```
c91f875|2026-01-25|feat(TASK-0006): add device labels and word-level attribution|
Audio Device Labels:
- New AudioDeviceInfo utility for cross-platform default device detection
- Recording screen shows current mic and system audio device names
...
Word-Level Timestamp Attribution:
- TranscriptionResult class holds text + timestamped words
...

42d3725|2026-01-25|fix(TASK-0006): address sluggishness audit findings|
- Remove 12 deleteOnExit() calls, replace with explicit cleanup
...
```

**Output Example**:
```markdown
### Added

- **Audio Device Labels** - Recording screen shows current mic and system audio device names
  - Cross-platform default output detection (Windows Core Audio API, Linux pactl, macOS system_profiler)
  - Auto-refresh on window focus to detect device changes
- **Word-Level Timestamp Attribution** - Enhanced [User]/[System] labels for dual-source recordings
  - Uses OpenAI's `verbose_json` response with word-level timestamps

### Fixed

- Temp file cleanup: replaced 12 `deleteOnExit()` calls with explicit cleanup
- Mic-only recording temp file leak (prefix now matches cleanup pattern)
```

### 3.3 ScaffoldX Integration Points

| Component | Integration |
|-----------|-------------|
| **NLP Patterns** | "sync changelog", "update changelog", "prepare release" |
| **Sentinel** | `changelog_sentinel.md` - triggers on version/changelog keywords |
| **Oracle** | Git commands for deterministic data |
| **Governance** | AI-GOV-013 (LLM-first), AI-GOV-009 (oracle usage) |

### 3.4 File Modifications

| File | Modification |
|------|--------------|
| `CHANGELOG.md` | Update Unreleased section |
| `README.md` | Update Version History (optional) |
| `pom.xml` | Update version on release (x-release-prep only) |

## 4. Acceptance Criteria

### 4.1 Functional Requirements

| ID | Requirement | Verification |
|----|-------------|--------------|
| FR-1 | Commits since last tag extracted correctly | Compare with manual git log |
| FR-2 | Related commits grouped into single entries | Review output coherence |
| FR-3 | Changelog style matches existing entries | Style comparison |
| FR-4 | No duplicate entries created | Check for redundancy |
| FR-5 | README Version History updated when requested | Verify both files updated |

### 4.2 Non-Functional Requirements

| ID | Requirement | Acceptance |
|----|-------------|------------|
| NFR-1 | Works for 50+ commits since last release | Test on large history |
| NFR-2 | Handles empty commit bodies | Graceful fallback |
| NFR-3 | Idempotent execution | Running twice produces same result |

## 5. Implementation Checklist

### Phase 1: Core Functionality

- [ ] Create x-changelog-sync command spec
- [ ] Implement git history extraction (oracle)
- [ ] Implement commit grouping logic (LLM)
- [ ] Implement changelog synthesis (LLM)
- [ ] Write to CHANGELOG.md Unreleased section

### Phase 2: Release Prep

- [ ] Create x-release-prep command spec
- [ ] Implement version bump (pom.xml, README badge)
- [ ] Implement Unreleased → versioned section migration
- [ ] Implement README Version History update

### Phase 3: ScaffoldX Core Integration

- [ ] Add NLP patterns to pareto command set
- [ ] Create changelog_sentinel.md
- [ ] Document in ScaffoldX command reference
- [ ] Add to x-help output

## 6. Context for Implementation

### 6.1 Session Origin

This task originated from a documentation audit session (2026-01-25) where:
1. README, CHANGELOG, and git history were found out of sync
2. Manual sync was performed using README as source of truth
3. v2.1.0 was missing from CHANGELOG entirely
4. v2.2.0 features were in "Unreleased" instead of versioned section

### 6.2 ADR Reference

Full architectural decision recorded in:
- `.scaffoldx/xcontext/development/adr/003_llm_changelog_sync.md`

### 6.3 Current State (Post-Audit)

| Document | Status |
|----------|--------|
| CHANGELOG.md | Fully synchronized (v2.0.0, v2.1.0, v2.2.0) |
| README.md | Fully synchronized, features section complete |
| ADR-003 | Created, documents LLM-driven approach |

### 6.4 Handoff Notes

1. **This is a ScaffoldX core feature** - Implementation should be generalized, not WhisperDog-specific
2. **Test on WhisperDog first** - Use this project as the validation ground
3. **Conventional commits assumed** - Input format follows `feat:`, `fix:`, etc.
4. **LLM reads commit bodies** - Not just subjects; important context is often in body

## 7. Related Documents

- **ADR**: [003_llm_changelog_sync.md](../../xcontext/development/adr/003_llm_changelog_sync.md)
- **CHANGELOG**: [CHANGELOG.md](../../../CHANGELOG.md)
- **README**: [README.md](../../../README.md)
