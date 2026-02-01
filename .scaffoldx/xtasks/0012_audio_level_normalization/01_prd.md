# PRD: Audio Level Normalization

## 1. Problem Statement

### 1.1 Current Behavior

When recording dual-source audio (mic + system), levels are captured as-is without normalization:

```
Capture → Merge → Attribution → Transcription
         ↑
         No level balancing
```

This causes severe issues when device gain settings differ dramatically:

| Scenario | Mic Level | System Level | Result |
|----------|-----------|--------------|--------|
| Headset + Zoom | -40 dB | 0 dB (clipping) | System distorted, mic inaudible |
| Desktop mic + Discord | -6 dB | -30 dB | Mic dominates, system barely heard |
| Default speakers + Teams | -20 dB | -20 dB | Balanced (lucky case) |

### 1.2 Impact Analysis

**Transcription Quality:**
- Clipped audio produces garbled Whisper output ("you you you" loops)
- Extremely quiet tracks may be ignored entirely
- Whisper expects normalized input (-16 to -20 LUFS)

**Attribution Accuracy:**
- SourceActivityTracker uses RMS comparison for dominance
- A 30dB level difference means one source ALWAYS dominates
- Results in incorrect [User] or [System] labels when both are speaking

**User Experience:**
- Merged recordings are painful to listen to
- Volume constantly swinging between extremes
- Professional-sounding output requires manual DAW post-processing

### 1.3 Processing Pipeline Position

```
1. Capture (mic + system separate channels)
2. ⭐ LEVEL ANALYSIS & NORMALIZATION ← This task
3. Silence Removal (Task 0011, if enabled)
4. Merge to stereo
5. Attribution analysis (Task 0008)
6. Transcription
```

Normalization MUST happen before merge/attribution because:
- Attribution compares relative levels (garbage in = garbage out)
- Post-merge normalization can't fix clipping (data already lost)
- Individual tracks need independent gain adjustment

## 2. Solution Architecture

### 2.1 Two-Phase Approach

```
Phase 1: Level Analysis & Basic Normalization
         - Analyze RMS/peak levels of both tracks
         - Compute gain factors to match target level
         - Apply with headroom protection

Phase 2: Advanced Normalization (Future)
         - LUFS-based loudness matching (broadcast standard)
         - Dynamic range compression option
         - Per-segment adaptive gain
```

### 2.2 Phase 1: Level Analysis & Basic Normalization

**Algorithm:**

```java
// 1. Analyze both tracks
TrackAnalysis micAnalysis = AudioLevelAnalyzer.analyze(micFile);
TrackAnalysis sysAnalysis = AudioLevelAnalyzer.analyze(sysFile);

// 2. Handle special cases
if (micAnalysis.isClipped && sysAnalysis.isClipped) {
    // Both clipped: attenuate to CLIPPED_ATTENUATION_DB (-6 dB)
    micGain = CLIPPED_ATTENUATION_DB - micAnalysis.peakDb;
    sysGain = CLIPPED_ATTENUATION_DB - sysAnalysis.peakDb;
} else {
    // 3. Compute normalization factors
    float targetRmsDb = configManager.getNormalizationTarget(); // default -20.0f
    micGain = computeGain(micAnalysis, targetRmsDb);
    sysGain = computeGain(sysAnalysis, targetRmsDb);
}

// 4. Apply gain with headroom protection
File normalizedMic = applyGain(micFile, micGain);
File normalizedSys = applyGain(sysFile, sysGain);

// 5. Proceed to merge/attribution with balanced levels
```

**Gain Calculation with Headroom:**

```java
// Constants defined in section 3.0.1

float computeGain(TrackAnalysis analysis, float targetRmsDb) {
    // Skip silent tracks
    if (analysis.isSilent) {
        return 0.0f;  // No gain change
    }

    // Short recording: use peak-based targeting (RMS unreliable)
    float desiredGain;
    if (analysis.durationMs < SHORT_RECORDING_MS) {
        desiredGain = (targetRmsDb + HEADROOM_DB) - analysis.peakDb;
    } else {
        desiredGain = targetRmsDb - analysis.rmsDb;
    }

    // Headroom protection: don't let peak exceed -(HEADROOM_DB) dB
    float maxAllowedGain = -HEADROOM_DB - analysis.peakDb;

    // Clamp to ±MAX_GAIN_DB range (prevent extreme boost/cut)
    float gain = Math.min(desiredGain, maxAllowedGain);
    return Math.max(-MAX_GAIN_DB, Math.min(gain, MAX_GAIN_DB));
}
```

### 2.3 Special Cases

**Clipping Detection:**
If peak is already at 0 dB (clipped), apply attenuation only:
- Cannot recover clipped data
- Reduce level to prevent further distortion in processing
- Log warning: "System audio appears clipped; quality may be degraded"

