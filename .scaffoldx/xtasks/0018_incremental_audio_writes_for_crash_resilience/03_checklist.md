# Checklist: Incremental Audio Writes for Crash Resilience

## Phase 1: IncrementalWavWriter

- [x] Create `src/main/java/org/whisperdog/recording/IncrementalWavWriter.java` (`org.whisperdog.recording`)
- [x] Implement constructor: open `RandomAccessFile`, write 44-byte WAV header with placeholder sizes (ChunkSize=36, Subchunk2Size=0)
- [x] Implement `write(byte[], int, int)`: append PCM data, seek to offset 4 and write updated ChunkSize, seek to offset 40 and write updated Subchunk2Size, seek back to end. Method is `synchronized`.
- [x] Implement `close()`: final header update, close `RandomAccessFile`
- [x] Implement `getFile()` and `getBytesWritten()` accessors
- [ ] **Verify**: Write 10 seconds of synthetic PCM data in 4096-byte chunks. Open resulting WAV in an audio player. Confirm playable, correct duration, no artifacts. Kill writer mid-write (don't call close), confirm partial file is also playable with correct header sizes up to last write.

## Phase 2: SystemAudioCapture integration

- [x] Replace `ByteArrayOutputStream capturedAudio` field with `IncrementalWavWriter writer` field in `src/main/java/org/whisperdog/audio/SystemAudioCapture.java` (field at line 45, init at line 322)
- [x] Initialize writer after WASAPI format negotiation, using converted format params (sample rate, bit depth, channels). Writer file placed in `ConfigManager.getTempDirectory()` with pattern `whisperdog_system_YYYYMMDD_HHMMSS.wav`
- [x] Replace `capturedAudio.write(converted)` (lines 388-390) with `writer.write(converted, 0, converted.length)`
- [x] Add IOException handling in audio callback: log error, set error flag, do not throw
- [x] Change `stop()` return type from `byte[]` to `File`. Implementation: call `writer.close()`, return `writer.getFile()`
- [ ] **Verify**: Record 30 seconds of system audio with dual-source enabled. Confirm system track file exists in temp dir during recording (not just after stop). Confirm final WAV is playable and identical quality to current behavior. Confirm `SilenceRemover.removeSilence()` accepts the file without errors.

## Phase 3: AudioCaptureManager wiring

- [x] Update `src/main/java/org/whisperdog/audio/AudioCaptureManager.java` to consume `File` from `systemCapture.stop()` instead of `byte[]` (lines 157-162)
- [x] Remove `writeWavFile()` method (lines 304-330) - no longer needed
- [x] Confirm `stopCapture()` returns mic file and system file references correctly
- [ ] **Verify**: Full dual-source recording flow: start -> record 15 seconds -> stop -> transcribe. Confirm transcript produced, both tracks retained if retention enabled, no behavioral change from user perspective.

## Phase 4: Crash resilience validation

- [ ] Test: start dual-source recording, record 10 seconds, force-kill JVM (Task Manager "End Process" or `taskkill /F /PID <pid>` on Windows)
- [ ] Confirm system track WAV file exists in temp dir with valid header
- [ ] Confirm partial system track is playable in VLC or similar
- [ ] Confirm mic track also exists on disk (already true, just verify)
- [ ] Confirm memory usage during a 5-minute recording is bounded (not proportional to duration)

## Phase 5: Regression

- [ ] Verify mic-only recording flow: record -> stop -> transcribe. No behavioral change.
- [ ] Verify dual-source recording flow: record -> stop -> transcribe. No behavioral change.
- [ ] Verify failed transcription still preserves files in temp dir (ISS_00012 behavior unchanged)
- [ ] Verify SilenceRemover works with incrementally-written WAVs
- [ ] Verify ChunkedTranscriptionWorker accepts incrementally-written WAVs
- [x] Build project with `mvn package` - confirm no compilation errors

## Phase 6 (optional): Mic WAV header resilience

- [ ] Decision: replace `AudioSystem.write()` in `src/main/java/org/whisperdog/recording/AudioRecorder.java` with `IncrementalWavWriter`, OR defer to header repair in task 0017 scanner
- [ ] If replacing: implement manual read loop from `TargetDataLine` (4096-byte buffer) into `IncrementalWavWriter`
- [ ] If replacing: verify mic-only recording produces identical WAV output
- [ ] If deferring: document decision in task 0017 as a future enhancement to `PreservedRecordingScanner`
