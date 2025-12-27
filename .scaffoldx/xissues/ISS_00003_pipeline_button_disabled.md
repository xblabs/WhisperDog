# ISS_00003: Pipeline button disabled after recording stops

**Alias**: BUG-002
**Status**: Open
**Priority**: High
**Severity**: Major
**Created**: 2025-12-30

## Summary

After recording stops, the pipeline execution button becomes disabled and cannot be clicked. The only workaround is to toggle the dropdown selection (select different option, then switch back), which re-enables the button.

## Problem

**Trigger**: Recording stopped

**Current Behavior**:
1. User records audio
2. Recording stops (button click or automatic)
3. Pipeline button becomes disabled (grayed out, non-clickable)
4. User cannot manually trigger pipeline execution

**Workaround**: Toggle dropdown selection:
1. Select a different pipeline
2. Select original pipeline back
3. Button re-enables

## Expected Behavior

- Pipeline button should be ENABLED after recording stops
- Button state should follow: "enabled when transcription available"
- No workaround should be required

## Root Cause Analysis

Likely causes:
1. `recordingStop` event doesn't trigger button enable logic
2. Button enable condition checks stale state variable
3. Enable logic only runs on dropdown change, not recording state change

**Investigation path**:
- Find button enable/disable logic
- Check what triggers enable state
- Verify `onRecordingStop()` updates relevant state variables

## Acceptance Criteria

- [ ] Button enabled immediately after recording stops
- [ ] No dropdown toggle workaround needed
- [ ] Button enabled when valid transcription is available
- [ ] Button disabled only when: no transcription, processing in progress

## Implementation Notes

**Probable fix location**: `RecorderForm.onRecordingStop()` or button state management

```java
// In onRecordingStop() - ensure button state is updated
private void onRecordingStop() {
    // ... existing logic ...

    // Fix: Explicitly enable pipeline button when transcription available
    if (hasTranscription()) {
        pipelineButton.setEnabled(true);
    }
}

// Or fix the state check
private void updatePipelineButtonState() {
    boolean enabled = hasTranscription()
        && !isProcessing()
        && pipelineDropdown.getSelectedItem() != null;
    pipelineButton.setEnabled(enabled);
}
```

## Test Cases

1. Record → Stop → Verify button enabled
2. Record → Stop → Click button → Verify pipeline executes
3. Multiple record/stop cycles → Verify button always enabled after stop
4. Record → Cancel → Verify button state appropriate

## Related Files

- `src/main/java/org/whisperdog/recording/RecorderForm.java`
- `src/main/java/org/whisperdog/ui/PostProcessingPanel.java` (if exists)
