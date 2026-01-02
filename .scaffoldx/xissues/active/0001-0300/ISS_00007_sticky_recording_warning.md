---
issue_id: ISS_00007
title: Sticky Recording Warning for Long Recordings
type: enhancement
priority: low
status: resolved
created: 2025-12-30
resolved: 2026-01-01
tags: [recording, ui, user-experience]
alias: WD-0007
---

# ISS_00007: Sticky Recording Warning for Long Recordings

## Problem Description

Show a persistent visual warning indicator when recording exceeds a configurable duration threshold (e.g., > 2 minutes). This reminds users they're still recording and helps prevent accidentally long recordings.

## Status

- **Current Status**: resolved ✅
- **Priority**: low
- **Type**: enhancement
- **Created**: 2025-12-30
- **Resolved**: 2026-01-01

## Tags

- recording
- ui
- user-experience

## Problem Details

Users sometimes forget a recording is active, especially when:
- Recording started via hotkey
- Application is minimized or in background
- User is focused on other work

Long recordings lead to:
- Large file sizes
- Longer transcription times
- Higher API costs
- Often contain unwanted content (meetings, calls, etc.)

## Expected Behavior

**New Setting**: "Warn after recording duration" (default: 2 minutes)

**Warning Behavior**:
1. Recording exceeds threshold duration
2. Visual indicator appears (pulsing border, color change, or icon)
3. Optional: System notification
4. Warning persists until recording stops

**Visual Options** (pick one):
- Pulsing red border around record button
- Status bar background changes to yellow/orange
- Animated warning icon appears
- Tray icon changes color

## Acceptance Criteria

- [ ] Setting added to ConfigManager
- [ ] UI slider in Settings → Audio
- [ ] Visual warning when threshold exceeded
- [ ] Tray icon indication for minimized state

## Implementation Notes

- Add `recordingWarningDuration` to ConfigManager (default: 120 seconds)
- Add slider in Settings → Audio (range: 30s - 10min, or disable with 0)
- Timer checks duration during recording
- Warning should be visible even if app minimized (tray icon change)

## UI Location

Settings → Audio → "Warn after recording: [slider] 2 min"

## Test Cases

1. Record for 30s → No warning (below threshold)
2. Record for 2:01 → Warning appears
3. Stop recording → Warning disappears
4. Set threshold to 0 → Feature disabled, no warning ever
5. App minimized → Tray icon indicates warning state

## Related Files

- `src/main/java/org/whisperdog/ConfigManager.java`
- `src/main/java/org/whisperdog/settings/SettingsForm.java`
- `src/main/java/org/whisperdog/recording/RecorderForm.java`
- `src/main/java/org/whisperdog/TrayIconManager.java`

## Notes

Feature request for improved user experience.

---

## Work Log

### 2026-01-01 - Session 1

**Diagnosis**: N/A - New feature implementation

**Approach**: Implement configurable recording duration warning with:
1. ConfigManager setting (recordingWarningDuration)
2. Settings slider (0-10 min range, 0 = disabled)
3. Timer-based duration check in RecorderForm
4. Visual indicators (pulsing orange status circle, button text with elapsed time)
5. Tray icon status update for minimized app

**Steps Taken**:
1. Added `getRecordingWarningDuration()` / `setRecordingWarningDuration()` to ConfigManager (default: 120s)
2. Added slider to SettingsForm in Audio section with `formatDurationLabel()` helper
3. Added Timer fields and `startRecordingWarningTimer()` / `stopRecordingWarningTimer()` methods to RecorderForm
4. Modified status circle paint logic to pulse orange when warning active
5. Button text shows elapsed time with warning icon when threshold exceeded
6. Added `setRecordingWarning()` to TrayIconManager with tooltip update
7. Console log shows warning message when triggered
8. Build successful with `mvn compile`

**Outcome**: Feature implemented successfully. All acceptance criteria met:
- ✅ Setting added to ConfigManager
- ✅ UI slider in Settings → Audio
- ✅ Visual warning when threshold exceeded (pulsing circle + button text)
- ✅ Tray icon indication for minimized state (status text update)

**Code Locations**:
- `ConfigManager.java:801-822`
- `SettingsForm.java:48, 489-537, 942-952`
- `RecorderForm.java:89-93, 110-148, 1056-1158`
- `TrayIconManager.java:19, 23, 79-115`
