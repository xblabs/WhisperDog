# PRD: Dual-Source Audio Attribution Accuracy

## 1. Problem Statement

### 1.1 Current Behavior

When WhisperDog records both microphone (user) and system audio simultaneously, the `SourceActivityTracker` analyzes RMS (Root Mean Square) levels at 100ms intervals to determine which source is active. The current logic uses independent threshold checks:

```java
// Current implementation (SourceActivityTracker.java:113-127)
boolean micActive = micRms[i] >= activityThreshold;     // threshold = 0.005
boolean systemActive = systemRms[i] >= activityThreshold;

if (micActive && systemActive) {
    source = Source.BOTH;  // ← PROBLEM: No relative comparison
} else if (micActive) {
    source = Source.USER;
} else if (systemActive) {
    source = Source.SYSTEM;
}
```

**The Issue**: If mic has ambient noise at RMS 0.006 and system audio is playing at RMS 0.5, both register as "active" → `[User+System]` even though the mic is clearly just background noise (83x quieter than system audio).

### 1.2 Real-World Example

**Actual Transcription Output (Incorrect):**
```
[User]: try a little bit with mic audio and system
[User+System]: audio Hello
[System]: this is Manni I'm a train driver from Berlin and I have some information...
[User]: this was coming from the system audio Now that was me now Mike
[System]: If I catch you then you'll have a big oh It's harder to get
[User]: That was the system audio again
[System]: In the train then in the Berghain If you come on your own...
[User+System]: have 30 different kinds of tickets die keiner
[System]: kapiert
[User+System]: But if
[System]: you have the
[User+System]: wrong
[System]: one
[User]: And now let it fizzle out
```

**Expected Transcription Output (Correct):**
```
[User]: try a little bit with mic audio and system audio
[System]: Hello this is Manni I'm a train driver from Berlin and I have some information for the Janssen Tourist Opfers First rule is don't piss in my train
[User]: So this was coming from the system audio Now that was me now Mike
[System]: If I catch you then you'll have a big oh It's harder to get
[User]: That was the system audio again
[System]: In the train then in the Berghain If you come on your own and I don't like you I will knock on the door in your face We have the newest technology You can always see the Verspätung in Ultra HD We have 30 different kinds of tickets die keiner kapiert But if you have the wrong one
[User]: And now let it fizzle out
```

### 1.3 Impact Analysis

| Metric | Current State | Impact |
|--------|---------------|--------|
| False `[User+System]` rate | ~15-25% of segments | Significant attribution errors |
| Word splitting | Frequent | Semantic breaks mid-word ("audio Hello" split) |
| Sentence fragmentation | Common | Interrupted speaker turns |
| Post-editing effort | High | Users must manually fix attribution |

### 1.4 Root Cause Analysis

The fundamental issue is **independent threshold comparison without relative level consideration**:

1. **Fixed threshold (0.005)** doesn't account for varying noise floors
2. **No dominance ratio** - a source 100x quieter still counts as "active"
3. **No contextual smoothing** - brief noise spikes create spurious segments
4. **Word timestamps are accurate** (from OpenAI) but activity segments are wrong

## 2. Solution Architecture

### 2.1 Three-Phase Approach

We implement a cascading solution with increasing sophistication:

```
Phase 1: Dominance Ratio    → Primary fix for false BOTH detections
         ↓
Phase 2: Segment Smoothing  → Catches edge cases from brief noise spikes
         ↓
Phase 3: LLM Post-Process   → Optional high-accuracy mode
```

### 2.1.1 Phase Gate Criteria

| Gate | Measurement | Pass Threshold | Action if Fail |
|------|-------------|----------------|----------------|
| Phase 1 → Done | False `[User+System]` rate on test corpus | ≤ 5% | Proceed to Phase 2 |
| Phase 2 → Done | False `[User+System]` rate on test corpus | ≤ 2% | Proceed to Phase 3 OR accept |
| Phase 3 → Done | False `[User+System]` rate on test corpus | ≤ 0.5% | Accept or escalate to VAD |

**Measurement Method:**
1. Run transcription on test corpus (5 recordings, ~10 min total)
2. Compare output against `artifacts/test_corpus/ground_truth.json`
3. Calculate: `false_both_rate = (false_both_segments / total_segments) * 100`

