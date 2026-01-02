---
issue_id: ISS_00004
title: Prompt before transcribing large recordings with high silence
type: enhancement
priority: low
status: resolved
created: 2025-12-30
tags: [user-experience, validation]
alias: WD-0005
---

# ISS_00004: Prompt before transcribing large recordings with high silence

## Problem Description

Show a confirmation prompt before transcribing recordings that have both long duration and high silence ratio. This prevents wasted API tokens and processing time on accidental long recordings that are mostly silence.

## Status

- **Current Status**: open
- **Priority**: low
- **Type**: enhancement
- **Created**: 2025-12-30

## Tags

- user-experience
- validation

## Problem Details

**Scenario**: User accidentally leaves recording running for 10+ minutes while doing other work. Recording is mostly silence with occasional background noise.

**Current Behavior**:
- Recording processed regardless of duration or silence content
- User wastes API tokens on transcribing silence
- Long wait time for minimal useful output

## Expected Behavior

When recording meets BOTH conditions:
- Duration > 600 seconds (10 minutes)
- Silence ratio > 50%

Show confirmation dialog:
```
Large Recording Detected

Duration: 12 minutes 34 seconds
Silence: 73%
Estimated useful audio: ~3 minutes

This recording has an unusually high silence ratio.
Transcribing may use significant API tokens for minimal output.

[Transcribe Anyway]  [Cancel]
```

## Acceptance Criteria

- [ ] Prompt appears when both thresholds exceeded
- [ ] User can choose to proceed or cancel
- [ ] Metrics visible in prompt (duration, silence %, estimated useful content)
- [ ] Does NOT prompt for recordings under thresholds
- [ ] Thresholds configurable in settings (optional)

## Implementation Notes

**Location**: After silence detection, before compression/transcription

```java
// After silence analysis completes
if (duration > LONG_DURATION_THRESHOLD &&
    silenceRatio > HIGH_SILENCE_THRESHOLD) {

    boolean proceed = showLargeRecordingDialog(
        duration,
        silenceRatio,
        estimatedUsefulSeconds
    );

    if (!proceed) {
        cancelTranscription();
        return;
    }
}
```

**Suggested thresholds**:
- Long duration: 600 seconds (10 min)
- High silence: 50%

**Note**: Silence detection must already be complete to calculate these metrics.

## Test Cases

1. 5 min recording, 30% silence → No prompt
2. 15 min recording, 20% silence → No prompt
3. 15 min recording, 60% silence → Prompt shown
4. Prompt → Click "Transcribe Anyway" → Proceeds
5. Prompt → Click "Cancel" → Transcription cancelled

## Related Files

- `src/main/java/org/whisperdog/recording/SilenceRemover.java`
- `src/main/java/org/whisperdog/recording/RecorderForm.java`

## Notes

Feature request for improved user experience.

## Work Log

### Session: 2025-12-31

**Diagnosis**:
- Need to warn users before transcribing recordings with high silence ratio
- Existing `SilenceRemover.removeSilence()` calculates metrics but doesn't expose them
- `AudioTranscriptionWorker.doInBackground()` is the integration point

**Approach**:
- Add pre-flight analysis capability to `SilenceRemover`
- Create modal dialog for user confirmation
- Integrate into transcription flow before silence removal

**Steps Taken**:
1. Created `SilenceAnalysisResult` class in `SilenceRemover.java` with:
   - `durationSeconds`, `silenceRatio`, `estimatedUsefulSeconds`
   - `exceedsWarningThreshold` flag (>600s AND >50% silence)
   - Formatted display methods
2. Added `analyzeForSilence()` method for pre-flight analysis
3. Added `detectSilenceQuiet()` helper (no console logging)
4. Created `LargeRecordingWarningDialog.java`:
   - Displays duration, silence %, estimated useful audio
   - "Transcribe Anyway" and "Cancel" buttons
5. Modified `AudioTranscriptionWorker.doInBackground()`:
   - Pre-flight analysis before silence removal
   - Shows dialog via `SwingUtilities.invokeAndWait()` if threshold exceeded
   - Tracks `cancelledByUser` for graceful abort handling
6. Updated `AudioTranscriptionWorker.done()`:
   - Handles user cancellation without error messages
   - Shows "Transcription cancelled" notification
7. Verified build: `mvn compile` → BUILD SUCCESS

**Outcome**:
- Feature implemented as specified
- Thresholds: 600s (10 min) duration AND 50% silence
- Status: resolved (pending manual testing)
