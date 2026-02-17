# PRD: Audio Recovery and Crash Resilience

## Problem

Two remaining data-loss gaps after ISS_00012:

1. **No recovery path for preserved files**: Failed transcriptions now preserve audio files in temp, but users have no in-app way to retry. They see console logs with paths but must manually locate files.
2. **Mid-recording crash loss**: Audio is held in memory during recording and only written to disk on stop. JVM crash, OOM, or OS kill = total loss of captured audio.

## Part A: Retry Failed Transcriptions

### User Flow

**Startup detection**:
1. App launches, scans `ConfigManager.getTempDirectory()` for orphaned `whisperdog_*` files
2. If found, show dialog listing files with name, size, last modified date
3. Actions: **Retry All** | **Discard All** | **Open Folder**
4. Retry queues files through existing transcription pipeline
5. Discard deletes after confirmation

**Menu item**: "Recover recordings" in side menu, opens same dialog on demand. Toast if no files found.

### Requirements

- Scan for `whisperdog_mic_*.wav`, `whisperdog_*_nosilence.wav`, `whisperdog_compressed_*.mp3`
- Age filter: only files from last 7 days
- After successful retry, clean up preserved files
- After failed retry, leave in place (ISS_00012 behavior)

## Part B: Incremental WAV Writes

### Problem

Audio accumulated in memory buffers during recording, flushed to WAV only on stop. JVM crash mid-recording = total loss.

### Solution

Replace record-to-memory with incremental disk writes:
- Open WAV file at recording start
- Write audio chunks to disk periodically (every 1-2 seconds)
- Update WAV header (RIFF + data chunk sizes) on each flush
- Finalize header on normal stop
- On crash, partial WAV is playable up to last flush

### Requirements

- Audio written incrementally during recording
- Partial WAV files are valid and playable
- No audible artifacts or gaps
- Memory usage bounded (not proportional to duration)
- SilenceRemover compatibility maintained
- Normal flow (start -> stop -> transcribe) produces identical results

## Out of Scope

- Automatic retry on failure (separate concern)
- Recording format changes (remain WAV/PCM)
- Dual-source recording pipeline changes
