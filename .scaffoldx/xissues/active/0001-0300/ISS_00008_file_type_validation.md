---
id: ISS_00008
title: Validate file type before OpenAI submission
status: in-progress
priority: medium
type: bug
created: 2026-01-02T09:36:49.465Z
updated: 2026-01-02T23:32:26.876Z
tags: [validation, openai-api, file-type, drag-drop]
relates_to: []
started: 2026-01-02T23:32:26.868Z
---

# Validate file type before OpenAI submission

## Problem Description

## Context
When a non-audio file is pasted/dropped into the paste area, the application sends it to OpenAI Whisper API without validation, resulting in cryptic HTTP 400 errors. Users accidentally pasting images, PDFs, or other files see confusing error messages instead of helpful guidance.

## Evidence

### Error Log (actual output)
```
2026-01-02 03:20:11 ERROR org.whispercat.recording.clients.OpenAITranscribeClient - OpenAI API returned status code: 400. Response: {
    "error": {
        "message": "Could not parse multipart form",
        "type": "invalid_request_error",
        "param": null,
        "code": null
    }
}
java.io.IOException: Error from OpenAI API (HTTP 400): Could not parse multipart form
    at org.whispercat.recording.clients.OpenAITranscribeClient.transcribe(OpenAITranscribeClient.java:239)
    at org.whispercat.recording.RecorderForm$AudioTranscriptionWorker.doInBackground(RecorderForm.java:1210)
```

### Affected Files
- src/main/java/org/whisperdog/recording/clients/OpenAITranscribeClient.java:239 (throws exception)
- src/main/java/org/whisperdog/recording/RecorderForm.java:1210 (paste/drop handler)

## Impact
Users who accidentally paste non-audio files (common with clipboard managers) see cryptic API errors instead of actionable feedback. This creates confusion and support burden. Affects all users who use paste/drop functionality.

## Goals
- Validate file extension before API submission
- Show clear error dialog for unsupported file types
- No API call made for invalid files
- List supported formats in error message

## Acceptance Criteria
- [x] File extension checked against supported list (flac, m4a, mp3, mp4, mpeg, mpga, oga, ogg, wav, webm)
- [x] Unsupported files show dialog: 'Unsupported file type: {ext}. Supported: wav, mp3, m4a, flac, ogg, webm, mp4'
- [x] No HTTP request sent for invalid files
- [x] Validation logged for debugging

## Status

- **Current Status**: implemented
- **Priority**: medium
- **Type**: bug
- **Created**: 2026-01-02

## Tags

- validation
- openai-api
- file-type
- drag-drop

## Notes

Issue created via x-issue-create.

## Work Log

### Session: 2026-01-02

**Diagnosis**:
- User pasted non-audio file, API returned HTTP 400 "Could not parse multipart form"
- No file type validation before sending to OpenAI Whisper API
- Error message was cryptic, not user-friendly

**Approach**:
- Add file type validation to existing TranscriptionValidator class (follows ISS_00001 pattern)
- Validate before any compression/API calls in transcribe() method
- Use TranscriptionException with HTTP 400 status for consistent error handling

**Steps Taken**:
1. Read TranscriptionValidator.java - found existing pattern for validateFileSize()
2. Added SUPPORTED_EXTENSIONS constant with OpenAI Whisper supported formats
3. Added validateFileType() method to check file extension before API call
4. Added isSupportedFileType() helper for non-throwing validation
5. Added getSupportedFormatsDisplay() for user-friendly format list
6. Modified OpenAITranscribeClient.transcribe() to call validateFileType() first
7. Ran `mvn compile` - build successful

**Outcome**:
- TranscriptionValidator.java: Added file type validation methods
- OpenAITranscribeClient.java:188-196: Added validation call at start of transcribe()
- Unsupported files now fail fast with clear message listing supported formats
- Status: implemented (needs QA)