**Test Corpus Location:** `.scaffoldx/xtasks/0008_dual_source_attribution_accuracy/artifacts/test_corpus/`

### 2.2 Phase 1: Dominance Ratio Comparison

**Concept**: When both sources appear active, compare their relative RMS levels. If one dominates by a configurable ratio (default 3:1), attribute exclusively to the dominant source.

**Algorithm:**
```java
// Proposed implementation
if (micActive && systemActive) {
    double ratio = micRms[i] / systemRms[i];
    if (ratio > DOMINANCE_RATIO) {           // mic dominates
        source = Source.USER;
    } else if (ratio < 1.0 / DOMINANCE_RATIO) {  // system dominates
        source = Source.SYSTEM;
    } else {
        source = Source.BOTH;                // genuine crosstalk
    }
}
```

**Parameters:**
- `DOMINANCE_RATIO`: Default 3.0 (configurable)
  - Ratio > 3.0: Mic is 3x louder → USER only
  - Ratio < 0.33: System is 3x louder → SYSTEM only
  - 0.33 ≤ Ratio ≤ 3.0: Genuine BOTH

**Why 3.0?**
- 3:1 ratio = ~9.5dB difference (perceptually significant)
- Conservative enough to catch genuine crosstalk
- Aggressive enough to filter ambient noise

### 2.3 Phase 2: Segment Smoothing

**Concept**: Short `BOTH` segments (< 500ms) surrounded by the same single-source type are likely spurious and should be absorbed.

**Algorithm:**
```java
// In mergeShortSegments() or new smoothBothSegments()
if (current.source == Source.BOTH && current.getDurationMs() < 500) {
    Source before = i > 0 ? segments.get(i-1).source : Source.SILENCE;
    Source after = i < segments.size()-1 ? segments.get(i+1).source : Source.SILENCE;

    // If surrounded by same source, absorb
    if (before == after && before != Source.SILENCE && before != Source.BOTH) {
        current = new ActivitySegment(current.startMs, current.endMs, before);
    }
}
```

**Rationale:**
- 500ms threshold: Short enough to catch noise, long enough to preserve real crosstalk
- Only smooths when context is consistent (same source before and after)
- Preserves genuine `BOTH` segments that are longer or have mixed context

### 2.4 Phase 3: LLM Post-Processing (Optional)

**Concept**: For high-accuracy use cases, add an optional pipeline step that uses semantic inference to clean up remaining misattributions.

**Prompt Template:**
```
You are cleaning up a dual-source transcription where [User] is microphone audio
and [System] is computer/system audio.

RULES:
1. If a word is split across sources (e.g., "[User+System]: audio Hello" should
   be "[System]: audio Hello" if it's part of system speech), assign the whole
   word to the semantically appropriate source
2. [User+System] segments adjacent to [System] with semantic continuity should
   merge into [System] (and vice versa for [User])
3. Preserve genuine interruptions/crosstalk only when speakers are semantically
   distinct (different topics, responses, reactions)
4. Consider sentence boundaries - a sentence should generally belong to one speaker

INPUT:
{raw_transcript}

OUTPUT (corrected transcript with same label format, no explanations):
```

**When to Use:**
- User-configurable option in settings
- High-accuracy transcription mode
- Important recordings where attribution matters

**Trade-offs:**
- +1-2 seconds latency per transcription
- +$0.001-0.005 cost (GPT-4o-mini or similar)
- Additional failure point (API errors)

## 3. Technical Specifications

### 3.1 Modified Classes

| Class | Modification |
|-------|-------------|
| `SourceActivityTracker` | Add dominance ratio logic, segment smoothing |
| `SourceActivityTrackerTest` | Add tests for new behavior |
| `PostProcessingService` (if Phase 3) | Add attribution cleanup pipeline unit |

### 3.2 New Configuration Parameters

```java
// SourceActivityTracker.java - new constants
public static final double DEFAULT_DOMINANCE_RATIO = 3.0;
private final double dominanceRatio;

// Constructor overload
public SourceActivityTracker(int sampleIntervalMs, double activityThreshold,
                             double dominanceRatio) {
    this.sampleIntervalMs = sampleIntervalMs;
    this.activityThreshold = activityThreshold;
    this.dominanceRatio = dominanceRatio;
}
```

