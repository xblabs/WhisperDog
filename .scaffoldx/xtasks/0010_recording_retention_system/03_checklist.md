# Checklist: Recording Retention System

## Phase 1: Configuration Settings
- [x] Add `isRecordingRetentionEnabled()` / `setRecordingRetentionEnabled()` to ConfigManager
- [x] Add `getRecordingRetentionCount()` / `setRecordingRetentionCount()` to ConfigManager
- [x] Add `getRecordingStoragePath()` / `setRecordingStoragePath()` to ConfigManager
- [x] Add `getRecordingsDirectory()` helper to ConfigManager
- [x] Add `isRetainChannelFilesEnabled()` / `setRetainChannelFilesEnabled()` to ConfigManager

## Phase 2: Data Model
- [x] Create `RecordingManifest.java` with `RecordingEntry` inner class
- [x] Implement JSON serialization with Gson
- [x] Implement `load()` / `save()` methods with atomic write
- [x] Implement `addRecording()`, `removeRecording()`, `pruneToCount()` methods
- [x] Add `micChannelFile` (String, nullable) field to RecordingEntry
- [x] Add `systemChannelFile` (String, nullable) field to RecordingEntry
- [x] Add getters/setters for channel file fields

## Phase 3: Retention Manager
- [x] Create `RecordingRetentionManager.java`
- [x] Update `retainRecording()` signature to accept `micChannelFile` and `systemChannelFile` parameters
- [x] Implement merged file copy with filename pattern: `recording_YYYYMMDD_HHmmss.wav`
- [x] Implement channel file copy when `isRetainChannelFilesEnabled()` is true
  - [x] Copy mic channel with pattern: `recording_YYYYMMDD_HHmmss_mic.wav` (if file exists)
  - [x] Copy system channel with pattern: `recording_YYYYMMDD_HHmmss_system.wav` (if file exists)
  - [x] Set `micChannelFile` and `systemChannelFile` in manifest entry (nullable)
- [x] Implement automatic pruning when count exceeds limit
  - [x] Delete merged file
  - [x] Delete channel files if they exist (null-safe)
- [x] Update `deleteRecording()` to delete channel files along with merged file
- [x] Implement `openInFileManager()` for folder access
- [x] Implement `getRecordings()` and `getAudioFile()` accessors

## Phase 4: RecorderForm Integration
- [x] Add `RecordingRetentionManager` field to RecorderForm
- [x] Initialize manager in constructor
- [x] Modify `AudioTranscriptionWorker.done()` finally block:
  - [x] Capture channel files: `micChannel = audioFile`, `systemChannel = systemTrackFile`
  - [x] Call `retainRecording()` with merged file + channel files before cleanup
  - [x] Pass channel files to `retainRecording()` (systemChannel may be null for single-source)
- [x] Add cleanup for merged file after retention
- [x] Add getter `getRecordingRetentionManager()` for UI access

## Phase 5: Settings UI
- [x] Add "Recording Retention" section to SettingsForm
- [x] Add enable/disable checkbox
- [x] Add retention count spinner (1-100)
- [x] Add storage path text field with Browse button
- [x] Add "Open Folder" button
- [x] Add "Retain channel files (for debugging)" checkbox

## Phase 6: Recordings Browser
- [x] Create `RecordingsPanel.java`
- [x] Implement list view with recording cards
- [x] Show timestamp, duration, size, dual-source flag, preview
- [x] Add Play button (opens in default audio player)
- [x] Add Delete button with confirmation
- [x] Add Refresh button
- [x] Add "Open Folder" header button

## Phase 7: Side Menu Integration
- [x] Add "Recordings" menu item to Menu.java
- [x] Wire up to show RecordingsPanel in main content area
- [x] Ensure panel refreshes when shown

## Verification
- [ ] Test basic retention (recording saved to recordings dir)
- [ ] Test manifest JSON structure
- [ ] Test pruning (set count to 3, make 5 recordings)
- [ ] Test settings persistence across app restart
- [ ] Test recordings browser navigation and actions
- [ ] Test custom storage path
- [ ] Test disable/enable retention toggle
- [ ] Test channel file retention:
  - [ ] Enable `retainChannelFiles` setting
  - [ ] Make dual-source recording
  - [ ] Verify 3 files saved: `recording_YYYYMMDD_HHmmss.wav`, `*_mic.wav`, `*_system.wav`
  - [ ] Verify manifest entry has `micChannelFile` and `systemChannelFile` populated
- [ ] Test single-source with channel retention enabled:
  - [ ] Make microphone-only recording
  - [ ] Verify only merged file saved (no channel files)
  - [ ] Verify manifest entry has `micChannelFile` and `systemChannelFile` as null
- [ ] Test channel files pruned with parent recording:
  - [ ] Set retention count to 2
  - [ ] Make 3 dual-source recordings with channel retention enabled
  - [ ] Verify oldest recording AND its channel files deleted
