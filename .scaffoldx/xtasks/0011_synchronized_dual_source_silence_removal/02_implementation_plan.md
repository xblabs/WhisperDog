# Implementation Plan: Synchronized Dual-Source Silence Removal

## Overview

This plan provides step-by-step implementation phases with explicit file:line targets, method signatures, and verification criteria. Each phase is independently testable and can be committed separately.

## Prerequisites

- ConfigManager.getTempDirectory() ✅ EXISTS (line 107)
- SilenceRemover.SilenceRegion ⚠️ EXISTS but private (line 66) - needs public exposure
- FFmpegUtil.mergeAudioTracks() ✅ EXISTS
- Integration point: RecorderForm.java:2121-2130 (dual-source branch)

---

## Phase 1: Data Structures and Region Detection

**Goal**: Expose SilenceRegion and add synchronized silence detection infrastructure.

### Step 1.1: Make SilenceRegion Public

**File**: `src/main/java/org/whisperdog/recording/SilenceRemover.java`
**Location**: Line 66-78

**Current**:
```java
private static class SilenceRegion {
    long startFrame;
    long endFrame;
```

**Change to**:
```java
/**
 * Represents a frame-aligned silence region in audio.
 * Used for synchronized dual-source silence removal where identical frame ranges
 * must be removed from both tracks.
 */
public static class SilenceRegion {
    public final long startFrame;   // inclusive
    public final long endFrame;     // exclusive
    public final float durationSeconds;  // pre-calculated for convenience
```

**Add constructor**:
```java
public SilenceRegion(long startFrame, long endFrame, float sampleRate) {
    this.startFrame = startFrame;
    this.endFrame = endFrame;
    this.durationSeconds = (endFrame - startFrame) / sampleRate;
}

// Legacy constructor for internal use
SilenceRegion(long startFrame, long endFrame) {
    this.startFrame = startFrame;
    this.endFrame = endFrame;
    this.durationSeconds = -1; // Not calculated
}

public long getDurationFrames() {
    return endFrame - startFrame;
}
```

**Impact**: Update internal usages (lines 159, 278, 323, 335, 396, 408) to use legacy constructor or pass sampleRate.

### Step 1.2: Add detectSilenceRegions() Public Method

**File**: `src/main/java/org/whisperdog/recording/SilenceRemover.java`
**Location**: After line 424 (after private detectSilence method)

**Signature**:
```java
/**
 * Detects silence regions in an audio file.
 * Returns frame-aligned regions suitable for synchronized removal.
 *
 * @param audioFile The audio file to analyze
 * @param silenceThresholdRMS RMS threshold (0.0-1.0, typically 0.01 = -40dB)
 * @param minSilenceDurationMs Minimum consecutive silence duration
 * @return List of silence regions, or empty list if analysis fails
 */
public static List<SilenceRegion> detectSilenceRegions(File audioFile,
                                                        float silenceThresholdRMS,
                                                        int minSilenceDurationMs)
```

**Implementation**: Extract common logic from existing detectSilenceQuiet() (line 294-340), return list of SilenceRegion with sampleRate populated.

### Step 1.3: Add intersectRegions() Method

**File**: `src/main/java/org/whisperdog/recording/SilenceRemover.java`
**Location**: After detectSilenceRegions()

**Signature**:
```java
/**
 * Computes the intersection of two silence region lists.
 * Returns regions where BOTH tracks are silent simultaneously.
 * This is essential for synchronized removal that maintains frame alignment.
 *
 * @param micRegions Silence regions from mic track
 * @param sysRegions Silence regions from system track
 * @return Intersection regions (both tracks silent), empty if no overlap
 */
public static List<SilenceRegion> intersectRegions(List<SilenceRegion> micRegions,
                                                    List<SilenceRegion> sysRegions,
                                                    float sampleRate)
```

**Algorithm**:
```
For each micRegion:
  For each sysRegion:
    If overlap exists (mic.start < sys.end AND sys.start < mic.end):
      intersectionStart = max(mic.start, sys.start)
      intersectionEnd = min(mic.end, sys.end)
      If intersectionEnd - intersectionStart >= minFrames:
        Add new SilenceRegion(intersectionStart, intersectionEnd, sampleRate)
Return merged/deduplicated list
```

