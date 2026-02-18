# Implementation Plan: Retry Failed Transcriptions

## New Classes

### 1. PreservedRecordingScanner

**Package**: `org.whisperdog.recording`
**File**: `src/main/java/org/whisperdog/recording/PreservedRecordingScanner.java`

Scans `ConfigManager.getTempDirectory()` for recoverable audio files from failed transcriptions.

```java
public class PreservedRecordingScanner {

    // Immutable value object representing a group of files from one recording session
    public static class RecoverableSession {
        private final String timestampPrefix;    // "YYYYMMDD_HHMMSS" or null for ungrouped
        private final File selectedFile;         // Best candidate for retry
        private final List<File> allFiles;       // All files in session (deleted together)
        private final String sourceLabel;        // "Raw recording" | "Silence-removed" | "Compressed"
        // getters only, no setters
    }

    public PreservedRecordingScanner(ConfigManager configManager) { ... }

    /**
     * Scan temp directory and return recoverable sessions.
     * Thread-safe. No side effects. May be called from any thread.
     *
     * @param isRecording true if a recording is currently in progress
     * @return list of recoverable sessions, empty if none found. Never null.
     */
    public List<RecoverableSession> scan(boolean isRecording) { ... }
}
```

**File selection priority per session**:

1. `*_nosilence.wav` (silence removal completed before failure)
2. `whisperdog_mic_*.wav` (raw recording)
3. `whisperdog_compressed_*.mp3` (lossy, standalone only)

**Filters applied**:

- Pattern match: `whisperdog_mic_*.wav`, `*_nosilence.wav`, `whisperdog_compressed_*.mp3`
- Age: `lastModified` within 7 days of scan time
- Recency guard: `lastModified` more than 60 seconds before scan time
- Recording guard: if `isRecording == true`, exclude files with today's date prefix

### 2. RecoveryDialog

**Package**: `org.whisperdog.ui`
**File**: `src/main/java/org/whisperdog/ui/RecoveryDialog.java`

Modal `JDialog` displaying recoverable sessions with action buttons.

```java
public class RecoveryDialog extends JDialog {

    public RecoveryDialog(
        Frame owner,
        List<PreservedRecordingScanner.RecoverableSession> sessions,
        RecordingRetentionManager retentionManager,
        ConfigManager configManager)

    // Called by RecoveryTranscriptionWorker to update per-row status
    // Must be called on EDT.
    public void updateSessionStatus(int sessionIndex, SessionStatus status, String detail)

    public enum SessionStatus { PENDING, TRANSCRIBING, DONE, FAILED }
}
```

**Layout**: FlatLaf-themed. Vertical list of session rows. Each row: timestamp, filename, size, date, status indicator. Bottom button bar: Retry All, Discard All, Open Folder, Close.

**Threading**: Dialog constructed and shown on EDT. Retry All launches `RecoveryTranscriptionWorker` instances on background threads. Workers call `updateSessionStatus()` via `SwingUtilities.invokeLater()`.

### 3. RecoveryTranscriptionWorker

**Package**: `org.whisperdog.recording`
**File**: `src/main/java/org/whisperdog/recording/RecoveryTranscriptionWorker.java`

`SwingWorker<String, String>` that transcribes a single recovered file and retains the result.

```java
public class RecoveryTranscriptionWorker extends SwingWorker<String, String> {

    public RecoveryTranscriptionWorker(
        PreservedRecordingScanner.RecoverableSession session,
        RecordingRetentionManager retentionManager,
        ConfigManager configManager,
        Consumer<String> onSuccess,     // receives transcription text
        Consumer<String> onFailure)     // receives error message

    @Override
    protected String doInBackground() throws Exception {
        // 1. Create OpenAITranscribeClient from configManager
        // 2. Call transcribe(session.getSelectedFile())
        // 3. On success: call retentionManager.retainRecoveredRecording(...)
        // 4. Return transcription text
    }

    @Override
    protected void done() {
        // On EDT: call onSuccess or onFailure callback
        // On success: delete all session files (session.getAllFiles())
        // On failure: leave files in place
    }
}
```

**Does NOT reuse AudioTranscriptionWorker** because that class depends on:

- `RecorderForm` state (active recording context)
- Pre-flight analysis dialogs (min speech duration, large file warnings)
- Dual-source merge logic (`FFmpegUtil.mergeAudioTracks`)
- SilenceRemover invocation

None of these apply to recovery (file already exists, user explicitly chose to retry, preprocessing already completed or not applicable).

## Modified Classes

