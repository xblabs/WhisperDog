# PRD: Synchronized Dual-Source Silence Removal

## 1. Problem Statement

### 1.1 Current Behavior

When system audio capture is active, silence removal is completely bypassed:

```java
// RecorderForm.java - current logic
if (systemTrackHasContent) {
    // Merge tracks, skip silence removal (breaks timeline alignment)
} else if (configManager.isSilenceRemovalEnabled()) {
    // Mic-only: apply silence removal
}
```

This means dual-source recordings always include all dead air. A 60-second recording with 20 seconds of mutual silence still sends all 60 seconds to Whisper.

### 1.2 Why Simple Silence Removal Breaks Dual-Source

Removing silence from only one track desynchronizes the tracks:

```
Before:  Mic    [speech][silence][speech]   = 10s
         System [speech][silence][speech]   = 10s  (frame-aligned)

After:   Mic    [speech][speech]            = 7s   (silence removed)
         System [speech][silence][speech]   = 10s  (untouched, misaligned!)
```

Source attribution relies on both tracks being frame-synchronized. Word timestamps from Whisper map to positions in the merged audio, which must correlate back to both original tracks.

### 1.3 Proposed Solution

Detect silence in both tracks independently, compute the intersection (where BOTH are silent simultaneously), and splice the exact same frame ranges from both tracks:

```
Mic silence:     ----XXXX------XXXX----XXXXXXXX----
System silence:  XXXXXXXX------XXXXXXXX--------XXXX
                 ----------------------------------
Common silence:  ----XXXX-----------XX----------XXX-
(intersection)       ^^^^          ^^           ^^^  <- cut from BOTH

Result:  Both tracks shortened identically, still frame-aligned
```

## 2. Solution Architecture

### 2.1 Two-Phase Approach

```
Phase 1: Intersection-Based Silence Detection
         - Analyze both tracks independently
         - Compute overlapping silence regions
         - Splice identical frame ranges from both
         - Merge the pruned tracks

Phase 2: Zero-Crossing Alignment
         - Snap cut points to nearest zero-crossing
         - Prevent audio clicks/pops at splice points
         - Crossfade at boundaries (optional)
```

### 2.2 Phase 1: Intersection-Based Silence Detection

**Algorithm:**

```java
// 1. Detect silence regions in each track
List<SilenceRegion> micSilence = detectSilenceRegions(micTrack, threshold, minDuration);
List<SilenceRegion> sysSilence = detectSilenceRegions(sysTrack, sysThreshold, minDuration);

// 2. Compute intersection (both tracks silent simultaneously)
List<SilenceRegion> commonSilence = intersectRegions(micSilence, sysSilence);

// 3. Splice identical frame ranges from both tracks
byte[] prunedMic = spliceFrames(micTrack, commonSilence);
byte[] prunedSys = spliceFrames(sysTrack, commonSilence);

// 4. Both tracks are still frame-aligned, safe to merge
File mergedFile = mergeAudioTracks(prunedMic, prunedSys);
```

**Key Constraints:**
- Both tracks must have the same sample rate (22050 Hz, enforced by capture)
- Cut regions must be frame-aligned (same start/end frame in both tracks)
- System audio threshold = mic threshold × 0.5 (system audio is pre-processed/normalized)
- Minimum silence duration = 500ms (configurable, but default to 500ms)

### 2.3 Phase 2: Zero-Crossing Alignment

Cutting audio at arbitrary points can produce clicks. Snap cut boundaries to the nearest zero-crossing sample within a 5ms window (±110 samples at 22050 Hz) to ensure clean splices.

## 3. Technical Specifications

### 3.1 New/Modified Classes

| Class | Modification |
|-------|-------------|
| `SilenceRemover` | Add `detectSilenceRegions()` returning frame-level regions |
| `SilenceRemover` | Add `intersectRegions()` for computing region overlap |
| `SilenceRemover` | Add `removeSynchronizedSilence(micFile, sysFile, ...)` entry point |
| `RecorderForm` | Update dual-source path to use synchronized removal before merge |
| `FFmpegUtil` (or new util) | Support merging pre-pruned byte arrays or temp files |

