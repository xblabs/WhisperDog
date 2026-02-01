---
task_id: "0011"
title: "Synchronized Dual-Source Silence Removal"
status: todo
priority: medium
created: 2026-01-28
task_type: executable
tags:
  - audio
  - silence-removal
  - dual-source
  - frame-sync
  - optimization
complexity: medium
estimated_phases: 2
parent_task: null
related_tasks: ["0008"]
---

# Task 0011: Synchronized Dual-Source Silence Removal

## Summary

Implement silence removal for dual-source (mic + system) recordings by detecting silence regions in both tracks independently, computing their intersection (segments where BOTH are silent), and splicing identical frame ranges from both tracks to maintain perfect synchronization.

## Problem Statement

Currently, silence removal is completely skipped when system audio capture is active because removing silence from only one track would break frame alignment needed for source attribution. This means dual-source recordings always include all dead air, leading to longer audio sent to Whisper and wasted transcription time.

## Key Files

- `src/main/java/org/whisperdog/recording/SilenceRemover.java` - Silence detection and removal logic
- `src/main/java/org/whisperdog/recording/RecorderForm.java` - Orchestration and code path selection
- `src/main/java/org/whisperdog/audio/FFmpegUtil.java` - Audio track merging

## Related

- Task 0008: Dual-Source Attribution Accuracy (complementary, independent)
