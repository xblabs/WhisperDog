# Error Handling & Retry Mechanism - Execution Checklist

## Phase 1: Error Classification Infrastructure
- [ ] Create error package structure with ErrorCategory enum and TranscriptionException class
- [ ] Implement error classification logic for HTTP status codes and response patterns
- [ ] Add user-friendly message generation for each error category

## Phase 2: Pre-Submission Validation
- [ ] Create TranscriptionValidator with file size check (26MB limit)
- [ ] Integrate validation into OpenAIClient before API call
- [ ] Add validation failure logging with file details

## Phase 3: Automatic Retry Handler
- [ ] Create RetryState class for tracking attempt count and backoff timing
- [ ] Implement RetryHandler with exponential backoff (1-2-4 second delays)
- [ ] Add SwingWorker-based async execution with progress callbacks

## Phase 4: User Interface Integration
- [ ] Create ErrorDialog with empty response prompt (retry/cancel options)
- [ ] Add retries-exhausted dialog with manual retry option
- [ ] Update RecorderForm to use RetryHandler for transcription

## Phase 5: State Persistence
- [ ] Add retry state save/load methods to ConfigManager
- [ ] Implement pending retry check on application startup
- [ ] Add cleanup of pending retry on success or user cancel

## Phase 6: Logging Enhancement
- [ ] Create TranscriptionLogger with structured error format
- [ ] Log all retry attempts with timing and error details
- [ ] Add success logging with attempt count and result size

## Phase 7: Testing & Verification
- [ ] Test HTTP 507 triggers automatic retry with backoff
- [ ] Test HTTP 413 caught by pre-validation with clear message
- [ ] Test empty response shows user dialog with retry option
- [ ] Verify retry state persists across app restart
- [ ] Verify UI remains responsive during retry sequence

---

**Note**: Each phase contains detailed code examples in `02_implementation_plan.md`
