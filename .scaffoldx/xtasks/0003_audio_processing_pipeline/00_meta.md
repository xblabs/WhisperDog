---
task_id: "0003"
task_name: "Mic Test Screen"
status: "todo"
priority: "medium"
created: "2025-12-30"
task_type: "executable"
tags: ["audio", "mic-test", "silence-detection", "calibration", "user-experience"]
---

# Task 0003: Mic Test Screen

## Summary

Create a microphone test screen in Options that allows users to calibrate silence detection thresholds with real-time feedback. Users can record a short test clip, see RMS metrics, and compare original vs silence-removed audio side-by-side.

## Context

Users currently have no way to test silence threshold settings before recording. Different environments (quiet office vs noisy cafe) require different RMS thresholds, but users must guess and adjust blindly. This leads to either:
- Too aggressive filtering (speech cut off)
- Too lenient filtering (noise included)

## Key Objectives

1. Test recording with real-time RMS meter
2. Calculate and display silence ratio and non-silence duration
3. Side-by-side playback: original vs filtered audio
4. Threshold adjustment sliders with live re-processing
5. Apply settings to persist configuration

## Success Criteria

- Mic test accessible from Options → Audio
- User can record up to 10 seconds of test audio
- RMS meter updates in real-time during recording
- Both original and filtered audio can be played back
- Threshold sliders immediately re-process without re-recording
- Settings persist when "Apply" is clicked

## Related Files

- Source specification: WD-0004 from whisperdog.md
- Silence detection: `src/main/java/org/whisperdog/audio/`