### 3.2 Data Structures

```java
// Frame-level silence region
public static class SilenceRegion {
    public final int startFrame;  // inclusive
    public final int endFrame;    // exclusive
    public final float durationSeconds;
}

// Synchronized silence removal result
public static class SyncResult {
    public final File micFile;       // Pruned mic track (temp file)
    public final File sysFile;       // Pruned system track (temp file)
    public final int removedRegions; // Count of removed silence regions
    public final long removedMs;     // Total milliseconds removed
    public final boolean wasProcessed; // false if no common silence found
}
```

### 3.3 Integration Point (RecorderForm)

```java
if (systemTrackHasContent) {
    if (configManager.isSilenceRemovalEnabled()) {
        // Synchronized silence removal: prune both tracks identically
        SilenceRemover.SyncResult pruned = SilenceRemover.removeSynchronizedSilence(
            audioFile,                                  // File: mic WAV
            validatedSystemTrack,                       // File: system WAV
            configManager.getSilenceThreshold(),        // float: mic silence dB threshold
            configManager.getSilenceThreshold() * 0.5f, // float: system threshold (50% of mic)
            500,                                        // int: min silence duration ms
            configManager.getTempDirectory()            // File: output temp dir
        );
        if (pruned.wasProcessed) {
            fileToTranscribe = FFmpegUtil.mergeAudioTracks(pruned.micFile, pruned.sysFile);
        } else {
            // No common silence found, merge originals
            fileToTranscribe = FFmpegUtil.mergeAudioTracks(audioFile, validatedSystemTrack);
        }
    } else {
        // Merge without silence removal
        fileToTranscribe = FFmpegUtil.mergeAudioTracks(audioFile, validatedSystemTrack);
    }
}
```

## 4. Acceptance Criteria

| ID | Requirement | Test Method |
|----|-------------|-------------|
| FR-1 | Common silence regions correctly identified from both tracks | Unit test with synthetic audio |
| FR-2 | Identical frame ranges removed from both tracks | Assert byte-level frame counts match |
| FR-3 | Merged output is shorter than unprocessed merge | Compare durations |
| FR-4 | Source attribution still works on pruned+merged audio | End-to-end test with dual-source |
| FR-5 | No audio clicks at splice points (Phase 2) | Manual listening test |
| NFR-1 | Processing latency < 100ms for 60s dual recording | Benchmark |
| NFR-2 | Backward compatible: mic-only silence removal unchanged | Regression test |

## 5. Edge Cases

| Edge Case | Expected Behavior |
|-----------|-------------------|
| No common silence (both tracks always have audio) | Skip removal, merge as-is |
| Entire recording is mutual silence (>90% silent) | Skip silence removal, return `wasProcessed=false` (existing min speech check handles empty transcription) |
| Tracks different lengths | Use shorter track's length as boundary |
| Common silence below min duration threshold | Skip that region (don't cut tiny gaps) |
| Single common silence region at end | Trim trailing silence from both |

## 6. Error Handling

| Error | Detection | Recovery |
|-------|-----------|----------|
| FFmpeg merge failure | Non-zero exit code from `FFmpegUtil.mergeAudioTracks()` | Fall back to unprocessed merge, log warning |
| Track length mismatch > 1s | Compare durations after loading both tracks | Truncate longer track to shorter length, log warning |
| OOM on large file (>10min) | `OutOfMemoryError` in `spliceFrames()` | Process in 60s chunks, merge chunk results |
| I/O error reading track | `IOException` during file read | Abort silence removal, return `wasProcessed=false`, use original files |
| Zero-crossing not found in window | No crossing within ±110 samples | Use original cut point (accept potential click) |

## 7. Related

- **Task 0008**: Dual-Source Attribution Accuracy - complementary (improves WHO), this task improves WHAT gets sent
- **SilenceRemover.java**: Existing single-track logic to extend
- **SourceActivityTracker.java**: Must still work correctly on pruned+merged output
