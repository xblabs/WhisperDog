# Test Corpus for Task 0008: Dual-Source Attribution Accuracy

## Purpose

This directory contains test recordings and ground truth annotations for validating the dual-source attribution fix. The test corpus enables quantified measurement of false `[User+System]` rates before and after implementation.

## Required Recordings

| File | Duration | Content Description | Expected Attribution |
| ---- | -------- | ------------------- | -------------------- |
| `scenario_a_system_only.wav` | 60s | System audio playing (podcast/video), mic captures ambient noise only (0.005-0.01 RMS) | 100% `[System]`, 0% `[User+System]` |
| `scenario_b_user_only.wav` | 60s | User speaking into mic, no system audio playing | 100% `[User]`, 0% `[User+System]` |
| `scenario_c_genuine_crosstalk.wav` | 60s | User intentionally speaking over system audio | Mixed with `[User+System]` at overlap points |
| `scenario_d_transitions.wav` | 60s | User pauses mid-sentence, system audio continues, user resumes | Clean `[User]` → `[System]` → `[User]` transitions |
| `realworld_manni.wav` | 120s | The "Manni train driver" recording from issue description | Per `ground_truth.json` |

## Recording Requirements

- **Sample rate**: 16kHz mono (matches Whisper input)
- **Format**: WAV (uncompressed)
- **Dual-source**: Each scenario requires TWO files:
  - `{scenario}_mic.wav` - Microphone channel
  - `{scenario}_system.wav` - System audio channel
- **Naming**: Use exact filenames above for automated measurement

## Ground Truth Format

File: `ground_truth.json`

```json
{
  "metadata": {
    "created": "2026-01-XX",
    "annotator": "manual",
    "version": "1.0"
  },
  "recordings": {
    "scenario_a_system_only": {
      "expected_user_percentage": 0,
      "expected_system_percentage": 100,
      "expected_both_percentage": 0,
      "tolerance_percentage": 2
    },
    "scenario_b_user_only": {
      "expected_user_percentage": 100,
      "expected_system_percentage": 0,
      "expected_both_percentage": 0,
      "tolerance_percentage": 2
    },
    "scenario_c_genuine_crosstalk": {
      "expected_both_percentage_min": 10,
      "expected_both_percentage_max": 40,
      "segments": [
        {"start_ms": 5000, "end_ms": 8000, "expected_source": "BOTH"},
        {"start_ms": 15000, "end_ms": 20000, "expected_source": "BOTH"}
      ]
    },
    "scenario_d_transitions": {
      "transitions": [
        {"time_ms": 10000, "from": "USER", "to": "SYSTEM"},
        {"time_ms": 25000, "from": "SYSTEM", "to": "USER"}
      ],
      "expected_both_percentage_max": 5
    },
    "realworld_manni": {
      "baseline_false_both_rate": 18.5,
      "target_false_both_rate": 5.0,
      "segments": [
        {"text": "try a little bit with mic audio and system audio", "expected_source": "USER"},
        {"text": "Hello this is Manni", "expected_source": "SYSTEM"},
        {"text": "this was coming from the system audio", "expected_source": "USER"}
      ]
    }
  }
}
```

## Measurement Script

Run attribution accuracy measurement:

```bash
# From project root
./gradlew test --tests "SourceActivityTrackerTest.measureAttributionAccuracy"

# Or standalone script (if created)
java -cp build/libs/whisperdog.jar org.whisperdog.test.MeasureAttribution artifacts/test_corpus/
```

Output format:

```json
{
  "recording": "scenario_a_system_only",
  "total_segments": 45,
  "user_segments": 0,
  "system_segments": 45,
  "both_segments": 0,
  "false_both_rate": 0.0,
  "pass": true
}
```

## Phase Gate Thresholds

| Phase | Maximum False BOTH Rate | Action if Exceeded |
| ----- | ----------------------- | ------------------ |
| Phase 1 (Dominance Ratio) | 5% | Proceed to Phase 2 |
| Phase 2 (Segment Smoothing) | 2% | Proceed to Phase 3 or accept |
| Phase 3 (LLM Cleanup) | 0.5% | Accept or escalate to VAD |

## Creating Test Recordings

### Option 1: Use existing recordings

If you have the original "Manni" recording from the issue, place the separate mic and system WAV files here.

### Option 2: Synthetic generation

The unit tests in `SourceActivityTrackerTest.java` include helper methods for creating synthetic WAV files with controlled RMS levels:

- `createTestWav(filename, rmsLevel, durationMs)` - Constant RMS
- `createVariableRmsWav(filename, rmsPerInterval)` - Variable RMS per 100ms interval

### Option 3: Manual recording

1. Start WhisperDog with dual-source recording enabled
2. Play known audio through system while speaking/staying silent as needed
3. Export the separate channel WAV files
4. Manually annotate expected attribution in `ground_truth.json`
