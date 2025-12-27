# Implementation Plan: Error Handling & Retry Mechanism

## Phase 1: Error Classification Infrastructure

### 1.1 Create Error Category Enum

**File**: `src/main/java/org/whisperdog/error/ErrorCategory.java` (new)

```java
package org.whisperdog.error;

public enum ErrorCategory {
    /**
     * Transient errors that may succeed on retry.
     * Examples: HTTP 507 (buffer limit), 429 (rate limit), network timeouts
     */
    TRANSIENT,

    /**
     * Permanent errors that cannot be recovered through retry.
     * Examples: HTTP 413 (file too large), 401 (invalid API key)
     */
    PERMANENT,

    /**
     * Errors requiring user decision before proceeding.
     * Examples: Empty transcription response, ambiguous audio quality
     */
    USER_ACTION,

    /**
     * Unknown errors - treat as transient but log for investigation.
     */
    UNKNOWN
}
```

### 1.2 Create Transcription Exception

**File**: `src/main/java/org/whisperdog/error/TranscriptionException.java` (new)

```java
package org.whisperdog.error;

public class TranscriptionException extends Exception {
    private final int httpStatus;
    private final String responseBody;
    private final ErrorCategory category;
    private final boolean emptyResponse;
    private final boolean jsonParseError;

    public TranscriptionException(String message, int httpStatus, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.emptyResponse = isEmptyTranscription(responseBody);
        this.jsonParseError = false;
        this.category = categorize();
    }

    public TranscriptionException(String message, Throwable cause, boolean isJsonError) {
        super(message, cause);
        this.httpStatus = -1;
        this.responseBody = null;
        this.emptyResponse = false;
        this.jsonParseError = isJsonError;
        this.category = categorize();
    }

    private boolean isEmptyTranscription(String body) {
        if (body == null) return false;
        // Check for {"text":"","usage":...} pattern
        return body.contains("\"text\":\"\"") || body.contains("\"text\": \"\"");
    }

    private ErrorCategory categorize() {
        if (httpStatus == 507) return ErrorCategory.TRANSIENT;
        if (httpStatus == 413) return ErrorCategory.PERMANENT;
        if (httpStatus == 429) return ErrorCategory.TRANSIENT;
        if (httpStatus == 401 || httpStatus == 403) return ErrorCategory.PERMANENT;
        if (emptyResponse) return ErrorCategory.USER_ACTION;
        if (jsonParseError) return ErrorCategory.TRANSIENT;
        if (httpStatus >= 500) return ErrorCategory.TRANSIENT;
        return ErrorCategory.UNKNOWN;
    }

    // Getters
    public int getHttpStatus() { return httpStatus; }
    public String getResponseBody() { return responseBody; }
    public ErrorCategory getCategory() { return category; }
    public boolean isEmptyResponse() { return emptyResponse; }
    public boolean isJsonParseError() { return jsonParseError; }
    public boolean isRetryable() {
        return category == ErrorCategory.TRANSIENT || category == ErrorCategory.UNKNOWN;
    }
}
```

### 1.3 Create Error Classifier Service

**File**: `src/main/java/org/whisperdog/error/ErrorClassifier.java` (new)

```java
package org.whisperdog.error;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ErrorClassifier {

    public static TranscriptionException classify(Exception e, int httpStatus, String responseBody) {
        if (e instanceof SocketTimeoutException || e instanceof UnknownHostException) {
            return new TranscriptionException("Network error: " + e.getMessage(), e, false);
        }

        if (e instanceof com.fasterxml.jackson.core.JsonParseException) {
            return new TranscriptionException("Invalid API response format", e, true);
        }

        return new TranscriptionException(e.getMessage(), httpStatus, responseBody);
    }

    public static String getUserFriendlyMessage(TranscriptionException e) {
        switch (e.getCategory()) {
            case TRANSIENT:
                if (e.getHttpStatus() == 507) {
                    return "OpenAI servers are temporarily overloaded. Retrying automatically...";
                }
                if (e.getHttpStatus() == 429) {
                    return "Too many requests. Waiting before retry...";
                }
                return "Temporary error occurred. Retrying...";

            case PERMANENT:
                if (e.getHttpStatus() == 413) {
                    return "File is too large for transcription (max 25MB after compression).";
                }
                if (e.getHttpStatus() == 401) {
                    return "Invalid API key. Please check your OpenAI settings.";
                }
                return "Transcription failed: " + e.getMessage();

            case USER_ACTION:
                if (e.isEmptyResponse()) {
                    return "No speech was detected in your recording.";
                }
                return "Please review and decide how to proceed.";

            default:
                return "An unexpected error occurred: " + e.getMessage();
        }
    }
}
```

