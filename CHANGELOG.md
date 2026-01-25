# Changelog

All notable changes to this project are automatically documented in this file.
This file is updated by git hooks when commits follow conventional commit format.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [2.2.0] - 2026-01-25 - System Audio Capture

### Added
- **System Audio Capture (WASAPI Loopback)** - Record computer audio alongside microphone
  - Windows-only via XT-Audio library
  - Configurable loopback device selection
  - Real-time mixing of mic + system audio
  - Source activity tracking for speaker attribution
- Device selection dropdown for loopback audio source

### Fixed
- Temp file cleanup: replaced 12 `deleteOnExit()` calls with explicit cleanup
- Mic-only recording temp file leak (prefix now matches cleanup pattern)
- WASAPI availability check cached to avoid blocking EDT on startup
- OpenAI transcription client now properly cleans up temp files

### Changed
- Dependency cleanup: aligned httpclient versions (4.5.14), removed redundant exclusions
- Moved test harness to proper `src/test/java` location

---

## [2.0.0] - 2025-12-26 - WhisperDog Release

### Changed
- **Complete rebrand from WhisperCat to WhisperDog**
- Package renamed: `org.whispercat` → `org.whisperdog`
- All UI text, icons, and branding updated
- Config directory: `~/.whispercat/` → `~/.whisperdog/`
- Build artifacts: `whisperdog-2.0.0-jar-with-dependencies.jar`

### Added
- New SVG icons for menu items (mic, sliders, git-pull-request, box, terminal)
- New SVG button icons (play, chevron-up, chevron-down)
- Improved list padding in Unit Library and Pipeline Library

### Fixed
- Replaced broken Unicode characters with proper SVG icons
- Fixed cramped list items with proper padding

### Acknowledgements
- Built on the foundation of [WhisperCat](https://github.com/ddxy/whispercat) by ddxy
- Enhanced by xblabs with pipeline system, silence removal, and many other features

---

## [1.x.x] - WhisperCat-xblabs Legacy

The following changes were made during the WhisperCat-xblabs era:

## [Unreleased]

### Added
- Auto-updating CHANGELOG.md with git hooks
- Large audio file chunking for files >25MB (b61fb8d)
- Manual pipeline runner with history tracking (71c893f)
- Save & Create Pipeline button in Unit Editor (1d47e9f)
- Log search with highlighting (1d47e9f)
- Apply Settings button with clear feedback (29cc624)
- Chunked transcription progress settings (1d47e9f)
- Large file handling preferences in ConfigManager (1d47e9f)

### Fixed
- 90% silence threshold now warns but still processes (1d47e9f)
- Pipeline dropdown reset when using hotkeys (1d47e9f)
- Run Pipeline button state after recording stops (1d47e9f)
- Silence threshold description was backwards (de6f2f1)
- Post-processing now runs asynchronously (ae6a56d)

### Changed
- SilenceRemover processes high-silence recordings with warning instead of dropping

### Documentation
- Add WhisperDog rebrand specification (e03636f)
- Add undocumented features to README (c9d82ac)
- Add documentation requirements to CLAUDE.md (8a61529)
- Update README with latest features (Dec 2025) (731250f)
- Add CLAUDE.md for Claude Code project context (5caf413)
- Added LARGE_AUDIO_CHUNKING_SPEC.md (fbbc135)
- Added MANUAL_PIPELINE_RUNNER_SPEC.md (c55f128)

---

## Commit Message Format

Use conventional commits for automatic changelog updates:

| Prefix | Section | Example |
|--------|---------|---------|
| `feat:` | Added | `feat: add dark mode support` |
| `fix:` | Fixed | `fix: resolve memory leak` |
| `docs:` | Documentation | `docs: update API reference` |
| `refactor:` | Changed | `refactor: simplify auth flow` |
| `perf:` | Changed | `perf: optimize query performance` |
| `test:` | (not logged) | `test: add unit tests` |
| `chore:` | (not logged) | `chore: update dependencies` |

---

<!-- AUTO-GENERATED BELOW - DO NOT EDIT MANUALLY -->
