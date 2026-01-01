package org.whisperdog.error;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.ConnectException;

/**
 * Utility class for classifying exceptions and generating user-friendly messages.
 */
public class ErrorClassifier {

    /**
     * Classifies a generic exception into a TranscriptionException.
     *
     * @param e The original exception
     * @param httpStatus HTTP status code (-1 if not applicable)
     * @param responseBody Response body (null if not applicable)
     * @return Classified TranscriptionException
     */
    public static TranscriptionException classify(Exception e, int httpStatus, String responseBody) {
        // Network errors
        if (e instanceof SocketTimeoutException) {
            return new TranscriptionException(
                "Connection timed out: " + e.getMessage(),
                e, false, true
            );
        }

        if (e instanceof UnknownHostException) {
            return new TranscriptionException(
                "Cannot reach server: " + e.getMessage(),
                e, false, true
            );
        }

        if (e instanceof ConnectException) {
            return new TranscriptionException(
                "Connection failed: " + e.getMessage(),
                e, false, true
            );
        }

        // JSON parse errors (check class name to avoid dependency)
        if (e.getClass().getName().contains("JsonParseException") ||
            e.getClass().getName().contains("JsonProcessingException")) {
            return new TranscriptionException(
                "Invalid API response format",
                e, true, false
            );
        }

        // HTTP response errors
        if (httpStatus > 0) {
            return new TranscriptionException(e.getMessage(), httpStatus, responseBody);
        }

        // Unknown error type
        return new TranscriptionException(
            "Transcription error: " + e.getMessage(),
            e, false, false
        );
    }

    /**
     * Generates a user-friendly error message based on the exception category.
     *
     * @param e The TranscriptionException
     * @return Human-readable error message
     */
    public static String getUserFriendlyMessage(TranscriptionException e) {
        switch (e.getCategory()) {
            case TRANSIENT:
                return getTransientMessage(e);
            case PERMANENT:
                return getPermanentMessage(e);
            case USER_ACTION:
                return getUserActionMessage(e);
            default:
                return "An unexpected error occurred: " + e.getMessage();
        }
    }

    private static String getTransientMessage(TranscriptionException e) {
        if (e.getHttpStatus() == 507) {
            return "OpenAI servers are temporarily overloaded. Retrying automatically...";
        }
        if (e.getHttpStatus() == 429) {
            return "Too many requests. Waiting before retry...";
        }
        if (e.isNetworkError()) {
            return "Network connection issue. Retrying...";
        }
        if (e.isJsonParseError()) {
            return "Received invalid response from server. Retrying...";
        }
        if (e.getHttpStatus() >= 500) {
            return "Server error (HTTP " + e.getHttpStatus() + "). Retrying...";
        }
        return "Temporary error occurred. Retrying...";
    }

    private static String getPermanentMessage(TranscriptionException e) {
        if (e.getHttpStatus() == 413) {
            return "File is too large for transcription (max 25MB after compression). " +
                   "Please record shorter audio.";
        }
        if (e.getHttpStatus() == 401) {
            return "Invalid API key. Please check your OpenAI API key in Settings.";
        }
        if (e.getHttpStatus() == 403) {
            return "Access denied. Please verify your OpenAI account has API access.";
        }
        return "Transcription failed: " + e.getMessage();
    }

    private static String getUserActionMessage(TranscriptionException e) {
        if (e.isEmptyResponse()) {
            return "No speech was detected in your recording. " +
                   "This may happen if the audio contains only background noise or silence.";
        }
        return "Please review and decide how to proceed.";
    }

    /**
     * Returns a short status message for progress display.
     *
     * @param e The TranscriptionException
     * @param attemptNumber Current retry attempt number
     * @param maxAttempts Maximum retry attempts
     * @return Short status message
     */
    public static String getProgressMessage(TranscriptionException e, int attemptNumber, int maxAttempts) {
        String category = e.getCategory().name().toLowerCase();
        if (e.isRetryable()) {
            return String.format("Retry %d/%d (%s error)", attemptNumber, maxAttempts, category);
        }
        return "Error: " + category;
    }
}
