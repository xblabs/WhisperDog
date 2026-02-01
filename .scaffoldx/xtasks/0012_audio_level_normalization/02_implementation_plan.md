# Implementation Plan: Audio Level Normalization

## Phase Overview

| Phase | Focus | Deliverables |
|-------|-------|--------------|
| 1 | Core Implementation | AudioLevelAnalyzer, AudioNormalizer, integration |
| 2 | Configuration & Testing | Settings UI, unit tests, validation |

## Phase 1: Core Implementation

### Step 1.1: Create AudioLevelAnalyzer

**File:** `src/main/java/org/whisperdog/audio/AudioLevelAnalyzer.java`

**Actions:**

1. Create class with static `analyze(File wavFile)` method
2. Read WAV header to extract sample rate, bit depth, channel count
3. Validate format: 16-bit PCM, mono (reject otherwise with IOException)
4. Iterate samples to compute:
   - RMS: `sqrt(sum(sample^2) / count)` converted to dB
   - Peak: `max(abs(sample))` converted to dB
   - Duration: `sampleCount / sampleRate * 1000` ms
5. Return `TrackAnalysis` with computed values and derived flags

**Reference:** Reuse RMS calculation pattern from `SourceActivityTracker.java`

**dB Conversion Formula:**

```java
float linearToDb(float linear) {
    return 20.0f * (float) Math.log10(Math.max(linear, 1e-10f));
}
```

### Step 1.2: Create AudioNormalizer

**File:** `src/main/java/org/whisperdog/audio/AudioNormalizer.java`

**Actions:**

1. Define constants from PRD section 3.0.1
2. Implement `normalizeForMerge()` entry point:
   - If `!enabled`, return `NormalizationResult` with `wasProcessed=false`
   - Analyze both tracks via `AudioLevelAnalyzer.analyze()`
   - If both silent, return originals with `wasProcessed=false`
   - If tracks within `BALANCE_THRESHOLD_DB`, return originals
   - Compute gains via `computeGain()` (handle both-clipped case)
   - Apply gains, write to temp files, return result
3. Implement `computeGain(TrackAnalysis, targetRmsDb)` per PRD algorithm
4. Implement `applyGain(File input, float gainDb, File output)`:
   - Read WAV samples
   - Multiply each sample by `Math.pow(10, gainDb / 20.0)`
   - Clamp to [-32768, 32767] to prevent overflow
   - Write output WAV with same header

**Inner Classes:**

- `TrackAnalysis` - immutable, package-private constructor
- `NormalizationResult` - immutable, public fields

### Step 1.3: Integrate into RecorderForm

**File:** `src/main/java/org/whisperdog/recording/RecorderForm.java`

**Location:** After `systemTrackHasContent` check, before merge/attribution

**Actions:**

1. Add import for `AudioNormalizer` and `NormalizationResult`
2. Insert normalization call per PRD section 3.3
3. Use normalized files for downstream processing
4. Add cleanup in finally block or after merge completes
5. Log warnings from `NormalizationResult.warning`

**Integration Pattern:**

```java
// After: if (systemTrackHasContent) {
NormalizationResult normalized = AudioNormalizer.normalizeForMerge(
    audioFile, validatedSystemTrack,
    configManager.getNormalizationTarget(),
    configManager.isNormalizationEnabled(),
    configManager.getTempDirectory()
);
if (normalized.warning != null) {
    LOG.warning(normalized.warning);
}
File micForProcessing = normalized.wasProcessed ? normalized.micFile : audioFile;
File sysForProcessing = normalized.wasProcessed ? normalized.sysFile : validatedSystemTrack;
// ... existing merge/attribution code using micForProcessing, sysForProcessing ...
// Cleanup after merge:
if (normalized.wasProcessed) {
    normalized.micFile.delete();
    normalized.sysFile.delete();
}
```

## Phase 2: Configuration & Testing

### Step 2.1: Add Configuration

**File:** `src/main/java/org/whisperdog/config/ConfigManager.java`

**Actions:**

1. Add property keys:
   - `audio.normalization.enabled` (boolean, default: true)
   - `audio.normalization.targetDb` (float, default: -20.0)
2. Add getters:
   - `isNormalizationEnabled()` -> boolean
   - `getNormalizationTarget()` -> float
3. Add setters for UI binding

### Step 2.2: Add UI Controls

**File:** `src/main/java/org/whisperdog/ui/OptionsDialog.java` (or equivalent)

**Actions:**

1. Add checkbox: "Normalize audio levels before merge"
2. Add slider: "Target level (dB)" with range -30 to -10, step 1
3. Wire to ConfigManager getters/setters
4. Disable slider when checkbox unchecked

### Step 2.3: Unit Tests

**File:** `src/test/java/org/whisperdog/audio/AudioLevelAnalyzerTest.java`

| Test | Input | Expected |
|------|-------|----------|
| `testRmsCalculation` | Sine wave at known amplitude | RMS within ±0.5 dB of theoretical |
| `testPeakDetection` | File with single max sample | Peak matches sample value |
| `testClippingDetection` | File with 0 dB peak | `isClipped = true` |
| `testSilenceDetection` | File with -70 dB RMS | `isSilent = true` |
| `testShortRecording` | 500ms file | `durationMs = 500` |

**File:** `src/test/java/org/whisperdog/audio/AudioNormalizerTest.java`

| Test | Input | Expected |
|------|-------|----------|
| `testGainComputation` | -40 dB RMS, -20 target | gain = +20 dB (clamped) |
| `testHeadroomProtection` | -10 dB RMS, -5 dB peak | gain limited by headroom |
| `testBalancedSkip` | Both tracks at -20 dB | `wasProcessed = false` |
| `testBothClipped` | Both at 0 dB peak | Both attenuated to -6 dB |
| `testBitExact` | Gain = 0 dB | Input bytes == output bytes |

### Step 2.4: Integration Test

**File:** `src/test/java/org/whisperdog/audio/NormalizationIntegrationTest.java`

**Test:** Full pipeline with imbalanced test files

1. Create mic file at -40 dB RMS
2. Create system file at -10 dB RMS
3. Run `normalizeForMerge()`
4. Assert both outputs within ±1 dB of target RMS
5. Assert no clipping (peak < -HEADROOM_DB)

## Dependencies

```
Phase 1.1 (Analyzer) --> Phase 1.2 (Normalizer) --> Phase 1.3 (Integration)
                                                          |
Phase 2.1 (Config) ----------------------------------------+
                                                          |
Phase 2.2 (UI) -------------------------------------------+
                                                          |
Phase 2.3 (Unit Tests) <-- Phase 1.1, 1.2                |
Phase 2.4 (Integration Test) <-- Phase 1.3 --------------+
```

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| WAV parsing edge cases | Use existing Java AudioSystem or copy proven pattern from FFmpegUtil |
| Performance on large files | Stream samples instead of loading all into memory |
| Floating-point precision | Use double for intermediate calculations, cast to float for output |
| Temp file leaks | Always delete in finally block; add cleanup on app exit |

## Completion Verification

Phase 1 complete when:

- [ ] `AudioLevelAnalyzer.analyze()` returns correct metrics for test files
- [ ] `AudioNormalizer.normalizeForMerge()` produces balanced output
- [ ] RecorderForm uses normalized files for dual-source merge
- [ ] No compilation errors, app starts successfully

Phase 2 complete when:

- [ ] Settings persist across app restart
- [ ] UI controls update behavior in real-time
- [ ] All unit tests pass
- [ ] Integration test validates end-to-end pipeline
