---
title: "ADR-002: Dual-Source Audio Attribution Accuracy"
adr_id: "002"
status: accepted
date: 2026-01-25
updated: 2026-01-29
context_type: adr
domain: development
tags: [adr, audio, transcription, attribution, dual-source, rms]
---

# ADR-002: Dual-Source Audio Attribution Accuracy

## Status

**Accepted** - Implemented (Task 0008)

## Context

When recording both microphone and system audio simultaneously, the source attribution in transcripts shows incorrect `[User+System]` labels when only one source is actually speaking. The current implementation in `SourceActivityTracker.java` uses independent RMS threshold checks:

```java
boolean micActive = micRms[i] >= 0.005;
boolean systemActive = systemRms[i] >= 0.005;

if (micActive && systemActive) {
    source = Source.BOTH;  // Often wrong!
}
```

**Problem**: If mic has ambient noise at RMS 0.006 and system audio is at 0.5, both register as "active" → `[User+System]` even though the mic is just background noise.

**Example of the problem**:

```
Actual (incorrect):
[User]: try a little bit with mic audio and system
[User+System]: audio Hello
[System]: this is Manni I'm a train driver...

Should be:
[User]: try a little bit with mic audio and system audio
[System]: Hello this is Manni I'm a train driver...
```

Word-level timestamps from OpenAI Whisper help, but the underlying activity detection still misattributes when one source has low-level noise above threshold.

## Decision

**Implemented Option 1: Dominance Ratio Comparison** with an additional fix to segment merging.

### Option 1: Dominance Ratio Comparison (Recommended)

Add relative level comparison when both sources appear active:

```java
if (micActive && systemActive) {
    double ratio = micRms[i] / systemRms[i];
    if (ratio > DOMINANCE_RATIO) {        // e.g., 3.0
        source = Source.USER;
    } else if (ratio < 1.0 / DOMINANCE_RATIO) {
        source = Source.SYSTEM;
    } else {
        source = Source.BOTH;
    }
}
```

**Pros**: Addresses root cause, minimal code change, no additional latency/cost
**Cons**: Requires tuning the ratio, may still miss edge cases

### Option 2: LLM Post-Processing Pipeline

Add optional cleanup step using semantic inference:

```
PROMPT: Fix misattributed segments in this dual-source transcript.
- If a word is split across sources, assign whole word to dominant source
- [User+System] next to [System] with semantic continuity → merge into [System]
- Preserve genuine crosstalk only when semantically distinct
```

**Pros**: Catches semantically obvious errors, works on existing transcripts
**Cons**: Adds 1-2s latency, ~$0.001-0.005 per transcription, another failure point

### Option 3: Segment Smoothing (No LLM)

Enhance `mergeShortSegments()` to absorb short `BOTH` segments into surrounding context:

```java
if (current.source == Source.BOTH && current.getDurationMs() < 500) {
    Source before = segments.get(i-1).source;
    Source after = segments.get(i+1).source;
    if (before == after && before != Source.SILENCE) {
        current = new ActivitySegment(current.startMs, current.endMs, before);
    }
}
```

**Pros**: No external dependencies, fast
**Cons**: Only fixes short spurious segments, doesn't help with longer misattributions

## Recommendation

Implement **Option 1 (Dominance Ratio)** first as the primary fix. If insufficient, add **Option 3 (Segment Smoothing)** as a second pass. Reserve **Option 2 (LLM Post-Processing)** as an optional pipeline step for high-accuracy use cases.

## Alternatives Rejected

### Whisper Metadata Injection

Idea: Pass signal level metadata to Whisper alongside audio to help it understand source dominance.

**Rejected**: Whisper API doesn't accept auxiliary metadata - it only processes raw audio. Would require custom model fine-tuning.

### Pre-Processing Noise Gate

Idea: Apply noise gate to mic channel when system audio is louder before merging.

**Rejected**: Destructive to audio; could suppress legitimate user speech during system playback. Hard to tune threshold dynamically.

## Implementation Results (2026-01-29)

### Measured Performance

| Metric                       | Baseline | After Fix   |
| ---------------------------- | -------- | ----------- |
| false_both_rate              | 9.04%    | **0.00%**   |
| Total segments               | 45       | 38          |
| Processing time (55s audio)  | ~0.9s    | ~0.9s       |

### Key Changes

1. **Dominance Ratio** (`DEFAULT_DOMINANCE_RATIO = 3.0`):
   - When both sources above threshold, requires 3:1 RMS ratio for single-source attribution
   - `MIN_RMS_FOR_RATIO = 0.0001` prevents division-by-zero

2. **Merge Logic Fix** (discovered during implementation):
   - Original `mergeShortSegments()` converted mismatched neighbors to BOTH
   - Fixed to use the longer neighbor's source, preserving correct attribution
   - This was critical to achieving 0% false BOTH rate

### Test Coverage

- 13 unit tests for dominance ratio scenarios
- 3 measurement tests for corpus validation
- All genuine crosstalk detection preserved

## Consequences

### With Option 1 Implemented

**Positive**:
- Fixes majority of misattributions at source
- No runtime cost increase
- Backward compatible (configurable ratio)

**Negative**:
- May require per-environment tuning (noisy vs quiet mic)
- Edge cases with similar volume levels still produce BOTH

### Future Considerations

1. **Adaptive threshold**: Learn noise floor from initial silence period
2. **Voice Activity Detection (VAD)**: Use dedicated VAD model instead of RMS
3. **Speaker diarization**: More sophisticated speaker separation post-Whisper

## Related

- [SourceActivityTracker.java](../../../../src/main/java/org/whisperdog/audio/SourceActivityTracker.java) - Implementation location
- [ADR-001: Pipeline Chaining](001_pipeline_chaining.md) - Related post-processing architecture
