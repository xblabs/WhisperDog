---
id: ISS_00012
title: Audio files lost on transcription failure - no WAV or MP3 recovery
status: in-progress
priority: critical
type: bug
created: 2026-02-17T17:18:24.753Z
updated: 2026-02-17T19:30:11.813Z
tags: [recording, data-loss, file-management]
relates_to: []
scrutinized: 2026-02-17
started: 2026-02-17T19:30:11.807Z
---

# Audio files lost on transcription failure - no WAV or MP3 recovery

## Problem Description

When a recording exceeds OpenAI's 25 MB file size limit, WhisperDog compresses it to MP3 before sending. If the API call fails (e.g., HTTP 400), **all audio files are deleted** by cascading cleanup logic. The original WAV, the silence-removed WAV, the system audio track (if dual-source), and the compressed MP3 are all lost, with no possibility of recovery or retry.

This is a critical data-loss bug: 4+ minutes of dictation can vanish with zero recoverability.

### Root Cause

Three cascading cleanup mechanisms delete files unconditionally, and none of the deletion calls gate on transcription success:

1. **OpenAITranscribeClient.transcribe() finally** (L328-335): Unconditionally deletes the compressed MP3 on any exit path, including exceptions
2. **OpenAITranscribeClient.transcribeWithTimestamps() finally** (L499-505): Identical unconditional MP3 deletion in the timestamps variant
3. **RecorderForm.done() finally** (L2303-2308): Calls `cleanupTempAudioFile()` on `audioFile`, `systemTrackFile`, and `transcribedFile` regardless of transcription outcome
4. **SilenceRemover.deleteOnExit()** (L215): Schedules `_nosilence.wav` for JVM-exit deletion when `keepCompressed == false`

Note: `RecorderForm.finally` does gate `recordingRetentionManager.retainRecording()` on `transcribedFile != null && transcript != null` (L2291), but the file deletion calls at L2303-2308 execute unconditionally after that block.

The result: on a transient API error, all file variants are destroyed sequentially before any retry or recovery can occur.

### File Lifecycle

Understanding which variable holds which file at each stage:

```text
Recording starts
  └─ audioFile = whisperdog_mic_YYYYMMDD_HHMMSS.wav  (original WAV, always exists)
  └─ systemTrackFile = whisperdog_sys_*.wav            (only if dual-source recording)

Silence removal (if enabled and recording long enough)
  └─ SilenceRemover.removeSilence(audioFile, ...) returns:
       - audioFile itself (if no silence detected, or error)
       - NEW file: audioFile_nosilence.wav (if silence removed)
  └─ transcribedFile = return value of removeSilence()
       So: transcribedFile == audioFile OR transcribedFile == _nosilence.wav

Compression (if transcribedFile > 25 MB)
  └─ compressedFile = whisperdog_compressed_*.mp3
       Created INSIDE OpenAITranscribeClient.transcribe() as a local variable
       The caller (RecorderForm) has NO visibility into this file

API call
  └─ Sends compressedFile (if created) or transcribedFile to OpenAI

Cleanup (current broken behavior — ALL paths):
  1. OpenAITranscribeClient.finally → deletes compressedFile (MP3)
  2. RecorderForm.finally → deletes audioFile (original WAV)
  3. RecorderForm.finally → deletes systemTrackFile (system WAV, if exists)
  4. RecorderForm.finally → deletes transcribedFile (if != audioFile, i.e. _nosilence.wav)
  5. JVM exit → deleteOnExit() on _nosilence.wav (if keepCompressed == false)
```

### Canonical Success Predicate

All gating logic in this issue uses a single predicate. Define it once, reference it everywhere:

```java
/** Canonical predicate — used by RecorderForm, ChunkedTranscriptionWorker, and acceptance tests. */
private boolean isSuccessfulTranscript(String transcript) {
    return transcript != null && !transcript.isBlank();
}
```

This is the ONLY definition of "transcription succeeded" for this issue. Every section below that references success means `isSuccessfulTranscript(transcript) == true`.

### Transcription Outcome Definitions

The fix must distinguish three outcomes:

