---
issue_id: ISS_00002
title: Post-processing dropdown resets on hotkey activation
type: bug
priority: high
status: resolved
created: 2025-12-30
tags: [state-management, hotkeys, ui]
alias: BUG-001
started: 2025-12-31T03:29:15.958Z
id: ISS_00002
updated: 2025-12-31T03:29:15.964Z
---

# ISS_00002: Post-processing dropdown resets on hotkey activation

## Problem Description

When recording is activated via global hotkeys while post-processing is enabled, the pipeline dropdown selection resets to the default option. This causes users to accidentally run the wrong pipeline.

## Status

- **Current Status**: open
- **Priority**: high
- **Type**: bug
- **Created**: 2025-12-30

## Tags

- state-management
- hotkeys
- ui

## Problem Details

**Trigger**: Recording activated via hotkeys while post-processing is active

**Current Behavior**:
1. User selects specific pipeline from dropdown
2. User triggers recording via hotkey
3. Dropdown resets to default/first option
4. When recording stops, wrong pipeline executes

**Workaround**: Uncheck "enable post-processing" before using hotkeys, re-enable after.

## Expected Behavior

- Hotkey activation should NOT affect dropdown selection
- Selected pipeline remains selected regardless of how recording is triggered
- State should be preserved across all recording triggers (button click, hotkey, API)

## Root Cause Analysis

Likely causes:
1. Hotkey event listener recreates/resets dropdown state
2. Focus change during hotkey triggers component reset
3. Recording start handler inadvertently resets UI state

**Investigation path**:
- Check `GlobalHotkeyListener` for UI interactions
- Check `RecorderForm.onRecordingStart()` for state modifications
- Check if dropdown has `ActionListener` that resets on focus change

## Acceptance Criteria

- [ ] Hotkey activation preserves dropdown selection
- [ ] Can reproduce issue with current code
- [ ] After fix, dropdown maintains selection through multiple hotkey triggers
- [ ] Works in both "post-processing visible" and "post-processing hidden" modes

## Implementation Notes

**Probable fix locations**:
- `src/main/java/org/whisperdog/GlobalHotkeyListener.java`
- `src/main/java/org/whisperdog/recording/RecorderForm.java`

**Approach**:
```java
// Option 1: Preserve state before hotkey action
String selectedPipeline = pipelineDropdown.getSelectedItem();
// ... hotkey action ...
pipelineDropdown.setSelectedItem(selectedPipeline);

// Option 2: Prevent reset in recording start handler
// Don't re-initialize dropdown in onRecordingStart()
```

## Test Cases

1. Select non-default pipeline → Trigger hotkey → Verify selection unchanged
2. Select pipeline → Multiple rapid hotkey triggers → Verify selection stable
3. Select pipeline → Close/reopen app → Use hotkey → Verify saved selection loads

## Related Files

- `src/main/java/org/whisperdog/GlobalHotkeyListener.java`
- `src/main/java/org/whisperdog/recording/RecorderForm.java`
- `src/main/java/org/whisperdog/ConfigManager.java`

## Notes

Issue created from user feedback.