package org.whisperdog.retry;

import org.whisperdog.error.ErrorCategory;
import org.whisperdog.error.ErrorClassifier;
import org.whisperdog.error.TranscriptionException;
import org.whisperdog.recording.clients.OpenAITranscribeClient;

import javax.swing.SwingWorker;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles transcription retries with exponential backoff.
 * Automatically retries transient errors, prompts user for user-actionable errors,
 * and fails immediately for permanent errors.
 */
public class RetryHandler {

    private final OpenAITranscribeClient openAIClient;
    private RetryState currentState;
    private SwingWorker<String, String> currentWorker;

    public RetryHandler(OpenAITranscribeClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    /**
     * Attempts transcription with automatic retry for transient errors.
     *
     * @param originalFile The original audio file (for reference)
     * @param compressedFile The compressed audio file to transcribe
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

    /**
     * Internal retry execution using SwingWorker for background processing.
     */
    private void executeWithRetry(
            Consumer<String> onProgress,
            Consumer<String> onSuccess,
            Consumer<TranscriptionException> onFailure,
            Consumer<RetryState> onUserAction
    ) {
        currentWorker = new SwingWorker<String, String>() {
            private TranscriptionException lastException;

            @Override
            protected String doInBackground() throws Exception {
                while (currentState.canRetry() && !isCancelled()) {
                    try {
                        publish("Transcribing... " + currentState.getProgressText());
                        String result = openAIClient.transcribe(currentState.getCompressedAudio());
                        return result;
                    } catch (TranscriptionException e) {
                        lastException = e;
                        currentState.recordAttempt(e.getMessage(), e.getHttpStatus());

                        // Handle based on error category
                        if (e.getCategory() == ErrorCategory.PERMANENT) {
                            // Don't retry permanent errors - fail immediately
                            publish("Error: " + ErrorClassifier.getUserFriendlyMessage(e));
                            throw e;
                        }

                        if (e.getCategory() == ErrorCategory.USER_ACTION) {
                            // Signal UI to prompt user - exit worker
                            publish("Needs your attention...");
                            return null; // Will trigger onUserAction in done()
                        }

                        // Transient or unknown error - check if we can retry
                        if (!currentState.canRetry()) {
                            publish("Max retries exhausted");
                            throw e;
                        }

                        // Wait before retry
                        long delayMillis = currentState.getDelayMillis();
                        publish(String.format("%s - Retrying in %ds...",
                            ErrorClassifier.getUserFriendlyMessage(e),
                            delayMillis / 1000));
                        Thread.sleep(delayMillis);
                    }
                }

                // Should only reach here if cancelled
                if (lastException != null) {
                    throw lastException;
                }
                throw new TranscriptionException("Operation cancelled");
            }

            @Override
            protected void process(List<String> chunks) {
                // Update UI with latest progress
                if (!chunks.isEmpty() && onProgress != null) {
                    onProgress.accept(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    String result = get();
                    if (result != null) {
                        // Success
                        onSuccess.accept(result);
                    } else if (lastException != null &&
                               lastException.getCategory() == ErrorCategory.USER_ACTION) {
                        // User action needed (empty response)
                        onUserAction.accept(currentState);
                    }
                } catch (Exception e) {
                    // Failure - extract the actual exception
                    Throwable cause = e.getCause();
                    if (cause instanceof TranscriptionException) {
                        onFailure.accept((TranscriptionException) cause);
                    } else {
                        onFailure.accept(new TranscriptionException(
                            "Transcription failed: " + e.getMessage()
                        ));
                    }
                }
            }
        };

        currentWorker.execute();
    }

    /**
     * Manually retry after user confirmation (for USER_ACTION errors).
     * Resets attempt count to allow fresh retries.
     */
    public void manualRetry(
            Consumer<String> onProgress,
            Consumer<String> onSuccess,
            Consumer<TranscriptionException> onFailure,
            Consumer<RetryState> onUserAction
    ) {
        if (currentState != null && !currentState.isCancelled()) {
            // Reset for fresh attempt
            currentState.reset();
            executeWithRetry(onProgress, onSuccess, onFailure, onUserAction);
        }
    }

    /**
     * Retries using the same state without resetting (continues from where we left off).
     */
    public void continueRetry(
            Consumer<String> onProgress,
            Consumer<String> onSuccess,
            Consumer<TranscriptionException> onFailure,
            Consumer<RetryState> onUserAction
    ) {
        if (currentState != null && currentState.canRetry()) {
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
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
    }

    /**
     * Returns the current retry state.
     */
    public RetryState getCurrentState() {
        return currentState;
    }

    /**
     * Sets the retry state (used when restoring from persistence).
     */
    public void setCurrentState(RetryState state) {
        this.currentState = state;
    }

    /**
     * Returns true if a retry operation is currently in progress.
     */
    public boolean isRetrying() {
        return currentWorker != null && !currentWorker.isDone();
    }
}
