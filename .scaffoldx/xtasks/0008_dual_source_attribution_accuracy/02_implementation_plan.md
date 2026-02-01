# Implementation Plan: Dual-Source Audio Attribution Accuracy

## Phase 1: Dominance Ratio Implementation

### 1.1 Add Configuration Constants

**File**: `src/main/java/org/whisperdog/audio/SourceActivityTracker.java`
**Location**: After line 29 (existing constants)

```java
// src/main/java/org/whisperdog/audio/SourceActivityTracker.java
// Add after DEFAULT_ACTIVITY_THRESHOLD constant

/** Default dominance ratio for filtering false BOTH detections (3:1 = ~9.5dB) */
public static final double DEFAULT_DOMINANCE_RATIO = 3.0;

/** Minimum valid dominance ratio */
public static final double MIN_DOMINANCE_RATIO = 1.5;

/** Maximum valid dominance ratio */
public static final double MAX_DOMINANCE_RATIO = 10.0;
```

### 1.2 Add Instance Field

**File**: `src/main/java/org/whisperdog/audio/SourceActivityTracker.java`
**Location**: After line 32 (existing fields)

```java
// Add after activityThreshold field
private final double dominanceRatio;
```

### 1.3 Update Constructors

**File**: `src/main/java/org/whisperdog/audio/SourceActivityTracker.java`
**Location**: Replace constructors at lines 71-83

```java
/**
 * Create tracker with default settings.
 */
public SourceActivityTracker() {
    this(DEFAULT_SAMPLE_INTERVAL_MS, DEFAULT_ACTIVITY_THRESHOLD, DEFAULT_DOMINANCE_RATIO);
}

/**
 * Create tracker with custom threshold settings.
 * @param sampleIntervalMs Interval between RMS samples in milliseconds
 * @param activityThreshold RMS threshold for detecting activity (0.0-1.0)
 */
public SourceActivityTracker(int sampleIntervalMs, double activityThreshold) {
    this(sampleIntervalMs, activityThreshold, DEFAULT_DOMINANCE_RATIO);
}

/**
 * Create tracker with full custom settings.
 * @param sampleIntervalMs Interval between RMS samples in milliseconds
 * @param activityThreshold RMS threshold for detecting activity (0.0-1.0)
 * @param dominanceRatio Ratio threshold for filtering false BOTH detections (1.5-10.0)
 */
public SourceActivityTracker(int sampleIntervalMs, double activityThreshold,
                             double dominanceRatio) {
    this.sampleIntervalMs = sampleIntervalMs;
    this.activityThreshold = activityThreshold;
    this.dominanceRatio = Math.max(MIN_DOMINANCE_RATIO,
                                   Math.min(MAX_DOMINANCE_RATIO, dominanceRatio));
}
```

### 1.4 Modify Activity Detection Logic

**File**: `src/main/java/org/whisperdog/audio/SourceActivityTracker.java`
**Location**: Replace lines 117-127 in `trackActivity()` method

```java
// Determine source for this interval with dominance ratio check
Source source;
if (micActive && systemActive) {
    // Both appear active - check dominance ratio
    double micLevel = micRms[i];
    double sysLevel = systemRms[i];

    // Guard against division by zero (near-zero system level)
    double ratio;
    if (sysLevel < 0.0001) {
        // System essentially silent - mic dominates if above threshold
        ratio = micLevel > activityThreshold ? Double.MAX_VALUE : 0.0;
    } else {
        ratio = micLevel / sysLevel;
    }

    if (ratio > dominanceRatio) {
        // Mic dominates by ratio threshold - likely system is just noise
        source = Source.USER;
        logger.trace("Interval %d: Mic dominates (%.4f/%.4f = %.2fx)",
                     i, micLevel, sysLevel, ratio);
    } else if (ratio < (1.0 / dominanceRatio)) {
        // System dominates by ratio threshold - likely mic is just noise
        source = Source.SYSTEM;
        logger.trace("Interval %d: System dominates (%.4f/%.4f = %.2fx)",
                     i, sysLevel, micLevel, 1.0/ratio);
    } else {
        // Neither dominates - genuine crosstalk
        source = Source.BOTH;
        logger.trace("Interval %d: Both active (ratio %.2f)", i, ratio);
    }
} else if (micActive) {
    source = Source.USER;
} else if (systemActive) {
    source = Source.SYSTEM;
} else {
    source = Source.SILENCE;
}
```

