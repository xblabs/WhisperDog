package org.whisperdog.error;

/**
 * Categorizes transcription errors by their recovery strategy.
 */
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
