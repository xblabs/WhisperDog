# PRD: MP4 Drag-Drop Support

## Problem Statement

Users frequently need to transcribe audio from video files (screen recordings, meeting recordings, phone videos). Currently, WhisperDog only accepts audio files, forcing users to:

1. Open command line or separate tool
2. Run ffmpeg manually to extract audio
3. Locate the extracted audio file
4. Drag/load it into WhisperDog

**Impact:**
- Workflow friction for video transcription
- Requires technical knowledge (ffmpeg commands)
- Multiple steps for common use case
- Users may abandon transcription entirely

## Solution Overview

Enable drag-drop of video files directly onto WhisperDog:

1. **Drop Detection**: Recognize video file formats on drop
2. **Auto-Extraction**: Use ffmpeg to extract audio track
3. **Seamless Flow**: Queue extracted audio for transcription
4. **Cleanup**: Remove temp files after completion

## Functional Requirements

### FR-1: Video File Drop Detection

**Supported Formats**:
- MP4 (.mp4)
- MOV (.mov)
- MKV (.mkv)
- AVI (.avi)
- WEBM (.webm)

**Drop Target**: Main application window (same as current audio drop)

**Behavior**:
- Detect file extension on drop
- Show "Extracting audio..." status
- Distinguish from audio files (different flow)

### FR-2: FFmpeg Integration

**Detection**:
1. Check bundled ffmpeg in app directory
2. Fall back to system PATH
3. Show error if neither available

**Extraction Command**:
```bash
ffmpeg -i input.mp4 -vn -acodec pcm_s16le -ar 16000 -ac 1 output.wav
```

- `-vn`: No video output
- `-acodec pcm_s16le`: 16-bit PCM (WhisperDog standard)
- `-ar 16000`: 16kHz sample rate (OpenAI optimal)
- `-ac 1`: Mono audio

### FR-3: Progress Indication

**During Extraction**:
- Status: "Extracting audio from video..."
- Optional: Progress bar (if ffmpeg outputs progress)
- Disable other actions during extraction

**After Extraction**:
- Transition to normal transcription flow
- File shows as "extracted from: video.mp4"

### FR-4: Temp File Management

**Location**: System temp directory with unique names
**Pattern**: `whisperdog_extract_<timestamp>.wav`
**Cleanup**: Delete temp file after transcription completes
**On Error**: Clean up partial files

### FR-5: Error Handling

**No FFmpeg Available**:
```
Video transcription requires ffmpeg.
Install ffmpeg and ensure it's in your PATH.
Download: https://ffmpeg.org/download.html
```

**Extraction Failed**:
```
Failed to extract audio from video.
Error: [ffmpeg error message]
```

**No Audio Track**:
```
This video contains no audio track.
```

## UI Mockup

```
┌─────────────────────────────────────────────────────────┐
│  WhisperDog                                         [X] │
├─────────────────────────────────────────────────────────┤
│                                                         │
│     [User drags video.mp4 onto window]                 │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Extracting audio from video.mp4...             │   │
│  │  ████████████░░░░░░░░  60%                      │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│     [After extraction, normal transcription flow]       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Non-Functional Requirements

- Extraction should not block UI thread
- Support videos up to 2GB in size
- Temp file cleanup on app exit (safety net)
- Works on Windows (primary), macOS, Linux

## Out of Scope

- Video preview or playback
- Audio track selection (uses first/default track)
- Bundling ffmpeg with the app (user must install)
- Editing or trimming video before extraction
- Batch video processing