| Outcome | Condition | File behavior |
|---------|-----------|---------------|
| **Success** | `isSuccessfulTranscript(transcript)` returns `true` | Delete all temp files after retention |
| **User cancellation** | `cancelledByUser == true` (user dismissed large-recording warning) | Delete all temp files (user chose to discard) |
| **Failure** | Exception thrown OR `isSuccessfulTranscript(transcript)` returns `false` with `cancelledByUser == false` | Preserve ALL files, log paths |

## Evidence

### Incident Log (2026-02-17)

```text
16:49:50 Recording started: whisperdog_mic_20260217_164950.wav
17:02:15 Recording stopped (7+ min dictation)
17:02:15 File size 29.4 MB exceeds 25 MB limit → compressing
17:02:17 Compressed to MP3: 3.6 MB
17:02:20 OpenAI API 400: "error parsing the body"
17:02:20 TranscriptionException thrown
         → OpenAITranscribeClient.finally: deletes MP3
         → RecorderForm.done().finally: deletes original WAV + _nosilence.wav
         ALL FILES GONE
```

### File State After Failure

- `whisperdog_mic_20260217_164950.wav` (29.4 MB) — **deleted by RecorderForm L2303**
- `whisperdog_mic_20260217_164950_nosilence.wav` — **deleted by RecorderForm L2307** (as `transcribedFile`)
- `whisperdog_compressed_18299898910594631625.mp3` (3.6 MB) — **deleted by OpenAITranscribeClient L330**
- Other WAV files in temp folder — present (unrelated recordings)

### Affected Code Paths

| File | Method | Location | Behavior |
|------|--------|----------|----------|
| `OpenAITranscribeClient.java` | `transcribe()` | finally block L328-335 | Unconditionally deletes `compressedFile` (MP3) |
| `OpenAITranscribeClient.java` | `transcribeWithTimestamps()` | finally block L499-505 | Unconditionally deletes `compressedFile` (MP3) |
| `RecorderForm.java` | `done()` | finally block L2303 | `cleanupTempAudioFile(audioFile)` — deletes original WAV |
| `RecorderForm.java` | `done()` | finally block L2304 | `cleanupTempAudioFile(systemTrackFile)` — deletes system track WAV |
| `RecorderForm.java` | `done()` | finally block L2306-2307 | `cleanupTempAudioFile(transcribedFile)` — deletes `_nosilence.wav` (when `transcribedFile != audioFile`) |
| `SilenceRemover.java` | `removeSilence()` | L214-216 | `compressedFile.deleteOnExit()` when `keepCompressed == false` |

## Goals

- Original WAV files must **never** be deleted when transcription fails (per outcome table above)
- Compressed/intermediate files must be preserved on failure for manual recovery
- File cleanup must only occur after confirmed success or user cancellation
- `OpenAITranscribeClient` must not own file lifecycle — it must not delete files

## Scope

### In Scope

- Fix file deletion logic in `OpenAITranscribeClient.java` (both `transcribe()` and `transcribeWithTimestamps()`), `RecorderForm.java`, and `SilenceRemover.java`
- Ensure failure paths preserve all audio files
- Add logging when files are cleaned up (with reason) and when files are preserved (with paths)
- Audit `ChunkedTranscriptionWorker.java` L279-282 for the same `deleteOnExit()` pattern

### Out of Scope

- OpenAI API 400 error root cause (likely a malformed MP3 — separate investigation)
- Retry logic improvements (existing `transcribeWithRetry` is separate concern)
- Recording retention policy changes
- Streaming WAV writes during recording (separate issue — see Notes)

## Deliverables

### D1. Remove file deletion from OpenAITranscribeClient (both methods)

**Files**: `OpenAITranscribeClient.java`
**Locations**: `transcribe()` L328-335 AND `transcribeWithTimestamps()` L499-505

Remove both `finally` blocks that delete `compressedFile`. The client must not own file lifecycle.

To make `compressedFile` visible to the caller, choose ONE of the following approaches:

**Option A (preferred — stateless):** Change `transcribe()` and `transcribeWithTimestamps()` to return a result carrier object instead of a raw `String`/`TranscriptionResult`. The carrier includes the transcript AND the compressed file path (if any):

```java
public class TranscribeOutput {
    public final String transcript;           // or TranscriptionResult for timestamps variant
    public final File compressedFile;         // null if no compression was needed
}
```

