# PRD: Mic Test Screen

## Problem Statement

WhisperDog's silence detection relies on RMS threshold values that users configure blindly. There's no feedback mechanism to verify settings work correctly for the user's environment.

**Issues:**
- Different environments require different thresholds (quiet room vs noisy cafe)
- Users must guess at appropriate RMS values
- No way to preview how silence removal affects their audio
- Misconfigured thresholds cause: speech cut off (too aggressive) or noise included (too lenient)
- Current "mic test" only shows signal peak - not useful for threshold calibration

**Impact:**
- ~15% of recordings require manual threshold adjustment
- Users resort to inspecting temp folder audio files manually
- No A/B comparison to verify filter behavior

## Solution Overview

A dedicated Mic Test Screen in Options → Audio that provides:

1. **Test Recording**: Record 5-10 second sample
2. **Real-time Metrics**: Live RMS meter during recording
3. **Post-Recording Analysis**: Silence ratio, non-silence duration
4. **A/B Comparison**: Play original vs filtered audio
5. **Live Adjustment**: Change thresholds and re-process without re-recording

## Functional Requirements

### FR-1: Test Recording

**Location**: Options → Audio → "Test Microphone" button opens dialog

**Recording Controls**:
- "Record Test" button starts recording
- "Stop" button ends recording (or auto-stop at 10 seconds)
- Duration display shows elapsed time
- Maximum recording: 10 seconds (prevents accidental long tests)

**Audio Format**:
- 16kHz sample rate (matches OpenAI requirements)
- 16-bit mono PCM
- Stored in memory (not saved to disk)

### FR-2: Real-Time RMS Meter

**During Recording**:
- Progress bar showing current RMS level (0.0 - 1.0)
- Updates at 10+ FPS for smooth visualization
- Color coding: Green (normal), Yellow (high), Red (clipping)

### FR-3: Post-Recording Metrics

**After Recording Stops**:
- **RMS Level**: Average RMS of entire recording
- **Peak Level**: Maximum amplitude detected
- **Silence Ratio**: Percentage classified as silence
- **Non-silence Duration**: Seconds of detected speech

### FR-4: A/B Playback Comparison

**Two Playback Options**:
- "Play Original" - Full unprocessed recording
- "Play Filtered" - After silence removal applied

**Behavior**:
- Only one can play at a time
- Filtered shows shorter duration (silence removed)

### FR-5: Threshold Adjustment

**Controls**:
- Silence RMS slider: 0.01 - 0.20 (default: 0.05)
- Min Duration slider: 0.1 - 1.0 seconds (default: 0.3s)

**Live Re-processing**:
- Changing sliders immediately re-processes stored audio
- Metrics update without re-recording
- Filtered playback reflects new settings

### FR-6: Settings Persistence

**Apply Button**: Saves current slider values to ConfigManager
**Reset Defaults**: Resets sliders to default values

## UI Layout

```
┌─────────────────────────────────────────────────────────┐
│  Microphone Test                                    [X] │
├─────────────────────────────────────────────────────────┤
│  [● Record Test]  [■ Stop]           Duration: 0:05     │
│  ─────────────────────────────────────────────────────  │
│  Metrics:                                               │
│    RMS Level:        ████████░░░░░░░░  0.42            │
│    Peak Level:       ██████████████░░  0.78            │
│    Silence Ratio:    67%                                │
│    Non-silence:      1.7 seconds                        │
│  ─────────────────────────────────────────────────────  │
│  Playback:                                              │
│    Original:    [▶ Play]  ═══════════════════  0:05    │
│    Filtered:    [▶ Play]  ═══════════════════  0:02    │
│  ─────────────────────────────────────────────────────  │
│  Threshold Settings:                                    │
│    Silence RMS:     [====●=====]  0.05                 │
│    Min Duration:    [====●=====]  0.3s                 │
│                              [Apply]  [Reset Defaults]  │
└─────────────────────────────────────────────────────────┘
```

## Non-Functional Requirements

- Recording starts within 100ms of button click
- RMS meter updates at 10+ FPS
- Audio playback starts within 200ms
- Re-processing completes within 500ms
- Test audio stored in memory only (no temp files)
- Works in both light and dark themes

## Out of Scope

- Waveform visualization
- Multiple test recordings comparison
- Noise profile analysis
- Automatic threshold recommendation
