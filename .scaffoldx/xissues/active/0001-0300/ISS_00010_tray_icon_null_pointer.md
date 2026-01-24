---
id: ISS_00010
title: NullPointerException when updating tray icon during recording
status: resolved
priority: high
created: 2026-01-18
category: bug
affects:
  - src/main/java/org/whisperdog/TrayIconManager.java
  - src/main/java/org/whisperdog/recording/RecorderForm.java
---

# ISS_00010: NullPointerException when updating tray icon during recording

## Problem

When WhisperDog recording is activated (via key sequence or button), a NullPointerException occurs when trying to update the tray icon. The error happens because `this.systemTray` is null when `setTrayImage()` is called.

## Error Log

```
2026-01-18 01:03:37 ERROR org.whisperdog.TrayIconManager - Error setting tray icon image
java.lang.NullPointerException: Cannot invoke "dorkbox.systemTray.SystemTray.setImage(java.awt.Image)" because "this.systemTray" is null
    at org.whisperdog.TrayIconManager.setTrayImage(TrayIconManager.java:122)
    at org.whisperdog.TrayIconManager.updateTrayIcon(TrayIconManager.java:109)
    at org.whisperdog.TrayIconManager.setProcessing(TrayIconManager.java:76)
    at org.whisperdog.recording.RecorderForm.setProcessingState(RecorderForm.java:84)
```

## Root Cause

The `TrayIconManager` class has a `systemTray` field that can be null if:
1. System tray is not supported on the platform
2. Tray initialization failed during startup
3. Tray was disposed or never initialized

The following methods do not guard against null `systemTray`:
- `setTrayImage()` at line 122
- `updateTrayIcon()` at lines 109, 113
- `setProcessing()` at line 76
- `updateTrayMenu()` at line 70

## Impact

- Error logged twice per transcription completion
- Tray icon not updated correctly during processing states
- Potential follow-on issues if tray operations are expected to succeed

## Solution

Add null checks before all `systemTray` operations in `TrayIconManager.java`:

```java
private void setTrayImage(Image image) {
    if (systemTray == null) {
        return; // Silently skip if tray not available
    }
    systemTray.setImage(image);
}
```

Apply similar guards to:
- `updateTrayIcon()`
- `setProcessing()`
- `updateTrayMenu()`
- Any other method that accesses `systemTray`

## Files to Modify

- `src/main/java/org/whisperdog/TrayIconManager.java`

## Testing

1. Run WhisperDog on a system where tray initialization might fail
2. Activate recording via key sequence
3. Complete a transcription
4. Verify no NullPointerException in logs
5. Verify graceful degradation when tray is unavailable