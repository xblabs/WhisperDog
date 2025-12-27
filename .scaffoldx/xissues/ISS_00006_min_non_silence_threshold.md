# ISS_00006: Minimum Non-Silence Duration Threshold

**Alias**: WD-0006
**Status**: Open
**Priority**: Low
**Severity**: Minor
**Created**: 2025-12-30

## Summary

Add a minimum non-silence duration threshold setting that discards recordings where detected speech is below a configurable minimum (e.g., < 0.5 seconds). This prevents accidental transcription of noise-only recordings.

## Problem

Users sometimes accidentally trigger recording and capture only ambient noise or very brief sounds. The current silence detection may pass these through if they exceed the RMS threshold, resulting in:

1. Wasted API calls to OpenAI for near-empty transcriptions
2. Transcription results that are just noise artifacts ("um", "[silence]", etc.)
3. Cluttered history with useless entries

## Expected Behavior

**New Setting**: "Minimum speech duration" (default: 0.5s)

**Flow**:
1. Recording completes
2. Silence removal applied
3. Non-silence duration calculated
4. If non-silence < minimum threshold:
   - Show warning: "Recording contains less than 0.5s of detected speech. Discard?"
   - Options: [Discard] [Transcribe Anyway]

## Implementation Notes

- Add `minNonSilenceDuration` to ConfigManager (default: 0.5)
- Add slider in Settings → Audio (range: 0.1 - 2.0 seconds)
- Check duration after silence removal, before API call
- Reuse AudioAnalyzer from Mic Test feature

## UI Location

Settings → Audio → "Minimum speech duration: [slider] 0.5s"

## Test Cases

1. Record 0.3s of speech → Warning shown
2. Record 1s of speech → No warning, transcription proceeds
3. Disable feature (set to 0) → No duration check
4. User clicks "Transcribe Anyway" → Transcription proceeds

## Related Files

- `src/main/java/org/whisperdog/ConfigManager.java`
- `src/main/java/org/whisperdog/settings/SettingsForm.java`
- `src/main/java/org/whisperdog/audio/` (AudioAnalyzer)
