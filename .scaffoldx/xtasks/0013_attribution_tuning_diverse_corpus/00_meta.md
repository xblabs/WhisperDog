---
task_id: "0013"
title: "Attribution Tuning with Diverse Test Corpus"
status: todo
priority: medium
created: 2026-01-31
tags: [audio, attribution, testing, tuning, regression, dual-source]
complexity: medium
estimated_phases: 2
parent_task: null
related_tasks: ["0008", "0010"]
dependencies: ["0010"]
adr_reference: ".scaffoldx/xcontext/development/adr/002_dual_source_attribution.md"
---

# Task 0013: Attribution Tuning with Diverse Test Corpus

## Summary

Build a diverse test corpus for dual-source attribution and use it to tune algorithm parameters. Depends on Task 0010 (Recording Retention System) to preserve audio fragments for iterative analysis.

## Motivation

Task 0008 achieved 0.00% false_both_rate on a single test recording (`realworld_manni`). While this passed the gate check, one recording represents a narrow sample of real-world usage patterns. Different acoustic scenarios may expose edge cases where the current dominance ratio (3.0) and RMS threshold (0.005) produce suboptimal results.

**Key insight**: Without fragment retention (Task 0010), each test is "fire and forget" - we can observe results but cannot re-analyze with tweaked parameters. This task should begin only after retention is in place.

## Success Criteria

1. Test corpus covers ≥8 distinct acoustic scenarios
2. Each scenario has ground-truth annotations
3. Algorithm parameters tuned to achieve:
   - ≤5% false_both_rate (single-source segments incorrectly labeled BOTH)
   - ≤5% false_single_rate (BOTH segments incorrectly labeled single-source)
   - Measured as: (misattributed_duration_ms / total_evaluated_duration_ms) × 100
   - Excludes segments marked confidence="low" from evaluation
4. Regression test suite runnable on future algorithm changes
5. Document any scenario-specific tradeoffs discovered

## Key Files

- `src/main/java/org/whisperdog/audio/SourceActivityTracker.java` - Algorithm parameters
- `src/test/java/org/whisperdog/audio/AttributionMeasurementTest.java` - Measurement harness
- `artifacts/test_corpus/` - Test recordings and ground truth

## Blocking Dependency

**Task 0010 (Recording Retention System)** must be complete before meaningful work can begin. Without retention, we cannot:
- Preserve fragments for re-analysis
- Iterate on parameters against the same recordings
- Build a persistent regression suite