### 1.5 Add Getter for Configuration

**File**: `src/main/java/org/whisperdog/audio/SourceActivityTracker.java`
**Location**: After existing methods (end of class, before closing brace)

```java
/**
 * Get the configured dominance ratio.
 * @return Dominance ratio threshold
 */
public double getDominanceRatio() {
    return dominanceRatio;
}
```

---

## Phase 2: Segment Smoothing

### 2.1 Add Smoothing Method

**File**: `src/main/java/org/whisperdog/audio/SourceActivityTracker.java`
**Location**: After `mergeShortSegments()` method (after line 271)

```java
/**
 * Smooth short BOTH segments by absorbing them into surrounding context.
 * A short BOTH segment surrounded by the same single source is likely spurious.
 *
 * @param segments Input timeline segments
 * @param maxBothDurationMs Maximum duration for BOTH segments to be smoothed (default 500ms)
 * @return Smoothed timeline with reduced spurious BOTH segments
 */
private List<ActivitySegment> smoothBothSegments(List<ActivitySegment> segments,
                                                  long maxBothDurationMs) {
    if (segments.size() <= 2) {
        return segments;
    }

    List<ActivitySegment> result = new ArrayList<>();

    for (int i = 0; i < segments.size(); i++) {
        ActivitySegment current = segments.get(i);

        // Check if this is a short BOTH segment that can be smoothed
        if (current.source == Source.BOTH && current.getDurationMs() <= maxBothDurationMs) {
            Source before = i > 0 ? segments.get(i - 1).source : Source.SILENCE;
            Source after = i < segments.size() - 1 ? segments.get(i + 1).source : Source.SILENCE;

            // If surrounded by the same single source, absorb into that source
            if (before == after && before != Source.SILENCE && before != Source.BOTH) {
                logger.debug("Smoothing BOTH segment [{}-{}ms] into {} context",
                            current.startMs, current.endMs, before);
                result.add(new ActivitySegment(current.startMs, current.endMs, before));
                continue;
            }

            // If one neighbor is single-source and other is SILENCE, use the single source
            if (before != Source.SILENCE && before != Source.BOTH && after == Source.SILENCE) {
                result.add(new ActivitySegment(current.startMs, current.endMs, before));
                continue;
            }
            if (after != Source.SILENCE && after != Source.BOTH && before == Source.SILENCE) {
                result.add(new ActivitySegment(current.startMs, current.endMs, after));
                continue;
            }
        }

        // Keep segment as-is
        result.add(current);
    }

    // Merge consecutive same-source segments created by smoothing
    return mergeConsecutiveSameSource(result);
}

/**
 * Merge consecutive segments with the same source.
 */
private List<ActivitySegment> mergeConsecutiveSameSource(List<ActivitySegment> segments) {
    if (segments.size() <= 1) {
        return segments;
    }

    List<ActivitySegment> merged = new ArrayList<>();
    ActivitySegment current = segments.get(0);

    for (int i = 1; i < segments.size(); i++) {
        ActivitySegment next = segments.get(i);
        if (next.source == current.source) {
            // Extend current segment to include next
            current = new ActivitySegment(current.startMs, next.endMs, current.source);
        } else {
            merged.add(current);
            current = next;
        }
    }
    merged.add(current);

    return merged;
}
```

### 2.2 Integrate Smoothing into Pipeline

**File**: `src/main/java/org/whisperdog/audio/SourceActivityTracker.java`
**Location**: Modify `trackActivity()` method, after line 146

```java
// Change this line:
timeline = mergeShortSegments(timeline, sampleIntervalMs * 2);

// To this (add smoothing after debounce):
timeline = mergeShortSegments(timeline, sampleIntervalMs * 2);
timeline = smoothBothSegments(timeline, 500);  // 500ms max for BOTH smoothing
```