## Phase 2: Pre-Submission Validation

### 2.1 Add File Size Validator

**File**: `src/main/java/org/whisperdog/validation/TranscriptionValidator.java` (new)

```java
package org.whisperdog.validation;

import org.whisperdog.error.TranscriptionException;
import org.whisperdog.error.ErrorCategory;

import java.io.File;

public class TranscriptionValidator {

    // OpenAI Whisper API limit is 25MB, use 26MB as safety threshold
    private static final long MAX_FILE_SIZE_BYTES = 26 * 1024 * 1024; // 26 MB

    /**
     * Validates that the compressed audio file is within OpenAI's size limits.
     *
     * @param compressedFile The MP3 file to validate
     * @throws TranscriptionException if validation fails
     */
    public static void validateFileSize(File compressedFile) throws TranscriptionException {
        if (!compressedFile.exists()) {
            throw new TranscriptionException(
                "Compressed file not found: " + compressedFile.getName(),
                -1, null
            );
        }

        long fileSizeBytes = compressedFile.length();
        double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);

        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            String message = String.format(
                "Compressed file is %.2f MB, exceeds 26MB limit. " +
                "Please record shorter audio or adjust compression settings.",
                fileSizeMB
            );
            throw new TranscriptionException(message, 413, null);
        }
    }

    /**
     * Returns the file size in a human-readable format.
     */
    public static String getFileSizeDisplay(File file) {
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
```

### 2.2 Integrate Validation into Transcription Flow

**File**: `src/main/java/org/whisperdog/openai/OpenAIClient.java` (modify)

Location: Before the HTTP request to OpenAI API

```java
// Add import
import org.whisperdog.validation.TranscriptionValidator;

// In transcribe() method, BEFORE making API call:
public String transcribe(File audioFile) throws TranscriptionException {
    // === NEW: Pre-submission validation ===
    TranscriptionValidator.validateFileSize(audioFile);

    // Existing transcription logic follows...
    // ...
}
```

## Phase 3: Retry Infrastructure

### 3.1 Create Retry State Class

**File**: `src/main/java/org/whisperdog/retry/RetryState.java` (new)

```java
package org.whisperdog.retry;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;

public class RetryState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String originalAudioPath;
    private final String compressedAudioPath;
    private int attemptCount;
    private int maxAttempts;
    private String lastErrorMessage;
    private int lastHttpStatus;
    private Instant lastAttemptTime;
    private Instant nextRetryTime;
    private boolean cancelled;

    public RetryState(File originalAudio, File compressedAudio) {
        this.originalAudioPath = originalAudio.getAbsolutePath();
        this.compressedAudioPath = compressedAudio.getAbsolutePath();
        this.attemptCount = 0;
        this.maxAttempts = 3;
        this.cancelled = false;
    }

    public void recordAttempt(String errorMessage, int httpStatus) {
        this.attemptCount++;
        this.lastErrorMessage = errorMessage;
        this.lastHttpStatus = httpStatus;
        this.lastAttemptTime = Instant.now();
        this.nextRetryTime = calculateNextRetryTime();
    }

    private Instant calculateNextRetryTime() {
        // Exponential backoff: 1s, 2s, 4s
        long delaySeconds = (long) Math.pow(2, attemptCount - 1);
        return Instant.now().plusSeconds(delaySeconds);
    }

    public boolean canRetry() {
        return !cancelled && attemptCount < maxAttempts;
    }

    public long getDelayMillis() {
        if (nextRetryTime == null) return 0;
        long delay = nextRetryTime.toEpochMilli() - System.currentTimeMillis();
        return Math.max(0, delay);
    }

    public void cancel() {
        this.cancelled = true;
    }

    // Getters
    public File getOriginalAudio() { return new File(originalAudioPath); }
    public File getCompressedAudio() { return new File(compressedAudioPath); }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public boolean isCancelled() { return cancelled; }

    public String getProgressText() {
        return String.format("Attempt %d/%d", attemptCount + 1, maxAttempts);
    }
}
```

### 3.2 Create Retry Handler

**File**: `src/main/java/org/whisperdog/retry/RetryHandler.java` (new)

