# PRD: Recording Retention System

## Problem Statement

WhisperDog currently deletes all temporary audio files after transcription completes or when the application closes. This creates several user pain points:

1. **No debugging capability**: When transcription quality is poor, users cannot re-listen to the original audio to understand why
2. **No audit trail**: Users cannot correlate past transcriptions with their source recordings
3. **No re-processing**: If users want to try different settings (e.g., vocabulary hints), they must re-record

### Current Cleanup Behavior

| Location | Trigger | What's Deleted |
|----------|---------|----------------|
| `RecorderForm.java:2273` | After transcription | `audioFile`, `systemTrackFile` |
| `ChunkedTranscriptionWorker.java:279` | After chunk processing | Chunk files (with `deleteOnExit` fallback) |
| `SilenceRemover.java:215` | JVM shutdown | Compressed files via `deleteOnExit()` |

### Desired State

Keep the last N recordings (configurable, default 20) in a persistent location with:
- Automatic pruning when limit exceeded
- Metadata for easy browsing (timestamp, duration, transcription preview)
- Configurable storage location (default: `%APPDATA%\WhisperDog\recordings`)
- UI panel to browse, play, and manage recordings

## Requirements

### Functional Requirements

#### FR-1: Recording Retention
- **FR-1.1**: After successful transcription, copy the final audio file (merged/processed) to the recordings directory
- **FR-1.2**: Generate a unique filename using timestamp: `recording_YYYYMMDD_HHmmss.wav`
- **FR-1.3**: Store metadata in a JSON manifest file (`recordings.json`)
- **FR-1.4**: Prune oldest recordings when count exceeds configured limit
- **FR-1.5**: (Optional) Retain separate channel files for dual-source recordings
  - Controlled by `retainChannelFiles` setting (default: false)
  - Channel files stored with suffix: `recording_YYYYMMDD_HHmmss_mic.wav`, `recording_YYYYMMDD_HHmmss_system.wav`
  - Channel files pruned along with parent recording
  - Enables debugging and testing of dual-source attribution (see Task 0008)

#### FR-2: Configuration Settings
- **FR-2.1**: `recordingRetentionEnabled` (boolean, default: true)
- **FR-2.2**: `recordingRetentionCount` (int, default: 20, range: 1-100)
- **FR-2.3**: `recordingStoragePath` (string, default: config directory + "/recordings")
- **FR-2.4**: `retainChannelFiles` (boolean, default: false) - for debugging dual-source attribution

#### FR-3: Settings UI
- **FR-3.1**: Checkbox to enable/disable retention
- **FR-3.2**: Spinner to set retention count (1-100)
- **FR-3.3**: Text field + Browse button for custom storage path
- **FR-3.4**: "Open Folder" button to open recordings directory in file manager

#### FR-4: Recordings Browser
- **FR-4.1**: Panel accessible from side menu showing all retained recordings
- **FR-4.2**: Each entry shows: timestamp, duration, file size, transcription preview
- **FR-4.3**: Play button to open recording in default audio player
- **FR-4.4**: Delete button to remove individual recordings
- **FR-4.5**: "Open Folder" button to open recordings directory

### Non-Functional Requirements

#### NFR-1: Performance
- Retention should not block UI (copy in background thread)
- Pruning should complete in <100ms for typical retention counts

#### NFR-2: Reliability
- Manifest should be atomic (write to temp file, then rename)
- Failed retention should log warning but not fail transcription

#### NFR-3: Storage
- Default to config directory (persistent, not temp)
- Support custom paths including network drives

## Data Model

### RecordingManifest

```java
public class RecordingManifest {
    private List<RecordingEntry> recordings;

    public static class RecordingEntry {
        String id;                    // UUID for linking/referencing
        String filename;              // e.g., "recording_20260125_143022.wav"
        long timestamp;               // Unix timestamp (ms)
        long durationMs;              // Recording duration in milliseconds
        long fileSizeBytes;           // File size in bytes
        String transcriptionPreview;  // First ~200 chars of transcription
        String pipelineResult;        // Final pipeline output (if any)
        String executionLogSnippet;   // Key log lines for this recording
        boolean hasSystemAudio;       // Whether dual-source was enabled
        String micChannelFile;        // Optional: mic channel file (e.g., "recording_20260125_143022_mic.wav")
        String systemChannelFile;     // Optional: system channel file (null if single-source)
    }
}
```

### Example Manifest

```json
{
  "version": "1.0.0",
  "recordings": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "filename": "recording_20260125_143022.wav",
      "timestamp": 1737823822000,
      "durationMs": 45000,
      "fileSizeBytes": 1440000,
      "transcriptionPreview": "Hello, this is a test recording to verify the transcription quality...",
      "pipelineResult": "Cleaned transcript with formatting applied.",
      "executionLogSnippet": "Using OpenAI whisper-1\nFile: 45s, 1.4MB\nTranscription: 2.3s",
      "hasSystemAudio": true
    }
  ]
}
```

## User Interface

### Settings Panel Section

```
+--------------------------------------------------+
| Recording Retention                               |
+--------------------------------------------------+
| [x] Keep recent recordings                        |
|                                                   |
| Number to keep: [  20  ] (1-100)                 |
|                                                   |
| Storage location:                                 |
| [C:\Users\X\AppData\Roaming\WhisperDog\recordings]|
| [Browse...]  [Open Folder]                        |
+--------------------------------------------------+
```

### Recordings Browser Panel

```
+--------------------------------------------------+
| Recordings                           [Open Folder]|
+--------------------------------------------------+
| Jan 25, 2026 2:30:22 PM     [Play] [Delete]      |
| Duration: 0:45 | Size: 1.4 MB | Dual-source      |
| "Hello, this is a test recording to verify the..." |
+--------------------------------------------------+
| Jan 25, 2026 1:15:08 PM     [Play] [Delete]      |
| Duration: 2:30 | Size: 4.5 MB                    |
| "Meeting notes from the product review session..." |
+--------------------------------------------------+
| Jan 24, 2026 5:45:33 PM     [Play] [Delete]      |
| Duration: 1:12 | Size: 2.1 MB | Dual-source      |
| "The quarterly results show a 15% increase in..."  |
+--------------------------------------------------+
| Showing 3 of 20 recordings                        |
+--------------------------------------------------+
```

## Out of Scope

- Cloud backup of recordings
- Sharing recordings via URL
- Audio waveform visualization
- Re-transcription from retained recordings (future task)
- Compression of retained recordings

## Success Metrics

1. **Adoption**: >80% of users leave retention enabled (default on)
2. **Storage**: Average user uses <500MB for 20 recordings
3. **Performance**: Retention adds <200ms to transcription flow
