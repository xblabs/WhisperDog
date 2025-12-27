---
task_id: "0005"
task_name: "MP4 Drag-Drop Support"
status: "todo"
priority: "low"
created: "2025-12-30"
task_type: "executable"
tags: ["audio", "mp4", "ffmpeg", "drag-drop", "file-handling"]
---

# Task 0005: MP4 Drag-Drop Support

## Summary

Add support for dragging MP4 video files directly onto the WhisperDog application window. The app will auto-extract audio via ffmpeg and queue it for transcription, enabling users to transcribe video content without manual audio extraction.

## Context

Users often have video recordings (screen captures, meeting recordings, phone videos) that need transcription. Currently they must manually extract audio using ffmpeg before using WhisperDog. This creates friction and requires command-line knowledge.

## Key Objectives

1. Implement drag-drop detection for video files on main window
2. Auto-detect ffmpeg installation or bundle it
3. Extract audio from MP4 to temp WAV file
4. Queue extracted audio for transcription
5. Clean up temp files after transcription

## Success Criteria

- User can drag MP4 file onto WhisperDog window
- Audio extraction happens automatically with progress indicator
- Transcription proceeds normally after extraction
- Works with common video formats (MP4, MOV, MKV, AVI)
- Graceful error handling if ffmpeg not available

## Related Files

- Source specification: WD-0008 from whisperdog.md
- Recording form: `src/main/java/org/whisperdog/recording/RecorderForm.java`
- File handling: `src/main/java/org/whisperdog/audio/`