```java
package org.whisperdog.retry;

import org.whisperdog.error.TranscriptionException;
import org.whisperdog.error.ErrorCategory;
import org.whisperdog.openai.OpenAIClient;

import javax.swing.SwingWorker;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RetryHandler {

    private final OpenAIClient openAIClient;
    private RetryState currentState;

    public RetryHandler(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    /**
     * Attempts transcription with automatic retry for transient errors.
     *
     * @param audioFile The compressed audio file to transcribe
     * @param onProgress Callback for progress updates (runs on EDT)
     * @param onSuccess Callback when transcription succeeds
     * @param onFailure Callback when all retries exhausted or permanent error
     * @param onUserAction Callback when user decision needed (empty response)
     */
    public void transcribeWithRetry(
            File originalFile,
            File compressedFile,
            Consumer<String> onProgress,
            Consumer<String> onSuccess,
            Consumer<TranscriptionException> onFailure,
            Consumer<RetryState> onUserAction
    ) {
        currentState = new RetryState(originalFile, compressedFile);
        executeWithRetry(onProgress, onSuccess, onFailure, onUserAction);
    }

    private void executeWithRetry(
            Consumer<String> onProgress,
            Consumer<String> onSuccess,
            Consumer<TranscriptionException> onFailure,
            Consumer<RetryState> onUserAction
    ) {
        new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() throws Exception {
                while (currentState.canRetry()) {
                    try {
                        publish("Transcribing... " + currentState.getProgressText());
                        return openAIClient.transcribe(currentState.getCompressedAudio());
                    } catch (TranscriptionException e) {
                        currentState.recordAttempt(e.getMessage(), e.getHttpStatus());

                        if (e.getCategory() == ErrorCategory.PERMANENT) {
                            throw e; // Don't retry permanent errors
                        }

                        if (e.getCategory() == ErrorCategory.USER_ACTION) {
                            // Signal UI to prompt user
                            javax.swing.SwingUtilities.invokeLater(() ->
                                onUserAction.accept(currentState));
                            return null; // Exit worker, user will decide
                        }

                        if (!currentState.canRetry()) {
                            throw e; // Max retries exhausted
                        }

                        // Wait before retry
                        long delay = currentState.getDelayMillis();
                        publish(String.format("Retry in %d seconds... (%s)",
                            delay / 1000, currentState.getProgressText()));
                        Thread.sleep(delay);
                    }
                }
                throw new TranscriptionException("Max retries exhausted", -1, null);
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    onProgress.accept(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    if (result != null) {
                        onSuccess.accept(result);
                    }
                } catch (Exception e) {
                    if (e.getCause() instanceof TranscriptionException) {
                        onFailure.accept((TranscriptionException) e.getCause());
                    } else {
                        onFailure.accept(new TranscriptionException(
                            e.getMessage(), -1, null));
                    }
                }
            }
        }.execute();
    }

    /**
     * Manually retry after user confirmation (for USER_ACTION errors).
     */
    public void manualRetry(
            Consumer<String> onProgress,
            Consumer<String> onSuccess,
            Consumer<TranscriptionException> onFailure,
            Consumer<RetryState> onUserAction
    ) {
        if (currentState != null && !currentState.isCancelled()) {
            // Reset attempt count for manual retry
            currentState = new RetryState(
                currentState.getOriginalAudio(),
                currentState.getCompressedAudio()
            );
            executeWithRetry(onProgress, onSuccess, onFailure, onUserAction);
        }
    }

    /**
     * Cancel any pending retry operations.
     */
    public void cancel() {
        if (currentState != null) {
            currentState.cancel();
        }
    }

    public RetryState getCurrentState() {
        return currentState;
    }
}
```

## Phase 4: UI Integration

### 4.1 Create Error Dialog

**File**: `src/main/java/org/whisperdog/ui/ErrorDialog.java` (new)

