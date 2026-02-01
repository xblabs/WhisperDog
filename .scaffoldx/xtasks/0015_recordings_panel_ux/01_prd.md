# PRD: Recordings Panel UX Improvements

## Problem Statement

The Recordings Panel has multiple UX issues affecting usability:

1. **Menu Icon Misalignment**: Icons are loaded by position index (`icon/{menuIndex}.svg`) at `MenuItem.java:65`. When "Recordings" was inserted at position 1, all icons shifted - Recordings shows gear icon, Settings shows pipeline icon, etc.

2. **Content Padding/Overlap**: The Refresh and Open Folder buttons overlap with window close controls. Content hugs the left menu edge. Need ~24px padding around content area.

3. **Button Interactivity**: Play and Delete buttons lack hover states, hand cursor, and click feedback. No visual indication of interactivity.

4. **Fixed Transcription Ellipsis**: Transcription preview is truncated at fixed 80 characters regardless of available space. Cannot view full transcription without opening external player.

5. **No Inline Playback**: Clicking Play opens system audio player, which is inconsistent UX. Should play inline with progress visualization.

6. **Manifest/Filesystem Desync**: The recordings manifest (`recordings.json`) doesn't reconcile with actual files on disk. After recompilation or if files are manually added/removed, the UI doesn't reflect physical state. Refresh button should scan filesystem and sync.

## Impact

- **Icon bug**: Confusing navigation, unprofessional appearance
- **Padding bug**: UI elements overlap, poor visual hierarchy
- **Button states**: Users unsure if buttons are clickable
- **Fixed ellipsis**: Can't read full transcription, wasted screen space
- **External playback**: Context switching, inconsistent experience
- **Manifest desync**: Data integrity issues, missing recordings, orphaned entries

## Solution Overview

### Phase 1: Critical Fixes (Icon + Padding + Manifest Sync)
- Change icon loading from index-based to name-based mapping
- Add icon for Recordings menu item (or use existing icon)
- Add 24px content padding to RecordingsPanel
- Implement filesystem scan on refresh to reconcile manifest

### Phase 2: Button Interactivity
- Add hover background color change
- Add hand cursor on hover
- Add pressed/click state visual feedback
- Consider creating reusable ButtonStyler utility for UI kit

### Phase 3: Expandable Transcription
- Make transcription text clickable
- Toggle between truncated and full view
- Add visual indicator (expand icon or "..." link)
- Animate expand/collapse transition

### Phase 4: Responsive Ellipsis
- Add ComponentListener for resize events
- Calculate available width dynamically
- Adjust truncation based on actual space
- Debounce resize calculations for performance

### Phase 5: Inline Audio Playback
- Implement audio playback using javax.sound.sampled.Clip
- Add progress bar (2-3px height) below each recording card
- Use system audio indicator color (light blue)
- Toggle Play/Stop button state
- Handle playback completion and cleanup
- Support concurrent playback prevention (stop current before starting new)

## Success Metrics

- All menu icons correctly associated
- No UI element overlap at any window size
- Buttons show clear interactive states
- Full transcription viewable without leaving app
- Audio playback works inline with visual progress
- Manifest always reflects actual filesystem state

## Technical Constraints

- Must work with existing FlatLaf theming
- Audio playback must not block EDT
- Progress updates must be thread-safe
- Must handle missing audio files gracefully

## Out of Scope

- Audio waveform visualization
- Playback speed control
- Audio editing capabilities
- Cloud sync of recordings
