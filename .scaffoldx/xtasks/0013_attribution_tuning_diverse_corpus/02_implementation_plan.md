# Implementation Plan: Attribution Tuning with Diverse Test Corpus

## Phase 1: Corpus Collection (Post-Task 0010)

### Prerequisites
- Task 0010 complete (retention system operational)
- Retention enabled for test recordings
- Storage allocated for corpus (~2GB estimated)

### Recording Protocol

For each scenario:
1. Configure dual-source recording (mic + system)
2. Enable retention flag to preserve fragments
3. Record 2-5 minute sample representing the scenario
4. Export both WAV files to corpus directory
5. Create ground-truth annotation (timestamps + expected labels)

### Ground Truth Annotation Format

```json
{
  "scenario_id": "rapid_alternation_01",
  "description": "Language learning app with 2-second prompts",
  "duration_seconds": 180,
  "valid_labels": ["NONE", "USER", "SYSTEM", "BOTH"],
  "segments": [
    {"start": 0.0, "end": 2.3, "expected": "SYSTEM", "confidence": "high"},
    {"start": 2.3, "end": 4.1, "expected": "USER", "confidence": "high"},
    {"start": 4.1, "end": 4.4, "expected": "NONE", "confidence": "medium"}
  ],
  "confidence_rules": {
    "high": "Exact boundary ±50ms tolerance",
    "medium": "Boundary ±200ms tolerance",
    "low": "Excluded from error calculation"
  },
  "notes": "Some boundary timestamps are approximate due to natural speech overlap"
}
```

### Confidence Levels
- **high**: Clear, unambiguous source
- **medium**: Reasonable judgment, others might disagree on exact boundary
- **low**: Genuinely ambiguous, acceptable error zone

## Phase 2: Measurement & Tuning

### Measurement Harness Extension

Extend `AttributionMeasurementTest.java` to:
1. Load multiple scenarios from corpus
2. Run attribution on each
3. Calculate per-scenario error rates
4. Generate summary report

### Error Metrics

For each scenario, measure:
- **false_both_rate**: Single-source segments incorrectly labeled [User+System]
- **false_single_rate**: Genuine crosstalk incorrectly labeled single-source
- **boundary_error_ms**: Average deviation from ground-truth segment boundaries
- **segment_count_diff**: Difference in segment count vs. ground truth

### Tuning Protocol

```
FOR each parameter combination:
    FOR each scenario:
        Run attribution with parameters
        Measure error metrics
        Record results
    END
    Calculate aggregate scores:
        - max_error (worst scenario)
        - mean_error (average across scenarios)
        - weighted_error (scenarios weighted by importance)
    END
END
SELECT parameter set with best aggregate score
DOCUMENT trade-offs for any scenarios with elevated error
```

### Parameter Search Space

| Parameter | Current | Search Range | Step |
|-----------|---------|--------------|------|
| RMS_THRESHOLD | 0.005 | 0.001 - 0.02 | 0.002 |
| DOMINANCE_RATIO | 3.0 | 1.5 - 6.0 | 0.5 |
| MIN_SEGMENT_MS | 300 | 100 - 500 | 100 |

Total combinations: 10 × 10 × 5 = 500 (feasible to exhaustively search)

### Parameter Search Failure Handling

IF exhaustive search finds no parameter set with ≤5% error on all scenarios:

1. **Document**: Record best achievable parameters and per-scenario error rates
2. **Identify outliers**: Which scenarios prevent convergence?
3. **Triage**:
   - If ≤2 scenarios fail: Accept elevated error, document trade-off, create follow-up task for scenario-specific profiles
   - If ≥3 scenarios fail: Escalate to `x-role-activate --as conceptualizer` for algorithm redesign consideration
4. **Ship with documentation**: Note known limitations in ADR-002 update

## Deliverables

1. **Test corpus**: 8-10 annotated recordings covering diverse scenarios
2. **Measurement report**: Per-scenario error rates with current vs. tuned parameters
3. **Parameter recommendation**: Final values with documented trade-offs
4. **Regression suite**: Automated test that runs all scenarios, fails if any exceed threshold
5. **ADR update**: Extend ADR-002 with tuning methodology and results