```java
package org.whisperdog.ui;

import org.whisperdog.error.TranscriptionException;
import org.whisperdog.retry.RetryState;

import javax.swing.*;
import java.awt.*;

public class ErrorDialog {

    /**
     * Shows dialog for empty transcription response, allowing user to retry or cancel.
     *
     * @return true if user wants to retry, false to cancel
     */
    public static boolean showEmptyResponseDialog(Component parent, RetryState state) {
        String message = "<html><body style='width: 300px'>" +
            "<h3>No Speech Detected</h3>" +
            "<p>Your recording was processed but no speech was detected.</p>" +
            "<p>This can happen when:</p>" +
            "<ul>" +
            "<li>The audio contains only background noise</li>" +
            "<li>The microphone sensitivity was too low</li>" +
            "<li>The recording captured mostly silence</li>" +
            "</ul>" +
            "<p><b>File:</b> " + state.getCompressedAudio().getName() + "</p>" +
            "</body></html>";

        int result = JOptionPane.showOptionDialog(
            parent,
            message,
            "Transcription Result",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new String[]{"Retry Transcription", "Cancel"},
            "Retry Transcription"
        );

        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Shows error dialog for permanent failures.
     */
    public static void showPermanentErrorDialog(Component parent, TranscriptionException e) {
        String message = "<html><body style='width: 300px'>" +
            "<h3>Transcription Failed</h3>" +
            "<p>" + getUserMessage(e) + "</p>" +
            "<p><small>Error code: " + e.getHttpStatus() + "</small></p>" +
            "</body></html>";

        JOptionPane.showMessageDialog(
            parent,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Shows dialog when all retries exhausted.
     *
     * @return true if user wants to try again, false to give up
     */
    public static boolean showRetriesExhaustedDialog(Component parent, RetryState state) {
        String message = "<html><body style='width: 300px'>" +
            "<h3>Retries Exhausted</h3>" +
            "<p>Transcription failed after " + state.getMaxAttempts() + " attempts.</p>" +
            "<p><b>Last error:</b> " + state.getLastErrorMessage() + "</p>" +
            "<p>Would you like to try again?</p>" +
            "</body></html>";

        int result = JOptionPane.showOptionDialog(
            parent,
            message,
            "Transcription Failed",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            new String[]{"Try Again", "Cancel"},
            "Cancel"
        );

        return result == JOptionPane.YES_OPTION;
    }

    private static String getUserMessage(TranscriptionException e) {
        if (e.getHttpStatus() == 413) {
            return "The audio file is too large (max 25MB after compression). " +
                   "Please record shorter audio.";
        }
        if (e.getHttpStatus() == 401) {
            return "Invalid API key. Please check your OpenAI settings.";
        }
        return e.getMessage();
    }
}
```

### 4.2 Update RecorderForm for Error Handling

**File**: `src/main/java/org/whisperdog/recording/RecorderForm.java` (modify)

Add retry handler integration:

```java
// Add imports
import org.whisperdog.retry.RetryHandler;
import org.whisperdog.retry.RetryState;
import org.whisperdog.ui.ErrorDialog;
import org.whisperdog.error.TranscriptionException;

// Add field
private RetryHandler retryHandler;

// In constructor or initialization:
this.retryHandler = new RetryHandler(openAIClient);

// Replace direct transcription call with retry-enabled version:
private void transcribeRecording(File originalFile, File compressedFile) {
    retryHandler.transcribeWithRetry(
        originalFile,
        compressedFile,
        // Progress callback
        progress -> updateStatusLabel(progress),
        // Success callback
        transcription -> {
            handleTranscriptionSuccess(transcription);
        },
        // Failure callback
        error -> {
            if (ErrorDialog.showRetriesExhaustedDialog(this, retryHandler.getCurrentState())) {
                retryHandler.manualRetry(
                    this::updateStatusLabel,
                    this::handleTranscriptionSuccess,
                    e -> ErrorDialog.showPermanentErrorDialog(this, e),
                    this::handleUserAction
                );
            }
        },
        // User action callback (empty response)
        this::handleUserAction
    );
}

private void handleUserAction(RetryState state) {
    if (ErrorDialog.showEmptyResponseDialog(this, state)) {
        retryHandler.manualRetry(
            this::updateStatusLabel,
            this::handleTranscriptionSuccess,
            e -> ErrorDialog.showPermanentErrorDialog(this, e),
            this::handleUserAction
        );
    }
}

private void handleTranscriptionSuccess(String transcription) {
    // Existing success handling logic
    updateStatusLabel("Transcription complete");
    // ... display transcription, trigger pipeline, etc.
}
```

## Phase 5: State Persistence

### 5.1 Add Retry State to ConfigManager

**File**: `src/main/java/org/whisperdog/ConfigManager.java` (modify)

```java
// Add methods for retry state persistence

private static final String RETRY_STATE_FILE = "pending_retry.json";

public void savePendingRetry(RetryState state) {
    if (state == null) {
        deletePendingRetry();
        return;
    }

    try {
        File retryFile = new File(getConfigDir(), RETRY_STATE_FILE);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(retryFile, state);
    } catch (IOException e) {
        logger.warn("Failed to save retry state", e);
    }
}

public RetryState loadPendingRetry() {
    File retryFile = new File(getConfigDir(), RETRY_STATE_FILE);
    if (!retryFile.exists()) {
        return null;
    }

    try {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(retryFile, RetryState.class);
    } catch (IOException e) {
        logger.warn("Failed to load retry state", e);
        return null;
    }
}

public void deletePendingRetry() {
    File retryFile = new File(getConfigDir(), RETRY_STATE_FILE);
    if (retryFile.exists()) {
        retryFile.delete();
    }
}
```

