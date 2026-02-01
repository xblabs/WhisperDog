---
task_id: "0008"
title: "Dual-Source Audio Attribution Accuracy"
status: review
priority: high
created: 2026-01-25
completed: 2026-01-29
tags: [audio, transcription, attribution, dual-source, rms, whisper]
complexity: medium
estimated_phases: 3
actual_phases: 1
parent_task: null
related_tasks: []
adr_reference: ".scaffoldx/xcontext/development/adr/002_dual_source_attribution.md"
---

# Task 0008: Dual-Source Audio Attribution Accuracy

## Summary

Fix incorrect `[User+System]` source attribution in dual-source (mic + system audio) transcriptions. The current implementation uses independent RMS threshold checks that misattribute audio when one source has low-level noise above threshold while the other source is clearly dominant.

## Problem Statement

When recording both microphone and system audio simultaneously, transcription output shows incorrect source labels:

**Current (Wrong):**
```
[User]: try a little bit with mic audio and system
[User+System]: audio Hello
[System]: this is Manni I'm a train driver...
```

**Expected (Correct):**
```
[User]: try a little bit with mic audio and system audio
[System]: Hello this is Manni I'm a train driver...
```

The root cause is in `SourceActivityTracker.java` where both sources are independently checked against a fixed threshold (0.005 RMS), without comparing relative signal levels.

## Success Criteria

1. Eliminate false `[User+System]` labels when only one source is actually speaking
2. Preserve accurate `[User+System]` labels for genuine crosstalk
3. No regression in single-source attribution accuracy
4. Minimal latency impact (<50ms additional processing)

## Key Files

- `src/main/java/org/whisperdog/audio/SourceActivityTracker.java` - Primary implementation target
- `src/test/java/org/whisperdog/audio/SourceActivityTrackerTest.java` - Test coverage
- `.scaffoldx/xcontext/development/adr/002_dual_source_attribution.md` - Architecture decision record

## Approach

Three-phase implementation with cascading options:
1. **Phase 1**: Dominance ratio comparison (primary fix)
2. **Phase 2**: Segment smoothing for edge cases
3. **Phase 3**: Optional LLM post-processing pipeline (high-accuracy mode)
