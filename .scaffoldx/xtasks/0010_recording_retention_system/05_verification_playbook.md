# Verification Playbook: Recording Retention System

**Task**: 0010
**Status**: Implementation Complete
**Generated**: 2026-02-01

---

## Quick Summary

This task adds persistent storage for audio recordings, allowing users to browse, play, and manage their recent recordings. Recordings are automatically retained after transcription and pruned when the configured limit is exceeded.

**Solution**: RecordingRetentionManager copies recordings to `%APPDATA%\WhisperDog\recordings`, maintains a JSON manifest with metadata, and integrates with a new Recordings panel accessible from the side menu.

---

## Stack Detection

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Main application language |
| Maven | - | Build system |
| Gson | 2.10.1 | JSON serialization for manifest |
| Swing | - | UI components (RecordingsPanel) |

**Key Files Modified**:
- `src/main/java/org/whisperdog/ConfigManager.java` - Retention settings
- `src/main/java/org/whisperdog/MainForm.java` - Menu routing
- `src/main/java/org/whisperdog/recording/RecorderForm.java` - Integration point
- `src/main/java/org/whisperdog/settings/SettingsForm.java` - Settings UI
- `src/main/java/org/whisperdog/sidemenu/Menu.java` - Menu item

**Files Created**:
- `src/main/java/org/whisperdog/recording/RecordingManifest.java`
- `src/main/java/org/whisperdog/recording/RecordingRetentionManager.java`
- `src/main/java/org/whisperdog/recording/RecordingsPanel.java`

---

## Testing Steps

### Build Verification

```bash
cd C:\__dev\_projects\whisperdog
mvn compile

# Expected: BUILD SUCCESS (warnings OK, no errors)
```

### Manual Verification

#### Test 1: Basic Retention
1. Build: `mvn package`
2. Launch WhisperDog
3. Go to Settings > enable "Keep recent recordings"
4. Make a recording and transcribe
5. Navigate to "Recordings" in side menu
6. **Expected**: Recording appears with timestamp, duration, size, preview

#### Test 2: File System Check
1. After Test 1, open `%APPDATA%\WhisperDog\recordings\`
2. **Expected**:
   - `manifest.json` file exists
   - `recording_YYYYMMDD_HHmmss.wav` file exists
   - JSON contains entry with matching filename

#### Test 3: Pruning
1. Set retention count to 3 in Settings
2. Make 5 recordings
3. Check recordings folder
4. **Expected**: Only 3 most recent recordings remain (oldest 2 deleted)

#### Test 4: Play Button
1. Open Recordings panel
2. Click "Play" on any recording
3. **Expected**: Default audio player opens with the recording

#### Test 5: Delete Button
1. Open Recordings panel
2. Click "Delete" on a recording
3. Confirm deletion
4. **Expected**:
   - Recording removed from list
   - File deleted from disk
   - Manifest updated

#### Test 6: Settings Persistence
1. Change retention settings (count, path)
2. Close and relaunch app
3. **Expected**: Settings preserved

#### Test 7: Disable/Enable Toggle
1. Disable "Keep recent recordings"
2. Make a recording
3. **Expected**: No new file in recordings folder
4. Re-enable and record again
5. **Expected**: New file appears

#### Test 8: Custom Storage Path
1. Set custom storage path in Settings
2. Make a recording
3. **Expected**: Recording saved to custom path

#### Test 9: Channel File Retention (Dual-Source)
1. Enable dual-source recording (Settings > System Audio)
2. Enable "Retain channel files (for debugging)"
3. Make a dual-source recording
4. Check recordings folder
5. **Expected**: 3 files: `recording_*.wav`, `recording_*_mic.wav`, `recording_*_system.wav`

#### Test 10: Channel Files Pruned Together
1. Set retention count to 2
2. Enable channel file retention
3. Make 3 dual-source recordings
4. **Expected**: Oldest recording AND its channel files deleted

---

## Verification Matrix

| Checklist Item | Status | Verification |
|----------------|--------|--------------|
| ConfigManager settings (5 methods) | ✅ | Lines 905-1010 |
| RecordingManifest with JSON serialization | ✅ | Atomic write via temp file |
| RecordingRetentionManager | ✅ | Null-safe channel handling |
| RecorderForm integration | ✅ | Lines 2290-2300 |
| Settings UI (all controls) | ✅ | Enable, count, path, channel files |
| RecordingsPanel | ✅ | Cards with Play/Delete |
| Menu integration | ✅ | Index 1 = Recordings |
| Scrutinizer bug fix | ✅ | RecordingEntry constructor fixed |

### Pending Manual Verification

| Item | Status | Notes |
|------|--------|-------|
| Basic retention | ⬜ | Test 1 |
| Manifest JSON structure | ⬜ | Test 2 |
| Pruning (count=3, 5 recordings) | ⬜ | Test 3 |
| Settings persistence | ⬜ | Test 6 |
| Channel file retention | ⬜ | Test 9 |
| Channel files pruned together | ⬜ | Test 10 |

---

## Code Changes Summary

### ConfigManager.java (Lines 905-1010)

```java
// Retention settings
isRecordingRetentionEnabled() / setRecordingRetentionEnabled()
getRecordingRetentionCount() / setRecordingRetentionCount()
getRecordingStoragePath() / setRecordingStoragePath()
getRecordingsDirectory()
isRetainChannelFilesEnabled() / setRetainChannelFilesEnabled()
```

### RecorderForm.java (Lines 2290-2300)

```java
// Retention integration in AudioTranscriptionWorker.done()
if (transcribedFile != null && transcript != null) {
    recordingRetentionManager.retainRecording(
        transcribedFile,    // merged file
        audioFile,          // mic channel
        systemTrackFile,    // system channel (may be null)
        recordingDurationMs,
        transcript,
        isDualSource
    );
}
```

---

## Rollback Plan

If issues arise:

### Option 1: Disable Feature
```
Settings > uncheck "Keep recent recordings"
```

### Option 2: Git Revert
```bash
git revert 75c5c35
mvn package
```

---

## Related Commits

| Commit | Message |
|--------|---------|
| 75c5c35 | feat(TASK-0010): implement recording retention system |

---

## Risk Assessment

| Risk | Level | Mitigation |
|------|-------|------------|
| Disk space usage | LOW | Default 20 recordings, pruning active |
| IO errors during copy | LOW | Graceful failure, logs warning |
| Manifest corruption | LOW | Atomic write (temp file + rename) |
| Performance impact | LOW | Copy runs after transcription completes |
