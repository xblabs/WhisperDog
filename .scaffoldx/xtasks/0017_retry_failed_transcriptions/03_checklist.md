# Checklist: Audio Recovery and Crash Resilience

## Part A: Retry Failed Transcriptions

### A1. Scanner

- [ ] Create `PreservedRecordingScanner` class
- [ ] Implement temp directory scan with file pattern matching
- [ ] Add age filter (7-day threshold)
- [ ] Group related files by recording session

### A2. Dialog

- [ ] Create `RecoveryDialog` with file listing
- [ ] Implement Retry All / Discard All / Open Folder actions
- [ ] Add confirmation prompt for Discard All

### A3. Re-submission

- [ ] Implement `RecoveryWorker` for background transcription
- [ ] Wire to existing transcription pipeline
- [ ] Clean up files on success, preserve on failure

### A4. Integration

- [ ] Add "Recover recordings" item to side menu
- [ ] Add post-init startup scanner hook
- [ ] Verify non-blocking behavior

## Part B: Incremental WAV Writes

### B1. Writer

- [ ] Create `IncrementalWavWriter` class
- [ ] Implement WAV header write with placeholder sizes
- [ ] Implement incremental data append + header update via RandomAccessFile
- [ ] Implement finalize for normal recording stop

### B2. Pipeline integration

- [ ] Replace in-memory buffer accumulation with incremental writes
- [ ] Wire recording stop to finalize
- [ ] Verify normal recording flow produces identical results

### B3. Compatibility

- [ ] Verify SilenceRemover works with incrementally-written WAVs
- [ ] Verify existing transcription pipeline accepts files

### B4. Crash validation

- [ ] Test: kill JVM mid-recording, verify partial WAV is playable
- [ ] Test: partial WAV header is self-consistent
- [ ] Test: memory usage bounded during long recordings
