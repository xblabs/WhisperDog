---
issue_id: ISS_00005
title: Add line breaks around transcribed text in logs
type: enhancement
priority: low
status: open
created: 2025-12-30
tags: [ui, logs, readability]
alias: WD-0010
---

# ISS_00005: Add line breaks around transcribed text in logs

## Problem Description

Add blank lines before and after transcribed text in the log output. This makes it easier to visually distinguish transcription results from status messages and simplifies text selection for copying.

## Status

- **Current Status**: open
- **Priority**: low
- **Type**: enhancement
- **Created**: 2025-12-30

## Tags

- ui
- logs
- readability

## Problem Details

**Current log format**:
```
[11:42:15] Transcription complete
[11:42:15] The meeting discussed three main points. First, we need to update the API documentation...
[11:42:16] Pipeline execution started
```

**Issues**:
- Transcribed text blends with status messages
- Hard to select just the transcription text
- Difficult to scan logs for actual content

## Expected Behavior

**Improved log format**:
```
[11:42:15] Transcription complete

The meeting discussed three main points. First, we need to update the API documentation...

[11:42:16] Pipeline execution started
```

**Benefits**:
- Clear visual separation
- Easy triple-click selection (select paragraph)
- Scannable log structure

## Acceptance Criteria

- [ ] Blank line before transcribed text
- [ ] Blank line after transcribed text
- [ ] Only applies to transcription output, not status messages
- [ ] Works in both log panel and log file output

## Implementation Notes

**Simple fix**: Modify log append for transcription results

```java
// Before
appendLog("[" + timestamp + "] " + transcription);

// After
appendLog(""); // blank line before
appendLog(transcription); // transcription without timestamp prefix
appendLog(""); // blank line after
```

**Alternative**: Use visual separator

```java
appendLog("");
appendLog("─".repeat(40)); // visual separator
appendLog(transcription);
appendLog("─".repeat(40));
appendLog("");
```

## Test Cases

1. Complete transcription → Verify blank lines in log panel
2. Multiple transcriptions → Verify each has surrounding blank lines
3. Export log → Verify blank lines preserved in file

## Related Files

- `src/main/java/org/whisperdog/LogsForm.java`
- `src/main/java/org/whisperdog/recording/RecorderForm.java`

## Notes

Quality of life improvement for readability.
