---
id: ISS_00011
title: Drag-and-drop functionality for audio/MP4 files not working in recording screen
status: resolved
priority: high
type: bug
created: 2026-01-30T16:20:51.022Z
updated: 2026-01-30T16:31:20.791Z
tags: [regression, drag-drop, ui]
relates_to: []
started: 2026-01-30T16:31:20.786Z
---

# Drag-and-drop functionality for audio/MP4 files not working in recording screen

## Problem Description

Drag-and-drop functionality for audio and MP4 video files into the recording screen has stopped working. Previously, users could drag audio files (WAV, MP3, OGG, M4A, FLAC) or video files (MP4, MOV, MKV, AVI, WEBM) onto the recording panel, the cursor would change to indicate a valid drop zone, and the file would be imported for transcription. This feature is now completely non-functional - the cursor does not change and files cannot be dropped.

**Location**: [RecorderForm.java](src/main/java/org/whisperdog/recording/RecorderForm.java)

**User Impact**: Users must use alternative methods to transcribe pre-recorded files, breaking an intuitive and frequently-used workflow.

## Evidence

The drag-and-drop implementation code is present and appears intact in the codebase:

**Line 348**: UI hint label present

```java
JLabel dragDropLabel = new JLabel("Drag & drop an audio or video file here.");
```

**Line 633**: Setup method is called

```java
setupDragAndDrop(centerPanel);
```

**Lines 663-715**: Complete TransferHandler implementation exists

- `setupDragAndDrop()` method creates TransferHandler
- `canImport()` method checks for file data flavors
- `importData()` method routes to `handleDroppedAudioFile()` or `handleDroppedVideoFile()`

**Observation**: Code structure suggests drag-and-drop should be working, indicating this is likely a runtime initialization issue or panel configuration problem rather than missing code.

## Goals

- Restore drag-and-drop functionality for audio and video files

- Ensure cursor changes to indicate drop zone when dragging files over the recording panel
- Verify all supported file formats work correctly (audio: WAV, MP3, OGG, M4A, FLAC; video: MP4, MOV, MKV, AVI, WEBM)

## Scope

**In Scope:**

- Investigate why TransferHandler is not responding to drag events

- Verify panel configuration and event handling setup
- Test cursor feedback during drag operations
- Confirm file import and transcription flow after drop

**Out of Scope:**

- Adding new file format support

- Changing drag-and-drop UI feedback design
- Modifying transcription processing logic

## Deliverables

1. Root cause analysis of why drag-and-drop stopped working

2. Code fix to restore drag-and-drop functionality
3. Verification that cursor feedback works correctly during drag operations
4. Test coverage for audio and video file drops

## Acceptance Criteria

- [ ] User can drag audio files (WAV, MP3, OGG, M4A, FLAC) onto recording panel

- [ ] User can drag video files (MP4, MOV, MKV, AVI, WEBM) onto recording panel
- [ ] Cursor changes to indicate valid drop zone when dragging files over panel
- [ ] Files are correctly imported and routed to transcription after drop
- [ ] Error messages appear for unsupported file types
- [ ] Functionality verified on Windows platform

## Related

**Related Tasks:**

- Task 0005: MP4 Drag-Drop Support - Previous work on video file support

- Task 0006: System Audio Capture - Recent changes to recording infrastructure

**Recent Commits to RecorderForm.java:**

- `0f80ef4` - fix: gate dual-source merge/attribution on actual content

- `c91f875` - feat(TASK-0006): add device labels and word-level attribution
- `42d3725` - fix(TASK-0006): address sluggishness audit findings

**Potential Investigation Areas:**

- Panel layering or visibility changes that might block drop events

- Component focus or enable state changes
- Event listener registration timing
- UI layout changes affecting hit detection

## Status

- **Current Status**: open
- **Priority**: high
- **Type**: bug
- **Created**: 2026-01-30

## Tags

- regression
- drag-drop
- ui

## Work Log

### Session: 2026-01-30

**Diagnosis**:

- Compared code between v2.0.0 release (322a902) and HEAD
- Found the `canImport()` method in `setupDragAndDrop()` was modified to add: `if (!support.isDrop()) return false;`
- Root cause: In Swing DnD, `canImport()` is called during both drag-over (cursor feedback) AND actual drop
- The `isDrop()` check returns `false` during drag-over, causing all drag-over events to be rejected
- This broke cursor feedback (hand cursor never shown) making users think DnD wasn't working

**Approach**:

- Remove the erroneous `isDrop()` check from `canImport()`
- Keep the comment about not accessing `getTransferData()` during drag-over (the actual cause of InvalidDnDOperationException)
- The existing code already correctly only checks `isDataFlavorSupported()` which is safe during drag-over

**Steps Taken**:

1. Searched codebase for drag-and-drop related code in [RecorderForm.java:663-715](src/main/java/org/whisperdog/recording/RecorderForm.java#L663-L715)
2. Compared git diff between v2.0.0 and HEAD to identify the change
3. Removed the `if (!support.isDrop()) return false;` check from `canImport()`
4. Updated comment to clarify what causes InvalidDnDOperationException (accessing transfer data, not checking flavor)
5. Compiled project successfully with `mvn compile`

**Outcome**:

- Fixed [RecorderForm.java:666-670](src/main/java/org/whisperdog/recording/RecorderForm.java#L666-L670)
- `canImport()` now correctly returns `true` during drag-over for file list flavors
- Cursor feedback should now work when dragging files over the recording panel
- Status: resolved (pending user verification)

## Notes

Issue scaffolded via x-issue-create. Content to be filled by LLM.