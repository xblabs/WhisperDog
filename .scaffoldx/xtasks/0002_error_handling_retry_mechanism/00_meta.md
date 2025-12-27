---
task_id: "0002"
task_name: "Error Handling & Retry Mechanism"
status: "todo"
priority: "critical"
created: "2025-12-30"
task_type: "executable"
tags: ["reliability", "error-handling", "retry", "openai-api", "user-experience"]
dependencies: ["BUG-003"]
---

# Task 0002: Error Handling & Retry Mechanism

## Summary

Implement intelligent error handling and retry mechanism for OpenAI transcription failures. Currently, when transcription fails, the recording is lost and the user must re-record. This task introduces error categorization, automatic retry for transient errors, manual retry options for user-actionable errors, and audio file preservation throughout all error states.

## Context

Real-world failures observed:
- **HTTP 507**: "exceeded request buffer limit while retrying upstream" - OpenAI overload (transient)
- **HTTP 413**: "Maximum content size limit exceeded" - File too large (permanent, requires pre-validation)
- **Empty Response**: `{"text":"","usage":{"type":"duration","seconds":7}}` - No speech detected (user action needed)
- **JSON Parse Error**: Malformed API response (transient)

Current behavior loses recordings on any error. Users have no recovery path and must re-record, which is unacceptable for important audio captures.

## Key Objectives

1. **Error Categorization**: Classify errors as transient vs permanent vs user-actionable
2. **Pre-submission Validation**: Check compressed file size before OpenAI call (blocks HTTP 413)
3. **Automatic Retry**: Exponential backoff for transient errors (HTTP 507, JSON errors)
4. **Manual Retry Flow**: User prompts for errors requiring investigation (empty response)
5. **Audio Preservation**: Keep recording available during all error states
6. **Error Context Logging**: Record error type, response, retry count for debugging

## Success Criteria

- HTTP 507 errors retry automatically (max 3 attempts, 1-2-4 second backoff)
- HTTP 413 errors caught by pre-validation with clear message before API call
- Empty response errors show "No speech detected. Retry?" prompt with user control
- User can cancel retry at any point (manual control over token spend)
- Retry state persisted (user closes app mid-retry, can resume)
- Error log shows retry history for debugging

## Dependencies

- **BUG-003** (file validation) must be completed first - pre-validation prevents HTTP 413

## Related Files

- Source specification: `C:\Users\henry\Documents\Obsidian\Main\02_INBOX\PROJECTS\whisperdog.md`
- OpenAI API client: `src/main/java/org/whisperdog/openai/`