### 5.2 Check for Pending Retry on Startup

**File**: `src/main/java/org/whisperdog/WhisperDogMain.java` (modify)

```java
// In application startup, after UI is ready:

private void checkPendingRetry() {
    RetryState pendingRetry = configManager.loadPendingRetry();
    if (pendingRetry != null && pendingRetry.getCompressedAudio().exists()) {
        int result = JOptionPane.showConfirmDialog(
            mainFrame,
            "A previous transcription was interrupted. Would you like to retry?",
            "Pending Transcription",
            JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            recorderForm.resumeRetry(pendingRetry);
        } else {
            configManager.deletePendingRetry();
        }
    }
}
```

## Phase 6: Logging Enhancement

### 6.1 Add Structured Error Logging

**File**: `src/main/java/org/whisperdog/logging/TranscriptionLogger.java` (new)

```java
package org.whisperdog.logging;

import org.whisperdog.error.TranscriptionException;
import org.whisperdog.retry.RetryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TranscriptionLogger {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionLogger.class);
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logAttempt(RetryState state, String status) {
        logger.info("[{}] Transcription attempt {}/{}: {}",
            LocalDateTime.now().format(formatter),
            state.getAttemptCount(),
            state.getMaxAttempts(),
            status
        );
    }

    public static void logError(TranscriptionException e, RetryState state) {
        logger.error(
            "\n[{}] ERROR: Transcription failed\n" +
            "  Category: {}\n" +
            "  HTTP Status: {}\n" +
            "  Message: {}\n" +
            "  File: {} ({})\n" +
            "  Retry: {}/{}\n" +
            "  Next retry in: {} seconds",
            LocalDateTime.now().format(formatter),
            e.getCategory(),
            e.getHttpStatus(),
            e.getMessage(),
            state.getCompressedAudio().getName(),
            getFileSize(state.getCompressedAudio()),
            state.getAttemptCount(),
            state.getMaxAttempts(),
            state.getDelayMillis() / 1000
        );
    }

    public static void logSuccess(RetryState state, int transcriptionLength) {
        logger.info("[{}] Transcription successful after {} attempt(s). Result: {} characters",
            LocalDateTime.now().format(formatter),
            state.getAttemptCount(),
            transcriptionLength
        );
    }

    public static void logValidationFailure(File file, String reason) {
        logger.warn("[{}] Pre-validation failed for {}: {}",
            LocalDateTime.now().format(formatter),
            file.getName(),
            reason
        );
    }

    private static String getFileSize(File file) {
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
```

## File Summary

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/org/whisperdog/error/ErrorCategory.java` | Create | Error category enum |
| `src/main/java/org/whisperdog/error/TranscriptionException.java` | Create | Custom exception with categorization |
| `src/main/java/org/whisperdog/error/ErrorClassifier.java` | Create | Error classification service |
| `src/main/java/org/whisperdog/validation/TranscriptionValidator.java` | Create | Pre-submission validation |
| `src/main/java/org/whisperdog/retry/RetryState.java` | Create | Retry state tracking |
| `src/main/java/org/whisperdog/retry/RetryHandler.java` | Create | Retry execution handler |
| `src/main/java/org/whisperdog/ui/ErrorDialog.java` | Create | User-facing error dialogs |
| `src/main/java/org/whisperdog/logging/TranscriptionLogger.java` | Create | Structured error logging |
| `src/main/java/org/whisperdog/openai/OpenAIClient.java` | Modify | Add pre-validation call |
| `src/main/java/org/whisperdog/recording/RecorderForm.java` | Modify | Integrate retry handler |
| `src/main/java/org/whisperdog/ConfigManager.java` | Modify | Add retry state persistence |
| `src/main/java/org/whisperdog/WhisperDogMain.java` | Modify | Check pending retry on startup |

## Testing Checklist

- [ ] Unit test: ErrorCategory classification for each HTTP status
- [ ] Unit test: TranscriptionValidator rejects files > 26MB
- [ ] Unit test: RetryState calculates exponential backoff correctly
- [ ] Integration test: HTTP 507 triggers automatic retry
- [ ] Integration test: HTTP 413 fails immediately with pre-validation
- [ ] Integration test: Empty response shows user dialog
- [ ] UI test: Error dialogs display correctly
- [ ] UI test: Progress updates during retry
- [ ] Persistence test: Retry state survives app restart
