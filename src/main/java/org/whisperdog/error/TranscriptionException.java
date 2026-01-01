package org.whisperdog.error;

/**
 * Custom exception for transcription failures with automatic error categorization.
 * Captures HTTP status, response body, and determines recovery strategy.
 */
public class TranscriptionException extends Exception {

    private final int httpStatus;
    private final String responseBody;
    private final ErrorCategory category;
    private final boolean emptyResponse;
    private final boolean jsonParseError;
    private final boolean networkError;

    /**
     * Creates exception from HTTP response.
     */
    public TranscriptionException(String message, int httpStatus, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.emptyResponse = isEmptyTranscription(responseBody);
        this.jsonParseError = false;
        this.networkError = false;
        this.category = categorize();
    }

    /**
     * Creates exception from parsing or network error.
     */
    public TranscriptionException(String message, Throwable cause, boolean isJsonError, boolean isNetworkError) {
        super(message, cause);
        this.httpStatus = -1;
        this.responseBody = null;
        this.emptyResponse = false;
        this.jsonParseError = isJsonError;
        this.networkError = isNetworkError;
        this.category = categorize();
    }

    /**
     * Creates exception with just a message (for validation failures).
     */
    public TranscriptionException(String message) {
        super(message);
        this.httpStatus = -1;
        this.responseBody = null;
        this.emptyResponse = false;
        this.jsonParseError = false;
        this.networkError = false;
        this.category = ErrorCategory.PERMANENT;
    }

    private boolean isEmptyTranscription(String body) {
        if (body == null) return false;
        // Check for {"text":"","usage":...} pattern (empty transcription result)
        return body.contains("\"text\":\"\"") || body.contains("\"text\": \"\"");
    }

    private ErrorCategory categorize() {
        // HTTP 507: OpenAI buffer limit exceeded (transient overload)
        if (httpStatus == 507) return ErrorCategory.TRANSIENT;

        // HTTP 413: File too large (permanent, should be caught by pre-validation)
        if (httpStatus == 413) return ErrorCategory.PERMANENT;

        // HTTP 429: Rate limit (transient, retry with delay)
        if (httpStatus == 429) return ErrorCategory.TRANSIENT;

        // HTTP 401/403: Auth errors (permanent, bad API key)
        if (httpStatus == 401 || httpStatus == 403) return ErrorCategory.PERMANENT;

        // Empty response: No speech detected (user decides)
        if (emptyResponse) return ErrorCategory.USER_ACTION;

        // JSON parse error: Malformed response (transient)
        if (jsonParseError) return ErrorCategory.TRANSIENT;

        // Network error: Connection issues (transient)
        if (networkError) return ErrorCategory.TRANSIENT;

        // 5xx errors: Server issues (transient)
        if (httpStatus >= 500 && httpStatus < 600) return ErrorCategory.TRANSIENT;

        // 4xx errors (other than handled above): Client errors (permanent)
        if (httpStatus >= 400 && httpStatus < 500) return ErrorCategory.PERMANENT;

        return ErrorCategory.UNKNOWN;
    }

    // Getters
    public int getHttpStatus() { return httpStatus; }
    public String getResponseBody() { return responseBody; }
    public ErrorCategory getCategory() { return category; }
    public boolean isEmptyResponse() { return emptyResponse; }
    public boolean isJsonParseError() { return jsonParseError; }
    public boolean isNetworkError() { return networkError; }

    /**
     * Returns true if automatic retry is appropriate for this error.
     */
    public boolean isRetryable() {
        return category == ErrorCategory.TRANSIENT || category == ErrorCategory.UNKNOWN;
    }

    /**
     * Returns true if user input is needed to proceed.
     */
    public boolean requiresUserAction() {
        return category == ErrorCategory.USER_ACTION;
    }
}