### Step 1.4: Unit Tests for Phase 1

**File**: `src/test/java/org/whisperdog/recording/SilenceRemoverRegionTest.java` (NEW)

**Test cases**:
1. `testIntersectRegions_fullOverlap` - Identical regions → same regions returned
2. `testIntersectRegions_partialOverlap` - Overlapping regions → intersection returned
3. `testIntersectRegions_noOverlap` - Non-overlapping → empty list
4. `testIntersectRegions_multipleRegions` - Complex scenario with multiple intersections
5. `testDetectSilenceRegions_syntheticAudio` - Synthetic WAV with known silence

**Verification**: `mvn test -Dtest=SilenceRemoverRegionTest`

---

## Phase 2: Synchronized Removal Core Logic

**Goal**: Implement the main entry point for synchronized dual-source silence removal.

### Step 2.1: Add SyncResult Data Class

**File**: `src/main/java/org/whisperdog/recording/SilenceRemover.java`
**Location**: After SilenceRegion class (after line ~85)

**Definition**:
```java
/**
 * Result of synchronized dual-source silence removal.
 * Contains pruned track files and metadata about what was removed.
 */
public static class SyncResult {
    public final File micFile;          // Pruned mic track (temp file)
    public final File sysFile;          // Pruned system track (temp file)
    public final int removedRegions;    // Count of removed silence regions
    public final long removedMs;        // Total milliseconds removed
    public final boolean wasProcessed;  // false if no common silence found

    public SyncResult(File micFile, File sysFile, int removedRegions,
                      long removedMs, boolean wasProcessed) {
        this.micFile = micFile;
        this.sysFile = sysFile;
        this.removedRegions = removedRegions;
        this.removedMs = removedMs;
        this.wasProcessed = wasProcessed;
    }

    /** Factory for "no processing needed" result */
    public static SyncResult notProcessed() {
        return new SyncResult(null, null, 0, 0, false);
    }
}
```

### Step 2.2: Add removeSynchronizedSilence() Entry Point

**File**: `src/main/java/org/whisperdog/recording/SilenceRemover.java`
**Location**: After SyncResult class

**Signature**:
```java
/**
 * Removes silence from dual-source recordings by detecting silence in both tracks
 * independently, computing their intersection (where BOTH are silent), and removing
 * identical frame ranges from both tracks to maintain perfect synchronization.
 *
 * @param micFile Microphone audio file (WAV)
 * @param sysFile System audio file (WAV)
 * @param micThresholdRMS Silence threshold for mic (typically 0.01)
 * @param sysThresholdRMS Silence threshold for system (typically mic × 0.5)
 * @param minSilenceDurationMs Minimum silence duration to remove
 * @param tempDir Directory for output temp files
 * @return SyncResult with pruned files, or wasProcessed=false if no common silence
 */
public static SyncResult removeSynchronizedSilence(File micFile, File sysFile,
                                                    float micThresholdRMS,
                                                    float sysThresholdRMS,
                                                    int minSilenceDurationMs,
                                                    File tempDir)
```

**Implementation outline**:
```java
// 1. Detect silence in both tracks
List<SilenceRegion> micSilence = detectSilenceRegions(micFile, micThresholdRMS, minSilenceDurationMs);
List<SilenceRegion> sysSilence = detectSilenceRegions(sysFile, sysThresholdRMS, minSilenceDurationMs);

// 2. Compute intersection
List<SilenceRegion> commonSilence = intersectRegions(micSilence, sysSilence, sampleRate);

// 3. Early exit if no common silence
if (commonSilence.isEmpty()) {
    console.log("No common silence regions found, skipping synchronized removal");
    return SyncResult.notProcessed();
}

// 4. Splice identical frames from both tracks
byte[] prunedMicData = spliceAudioData(micData, format, commonSilence);
byte[] prunedSysData = spliceAudioData(sysData, format, commonSilence);

// 5. Write to temp files with deterministic naming
String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
File prunedMicFile = new File(tempDir, "sync_mic_" + timestamp + ".wav");
File prunedSysFile = new File(tempDir, "sync_sys_" + timestamp + ".wav");

// 6. Write WAV files and return result
writeWavFile(prunedMicFile, prunedMicData, format);
writeWavFile(prunedSysFile, prunedSysData, format);

long removedMs = commonSilence.stream()
    .mapToLong(r -> (long)(r.durationSeconds * 1000))
    .sum();

return new SyncResult(prunedMicFile, prunedSysFile, commonSilence.size(), removedMs, true);
```

