---
issue_id: ISS_00006
title: Minimum Non-Silence Duration Threshold
type: enhancement
priority: low
status: open
created: 2025-12-30
tags: [audio, validation, user-experience]
alias: WD-0006
---

# ISS_00006: Minimum Non-Silence Duration Threshold

## Problem Description

Add a minimum non-silence duration threshold setting that discards recordings where detected speech is below a configurable minimum (e.g., < 0.5 seconds). This prevents accidental transcription of noise-only recordings.

## Status

- **Current Status**: resolved
- **Priority**: low
- **Type**: enhancement
- **Created**: 2025-12-30
- **Resolved**: 2025-12-31

## Tags

- audio
- validation
- user-experience

## Problem Details

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

## Acceptance Criteria

- [ ] Setting added to ConfigManager
- [ ] UI slider in Settings → Audio
- [ ] Warning dialog when threshold not met
- [ ] User can choose to discard or proceed

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
- `src/main/java/org/whisperdog/recording/AudioFileAnalyzer.java`

## Notes

Feature request for improved user experience.

## Work Log

### 2025-12-31 - Session 1

**Diagnosis**: No minimum speech duration check existed. Short/accidental recordings went directly to API.

**Approach**: Add configurable threshold with UI slider and warning dialog, reusing existing silence analysis.

**Steps Taken**:
1. Added `getMinSpeechDuration()`/`setMinSpeechDuration()` to `ConfigManager.java:333-341`
2. Added slider (0-2.0s range) to `SettingsForm.java:437-486`
3. Created `MinSpeechDurationDialog.java` with Discard/Transcribe Anyway buttons
4. Integrated check in `RecorderForm.java:1249-1283` after silence analysis

**Outcome**: Success - build compiles. Feature uses existing `estimatedUsefulSeconds` from silence analysis.