**Silent Track:**
If RMS is below noise floor (-60 dB):
- Skip normalization for that track
- Log info: "Mic track below noise floor, skipping normalization"

**Already Balanced:**
If `Math.abs(micAnalysis.rmsDb - sysAnalysis.rmsDb) < 6.0f`:
- Set `wasProcessed = false`
- Return original files without modification
- Log debug: "Tracks within 6 dB RMS, skipping normalization"

## 3. Technical Specifications

### 3.0 Input Format Requirements

| Property | Value | Notes |
|----------|-------|-------|
| Format | WAV (PCM) | RIFF header required |
| Bit depth | 16-bit signed | Standard WhisperDog capture format |
| Sample rate | 22050 Hz | Enforced by capture pipeline |
| Channels | 1 (mono) | Each track analyzed/normalized independently |

**Note:** Input is always mono from WhisperDog capture pipeline. No stereo handling required.

### 3.0.1 Constants Specification

| Constant | Value | Purpose |
|----------|-------|---------|
| `CLIPPING_THRESHOLD_DB` | -0.1f | Peak at or above indicates clipping |
| `MAX_GAIN_DB` | 20.0f | Maximum gain boost/cut applied |
| `HEADROOM_DB` | 3.0f | Peak headroom preserved below 0 dB |
| `SILENCE_THRESHOLD_DB` | -60.0f | RMS below this = silent track |
| `BALANCE_THRESHOLD_DB` | 6.0f | RMS difference below this = skip normalization |
| `SHORT_RECORDING_MS` | 1000 | Recordings shorter than this use peak-based targeting |
| `DEFAULT_TARGET_RMS_DB` | -20.0f | Default target RMS level |
| `CLIPPED_ATTENUATION_DB` | -6.0f | Target level when both tracks are clipped |

### 3.1 New Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `AudioLevelAnalyzer` | `org.whisperdog.audio` | Analyze RMS, peak, dynamic range of audio file |
| `AudioNormalizer` | `org.whisperdog.audio` | Apply gain normalization with headroom protection |
| `TrackAnalysis` | `org.whisperdog.audio` | Immutable result from AudioLevelAnalyzer (inner class) |
| `NormalizationResult` | `org.whisperdog.audio` | Immutable result from AudioNormalizer (inner class) |

### 3.1.1 AudioLevelAnalyzer API

```java
public class AudioLevelAnalyzer {

    /**
     * Analyze audio levels in a WAV file.
     *
     * @param wavFile 16-bit mono PCM WAV file
     * @return TrackAnalysis with RMS, peak, duration, clipping/silence flags
     * @throws IOException if file cannot be read or is invalid format
     */
    public static TrackAnalysis analyze(File wavFile) throws IOException;

    /**
     * Analyze audio levels from raw PCM samples.
     *
     * @param samples 16-bit signed PCM samples
     * @param sampleRate sample rate in Hz (for duration calculation)
     * @return TrackAnalysis with computed metrics
     */
    public static TrackAnalysis analyze(short[] samples, int sampleRate);
}
```

### 3.1.2 AudioNormalizer API

```java
public class AudioNormalizer {

    /**
     * Normalize mic and system audio tracks for merge.
     *
     * @param micFile mic track WAV file
     * @param sysFile system track WAV file
     * @param targetRmsDb target RMS level in dB (typically -20.0)
     * @param enabled if false, returns wasProcessed=false immediately
     * @param tempDir directory for normalized output files
     * @return NormalizationResult with normalized temp files or originals
     */
    public static NormalizationResult normalizeForMerge(
        File micFile,
        File sysFile,
        float targetRmsDb,
        boolean enabled,
        File tempDir
    );

    /**
     * Apply gain to a single WAV file.
     *
     * @param inputFile source WAV file
     * @param gainDb gain to apply in dB (positive = boost, negative = cut)
     * @param outputFile destination file
     * @throws IOException if read/write fails
     */
    static void applyGain(File inputFile, float gainDb, File outputFile) throws IOException;
}

### 3.2 Data Structures

```java
public class TrackAnalysis {
    public final float rmsDb;           // Root mean square level in dB
    public final float peakDb;          // Peak sample level in dB
    public final float dynamicRangeDb;  // Peak - RMS
    public final long durationMs;       // Track duration in milliseconds
    public final boolean isClipped;     // Peak >= CLIPPING_THRESHOLD_DB (-0.1 dB)
    public final boolean isSilent;      // RMS < SILENCE_THRESHOLD_DB (-60 dB)

    // Package-private constructor (created by AudioLevelAnalyzer only)
    TrackAnalysis(float rmsDb, float peakDb, long durationMs) {
        this.rmsDb = rmsDb;
        this.peakDb = peakDb;
        this.dynamicRangeDb = peakDb - rmsDb;
        this.durationMs = durationMs;
        this.isClipped = peakDb >= CLIPPING_THRESHOLD_DB;
        this.isSilent = rmsDb < SILENCE_THRESHOLD_DB;
    }
}

