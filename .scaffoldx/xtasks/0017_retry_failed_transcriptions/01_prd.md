# PRD: Retry Failed Transcriptions

## Problem

ISS_00012 added file preservation on transcription failure. Preserved audio files remain in `ConfigManager.getTempDirectory()` (platform temp + `/WhisperDog/`), but users have no in-app path to retry them. Console logs print file paths, but users must manually locate and re-process files outside the app.

## User Flow

### Startup detection

1. App launches. After `MainForm` initialization completes (after native library extraction and hotkey setup), a background `SwingWorker` scans `ConfigManager.getTempDirectory()` for recoverable files.
2. Scanner returns a list of `RecoverableSession` objects (grouped by timestamp).
3. If the list is non-empty, show `RecoveryDialog` on EDT.
4. If empty, do nothing (no toast, no UI disruption).

### Recovery dialog

Modal dialog listing recoverable sessions. Each row shows:
- Recording timestamp (extracted from filename)
- Selected file name, size, last modified date
- Source type label (e.g., "Raw recording", "Silence-removed", "Compressed")

**Actions**:
- **Retry All**: Queue all sessions for transcription via `RecoveryTranscriptionWorker`. Dialog stays open showing per-session progress (pending/transcribing/done/failed). Close button enabled after all complete.
- **Discard All**: Confirmation prompt ("Delete N files permanently?"). On confirm, delete all listed files. On cancel, return to dialog.
- **Open Folder**: `Desktop.open(ConfigManager.getTempDirectory())`.
- **Close**: Dismiss dialog. Files remain in place for next startup or manual trigger.

### On-demand trigger

"Recover" button added to `RecordingsPanel` toolbar (alongside existing Refresh and Open Folder buttons). Clicking it runs the same scanner. If files found, opens `RecoveryDialog`. If no files found, shows toast: "No recoverable recordings found."

## File Scanner Requirements

### Target directory

`ConfigManager.getTempDirectory()` - resolves to `%TEMP%\WhisperDog\` on Windows, `/tmp/WhisperDog/` on Unix.

### File patterns

Scan for files matching these patterns:
- `whisperdog_mic_YYYYMMDD_HHMMSS.wav` (raw mic recording)
- `whisperdog_mic_YYYYMMDD_HHMMSS_nosilence.wav` (silence-removed)
- `whisperdog_compressed_*.mp3` (compressed for API upload)

### Filters

- **Age**: Only files modified within the last 7 days. Older files are stale and likely orphaned from abandoned sessions.
- **Recency guard**: Exclude files modified within the last 60 seconds. These may belong to an active recording in progress.
- **Recording state guard**: If `RecorderForm.isRecording()` returns true, exclude all `whisperdog_mic_*` files with today's date prefix. Prevents scanner from picking up the file currently being written.

### Grouping and selection

Files are grouped into sessions by timestamp prefix (`YYYYMMDD_HHMMSS`). For each session, the scanner selects one file to retry using this priority:

1. `*_nosilence.wav` - silence removal completed successfully before failure; best candidate (smaller, cleaner audio)
2. `whisperdog_mic_*.wav` - raw recording; usable but may contain silence
3. `whisperdog_compressed_*.mp3` - lossy; only if WAV variants are missing

Compressed files (`whisperdog_compressed_*.mp3`) have random suffixes and cannot be grouped by timestamp. They are listed as standalone sessions.

All files in a session (not just the selected one) are tracked. On successful retry, ALL files in that session are deleted. On failure, ALL files are preserved.

## Transcription and Retention

### Pipeline entry point

A new `RecoveryTranscriptionWorker` (extends `SwingWorker<String, String>`) handles recovery transcription. It does NOT reuse `AudioTranscriptionWorker` because that class depends on `RecorderForm` state (active recording context, pre-flight dialogs, dual-source merge logic) that is unavailable during recovery.

`RecoveryTranscriptionWorker` performs:
1. Call `OpenAITranscribeClient.transcribe(selectedFile)` directly. No silence removal (already done or not applicable). No pre-flight analysis (file already exists, user explicitly chose to retry).
2. On success: call `RecordingRetentionManager.retainRecoveredRecording(audioFile, transcription)` to persist the transcript with `imported = true`.
3. On failure: leave all session files in place. Report error to dialog.

### Retention integration

`RecordingRetentionManager` gets a new method:

```java
public synchronized RecordingManifest.RecordingEntry retainRecoveredRecording(
    File audioFile,
    String fullTranscription)
```

This method:
- Copies `audioFile` to recordings directory
- Saves transcription to text file
- Creates `RecordingEntry` with `imported = true`, `dualSource = false`, `durationMs` read from WAV header (or 0 if unreadable)
- Adds entry to manifest
- Returns the entry (or null if retention is disabled)

Recovered recordings appear in `RecordingsPanel` with source label "Imported" (the `imported` field already exists on `RecordingEntry`).

## Failure Modes

| Scenario | Behavior |
|----------|----------|
| Scanner finds 0 files | Startup: silent. On-demand: toast "No recoverable recordings found." |
| File is corrupt/truncated | `OpenAITranscribeClient.transcribe()` will fail. Files preserved. Error shown in dialog row. |
| Transcription backend config changed | Uses current config (API key, model, backend). If current config is invalid, standard TranscriptionException handling applies. |
| User closes dialog mid-retry | In-progress workers continue. Files cleaned up on success, preserved on failure. No orphan state. |
| Disk full during retention | `retainRecoveredRecording` returns null. Transcription text still shown in dialog. Original files preserved. User can copy transcript manually. |
| Retention disabled in settings | Transcription succeeds but is not persisted. Dialog shows transcript text. Original files cleaned up (transcription was successful). |
| Multiple retries of same session | If user opens dialog again after a failed retry, the same files appear. No duplicate state. |

## Out of Scope

- Automatic retry on failure (requires background scheduling, separate concern)
- Incremental WAV writes / crash resilience during recording (separate task with different premises)
- Recording format changes (remain WAV/PCM)
- Dual-source recording pipeline changes
- Individual file selection within a session (scanner picks the best candidate automatically)
