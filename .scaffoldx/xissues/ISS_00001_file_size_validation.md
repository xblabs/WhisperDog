# ISS_00001: Validate compressed file size before OpenAI submission

**Alias**: BUG-003
**Status**: Open
**Priority**: High
**Severity**: Major
**Created**: 2025-12-30

## Summary

Check compressed file size before sending to OpenAI API to prevent HTTP 413 (file too large) and HTTP 507 (buffer limit) errors. Currently, files exceeding 25MB are still submitted, resulting in failed transcriptions and lost processing time.

## Problem

**Trigger**: Recording files > 25MB after compression

**Current Behavior**:
1. Compress WAV to MP3 via ffmpeg
2. If compressed result also > 25MB, still attempts submission to OpenAI
3. OpenAI API returns error:
   - `HTTP 413: Maximum content size limit exceeded`
   - `HTTP 507: exceeded request buffer limit`

**Real-world Example**:
```
[11:14:29] Compressing audio file to MP3: record_20251216_023345.wav (size: 369.49 MB)
[11:14:51] Successfully compressed to MP3: 35.57 MB (10.4x compression ratio)
[11:15:31] ERROR: Transcription failed: HTTP 507 - exceeded request buffer limit
```

Result: 35.57MB file sent despite exceeding the 25MB limit.

## Expected Behavior

After compression completes:
1. Check if compressed file size > 26MB (using 26MB as safety threshold)
2. If exceeds: Display clear error message WITHOUT calling API
3. Error message: "Compressed file is {size}MB, exceeds 26MB limit. Please record shorter audio."
4. Log the validation failure with file details

## Root Cause

Missing validation step between compression and API submission.

## Acceptance Criteria

- [ ] Validation catches oversized compressed files before API call
- [ ] Error message clearly states the file size and limit
- [ ] No API call is made for files exceeding the limit
- [ ] Validation logged for debugging

## Implementation Notes

**Location**: Add check after compression in transcription pipeline

```java
// After compression completes
File compressedFile = /* result of compression */;
long fileSizeBytes = compressedFile.length();
double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);

if (fileSizeMB > 26.0) {
    String errorMsg = String.format(
        "Compressed file is %.2f MB, exceeds 26MB limit. " +
        "Please record shorter audio.", fileSizeMB);
    throw new ValidationException(errorMsg);
}
```

## Dependencies

- **Blocks**: Task 0002 (Error Handling) depends on this fix

## Related Files

- `src/main/java/org/whisperdog/openai/OpenAIClient.java`
- `src/main/java/org/whisperdog/audio/AudioCompressor.java`