This eliminates mutable state on the client and is safe for any future reuse pattern.

**Option B (acceptable — single-flight constraint):** Add a `private File lastCompressedFile` field. Reset it to `null` at the top of each `transcribe()`/`transcribeWithTimestamps()` call before any work begins. Expose via `getLastCompressedFile()`. **Constraint**: `OpenAITranscribeClient` is single-flight per recording — the caller must not invoke `transcribe()` concurrently on the same instance. This constraint is already satisfied by the current `RecorderForm` call pattern (sequential SwingWorker), but document it with a class-level Javadoc note.

The caller (`RecorderForm`) uses whichever approach is chosen to include the MP3 in its cleanup or preservation logic.

### D2. Gate RecorderForm cleanup on transcription outcome

**File**: `RecorderForm.java`
**Location**: `done()` finally block L2287-2309

Replace the unconditional cleanup calls at L2303-2308 with outcome-gated logic:

```java
// In the finally block, after retention:
boolean transcriptionSucceeded = isSuccessfulTranscript(transcript);
boolean userCancelled = cancelledByUser;

if (transcriptionSucceeded || userCancelled) {
    // Success or user-chosen discard: clean up all temp files
    cleanupTempAudioFile(audioFile);
    cleanupTempAudioFile(systemTrackFile);
    if (transcribedFile != null && !transcribedFile.equals(audioFile)) {
        cleanupTempAudioFile(transcribedFile);
    }
    // Also clean up compressed MP3 if OpenAITranscribeClient created one
    File compressedMp3 = transcribeClient.getLastCompressedFile();
    if (compressedMp3 != null) {
        cleanupTempAudioFile(compressedMp3);
    }
} else {
    // Failure: preserve all files, log paths for recovery
    logPreservedFiles(audioFile, systemTrackFile, transcribedFile,
                      transcribeClient.getLastCompressedFile());
}
```

### D3. Replace SilenceRemover.deleteOnExit() with caller-managed cleanup

**File**: `SilenceRemover.java`
**Location**: L214-216

Remove the `deleteOnExit()` call entirely. The `_nosilence.wav` file is already tracked as `transcribedFile` in `RecorderForm` and will be cleaned up by D2's gated logic on success. The `keepCompressed` parameter remains — when `true`, the file is preserved permanently (existing behavior). When `false`, the caller deletes it after success (new behavior from D2).

### D4. Add file-preservation logging

**File**: `RecorderForm.java`

Add a `logPreservedFiles(...)` method that:

- Logs each non-null, existing file to both `logger.warn()` and `ConsoleLogger`
- Per-file format (exact): `Audio file preserved for recovery: <file.getAbsolutePath()> (<String.format(Locale.US, "%.2f", sizeBytes / 1_048_576.0)> MB)`
- Summary format (exact): `Transcription failed. <count> audio file(s) preserved in <ConfigManager.getTempDirectory().getAbsolutePath()>`

### D5. Audit ChunkedTranscriptionWorker for same pattern

**File**: `ChunkedTranscriptionWorker.java`
**Location**: L279-282

Check whether chunk file deletion uses `deleteOnExit()` and apply the same gating pattern using the following failure-state matrix:

**Chunk success predicate**: A chunk succeeds when `isSuccessfulTranscript(chunkTranscript)` returns `true` for that chunk's individual API response.

| Scenario | Condition | File behavior |
|----------|-----------|---------------|
| All chunks succeed, assembly succeeds | Every chunk returns `isSuccessfulTranscript == true` AND final assembled transcript is non-blank | Delete all chunk files |
| One or more chunks fail (incl. retries exhausted) | Any chunk throws exception or returns blank/null after all retry attempts | Preserve ALL chunk files (not just the failed one) |
| All chunks succeed, assembly fails | Chunks OK but final concatenation/post-processing throws | Preserve ALL chunk files |

**Rationale for preserving all chunks on partial failure**: Individual chunks have no recovery value — only the complete set allows manual reassembly or retry.

## Acceptance Criteria

