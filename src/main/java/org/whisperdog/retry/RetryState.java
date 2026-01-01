package org.whisperdog.retry;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;

/**
 * Tracks the state of a retry operation including attempt count,
 * timing, and file references for recovery.
 */
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

    /**
     * Creates a new retry state for the given audio files.
     *
     * @param originalAudio The original recorded audio file
     * @param compressedAudio The compressed audio file sent to API
     */
    public RetryState(File originalAudio, File compressedAudio) {
        this.originalAudioPath = originalAudio != null ? originalAudio.getAbsolutePath() : null;
        this.compressedAudioPath = compressedAudio.getAbsolutePath();
        this.attemptCount = 0;
        this.maxAttempts = 3;
        this.cancelled = false;
    }

    /**
     * Records an attempt and calculates next retry time with exponential backoff.
     *
     * @param errorMessage The error message from the failed attempt
     * @param httpStatus The HTTP status code (-1 if not applicable)
     */
    public void recordAttempt(String errorMessage, int httpStatus) {
        this.attemptCount++;
        this.lastErrorMessage = errorMessage;
        this.lastHttpStatus = httpStatus;
        this.lastAttemptTime = Instant.now();
        this.nextRetryTime = calculateNextRetryTime();
    }

    /**
     * Calculates the next retry time using exponential backoff.
     * Delays: 1s, 2s, 4s for attempts 1, 2, 3
     */
    private Instant calculateNextRetryTime() {
        // Exponential backoff: 2^(attempt-1) seconds = 1, 2, 4...
        long delaySeconds = (long) Math.pow(2, attemptCount - 1);
        return Instant.now().plusSeconds(delaySeconds);
    }

    /**
     * Returns true if more retries are allowed.
     */
    public boolean canRetry() {
        return !cancelled && attemptCount < maxAttempts;
    }

    /**
     * Returns the delay in milliseconds until the next retry.
     */
    public long getDelayMillis() {
        if (nextRetryTime == null) return 0;
        long delay = nextRetryTime.toEpochMilli() - System.currentTimeMillis();
        return Math.max(0, delay);
    }

    /**
     * Returns the delay in seconds until the next retry.
     */
    public int getDelaySeconds() {
        return (int) Math.ceil(getDelayMillis() / 1000.0);
    }

    /**
     * Cancels all pending retries.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Resets the retry state for a fresh attempt (used for manual retry).
     */
    public void reset() {
        this.attemptCount = 0;
        this.lastErrorMessage = null;
        this.lastHttpStatus = 0;
        this.lastAttemptTime = null;
        this.nextRetryTime = null;
        this.cancelled = false;
    }

    // Getters
    public File getOriginalAudio() {
        return originalAudioPath != null ? new File(originalAudioPath) : null;
    }

    public File getCompressedAudio() {
        return new File(compressedAudioPath);
    }

    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public int getLastHttpStatus() { return lastHttpStatus; }
    public Instant getLastAttemptTime() { return lastAttemptTime; }
    public boolean isCancelled() { return cancelled; }

    /**
     * Returns a progress string like "Attempt 2/3".
     */
    public String getProgressText() {
        return String.format("Attempt %d/%d", attemptCount + 1, maxAttempts);
    }

    /**
     * Returns a status string for the current state.
     */
    public String getStatusText() {
        if (cancelled) return "Cancelled";
        if (attemptCount >= maxAttempts) return "Max retries reached";
        if (attemptCount == 0) return "Ready";
        return String.format("Retry %d/%d in %ds", attemptCount + 1, maxAttempts, getDelaySeconds());
    }
}