### Step 2.3: Temp File Naming Pattern

**Pattern**: `sync_{mic|sys}_{yyyyMMdd_HHmmss}.wav`
**Location**: ConfigManager.getTempDirectory() (verified at line 107)
**Cleanup**: Files are marked `deleteOnExit()` and cleaned up by existing temp file management

### Step 2.4: Unit Tests for Phase 2

**File**: `src/test/java/org/whisperdog/recording/SilenceRemoverSyncTest.java` (NEW)

**Test cases**:
1. `testRemoveSynchronizedSilence_commonSilence` - Both tracks have overlapping silence
2. `testRemoveSynchronizedSilence_noCommonSilence` - No overlap → wasProcessed=false
3. `testRemoveSynchronizedSilence_frameAlignment` - Verify output frame counts match
4. `testRemoveSynchronizedSilence_tempFileCleanup` - Verify temp files created in correct location

**Verification**: `mvn test -Dtest=SilenceRemoverSyncTest`

---

## Phase 3: RecorderForm Integration

**Goal**: Wire synchronized silence removal into the dual-source recording path.

### Step 3.1: Update Dual-Source Branch

**File**: `src/main/java/org/whisperdog/recording/RecorderForm.java`
**Location**: Lines 2121-2130 (inside `if (systemTrackHasContent)` block)

**Current code** (line 2121-2130):
```java
if (systemTrackHasContent) {
    // Dual-source recording: merge mic + system tracks
    // Only merge if system track has actual content (> 0.5s)
    console.log("Merging mic + system audio tracks...");
    File mergedFile = FFmpegUtil.mergeAudioTracks(audioFile, validatedSystemTrack);
    if (mergedFile != null) {
        fileToTranscribe = mergedFile;
        console.log("Audio tracks merged successfully");
    } else {
        console.log("Track merge failed, using mic track only");
    }
```

**New code**:
```java
if (systemTrackHasContent) {
    // Dual-source recording: synchronized silence removal + merge
    File micToMerge = audioFile;
    File sysToMerge = validatedSystemTrack;

    if (configManager.isSilenceRemovalEnabled()) {
        console.log("Performing synchronized silence removal on dual-source tracks...");

        // System audio threshold: 50% of mic threshold
        // Rationale: System audio is pre-normalized by the capture pipeline,
        // resulting in ~2x higher RMS for equivalent perceived silence.
        float sysThreshold = (float) Math.max(0.001,
            configManager.getSilenceThreshold() * 0.5);

        SilenceRemover.SyncResult syncResult = SilenceRemover.removeSynchronizedSilence(
            audioFile,
            validatedSystemTrack,
            (float) configManager.getSilenceThreshold(),
            sysThreshold,
            configManager.getMinSilenceDuration(),
            ConfigManager.getTempDirectory()
        );

        if (syncResult.wasProcessed) {
            console.log(String.format("Removed %d silence regions (%.1fs total)",
                syncResult.removedRegions, syncResult.removedMs / 1000.0));
            micToMerge = syncResult.micFile;
            sysToMerge = syncResult.sysFile;
        } else {
            console.log("No common silence found, merging original tracks");
        }
    }

    console.log("Merging mic + system audio tracks...");
    File mergedFile = FFmpegUtil.mergeAudioTracks(micToMerge, sysToMerge);
    if (mergedFile != null) {
        fileToTranscribe = mergedFile;
        console.log("Audio tracks merged successfully");
    } else {
        console.log("Track merge failed, using mic track only");
    }
}
```

### Step 3.2: Verify getMinSilenceDuration() Exists

**File**: `src/main/java/org/whisperdog/ConfigManager.java`
**Status**: ✅ VERIFY EXISTS (used at line 2069 in RecorderForm)

