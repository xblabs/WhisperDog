# PRD: Error Handling & Retry Mechanism

## Problem Statement

WhisperDog's transcription pipeline currently has no error recovery capability. When OpenAI's Whisper API returns an error (HTTP 507, 413, empty response, or JSON parse failure), the application throws an exception and the user's recording is effectively lost. Users must manually re-record, which is frustrating for important audio captures and wastes time.

**Quantified Impact:**
- Users lose 100% of recordings on any transcription error
- HTTP 507 errors are transient (OpenAI overload) and would succeed on retry ~95% of the time
- HTTP 413 errors are preventable with pre-validation (checking compressed file size)
- Empty response errors require user judgment but currently provide no retry option
- Average re-recording time: 2-10 minutes depending on content length

**Real-world failure example (from logs):**
```
[11:14:29] Compressing audio file to MP3: record_20251216_023345.wav (size: 369.49 MB)
[11:14:51] Successfully compressed to MP3: 35.57 MB (10.4x compression ratio)
[11:15:31] ERROR: Transcription failed: HTTP 507 - exceeded request buffer limit
```
Result: 369MB recording lost. User had no option to retry despite the error being transient.

## Solution Overview

Implement a multi-tier error handling system that:

1. **Prevents predictable failures** through pre-submission validation
2. **Automatically recovers** from transient errors via exponential backoff retry
3. **Empowers users** with manual retry options for ambiguous errors
4. **Preserves recordings** throughout all error states until user explicitly discards

### Architecture Approach

```
Recording → Compression → [Pre-Validation] → API Call → [Error Handler] → Result
                              ↓                              ↓
                         Fail fast if              Categorize & Route:
                         file > 26MB               - Transient → Auto retry
                                                   - Permanent → Fail with message
                                                   - Ambiguous → User prompt
```

## Error Categories & Strategies

| Error Type | HTTP Code | Root Cause | Strategy | Max Retries |
|-----------|-----------|-----------|----------|-------------|
| Buffer Limit | 507 | OpenAI overload | Auto-retry (exponential backoff) | 3 |
| File Size | 413 | File > 26MB | Pre-validation fail-fast | 0 |
| Empty Response | 200 + empty | No speech in audio | User prompt with retry option | User-controlled |
| JSON Parse | N/A | Malformed response | Auto-retry | 3 |
| Network | Various | Connection issues | Auto-retry | 3 |
| Rate Limit | 429 | Too many requests | Auto-retry with longer delay | 5 |

## Functional Requirements

### FR-1: Pre-Submission Validation

**Requirement**: Before sending any file to OpenAI, validate that the compressed file size is under 26MB.

**Rationale**: OpenAI's Whisper API has a hard 25MB limit. Files exceeding this will always fail with HTTP 413. Checking beforehand prevents wasted API calls and provides immediate user feedback.

**Acceptance Criteria**:
- Check compressed file size after MP3 conversion completes
- If size > 26MB: Display error message without calling API
- Error message: "Compressed file is {size}MB, exceeds 26MB limit. Please record shorter audio or try a different compression setting."
- Log the validation failure with file details

### FR-2: Error Categorization Engine

**Requirement**: Implement an error classifier that categorizes transcription failures by type and determines the appropriate recovery strategy.

**Error Classification Logic**:
```java
public enum ErrorCategory {
    TRANSIENT,      // Auto-retry appropriate
    PERMANENT,      // Cannot recover, fail immediately
    USER_ACTION,    // Requires user decision
    UNKNOWN         // Log and treat as transient
}

public ErrorCategory categorize(TranscriptionException e) {
    if (e.getHttpStatus() == 507) return TRANSIENT;
    if (e.getHttpStatus() == 413) return PERMANENT;
    if (e.getHttpStatus() == 429) return TRANSIENT;
    if (e.isEmptyResponse()) return USER_ACTION;
    if (e.isJsonParseError()) return TRANSIENT;
    if (e.isNetworkError()) return TRANSIENT;
    return UNKNOWN;
}
```

### FR-3: Automatic Retry Handler

**Requirement**: Implement exponential backoff retry for transient errors.

**Retry Parameters**:
- Initial delay: 1 second
- Backoff multiplier: 2x
- Max attempts: 3 (delays: 1s, 2s, 4s)
- Total max wait: 7 seconds

**Behavior**:
- On transient error, schedule retry with exponential backoff
- Show progress indicator: "Retrying... (attempt 2/3)"
- Log each retry attempt with error details
- If all retries exhausted, escalate to user prompt

### FR-4: Manual Retry Flow

**Requirement**: For user-actionable errors (empty response), present a dialog allowing the user to retry or cancel.

**Dialog Content for Empty Response**:
```
Title: No Speech Detected

Your recording was processed but no speech was detected.
This can happen when:
- The audio contains only background noise
- The microphone sensitivity was too low
- The recording captured silence

Duration: 7 seconds | File size: 2.3 MB

[Retry Transcription]  [Cancel]
```

**User Controls**:
- Retry: Resubmit the same audio file to OpenAI
- Cancel: Discard the transcription attempt (keep audio file available)

### FR-5: Audio File Preservation

**Requirement**: The original recording and compressed MP3 must remain available throughout all error states.

**Preservation Rules**:
- Never delete audio files during error handling
- Keep files available until user explicitly discards or transcription succeeds
- Store file paths in retry state for recovery
- On app restart, check for pending retries and offer to resume

### FR-6: Error Context Logging

**Requirement**: Log comprehensive error information for debugging and user awareness.

**Log Format**:
```
[TIMESTAMP] ERROR: Transcription failed
  Category: TRANSIENT
  HTTP Status: 507
  Message: exceeded request buffer limit while retrying upstream
  File: record_20251216_023345.mp3 (35.57 MB)
  Retry: 1/3
  Next retry in: 2 seconds
```

**Persistence**:
- Write to application log file
- Display summarized history in UI error panel
- Include in any crash/error reports

## Non-Functional Requirements

### NFR-1: Performance
- Retry logic must not block the UI thread
- Pre-validation check completes in < 100ms
- Error categorization completes in < 10ms

### NFR-2: Reliability
- Retry state persists across app restarts
- No data loss during retry sequences
- Graceful degradation if persistence fails

### NFR-3: User Experience
- Clear, non-technical error messages
- Progress indicators during retry
- One-click access to retry/cancel actions
- Error history accessible but not intrusive

## Out of Scope

- Audio chunking/splitting for oversized files (future enhancement)
- Alternative transcription providers (future enhancement)
- Offline transcription fallback (future enhancement)
- Automatic quality adjustment based on error history (future enhancement)

## Technical Considerations

### Thread Safety
Retry logic will execute on background threads. Ensure:
- UI updates happen on EDT (Event Dispatch Thread)
- Shared state (retry count, file paths) is thread-safe
- Cancellation is immediate and clean

### State Management
Create a `RetryState` class to track:
- Original audio file path
- Compressed file path
- Current retry count
- Last error details
- Timestamp of last attempt

### Integration Points
- `OpenAIClient.java`: Add retry wrapper around transcription call
- `RecorderForm.java`: Add error dialog and retry UI
- `ConfigManager.java`: Store retry state for persistence
- `TranscriptionService.java`: Implement error categorization
