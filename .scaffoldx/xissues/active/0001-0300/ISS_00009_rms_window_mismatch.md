---
id: ISS_00009
title: RMS Window Size Mismatch Between Calibration and Actual Silence Removal
status: resolved
priority: high
created: 2026-01-06
category: bug
affects:
  - src/main/java/org/whisperdog/audio/AudioAnalyzer.java
  - src/main/java/org/whisperdog/settings/SettingsForm.java
  - src/main/java/org/whisperdog/recording/SilenceRemover.java
---

# ISS_00009: RMS Window Size Mismatch

## Problem

The silence threshold calibration UI (MicTest and Settings live VU meter) uses different RMS window sizes than the actual silence removal during transcription. This causes calibrated threshold values to behave unexpectedly.

## Root Cause

Three different RMS analysis contexts use different window durations:

| Component | Window Size | File |
|-----------|-------------|------|
| SettingsForm (live VU) | ~128ms (raw 4096-byte buffer) | SettingsForm.java:1087 |
| AudioAnalyzer (MicTest) | **20ms** | AudioAnalyzer.java:81 |
| SilenceRemover (actual) | **100ms** | SilenceRemover.java:356 |

## Impact

- User calibrates threshold using 20ms windows in MicTest
- Actual transcription uses 100ms windows
- Shorter windows show more RMS variance (brief quiet moments register as low RMS)
- Threshold appears to "chop into audio" when set based on MicTest calibration

## Solution

Standardize all RMS analysis to use **100ms windows** matching SilenceRemover:

1. **AudioAnalyzer.java**: Change `sampleRate * 0.02` to `sampleRate * 0.1`
2. **SettingsForm.java**: Accumulate samples into 100ms buffer before RMS calculation

## Files to Modify

- `src/main/java/org/whisperdog/audio/AudioAnalyzer.java`
- `src/main/java/org/whisperdog/settings/SettingsForm.java`

## Testing

- Calibrate threshold in MicTest with moderate speech
- Record same speech in main app
- Verify silence removal matches calibration preview
