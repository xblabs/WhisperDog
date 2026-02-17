# Implementation Plan: Audio Recovery and Crash Resilience

## Part A: Retry Failed Transcriptions

### A1. Preserved file scanner

New class `PreservedRecordingScanner.java` in `org.whisperdog.recording`:
- Scan temp dir for `whisperdog_*` audio files
- Filter by age (< 7 days) and extensions (`.wav`, `.mp3`)
- Group related files by timestamp prefix
- Return list of `PreservedRecording` objects

### A2. Recovery dialog

New class `RecoveryDialog.java` in `org.whisperdog.ui`:
- Modal dialog with file listing (name, size, date)
- Retry All / Discard All / Open Folder / Close buttons
- Discard confirmation prompt
- FlatLaf themed

### A3. Re-submission logic

New `RecoveryWorker.java` (SwingWorker):
- Accept `PreservedRecording`, submit best available file (prefer WAV over MP3)
- Run through existing transcription pipeline
- Success: show transcript, clean up files
- Failure: leave files in place, show error

### A4. Integration

- Side menu: "Recover recordings" item -> scanner -> dialog
- Startup: post-init background scan -> dialog if files found

## Part B: Incremental WAV Writes

### B1. Incremental WAV writer

New class `IncrementalWavWriter.java` in `org.whisperdog.recording`:
- Opens WAV file at recording start with placeholder header
- `write(byte[] chunk)` appends audio data and updates header
- Uses `RandomAccessFile` to seek back and update RIFF/data sizes
- `finalize()` writes final header on normal stop
- Flush interval: configurable, default every 1-2 seconds

### B2. Recording pipeline integration

Modify recording classes to use `IncrementalWavWriter` instead of in-memory buffers:
- Replace `ByteArrayOutputStream` accumulation with incremental writes
- Recording stop triggers `finalize()` instead of `AudioSystem.write()`
- File reference available immediately (not just after stop)

### B3. SilenceRemover compatibility

- Verify `SilenceRemover.removeSilence()` works with incrementally-written WAVs
- No changes expected (standard WAV format) but needs verification

### B4. Crash recovery validation

- Verify partial WAV files are playable in standard audio tools
- Verify header is self-consistent after each flush
- Test: kill JVM mid-recording, verify partial file is recoverable