---

## Phase 3: LLM Post-Processing (Optional)

### 3.1 Create Attribution Cleanup Pipeline Unit

**File**: `src/main/java/org/whisperdog/pipeline/units/AttributionCleanupUnit.java` (NEW)

```java
package org.whisperdog.pipeline.units;

import org.whisperdog.pipeline.PipelineUnit;
import org.whisperdog.pipeline.PipelineContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Pipeline unit that uses LLM inference to clean up dual-source attribution errors.
 * This is an optional post-processing step for high-accuracy transcription mode.
 */
public class AttributionCleanupUnit implements PipelineUnit {
    private static final Logger logger = LogManager.getLogger(AttributionCleanupUnit.class);

    private static final String CLEANUP_PROMPT = """
        You are cleaning up a dual-source transcription where [User] is microphone audio
        and [System] is computer/system audio.

        RULES:
        1. If a word is split across sources (e.g., "[User+System]: audio Hello" where
           "Hello" is clearly system speech), assign the whole segment appropriately
        2. [User+System] segments adjacent to [System] with semantic continuity should
           merge into [System] (and vice versa for [User])
        3. Preserve genuine interruptions only when speakers are semantically distinct
        4. Sentences should generally belong to one speaker unless interrupted
        5. Look for semantic coherence - system audio is usually a continuous monologue

        CRITICAL: Return ONLY the corrected transcript with [User], [System], or
        [User+System] labels. No explanations, no markdown formatting.

        INPUT:
        %s

        OUTPUT:
        """;

    @Override
    public String getName() {
        return "Attribution Cleanup";
    }

    @Override
    public String getDescription() {
        return "Uses LLM to fix dual-source attribution errors";
    }

    @Override
    public String process(String input, PipelineContext context) throws Exception {
        // Only process if transcript has dual-source labels
        if (!input.contains("[User]") && !input.contains("[System]")) {
            logger.debug("Skipping attribution cleanup - no source labels found");
            return input;
        }

        // Only process if there are potential errors ([User+System] labels)
        if (!input.contains("[User+System]")) {
            logger.debug("Skipping attribution cleanup - no [User+System] segments");
            return input;
        }

        String prompt = String.format(CLEANUP_PROMPT, input);

        // Use context's LLM client (inherits from pipeline config)
        String cleaned = context.getLlmClient().complete(prompt);

        // Validate output has expected format
        if (!cleaned.contains("[User]") && !cleaned.contains("[System]")) {
            logger.warn("Attribution cleanup returned invalid format, using original");
            return input;
        }

        logger.info("Attribution cleanup processed {} chars -> {} chars",
                   input.length(), cleaned.length());
        return cleaned.trim();
    }
}
```

### 3.2 Register Pipeline Unit

**File**: `src/main/java/org/whisperdog/pipeline/PipelineRegistry.java`
**Location**: In `registerBuiltInUnits()` method, after existing unit registrations (~line 45)

```java
// In registerBuiltInUnits() method, add after other register() calls
register("attribution-cleanup", AttributionCleanupUnit.class);
```

### 3.3 Add Configuration Option

**File**: `src/main/java/org/whisperdog/ConfigManager.java`
**Location**: Add getter method after existing getters (around line 100)

```java
// In ConfigManager.java - add after existing getter methods

/** Property key for attribution cleanup feature */
private static final String PROP_ATTRIBUTION_CLEANUP_ENABLED = "transcription.attributionCleanup.enabled";

/**
 * Check if LLM-based attribution cleanup is enabled (high-accuracy mode).
 * @return true if attribution cleanup should be applied to dual-source transcriptions
 */
public boolean isAttributionCleanupEnabled() {
    return Boolean.parseBoolean(properties.getProperty(PROP_ATTRIBUTION_CLEANUP_ENABLED, "false"));
}

/**
 * Enable or disable LLM-based attribution cleanup.
 * @param enabled true to enable high-accuracy attribution cleanup
 */
public void setAttributionCleanupEnabled(boolean enabled) {
    properties.setProperty(PROP_ATTRIBUTION_CLEANUP_ENABLED, String.valueOf(enabled));
}
```

