# Checkpoint: CP_2026_01_25_002 - Word-Level Attribution
**Date**: 2026-01-25 12:54
**Type**: save_point
**Resumed From**: CP_2026_01_25_001
**Status**: Word-level timestamp attribution implemented

---

## Summary
Implemented accurate source attribution for dual-source (mic + system) recordings using OpenAI's word-level timestamps. Previously, attribution used proportional word distribution which was inaccurate. Now each word is mapped to its correct source based on actual timing.

---

## Changes This Session

### 1. Word-Level Timestamp Attribution (Major Feature)

**Problem**: Source attribution labels ([User]/[System]) were "mangled" - words assigned to wrong sources.

**Root Cause**: `labelTranscript()` distributed words proportionally across timeline assuming uniform speech pace.

**Solution**:
- Created `TranscriptionResult.java` - holds both text and timestamped words
- Added `transcribeWithTimestamps()` to `OpenAITranscribeClient.java`
  - Requests `response_format: "verbose_json"`
  - Requests `timestamp_granularities: ["word"]`
  - Parses word array with start/end timestamps (seconds → milliseconds)
- Added `transcribeWithTimestampsAndRetry()` to `RecorderForm.java`
- Flow now uses `labelTranscriptWithTimestamps()` for accurate per-word attribution

### 2. Dual-Source Speech Detection Fix

**Problem**: "Very little speech detected" warning triggered when system audio had content but mic only had noise.

**Solution**:
- Now checks BOTH audio tracks (not just mic as primary)
- Uses lower threshold (50%) for system audio analysis
- Takes MAXIMUM useful content from either source
- Console shows: `"Audio content: mic=Xs, system=Ys"`

---

## Files Modified

| File | Changes |
|------|---------|
| `TranscriptionResult.java` | NEW - holds text + List<TimestampedWord> |
| `OpenAITranscribeClient.java` | Added `transcribeWithTimestamps()` method (lines 333-501) |
| `RecorderForm.java` | Added `transcribeWithTimestampsAndRetry()`, updated transcription flow, fixed speech detection |

---

## Build State
- `mvn compile` ✅ successful
- No errors

---

## Discussed But Not Implemented

### Cross-Platform Default Audio Device Detection
- User asked about auto-detecting Windows default audio output
- Discussed cross-platform approach (Windows/Linux/Mac)
- Would use platform-specific commands: PowerShell / pactl / system_profiler
- Deferred for future work

---

## Next Steps
1. Test word-level attribution with real dual-source recording
2. Consider cross-platform default device detection
3. System audio loopback device selection improvements
