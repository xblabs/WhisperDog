---
id: ISS_00011
title: Very little speech warning only works when silence removal is enabled
status: resolved
priority: medium
created: 2026-01-18
category: bug
affects:
  - src/main/java/org/whisperdog/recording/RecorderForm.java
related:
  - ISS_00006
---

# ISS_00011: Very little speech warning only works when silence removal is enabled

## Problem

The "very little speech detected" warning dialog (implemented in ISS_00006) only appeared when silence removal was enabled. This was because the minimum speech check was inside the `if (configManager.isSilenceRemovalEnabled())` block.

## Root Cause

The minimum speech duration check (lines 1683-1717) was nested inside the silence removal enabled block (starting at line 1642). This meant:
- If silence removal was enabled → speech check ran
- If silence removal was disabled → speech check was skipped entirely

Both button and key combo activation used the same code path (`toggleRecording()`), so the issue wasn't about activation method but about the silence removal setting.

## Solution

Refactored `AudioTranscriptionWorker.doInBackground()` to:
1. Always run audio analysis upfront (for both speech detection and silence analysis)
2. Large recording warning still only shows when silence removal is enabled
3. Minimum speech check now runs **regardless** of silence removal setting
4. Single analysis pass is reused for both checks (no duplicate analysis)

## Files Modified

- `src/main/java/org/whisperdog/recording/RecorderForm.java`

## Testing

1. Disable silence removal in settings
2. Start recording with minimal/no speech
3. Stop recording
4. Verify "Very Little Speech Detected" warning appears
5. Enable silence removal and repeat - should still work