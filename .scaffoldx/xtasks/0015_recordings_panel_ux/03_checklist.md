# Checklist: Recordings Panel UX Improvements

## Phase 1: Critical Fixes

> **Icon items moved to Task 0016** (Icon Theming System)

- [x] Increase RecordingsPanel border padding (top: 48px, sides: 24px)
- [x] Implement manifest filesystem reconciliation on refresh
- [x] Sync manifest on first panel access (firstRun flag)

## Phase 2: Button Interactivity
- [x] Add hand cursor to Play and Delete buttons
- [x] Add FlatLaf hover/press styling to buttons
- [x] Use `IconLoader.getIcon()` from Task 0016 for button icons
- [x] Reduce button icon size to 10px (30% smaller)

## Phase 3: Expandable Transcription
- [x] Make transcription text clickable
- [x] Implement expand/collapse toggle functionality
- [x] Add visual indicator for expandable text (`[more]`/`[less]` links)
- [x] Handle re-layout after expansion
- [x] Fix: Show `[more]` for pre-truncated text (ends with "...")
- [x] Fix: Use fixed display length (60 chars) for consistent truncation

## Phase 4: Responsive Ellipsis
- [x] Add ComponentListener for panel resize events
- [x] Calculate truncation length based on available width
- [x] Debounce resize calculations
- [ ] Test at various window sizes

## Phase 5: Inline Audio Playback
- [x] Create InlineAudioPlayer utility class
- [x] Add progress bar component to recording cards
- [x] Implement Play/Stop button toggle
- [x] Style progress bar with system audio indicator color
- [x] Handle playback completion and cleanup
- [x] Prevent simultaneous playback of multiple recordings
- [x] Handle missing audio file errors gracefully

## Card Layout

- [x] Inner card padding: top/sides 10px, bottom 12px

## Verification
- ~~All menu icons correctly associated~~ → Task 0016
- [ ] No UI overlap at any window size
- [ ] Button states provide clear feedback
- [ ] Full transcription viewable in-app
- [ ] Audio playback works reliably
- [ ] Manifest stays in sync with filesystem
