# PRD: Incremental Audio Writes for Crash Resilience

## Problem

Two crash-loss vectors exist in the current recording architecture. They have different severity and require different solutions.

### Vector 1: System audio path (HIGH risk)

`SystemAudioCapture.java:322` initializes `capturedAudio = new ByteArrayOutputStream()`. Every audio callback at lines 388-390 appends converted bytes: `capturedAudio.write(converted)`. The entire buffer is retrieved only on `stop()` at lines 555-557: `return capturedAudio.toByteArray()`. `AudioCaptureManager.java:157-162` then writes the byte array to a WAV file via `writeWavFile()`.

If the JVM crashes during recording, all system audio data is lost. For a 10-minute recording at 16kHz mono 16-bit, this is approximately 19 MB held in memory with zero on-disk backup.

### Vector 2: Mic audio path (LOW risk)

`AudioRecorder.java:44` calls `AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile)` which streams directly from `TargetDataLine` to disk in real-time. Audio data IS on disk during recording. However, the WAV header (RIFF chunk size at byte offset 4, data chunk size at byte offset 40) is only written correctly when the stream completes normally. JVM crash mid-recording produces a file on disk with size fields set to 0 or -1. Technically unplayable by strict parsers, though many audio tools auto-repair truncated WAVs.

## Solution

### Part 1: System audio incremental writes (HIGH priority)

Replace the `ByteArrayOutputStream` accumulation in `SystemAudioCapture.java` with an `IncrementalWavWriter` that writes audio chunks to disk as they arrive.

**Target files**:

- `SystemAudioCapture.java`: Replace `ByteArrayOutputStream capturedAudio` with `IncrementalWavWriter` instance
- `AudioCaptureManager.java:157-162`: Replace `writeWavFile(systemCapture.stop())` with file reference from writer

**Behavior**:

- Writer opened at capture start with audio format parameters (sample rate, bit depth, channels) derived from the WASAPI loopback format after conversion
- Each audio callback writes converted PCM bytes to disk AND updates WAV header sizes via `RandomAccessFile.seek()`
- On normal `stop()`: finalize header, return file reference
- On JVM crash: partial WAV file on disk, playable up to last write, header reflects last-written sizes. If crash occurs before first write, the file contains only the 44-byte header with Subchunk2Size=0 (empty but valid WAV).

**Constraints**:

- Audio format: determined at runtime by WASAPI loopback. After conversion in `SystemAudioCapture`, output is 16-bit mono PCM at 16kHz (resampled from the system's native rate, typically 44100 or 48000 Hz). The `IncrementalWavWriter` must be initialized with the post-conversion format parameters, not the WASAPI native format.
- Thread safety: audio callback runs on WASAPI thread. Writer must handle concurrent calls (synchronized writes)
- Existing consumers (`AudioCaptureManager.stopCapture()`) must receive a `File` reference instead of `byte[]`. This changes the return type of `SystemAudioCapture.stop()`

### Part 2: Mic WAV header resilience (LOW priority)

Add periodic header updates to the mic recording path so that a crash-produced WAV file has valid RIFF/data sizes.

**Target file**: `AudioRecorder.java`

**Approach**: `AudioSystem.write()` is a blocking call that handles the entire stream. Replacing it requires switching to manual WAV writing (same `IncrementalWavWriter` class). The tradeoff: more code, but crash-resilient headers.

**Alternative**: Post-crash header repair in the scanner (task 0017). When `PreservedRecordingScanner` finds a WAV with invalid header sizes, calculate correct sizes from file length and repair in-place. Simpler, no changes to recording path, but only works if task 0017 is implemented.

**Error handling**:

- Disk full during write: `writer.write()` throws IOException. Audio callback must catch, log, set error flag, and stop writing. On `stop()`, report the error to caller. Partial WAV up to last successful write remains valid.
- Writer initialization failure (IOException on file creation): Set error flag immediately, log the failure, and fail system-audio recording start with a clear user-visible error. Do not fall back to in-memory buffering.
- `stop()` called before writer initialized (WASAPI format negotiation failed or no audio callbacks received): Return null file reference. Caller must handle null.

## Requirements

- Audio written incrementally during recording (system audio path)
- Partial WAV files are valid and playable after crash
- No audible artifacts or gaps in normal recordings
- Memory usage bounded (not proportional to recording duration)
- SilenceRemover compatibility maintained (standard WAV PCM format)
- Normal flow (start -> stop -> transcribe) produces identical results to current behavior
- `AudioCaptureManager.stopCapture()` returns file references (not byte arrays)

## Out of Scope

- Retry UI for recovered files (task 0017)
- Recording format changes (remain WAV/PCM)
- Mic/system source selection logic
- Transcription pipeline changes
