# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

### Added

- **Inline Audio Playback** - Play recordings directly in the panel without external player
  - Progress bar with system audio indicator color
  - Play/Stop button toggle
  - Prevents simultaneous playback of multiple recordings
  - Graceful handling of missing audio files
- **Expandable Transcription** - Click to expand/collapse full transcription text
  - `[more]`/`[less]` toggle links
  - Automatic re-layout after expansion
  - Works with pre-truncated text (ends with "...")
- **Responsive Ellipsis** - Transcription truncation adapts to panel width
  - ComponentListener for resize events
  - Debounced calculations
- Full transcription storage - `.txt` files saved alongside `.wav` recordings

### Changed

- Recording card button styling: hand cursor, FlatLaf hover/press effects
- Play/Stop button icons reduced to 10px (30% smaller)
- Card padding refined: top/sides 10px, bottom 12px
- Panel border padding: top 48px, sides 24px

### Fixed

- Thread-safe manifest with synchronized load/save and Windows file locking fallback
- Manifest syncs on first panel access (firstRun flag) to detect external file changes
- Text truncation uses fixed 60 char display length for consistency

---

## [2.2.1] - 2026-02-01 - Recording Retention System and Icon Theming

### Added

- **Recording Retention System** - Automatically save recordings for later access
  - Configurable retention: keep last N recordings (default 10)
  - Recordings browser panel with metadata display (duration, size, source type)
  - Transcription preview in recording cards
  - Play recordings in default audio player
  - Delete individual recordings with confirmation
  - Open recordings folder in file manager
  - JSON manifest for metadata persistence
- **Icon Theming System** - Runtime icon coloring with context-aware theming
  - Pure black source SVGs with runtime ColorFilter application
  - Semantic icon naming (replaces index-based menu icons)
  - Context-aware coloring: MENU, BUTTON, PANEL, DISABLED
  - Light/dark theme support via IconLoader utility

### Changed

- Menu icons now use semantic names instead of numeric indices (0.svg → mic.svg)
- Icon format updated from SVG to PNG for improved compatibility

### Fixed

- Drag-and-drop cursor feedback restored for audio/video files

- Remaining temp files (FFmpegUtil extract/merge, SystemAudioCapture POC) now route through `ConfigManager.createTempFile()` to use `%TEMP%\WhisperDog` instead of `%TEMP%` directly
- Dual-source merge and attribution now gated on actual system track content (>0.5s audio) rather than file existence alone

---

## [2.2.0] - 2026-01-25 - System Audio Capture

### Added

- **System Audio Capture (WASAPI Loopback)** - Record computer audio alongside microphone
  - Windows-only via XT-Audio library
  - Configurable loopback device selection
  - Real-time mixing of mic + system audio
  - Source activity tracking for speaker attribution
- **Word-Level Timestamp Attribution** - Enhanced [User]/[System] labels for dual-source recordings
  - Uses OpenAI's `verbose_json` response with `timestamp_granularities: ["word"]`
  - Each word mapped to correct source based on actual timing
- **Audio Device Labels** - Recording screen shows current mic and system audio device
  - Cross-platform default output detection (Windows Core Audio API, Linux pactl, macOS system_profiler)
  - Auto-refresh when app window gains focus (detects audio device changes)
  - Click label to navigate to Settings
- Device selection dropdown for loopback audio source

### Fixed

- Dual-source speech detection now checks both tracks and uses maximum content
- System audio threshold lowered (50%) to better detect pre-processed audio
- Temp file cleanup: replaced 12 `deleteOnExit()` calls with explicit cleanup
- Mic-only recording temp file leak (prefix now matches cleanup pattern)
- WASAPI availability check cached to avoid blocking EDT on startup
- OpenAI transcription client now properly cleans up temp files
- Hotkey recording toggle now runs on EDT for XT-Audio compatibility

### Changed

- Upgraded from Java 11 to Java 17 (required by XT-Audio)
- Dependency cleanup: aligned httpclient versions (4.5.14), removed redundant exclusions
- Moved test harness to proper `src/test/java` location

---

## [2.1.0] - 2026-01-02 - Error Handling & Mic Calibration

### Added

- **Error Handling & Resilience**
  - Automatic retry with exponential backoff for transient API errors (429, 500, 502, 503, 504)
  - User-friendly error dialogs with actionable guidance
  - Pre-submission file validation with supported format checking
  - Graceful timeout handling (120s default)

- **Mic Test Screen**
  - Real-time VU meter for audio level visualization
  - Silence threshold calibration slider
  - Test recording with playback functionality
  - Accessible via Options → Mic Test tab

- **UI/UX Improvements**
  - Post-Processing UI Reorganization: Separate "Show section" and "Auto-execute" controls
  - Hidden-but-active indicator when section collapsed but auto-processing enabled
  - Persistent visibility state across sessions
  - Searchable Log Screen with incremental search, yellow/orange highlighting
  - Previous/Next navigation with match counter (Ctrl+F keyboard shortcut)
  - Process progress panel with file path display
  - Copy Path and Open Folder buttons
  - Cancel and Retry functionality for file operations
  - Stage-aware indeterminate progress bar

- **Recording Enhancements**
  - Large recording warning dialog with configurable threshold
  - Minimum speech duration threshold with user prompt
  - Recording warning indicator in system tray
  - Visual separators in transcript log for readability

### Fixed

- File size validation before API submission
- Dropdown reset hotkey functionality
- Pipeline button disabled state fix
- File type validation with supported formats list

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

## Pre-2.0.0 History

For changes made during the WhisperCat-xblabs era, see the original [WhisperCat-xblabs repository](https://github.com/xblabs/whispercat-xb).

---

## Commit Message Format

Use conventional commits for changelog clarity:

| Prefix     | Section       | Example                          |
| ---------- | ------------- | -------------------------------- |
| `feat:`    | Added         | `feat: add dark mode support`    |
| `fix:`     | Fixed         | `fix: resolve memory leak`       |
| `docs:`    | Documentation | `docs: update API reference`     |
| `refactor:`| Changed       | `refactor: simplify auth flow`   |
| `perf:`    | Changed       | `perf: optimize query performance` |
| `chore:`   | (not logged)  | `chore: update dependencies`     |
| `test:`    | (not logged)  | `test: add unit tests`           |