### 4. RecordingRetentionManager

**File**: `src/main/java/org/whisperdog/recording/RecordingRetentionManager.java`
**Change**: Add new method after existing `retainRecording()` (after line ~140)

```java
/**
 * Retain a recovered recording with imported flag.
 * Simpler than retainRecording(): no channel files, no dual-source.
 *
 * @return entry if retained, null if retention disabled or failed
 */
public synchronized RecordingManifest.RecordingEntry retainRecoveredRecording(
    File audioFile,
    String fullTranscription)
{
    // 1. Check retention enabled
    // 2. Copy audioFile to recordings directory
    // 3. Save transcription to text file
    // 4. Read duration from WAV header via AudioSystem.getAudioFileFormat()
    //    (0 if unreadable - file may be MP3 or truncated)
    // 5. Create RecordingEntry: imported=true, dualSource=false
    // 6. Add to manifest and save
    // 7. Prune old recordings
    // 8. Return entry
}
```

### 5. RecordingsPanel

**File**: `src/main/java/org/whisperdog/recording/RecordingsPanel.java`
**Change**: Add "Recover" button to toolbar (lines 50-85, alongside Refresh and Open Folder)

```java
// After the "Open Folder" button creation (~line 68):
JButton recoverButton = new JButton("Recover");
recoverButton.setToolTipText("Retry failed transcriptions");
// Same styling as existing toolbar buttons
recoverButton.addActionListener(e -> {
    PreservedRecordingScanner scanner = new PreservedRecordingScanner(configManager);
    List<RecoverableSession> sessions = scanner.scan(recorderForm.isRecording());
    if (sessions.isEmpty()) {
        // Toast: "No recoverable recordings found."
    } else {
        new RecoveryDialog(parentFrame, sessions, retentionManager, configManager)
            .setVisible(true);
    }
});
```

**Dependency**: `RecordingsPanel` needs access to `ConfigManager` and a way to check recording state. Currently constructed at `MainForm.java:133` with `retentionManager` only. Constructor needs `configManager` parameter added. `RecorderForm.isRecording()` accessed via reference passed through or queried from `MainForm`.

### 6. MainForm

**File**: `src/main/java/org/whisperdog/MainForm.java`
**Change**: Add startup recovery scan after initialization (after line 78)

```java
// After: GlobalHotkeyManager.init(configManager);  (line 78)
// Launch background recovery scan
SwingWorker<List<RecoverableSession>, Void> startupScanner = new SwingWorker<>() {
    @Override
    protected List<RecoverableSession> doInBackground() {
        PreservedRecordingScanner scanner = new PreservedRecordingScanner(configManager);
        return scanner.scan(false);  // not recording at startup
    }
    @Override
    protected void done() {
        try {
            List<RecoverableSession> sessions = get();
            if (!sessions.isEmpty()) {
                RecoveryDialog dialog = new RecoveryDialog(
                    MainForm.this, sessions,
                    recorderForm.getRetentionManager(), configManager);
                dialog.setVisible(true);
            }
        } catch (Exception ignored) {}
    }
};
startupScanner.execute();
```

This runs the scan off-EDT (non-blocking) and shows the dialog on EDT only if files are found.

## Integration Diagram

```
Startup:
  MainForm init complete
    -> SwingWorker (background)
      -> PreservedRecordingScanner.scan(false)
    -> EDT (if files found)
      -> RecoveryDialog.setVisible(true)
        -> [Retry All]
          -> RecoveryTranscriptionWorker (per session, background)
            -> OpenAITranscribeClient.transcribe(file)
            -> RecordingRetentionManager.retainRecoveredRecording(file, text)
            -> delete session files on success
          -> RecoveryDialog.updateSessionStatus() (EDT)

On-demand:
  RecordingsPanel [Recover] button
    -> PreservedRecordingScanner.scan(recorderForm.isRecording())
    -> RecoveryDialog (same flow as above)
    -> Toast if no files
```

## Files Summary

| Action | File | Location |
| ------ | ---- | -------- |
| CREATE | `PreservedRecordingScanner.java` | `org.whisperdog.recording` |
| CREATE | `RecoveryDialog.java` | `org.whisperdog.ui` |
| CREATE | `RecoveryTranscriptionWorker.java` | `org.whisperdog.recording` |
| MODIFY | `RecordingRetentionManager.java` | Add `retainRecoveredRecording()` method |
| MODIFY | `RecordingsPanel.java` | Add "Recover" button to toolbar |
| MODIFY | `MainForm.java` | Add startup scanner after init |