### 3.3 API Compatibility

- **Backward Compatible**: Default behavior unchanged if no dominance ratio specified
- **No Breaking Changes**: Existing code using `SourceActivityTracker()` constructor works
- **Opt-in Enhancement**: New parameters are optional

## 4. Acceptance Criteria

### 4.1 Functional Requirements

| ID | Requirement | Test Method |
|----|-------------|-------------|
| FR-1 | False `[User+System]` rate reduced by ≥70% | Compare before/after on test recordings |
| FR-2 | No increase in false `[User]` or `[System]` labels | Regression test suite |
| FR-3 | Genuine crosstalk still labeled as `[User+System]` | Manual verification with known crosstalk samples |
| FR-4 | Dominance ratio configurable (1.5 - 10.0 range) | Unit test parameter validation |

### 4.2 Non-Functional Requirements

| ID | Requirement | Acceptance |
|----|-------------|------------|
| NFR-1 | Processing latency increase < 50ms | Benchmark on 5-minute recording |
| NFR-2 | Memory footprint unchanged | Profile before/after |
| NFR-3 | Thread-safe implementation | Concurrent test with multiple recordings |

### 4.3 Test Scenarios

1. **Scenario A**: System audio playing, mic has ambient noise only
   - Expected: 100% `[System]` labels, zero `[User+System]`

2. **Scenario B**: User speaking, system silent
   - Expected: 100% `[User]` labels

3. **Scenario C**: User speaking over system audio (genuine crosstalk)
   - Expected: `[User+System]` for overlapping regions

4. **Scenario D**: User briefly pauses, system audio continues
   - Expected: Smooth transition from `[User+System]` or `[User]` to `[System]`

### 4.4 Edge Cases

| Edge Case | RMS Values | Expected Behavior |
|-----------|------------|-------------------|
| System RMS = 0 | mic=0.3, sys=0.0 | USER only (division guard: ratio=MAX_VALUE) |
| Both RMS = 0 | mic=0.0, sys=0.0 | SILENCE (neither above threshold) |
| Both RMS near-zero | mic=0.001, sys=0.001 | SILENCE (both below 0.005 threshold) |
| Very short recording (<100ms) | - | Single interval, direct attribution |
| Identical RMS levels | mic=0.3, sys=0.3 | BOTH (ratio=1.0, within range) |

## 5. Alternatives Considered

### 5.1 Whisper Metadata Injection (Rejected)

**Idea**: Pass signal level metadata to Whisper alongside audio to help with attribution.

**Why Rejected**:
- Whisper API doesn't accept auxiliary metadata
- Would require custom model fine-tuning
- Fundamental architecture mismatch

### 5.2 Pre-Processing Noise Gate (Rejected)

**Idea**: Apply noise gate to mic channel when system audio is louder, before merging tracks.

**Why Rejected**:
- Destructive to audio - could suppress legitimate user speech
- Hard to tune threshold dynamically
- Loses information that could be useful for edge cases

### 5.3 Voice Activity Detection Model (Deferred)

**Idea**: Use dedicated VAD model (Silero VAD, WebRTC VAD) instead of RMS.

**Why Deferred**:
- More complex integration
- Additional dependency
- Consider for future enhancement if RMS-based approach insufficient

## 6. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Dominance ratio too aggressive | Medium | Loses real crosstalk | Make ratio configurable, default conservative |
| Ratio too conservative | Medium | Doesn't fix enough | Test with real recordings, adjust default |
| LLM hallucination in Phase 3 | Low | Corrupted attribution | Constrained prompt, output validation |
| Performance regression | Low | Slower transcription | Benchmark, optimize if needed |

## 7. Related Documents

- **ADR**: [002_dual_source_attribution.md](../../.scaffoldx/xcontext/development/adr/002_dual_source_attribution.md)
- **Implementation**: [SourceActivityTracker.java](../../src/main/java/org/whisperdog/audio/SourceActivityTracker.java)
- **Tests**: [SourceActivityTrackerTest.java](../../src/test/java/org/whisperdog/audio/SourceActivityTrackerTest.java)
