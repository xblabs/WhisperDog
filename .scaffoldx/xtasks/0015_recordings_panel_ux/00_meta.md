---
id: "0015"
title: Recordings Panel UX Improvements
slug: recordings_panel_ux
status: todo
priority: medium
created: 2026-02-01
task_type: executable
tags:
  - ui
  - ux
  - recordings
  - audio-playback
---

# Task 0015: Recordings Panel UX Improvements

## Summary

UX improvements for the Recordings Panel: content padding, button interactivity, expandable transcription text, and inline audio playback with progress visualization.

> **Note**: Menu icon work moved to Task 0016 (Icon Theming System).

## Context

The Recordings feature (Task 0010) was recently implemented. During verification, several UX issues were identified that impact usability and visual consistency.

## Scope

- ~~Fix menu icon order bug~~ → **Moved to Task 0016**
- Add proper content padding to prevent UI overlap
- Implement button hover/click states (use IconLoader from Task 0016)
- Add expandable/collapsible transcription preview
- Implement responsive ellipsis based on viewport
- Add inline audio playback with progress bar

## Dependencies

- Task 0010 (Recording Retention System) - completed
- Task 0016 (Icon Theming System) - provides IconLoader for button icons

## Acceptance Criteria

- ~~Menu icons correctly associated~~ → **Task 0016**
- [ ] Content area has 24px padding, no overlap with window controls
- [ ] Buttons show hover state, hand cursor, and click feedback
- [ ] Transcription text expands on click, collapses on second click
- [ ] Ellipsis adjusts based on panel width (responsive)
- [ ] Audio plays inline with progress bar visualization
- [ ] Play button toggles to Stop during playback