**UI Integration**: Add checkbox in `src/main/java/org/whisperdog/settings/SettingsForm.java` under transcription settings section.

---

## Testing Strategy

### Unit Tests

**File**: `src/test/java/org/whisperdog/audio/SourceActivityTrackerTest.java`

```java
// Add these test methods

@Test
void testDominanceRatio_MicDominates() throws Exception {
    SourceActivityTracker tracker = new SourceActivityTracker(100, 0.005, 3.0);

    // Create test WAV files with controlled RMS levels
    // Mic: 0.6 RMS (loud speech), System: 0.1 RMS (background)
    // Ratio = 6:1, mic dominates (> 3.0 threshold)
    File micFile = createTestWav("mic_dominates_mic.wav", 0.6, 1000);
    File sysFile = createTestWav("mic_dominates_sys.wav", 0.1, 1000);

    List<SourceActivityTracker.ActivitySegment> timeline =
        tracker.trackActivity(micFile, sysFile);

    // Should be USER, not BOTH (mic dominates by 6x)
    assertEquals(1, timeline.size());
    assertEquals(SourceActivityTracker.Source.USER, timeline.get(0).source);

    // Cleanup
    micFile.delete();
    sysFile.delete();
}

@Test
void testDominanceRatio_SystemDominates() throws Exception {
    SourceActivityTracker tracker = new SourceActivityTracker(100, 0.005, 3.0);

    // Mic: 0.006 RMS (ambient noise), System: 0.5 RMS (loud playback)
    // Ratio = 0.012, system dominates (< 0.33 threshold)
    File micFile = createTestWav("sys_dominates_mic.wav", 0.006, 1000);
    File sysFile = createTestWav("sys_dominates_sys.wav", 0.5, 1000);

    List<SourceActivityTracker.ActivitySegment> timeline =
        tracker.trackActivity(micFile, sysFile);

    // Should be SYSTEM, not BOTH (system dominates by 83x)
    assertEquals(1, timeline.size());
    assertEquals(SourceActivityTracker.Source.SYSTEM, timeline.get(0).source);

    micFile.delete();
    sysFile.delete();
}

@Test
void testDominanceRatio_GenuineCrosstalk() throws Exception {
    SourceActivityTracker tracker = new SourceActivityTracker(100, 0.005, 3.0);

    // Mic: 0.3 RMS, System: 0.4 RMS
    // Ratio = 0.75, within range (0.33 < 0.75 < 3.0) = genuine crosstalk
    File micFile = createTestWav("crosstalk_mic.wav", 0.3, 1000);
    File sysFile = createTestWav("crosstalk_sys.wav", 0.4, 1000);

    List<SourceActivityTracker.ActivitySegment> timeline =
        tracker.trackActivity(micFile, sysFile);

    assertEquals(1, timeline.size());
    assertEquals(SourceActivityTracker.Source.BOTH, timeline.get(0).source);

    micFile.delete();
    sysFile.delete();
}

@Test
void testDominanceRatio_DivisionByZeroGuard() throws Exception {
    SourceActivityTracker tracker = new SourceActivityTracker(100, 0.005, 3.0);

    // Mic: 0.3 RMS, System: 0.0 RMS (silent)
    // Should handle gracefully without ArithmeticException
    File micFile = createTestWav("div_zero_mic.wav", 0.3, 1000);
    File sysFile = createTestWav("div_zero_sys.wav", 0.0, 1000);

    List<SourceActivityTracker.ActivitySegment> timeline =
        tracker.trackActivity(micFile, sysFile);

    // Mic is active, system is silent -> USER only
    assertEquals(SourceActivityTracker.Source.USER, timeline.get(0).source);

    micFile.delete();
    sysFile.delete();
}

@Test
void testBothSegmentSmoothing_SurroundedBySameSource() throws Exception {
    SourceActivityTracker tracker = new SourceActivityTracker(100, 0.005, 3.0);

    // Create timeline: [SYSTEM 0-1000] [BOTH 1000-1200] [SYSTEM 1200-2000]
    // Build WAV with: system-only, brief overlap, system-only
    File micFile = createVariableRmsWav("smooth_mic.wav",
        new double[]{0.001, 0.001, 0.001, 0.001, 0.001,  // 0-500ms: silent
                     0.001, 0.001, 0.001, 0.001, 0.001,  // 500-1000ms: silent
                     0.3, 0.3,                           // 1000-1200ms: brief spike
                     0.001, 0.001, 0.001, 0.001, 0.001,  // 1200-1700ms: silent
                     0.001, 0.001, 0.001});              // 1700-2000ms: silent
    File sysFile = createVariableRmsWav("smooth_sys.wav",
        new double[]{0.5, 0.5, 0.5, 0.5, 0.5,            // 0-500ms: active
                     0.5, 0.5, 0.5, 0.5, 0.5,            // 500-1000ms: active
                     0.5, 0.5,                           // 1000-1200ms: active (overlap)
                     0.5, 0.5, 0.5, 0.5, 0.5,            // 1200-1700ms: active
                     0.5, 0.5, 0.5});                    // 1700-2000ms: active

    List<SourceActivityTracker.ActivitySegment> timeline =
        tracker.trackActivity(micFile, sysFile);

    // After smoothing: short BOTH segment (200ms < 500ms) surrounded by SYSTEM
    // should be absorbed into SYSTEM -> single SYSTEM segment
    assertEquals(1, timeline.size());
    assertEquals(SourceActivityTracker.Source.SYSTEM, timeline.get(0).source);

    micFile.delete();
    sysFile.delete();
}

@Test
void testBothSegmentSmoothing_PreservesLongBoth() throws Exception {
    SourceActivityTracker tracker = new SourceActivityTracker(100, 0.005, 3.0);

    // Create timeline with 1-second BOTH segment (> 500ms threshold)
    // Should NOT be smoothed away
    File micFile = createVariableRmsWav("long_both_mic.wav",
        new double[]{0.001, 0.001, 0.001, 0.001, 0.001,  // 0-500ms: silent
                     0.001, 0.001, 0.001, 0.001, 0.001,  // 500-1000ms: silent
                     0.3, 0.3, 0.3, 0.3, 0.3,            // 1000-1500ms: active (BOTH)
                     0.3, 0.3, 0.3, 0.3, 0.3,            // 1500-2000ms: active (BOTH)
                     0.001, 0.001, 0.001, 0.001, 0.001}); // 2000-2500ms: silent
    File sysFile = createVariableRmsWav("long_both_sys.wav",
        new double[]{0.5, 0.5, 0.5, 0.5, 0.5,            // 0-500ms: active
                     0.5, 0.5, 0.5, 0.5, 0.5,            // 500-1000ms: active
                     0.4, 0.4, 0.4, 0.4, 0.4,            // 1000-1500ms: active (similar to mic)
                     0.4, 0.4, 0.4, 0.4, 0.4,            // 1500-2000ms: active (similar to mic)
                     0.5, 0.5, 0.5, 0.5, 0.5});          // 2000-2500ms: active

    List<SourceActivityTracker.ActivitySegment> timeline =
        tracker.trackActivity(micFile, sysFile);

    // 1-second BOTH segment should be preserved (not smoothed)
    boolean hasBothSegment = timeline.stream()
        .anyMatch(s -> s.source == SourceActivityTracker.Source.BOTH);
    assertTrue(hasBothSegment, "Long BOTH segment should be preserved");

    micFile.delete();
    sysFile.delete();
}

@Test
void testDominanceRatioConfiguration_Clamping() {
    // Test that ratio is clamped to valid range [1.5, 10.0]
    SourceActivityTracker tooLow = new SourceActivityTracker(100, 0.005, 0.5);
    assertEquals(1.5, tooLow.getDominanceRatio(), 0.001); // Clamped to MIN

    SourceActivityTracker tooHigh = new SourceActivityTracker(100, 0.005, 20.0);
    assertEquals(10.0, tooHigh.getDominanceRatio(), 0.001); // Clamped to MAX

    SourceActivityTracker inRange = new SourceActivityTracker(100, 0.005, 5.0);
    assertEquals(5.0, inRange.getDominanceRatio(), 0.001); // Unchanged
}

// === Test Utility Methods ===

/**
 * Create a test WAV file with constant RMS level.
 * @param filename Output filename
 * @param rmsLevel Target RMS level (0.0-1.0)
 * @param durationMs Duration in milliseconds
 * @return Created WAV file
 */
private File createTestWav(String filename, double rmsLevel, int durationMs) throws Exception {
    File file = new File(System.getProperty("java.io.tmpdir"), filename);
    int sampleRate = 16000;
    int numSamples = (sampleRate * durationMs) / 1000;

    // Generate sine wave at target RMS
    // RMS of sine = amplitude / sqrt(2), so amplitude = RMS * sqrt(2)
    double amplitude = rmsLevel * Math.sqrt(2) * 32767;
    byte[] audioData = new byte[numSamples * 2]; // 16-bit samples

    for (int i = 0; i < numSamples; i++) {
        double sample = amplitude * Math.sin(2 * Math.PI * 440 * i / sampleRate);
        short sampleShort = (short) Math.max(-32768, Math.min(32767, sample));
        audioData[i * 2] = (byte) (sampleShort & 0xFF);
        audioData[i * 2 + 1] = (byte) ((sampleShort >> 8) & 0xFF);
    }

    // Write WAV file
    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
    ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
    AudioInputStream ais = new AudioInputStream(bais, format, numSamples);
    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);

    return file;
}

/**
 * Create a test WAV with variable RMS levels per 100ms interval.
 */
private File createVariableRmsWav(String filename, double[] rmsPerInterval) throws Exception {
    File file = new File(System.getProperty("java.io.tmpdir"), filename);
    int sampleRate = 16000;
    int samplesPerInterval = sampleRate / 10; // 100ms intervals
    int totalSamples = rmsPerInterval.length * samplesPerInterval;

    byte[] audioData = new byte[totalSamples * 2];

    for (int interval = 0; interval < rmsPerInterval.length; interval++) {
        double amplitude = rmsPerInterval[interval] * Math.sqrt(2) * 32767;
        int startSample = interval * samplesPerInterval;

        for (int i = 0; i < samplesPerInterval; i++) {
            double sample = amplitude * Math.sin(2 * Math.PI * 440 * i / sampleRate);
            short sampleShort = (short) Math.max(-32768, Math.min(32767, sample));
            int idx = (startSample + i) * 2;
            audioData[idx] = (byte) (sampleShort & 0xFF);
            audioData[idx + 1] = (byte) ((sampleShort >> 8) & 0xFF);
        }
    }

    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
    ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
    AudioInputStream ais = new AudioInputStream(bais, format, totalSamples);
    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);

    return file;
}
```

### Integration Tests

1. **Test with real dual-source recordings** from the issue description
2. **Verify backward compatibility** - existing single-source recordings unaffected
3. **Benchmark performance** - ensure <50ms overhead

---

## Rollout Plan

1. **Phase 1 Implementation** (Dominance Ratio)
   - Implement and test
   - Deploy with default ratio 3.0
   - Collect user feedback

2. **Phase 2 Implementation** (Segment Smoothing)
   - Add if Phase 1 doesn't fully resolve
   - Enable by default

3. **Phase 3 Implementation** (LLM Post-Processing)
   - Implement as optional pipeline unit
   - Disabled by default
   - Document as "High Accuracy Mode" in settings

---

## Files Modified Summary

| File | Change Type | Description |
|------|-------------|-------------|
| `SourceActivityTracker.java` | Modify | Add dominance ratio, segment smoothing |
| `SourceActivityTrackerTest.java` | Modify | Add unit tests |
| `AttributionCleanupUnit.java` | New (Phase 3) | LLM post-processing unit |
| `PipelineRegistry.java` | Modify (Phase 3) | Register new unit |
