# Checklist: Retry Failed Transcriptions

## Phase 1: Scanner

- [x] Create `PreservedRecordingScanner.java` in `org.whisperdog.recording`
- [x] Implement `RecoverableSession` value object (timestampPrefix, selectedFile, allFiles, sourceLabel)
- [x] Implement `scan()` method with file pattern matching (`whisperdog_mic_*.wav`, `*_nosilence.wav`, `whisperdog_compressed_*.mp3`)
- [x] Add 7-day age filter and 60-second recency guard
- [x] Add recording-in-progress guard (exclude today's files when `isRecording == true`)
- [x] Implement timestamp grouping and file priority selection (_nosilence > raw > compressed)
- [ ] **Verify**: Place test fixtures in temp dir, call `scan(false)`, confirm correct grouping and file selection. Confirm empty dir returns empty list. Confirm files older than 7 days are excluded. Confirm files younger than 60 seconds are excluded.

## Phase 2: Retention Extension

- [x] Add `retainRecoveredRecording(File audioFile, String fullTranscription)` to `RecordingRetentionManager.java` (after existing `retainRecording()`)
- [x] Implementation: copy file, save transcript, read WAV duration (0 if unreadable), create entry with `imported=true`, add to manifest, prune
- [ ] **Verify**: Call `retainRecoveredRecording()` with a test WAV file. Confirm file copied to recordings dir, manifest entry created with `imported=true`, transcription text file saved. Confirm null return when retention is disabled.

## Phase 3: Recovery Worker

- [x] Create `RecoveryTranscriptionWorker.java` in `org.whisperdog.recording`
- [x] Implement `doInBackground()`: create transcribe client based on configured server, call `transcribe(selectedFile)`
- [x] Implement `done()`: on success call `retainRecoveredRecording()` and delete all session files; on failure leave files in place
- [x] Wire `onSuccess` and `onFailure` callbacks (Consumer<String>)
- [ ] **Verify**: Run worker with a valid audio file and valid API key. Confirm transcription returned, recording retained with `imported=true`, temp files cleaned up. Run with invalid file. Confirm files preserved and `onFailure` called.

## Phase 4: Recovery Dialog

- [x] Create `RecoveryDialog.java` in `org.whisperdog.ui`
- [x] Implement session list layout (timestamp, filename, size, date, status indicator per row)
- [x] Implement Retry All button: launch `RecoveryTranscriptionWorker` per session, update row status via `SwingUtilities.invokeLater()`
- [x] Implement Discard All button with confirmation prompt, delete all listed files on confirm
- [x] Implement Open Folder button: `Desktop.open(ConfigManager.getTempDirectory())`
- [x] Implement Close button: dismiss dialog, no file changes
- [x] Apply FlatLaf theming consistent with existing dialogs
- [ ] **Verify**: Open dialog with mock sessions. Confirm all buttons work. Confirm Retry All updates row statuses. Confirm Discard All shows confirmation and deletes files. Confirm Close leaves files untouched.

## Phase 5: Integration

- [x] Add "Recover" button to `RecordingsPanel.java` toolbar (alongside Refresh and Open Folder, ~line 68)
- [x] Add `configManager` and `recorderForm` parameters to `RecordingsPanel` constructor; update call site in `MainForm.java` (~line 133)
- [x] Wire Recover button: scan -> dialog if files found, toast if empty
- [ ] **Verify**: Click Recover with no files in temp dir. Confirm toast appears. Place test fixtures, click Recover. Confirm dialog opens with correct file listing.

- [x] Add startup scanner to `MainForm.java` (after line 78, after `GlobalHotkeyListener` init)
- [x] Implement as `SwingWorker`: background scan, EDT dialog if files found
- [ ] **Verify**: Place test fixtures in temp dir, launch app. Confirm dialog appears after main window shows. Remove fixtures, launch app. Confirm no dialog and no delay.

## Phase 6: Regression

- [ ] Verify normal mic-only recording flow: record -> stop -> transcribe -> retained in RecordingsPanel. No behavioral change.
- [ ] Verify dual-source recording flow (if system audio available): same as above with dual-source. No behavioral change.
- [ ] Verify failed transcription still preserves files in temp dir (ISS_00012 behavior unchanged)
- [ ] Verify RecordingsPanel displays recovered recordings with "Imported" source label
- [x] Build project with `mvn package` - confirm no compilation errors
