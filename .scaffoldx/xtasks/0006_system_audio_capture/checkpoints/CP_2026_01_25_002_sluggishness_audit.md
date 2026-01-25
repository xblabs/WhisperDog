# Checkpoint: CP_2026_01_25_002 - Sluggishness Audit Fixes

**Date**: 2026-01-25
**Type**: save_point
**Resumed From**: CP_2026_01_25_001
**Status**: Audit fixes applied for continuous-use sluggishness

---

## Summary

Applied fixes for Codex audit findings focused on long-running session sluggishness risks. Reduced deleteOnExit() calls from 14 to 2 (justified cases only), fixed temp file leaks, and cached expensive EDT operations.

---

## Audit Findings Addressed

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 1 | Mic-only recordings leak `record_*.wav` in temp | High | FIXED |
| 2 | deleteOnExit() accumulates in JVM list | High | FIXED (12 removed) |
| 3 | In-memory buffering for system audio | Medium | DEFERRED (issue created) |
| 4 | isAvailable() blocks EDT | Medium | FIXED |

---

## Changes Made

### 1. Mic-only temp file leak (High)
**File**: [RecorderForm.java:1373](src/main/java/org/whisperdog/recording/RecorderForm.java#L1373)
- Changed temp file prefix from `record_` to `whisperdog_mic_`
- Now matches existing `cleanupTempAudioFile()` pattern

### 2. deleteOnExit() cleanup (High)
Removed deleteOnExit() from 12 locations, replaced with explicit cleanup:

| File | Lines | Cleanup Mechanism |
|------|-------|-------------------|
| AudioCaptureManager.java | 82, 96, 216 | cleanupTempFiles() |
| FFmpegUtil.java | 158, 319 | Caller's cleanupTempAudioFile() |
| RecorderForm.java | 1192, 1240 | cleanupTempAudioFile() |
| WavChunker.java | 142 | ChunkedTranscriptionWorker.cleanupChunks() |
| FfmpegChunker.java | 149 | ChunkedTranscriptionWorker.cleanupChunks() |
| FfmpegCompressor.java | 155 | cleanupTempAudioFile() |
| OpenAITranscribeClient.java | 57, 144 | New finally block in transcribe() |

**Remaining justified deleteOnExit() calls (2)**:
- SilenceRemover.java:215 - User-configurable, file in user's directory
- ChunkedTranscriptionWorker.java:281 - Fallback only when delete() fails

### 3. In-memory buffering (Medium) - DEFERRED
**Issue created**: [ISS_0006_001_inmemory_buffering_tradeoff.md](issues/ISS_0006_001_inmemory_buffering_tradeoff.md)
- Current ~58MB/30min buffer is manageable
- Full analysis of pros/cons documented
- Will address if users report actual issues

### 4. isAvailable() EDT optimization (Medium)
**File**: [SystemAudioCapture.java:47](src/main/java/org/whisperdog/audio/SystemAudioCapture.java#L47)
- Added `cachedAvailability` static field
- WASAPI availability cached after first check
- Prevents repeated native device enumeration on EDT

---

## Files Modified

- RecorderForm.java - temp file prefix fix
- SystemAudioCapture.java - availability caching
- AudioCaptureManager.java - removed 3 deleteOnExit()
- FFmpegUtil.java - removed 2 deleteOnExit()
- WavChunker.java - removed 1 deleteOnExit()
- FfmpegChunker.java - removed 1 deleteOnExit()
- FfmpegCompressor.java - removed 1 deleteOnExit()
- OpenAITranscribeClient.java - removed 2 deleteOnExit(), added cleanup finally block

---

## Build State

- Build: Clean (`mvn compile` succeeds)
- deleteOnExit() count: 14 → 2

---

## Remaining Work

- Manual GUI testing (Cluster 6): NOT STARTED
- Low-priority audit items (#13-17 from original audit): NOT ADDRESSED
- In-memory buffering refactor: DEFERRED (documented in issue)

---

**Checkpoint Created**: 2026-01-25
**Status**: Ready for commit