- [ ] When transcription fails (exception or null transcript without cancellation), all audio files (`audioFile`, `transcribedFile`/`_nosilence.wav`, `compressedFile`/MP3, `systemTrackFile`) remain in the temp directory
- [ ] Console log output on failure includes absolute paths of all preserved files
- [ ] When transcription succeeds, temp files are deleted immediately after retention (not deferred to JVM exit)
- [ ] When user cancels via large-recording warning, temp files are deleted (user chose to discard)
- [ ] No `deleteOnExit()` calls remain for audio files in the transcription pipeline (`SilenceRemover`, `ChunkedTranscriptionWorker`)
- [ ] `OpenAITranscribeClient` does not delete any files in either `transcribe()` or `transcribeWithTimestamps()` — file lifecycle is caller-managed
- [ ] `OpenAITranscribeClient` exposes `getLastCompressedFile()` so the caller can manage the MP3 lifecycle
- [ ] Manual test: record >25 MB, disconnect network, verify all files preserved after API failure
- [ ] Manual test: record >25 MB, successful transcription, verify all temp files cleaned up after completion

## Related

- **Task 0007**: Whisper Vocabulary Hints (transcription accuracy)
- **SilenceRemover.java**: Currently modified in working tree (git status shows changes)
- **src/main/java/org/whisperdog/tools/**: New untracked directory (potentially related tooling)
- **ChunkedTranscriptionWorker.java**: Same `deleteOnExit()` pattern needs audit (D5)
- **FfmpegCompressor.java**: Creates the MP3 that `OpenAITranscribeClient` currently deletes — ownership mismatch is the root of the lifecycle bug

## Status

- **Current Status**: open
- **Priority**: critical
- **Type**: bug
- **Created**: 2026-02-17
- **Scrutinized**: 2026-02-17

## Tags

- recording
- data-loss
- file-management

## Work Log

### Session: 2026-02-17

**Diagnosis**:

- Reviewed all four affected files: `OpenAITranscribeClient.java`, `RecorderForm.java`, `SilenceRemover.java`, `ChunkedTranscriptionWorker.java`
- Confirmed root cause: three cascading unconditional file deletion mechanisms (two `finally` blocks in OpenAITranscribeClient, one `finally` block in RecorderForm, plus `deleteOnExit()` in SilenceRemover and ChunkedTranscriptionWorker)

**Approach**:

- Option B for D1: `lastCompressedFile` field + `getLastCompressedFile()` getter (less invasive than changing return types)
- Canonical `isSuccessfulTranscript()` predicate for all gating decisions
- Three-outcome model: success (cleanup), user cancellation (cleanup), failure (preserve + log)

**Steps Taken**:

1. **D1**: Removed both `finally` deletion blocks in `OpenAITranscribeClient`. Added `lastCompressedFile` field with getter. Added class-level Javadoc documenting single-flight constraint.
2. **D2**: Replaced unconditional cleanup in `RecorderForm.done()` with outcome-gated logic. Failure path preserves all files and logs paths.
3. **D3**: Removed `compressedFile.deleteOnExit()` in `SilenceRemover.removeSilence()`. File lifecycle now caller-managed.
4. **D4**: Added `logPreservedFiles()` / `logPreservedFile()` methods with exact log formats per issue spec.
5. **D5**: Gated `cleanupChunks()` in `ChunkedTranscriptionWorker.done()`. Added `isSuccessfulChunkedTranscript()` and `logPreservedChunks()`. Removed `deleteOnExit()` fallback.
6. Verified `mvn compile` succeeds.

**Outcome**:

- All five deliverables implemented across 4 files
- Build compiles cleanly
- Status: in-progress (pending manual testing per acceptance criteria)

## Notes

- Triggered by real incident: 4+ minutes of dictation lost with no recovery. The bug has likely existed since MP3 compression was introduced for oversized files.
- **Streaming WAV writes** (writing audio to disk incrementally during recording) is a separate concern that addresses crash-resilience, not transcription-failure recovery. It should be filed as a separate issue to avoid scope creep. This issue focuses strictly on the file deletion gating bug.
- **Preserved file accumulation**: Files preserved on failure remain in the system temp directory until the user manually deletes them. This is acceptable for a critical-priority data-loss fix. A future enhancement could add a "recover failed recordings" UI, but that is out of scope here.