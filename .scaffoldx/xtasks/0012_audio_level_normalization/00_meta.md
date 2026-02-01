---
task_id: "0012"
title: "Audio Level Normalization"
status: todo
priority: high
created: 2026-01-30
task_type: executable
tags:
  - audio
  - normalization
  - dual-source
  - gain
  - clipping-prevention
complexity: medium
estimated_phases: 2
parent_task: null
related_tasks: ["0006", "0008", "0011"]
---

# Task 0012: Audio Level Normalization

## Summary

Implement audio level normalization for dual-source recordings to handle device-dependent gain imbalances. Analyzes and balances mic and system audio levels BEFORE merge to prevent clipping, improve transcription quality, and ensure accurate source attribution.

## Problem Statement

Device-dependent gain settings cause extreme level imbalances in dual-source recordings. Example: Headset + Zoom produces clipped/distorted system audio while mic is barely audible. Different output devices (headset vs speakers) and software (Zoom, Discord, etc.) have wildly different internal gain levels, making merged recordings unusable without manual post-processing.

**Impacts:**
- Transcription quality (Whisper struggles with clipping)
- Attribution accuracy (dominance ratio confused by extreme level differences)
- User experience (unlistenable merged output)

## Key Files

- `src/main/java/org/whisperdog/recording/RecorderForm.java` - Integration point between capture and merge
- `src/main/java/org/whisperdog/audio/FFmpegUtil.java` - Audio processing utilities
- `src/main/java/org/whisperdog/audio/SourceActivityTracker.java` - Attribution (affected by level imbalances)

## Related

- Task 0006: System Audio Capture (provides the dual-source recordings)
- Task 0008: Dual-Source Attribution Accuracy (depends on balanced levels)
- Task 0011: Synchronized Dual-Source Silence Removal (runs after normalization)