If not present, add:
```java
public int getMinSilenceDuration() {
    return prefs.getInt("minSilenceDuration", 500); // Default 500ms
}
```

### Step 3.3: Integration Test

**File**: `src/test/java/org/whisperdog/recording/DualSourceSilenceRemovalIntegrationTest.java` (NEW)

**Test cases**:
1. `testDualSourceWithSilenceRemoval_reducedDuration` - End-to-end with synthetic dual-source
2. `testDualSourceWithSilenceRemoval_attributionPreserved` - Verify source attribution works post-pruning
3. `testDualSourceWithSilenceRemoval_micOnlyFallback` - Verify mic-only path unchanged

---

## Phase 4: Zero-Crossing Alignment (Optional Enhancement)

**Goal**: Snap cut points to nearest zero-crossing to prevent audio clicks.

### Step 4.1: Add snapToZeroCrossing() Method

**File**: `src/main/java/org/whisperdog/recording/SilenceRemover.java`

**Signature**:
```java
/**
 * Snaps a frame position to the nearest zero-crossing within a window.
 * Prevents audio clicks at splice points.
 *
 * @param audioData Raw audio bytes
 * @param frame Target frame position
 * @param windowFrames Search window (±frames, typically ±110 for 5ms at 22050Hz)
 * @param format Audio format for sample interpretation
 * @return Adjusted frame position at zero-crossing, or original if none found
 */
private static long snapToZeroCrossing(byte[] audioData, long frame,
                                        int windowFrames, AudioFormat format)
```

**Algorithm**: Search ±windowFrames for sample sign change, return nearest.

### Step 4.2: Apply to Region Boundaries

Modify `intersectRegions()` or `spliceAudioData()` to snap boundaries before cutting.

### Step 4.3: Listening Test

Manual verification: Play merged output, listen for clicks at splice points.

---

## Verification Checklist

### Phase 1 Complete When:
- [ ] SilenceRegion is public with durationSeconds field
- [ ] detectSilenceRegions() returns List<SilenceRegion>
- [ ] intersectRegions() handles all overlap cases
- [ ] Unit tests pass: `mvn test -Dtest=SilenceRemoverRegionTest`

### Phase 2 Complete When:
- [ ] SyncResult class exists with all fields
- [ ] removeSynchronizedSilence() entry point works
- [ ] Temp files created with correct naming pattern
- [ ] Unit tests pass: `mvn test -Dtest=SilenceRemoverSyncTest`

### Phase 3 Complete When:
- [ ] RecorderForm dual-source path calls synchronized removal
- [ ] Silence removal can be disabled via config (existing toggle)
- [ ] Integration test passes with attribution verification
- [ ] Manual test: dual-source recording shows reduced duration

### Phase 4 Complete When (Optional):
- [ ] Zero-crossing snap implemented
- [ ] No audible clicks at splice points
- [ ] Listening test passed

---

## Error Handling Summary

| Error | Detection | Recovery |
|-------|-----------|----------|
| Track length mismatch > 1s | Compare frame counts after load | Truncate longer, log warning |
| No common silence | intersectRegions() returns empty | Return SyncResult.notProcessed() |
| OOM on large file | OutOfMemoryError | Process in chunks (future enhancement) |
| I/O error | IOException | Return notProcessed(), use original files |
| Format mismatch | Sample rate differs | Abort sync removal, merge originals |

---

## Threshold Multiplier Justification

**Why 0.5 for system audio?**

System audio is captured via loopback and passes through the OS audio mixer, which typically applies:
1. Automatic gain normalization
2. Compression to prevent clipping
3. Pre-emphasis filtering

The result: system audio has ~2x higher RMS for equivalent perceived silence compared to raw mic input. The 0.5 multiplier compensates for this pre-processing, ensuring consistent silence detection across both tracks.

**Evidence**: Line 2065 in RecorderForm already uses this multiplier for system audio analysis:
```java
float systemThreshold = (float) Math.max(0.001, configManager.getSilenceThreshold() * 0.5);
```

This is not arbitrary—it's consistent with existing behavior.

---

*Implementation plan generated by Scrutinizer audit process*
*Standard: Zero follow-up, zero guessing, deterministic execution*
