package org.whisperdog.logging;

import org.whisperdog.error.TranscriptionException;
import org.whisperdog.ConsoleLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Structured logging for transcription operations and errors.
 * Logs to both the application log file and the UI console.
 */
public class TranscriptionLogger {

    private static final Logger logger = LogManager.getLogger(TranscriptionLogger.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs a transcription attempt.
     *
     * @param attemptNumber Current attempt number
     * @param maxAttempts Maximum attempts allowed
     * @param file The audio file being transcribed
     */
    public static void logAttempt(int attemptNumber, int maxAttempts, File file) {
        String message = String.format("Transcription attempt %d/%d: %s (%s)",
            attemptNumber, maxAttempts, file.getName(), getFileSize(file));

        logger.info(message);
        ConsoleLogger.getInstance().log(message);
    }

    /**
     * Logs a transcription error with full context.
     *
     * @param e The TranscriptionException
     * @param file The audio file
     * @param attemptNumber Current attempt number
     * @param maxAttempts Maximum attempts allowed
     */
    public static void logError(TranscriptionException e, File file, int attemptNumber, int maxAttempts) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("[%s] ERROR: Transcription failed%n", timestamp));
        logMessage.append(String.format("  Category: %s%n", e.getCategory()));

        if (e.getHttpStatus() > 0) {
            logMessage.append(String.format("  HTTP Status: %d%n", e.getHttpStatus()));
        }

        logMessage.append(String.format("  Message: %s%n", e.getMessage()));

        if (file != null) {
            logMessage.append(String.format("  File: %s (%s)%n", file.getName(), getFileSize(file)));
        }

        logMessage.append(String.format("  Attempt: %d/%d", attemptNumber, maxAttempts));

        // Log to file
        logger.error(logMessage.toString());

        // Simplified version for console
        ConsoleLogger console = ConsoleLogger.getInstance();
        console.logError(String.format("Transcription failed (attempt %d/%d): %s",
            attemptNumber, maxAttempts, e.getMessage()));
    }

    /**
     * Logs a successful transcription.
     *
     * @param attemptNumber The attempt that succeeded
     * @param transcriptionLength Length of the transcription result
     * @param durationMs How long the transcription took
     */
    public static void logSuccess(int attemptNumber, int transcriptionLength, long durationMs) {
        String message = String.format(
            "Transcription successful after %d attempt(s) - %d chars in %dms",
            attemptNumber, transcriptionLength, durationMs);

        logger.info(message);
        ConsoleLogger.getInstance().logSuccess(message);
    }

    /**
     * Logs a pre-validation failure.
     *
     * @param file The file that failed validation
     * @param reason The reason for failure
     */
    public static void logValidationFailure(File file, String reason) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String message = String.format("[%s] Pre-validation failed for %s: %s",
            timestamp, file.getName(), reason);

        logger.warn(message);
        ConsoleLogger.getInstance().logError("Validation failed: " + reason);
    }

    /**
     * Logs a retry delay.
     *
     * @param delaySeconds Seconds until retry
     * @param attemptNumber Next attempt number
     * @param maxAttempts Maximum attempts allowed
     */
    public static void logRetryDelay(int delaySeconds, int attemptNumber, int maxAttempts) {
        String message = String.format("Retrying in %ds (attempt %d/%d)...",
            delaySeconds, attemptNumber, maxAttempts);

        logger.info(message);
        ConsoleLogger.getInstance().log(message);
    }

    /**
     * Logs when max retries are exhausted.
     *
     * @param lastError The last error encountered
     */
    public static void logRetriesExhausted(TranscriptionException lastError) {
        logger.error("Max retries exhausted. Last error: " + lastError.getMessage());
        ConsoleLogger.getInstance().logError("Max retries exhausted");
    }

    /**
     * Logs user cancellation.
     *
     * @param reason The reason for cancellation
     */
    public static void logUserCancelled(String reason) {
        String message = "Transcription cancelled by user: " + reason;
        logger.info(message);
        ConsoleLogger.getInstance().log(message);
    }

    /**
     * Returns a human-readable file size.
     */
    private static String getFileSize(File file) {
        if (file == null || !file.exists()) return "N/A";
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
