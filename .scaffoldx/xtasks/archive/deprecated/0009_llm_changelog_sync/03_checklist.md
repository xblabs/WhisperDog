# Task 0009: LLM-Driven Changelog Sync - Checklist

## Status: Draft

---

## Phase 1: Core Functionality

- [ ] **1.1** Define x-changelog-sync command specification
  - Input parameters (--since, --include-readme, --dry-run)
  - Output format (markdown)
  - Error handling

- [ ] **1.2** Implement git history extraction
  - Oracle: `git log` with proper format
  - Oracle: `git describe --tags` for last version
  - Filter commits by type (exclude chore, test by default)

- [ ] **1.3** Implement commit grouping logic
  - LLM: Identify related commits (same feature, same area)
  - LLM: Group by changelog category (Added/Fixed/Changed)
  - LLM: Synthesize multiple commits into coherent entries

- [ ] **1.4** Implement changelog writing
  - Read existing CHANGELOG.md structure
  - Update Unreleased section (not overwrite)
  - Deduplicate with existing entries
  - Preserve formatting/style

- [ ] **1.5** Test on WhisperDog
  - Run with current git history
  - Verify output matches manual sync results
  - Test idempotency (run twice, same result)

---

## Phase 2: Release Prep Command

- [ ] **2.1** Define x-release-prep command specification
  - Version parameter (required)
  - --no-readme flag
  - Validation (semver format)

- [ ] **2.2** Implement version bump
  - Update pom.xml `<version>` tag
  - Update README badge version
  - Update jar filename references

- [ ] **2.3** Implement changelog migration
  - Move Unreleased → `## [version] - date - title`
  - Create new empty Unreleased section
  - Auto-generate release title from features

- [ ] **2.4** Implement README Version History
  - Add new version entry
  - Match existing format/style
  - Include key features summary

---

## Phase 3: ScaffoldX Core Integration

- [ ] **3.1** Add NLP patterns
  - "sync changelog" → x-changelog-sync
  - "update changelog" → x-changelog-sync
  - "prepare release X" → x-release-prep X
  - "bump version to X" → x-release-prep X

- [ ] **3.2** Create sentinel
  - changelog_sentinel.md
  - Trigger patterns: "changelog", "release", "version bump"
  - Load on-demand behavior

- [ ] **3.3** Documentation
  - Add to command reference
  - Add to x-help output
  - Update CLAUDE.md pareto patterns

---

## Validation Criteria

- [ ] Commits since v2.2.0 correctly extracted
- [ ] Related commits grouped (not 1:1 mapping)
- [ ] Changelog style matches existing entries
- [ ] No duplicate entries on re-run
- [ ] README updated when --include-readme used
- [ ] Version bump updates all relevant files

---

## Notes

- **Origin**: Documentation audit session 2026-01-25
- **Reference**: ADR-003 (003_llm_changelog_sync.md)
- **Target**: ScaffoldX core (generalized, not WhisperDog-specific)