public class NormalizationResult {
    public final File micFile;           // Normalized mic track (temp file)
    public final File sysFile;           // Normalized system track (temp file)
    public final float micGainApplied;   // Actual gain applied to mic
    public final float sysGainApplied;   // Actual gain applied to system
    public final boolean wasProcessed;   // false if skipped (already balanced)
    public final String warning;         // null or warning message (clipping, etc.)
}
```

### 3.3 Integration Point (RecorderForm)

```java
if (systemTrackHasContent) {
    // Step 1: Normalize levels BEFORE any other processing
    NormalizationResult normalized = AudioNormalizer.normalizeForMerge(
        audioFile,                              // File: mic WAV
        validatedSystemTrack,                   // File: system WAV
        configManager.getNormalizationTarget(), // float: target RMS dB (-20 default)
        configManager.isNormalizationEnabled(), // boolean: user preference
        configManager.getTempDirectory()        // File: output temp dir
    );

    if (normalized.warning != null) {
        logWarning(normalized.warning);
    }

    File micForProcessing = normalized.wasProcessed ? normalized.micFile : audioFile;
    File sysForProcessing = normalized.wasProcessed ? normalized.sysFile : validatedSystemTrack;

    // Step 2: Silence removal (if enabled) - Task 0011
    // Step 3: Merge tracks
    // Step 4: Attribution analysis

    // Step 5: Cleanup temp files from normalization
    if (normalized.wasProcessed) {
        normalized.micFile.delete();
        normalized.sysFile.delete();
    }
}
```

**Temp file lifecycle:** Caller (RecorderForm) is responsible for deleting temp files returned in `NormalizationResult` after merge is complete.

**Temp file naming convention:**
- Mic: `norm_mic_{timestamp}.wav` where timestamp is `System.currentTimeMillis()`
- System: `norm_sys_{timestamp}.wav`
- Location: `configManager.getTempDirectory()`

**Logging convention:** Use `LOG.warn()` / `LOG.info()` / `LOG.debug()` via the existing `java.util.logging.Logger` pattern (see RecorderForm.java for reference).

### 3.4 Configuration

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `audio.normalization.enabled` | boolean | true | Enable/disable level normalization |
| `audio.normalization.targetDb` | float | -20.0 | Target RMS level in dB |
| `audio.normalization.headroomDb` | float | 3.0 | Peak headroom to preserve |

## 4. Acceptance Criteria

| ID | Requirement | Test Method |
|----|-------------|-------------|
| FR-1 | Analyze RMS and peak levels of WAV files | Unit test with known-level audio |
| FR-2 | Compute gain to reach target level with headroom protection | Unit test edge cases |
| FR-3 | Apply gain without introducing clipping | Assert peak < -HEADROOM_DB after normalization |
| FR-4 | Skip normalization if tracks within BALANCE_THRESHOLD_DB | Test with matched levels |
| FR-5 | Handle clipped input gracefully (attenuate, warn) | Test with 0dB peak file |
| FR-6 | Handle silent track gracefully (skip, log) | Test with file below SILENCE_THRESHOLD_DB |
| NFR-1 | Processing < 50ms for 60s mono WAV | Benchmark |
| NFR-2 | Bit-exact output when gain is 0 dB | Compare input/output bytes when no gain applied |

## 5. Edge Cases

| Edge Case | Expected Behavior |
|-----------|-------------------|
| Both tracks at 0 dB (clipped) | Attenuate both to -6 dB, warn user |
| Mic silent, system normal | Skip mic normalization, normalize system only |
| System silent, mic normal | Skip system normalization, normalize mic only |
| Both tracks identical level | Skip normalization entirely |
| Extreme difference (>40 dB) | Apply max reasonable gain (+20 dB limit) |
| Very short recording (<1s) | If `durationMs < 1000`, use `peakDb - HEADROOM_DB` as target instead of RMS-based calculation |

## 6. Error Handling

| Error | Detection | Recovery |
|-------|-----------|----------|
| File read error | IOException | Skip normalization, use original files, log warning |
| Invalid audio format | Unsupported bits/channels | Skip normalization, use original files, log warning |
| Gain calculation overflow | Float check | Clamp to ±MAX_GAIN_DB (±20 dB) range |
| Temp file write failure | IOException | Abort, use original files, log error |

## 7. Related

- **Task 0006**: System Audio Capture - provides dual-source input
- **Task 0008**: Dual-Source Attribution Accuracy - benefits from balanced levels
- **Task 0011**: Synchronized Dual-Source Silence Removal - runs after normalization
- **SourceActivityTracker.java**: Attribution logic that depends on level balance
