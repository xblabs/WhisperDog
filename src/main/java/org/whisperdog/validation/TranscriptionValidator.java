package org.whisperdog.validation;

import org.whisperdog.error.TranscriptionException;

import java.io.File;

/**
 * Pre-submission validation for transcription files.
 * Validates files before sending to OpenAI to fail fast on preventable errors.
 */
public class TranscriptionValidator {

    // OpenAI Whisper API limit is 25MB, use 26MB as hard limit
    public static final long MAX_FILE_SIZE_BYTES = 26 * 1024 * 1024; // 26 MB

    /**
     * Validates that the compressed audio file is within OpenAI's size limits.
     *
     * @param compressedFile The MP3 file to validate
     * @throws TranscriptionException if validation fails (category: PERMANENT)
     */
    public static void validateFileSize(File compressedFile) throws TranscriptionException {
        if (!compressedFile.exists()) {
            throw new TranscriptionException(
                "Compressed file not found: " + compressedFile.getName()
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
            // Throw with HTTP 413 status to categorize as PERMANENT
            throw new TranscriptionException(message, 413, null);
        }
    }

    /**
     * Returns the file size in a human-readable format.
     *
     * @param file The file to measure
     * @return Human-readable size string
     */
    public static String getFileSizeDisplay(File file) {
        if (!file.exists()) return "N/A";
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Checks if a file is within the size limit without throwing.
     *
     * @param file The file to check
     * @return true if file is within limits, false otherwise
     */
    public static boolean isWithinSizeLimit(File file) {
        return file.exists() && file.length() <= MAX_FILE_SIZE_BYTES;
    }
}
