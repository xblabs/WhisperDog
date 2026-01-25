package org.whisperdog.recording.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.ConfigManager;
import org.whisperdog.error.ErrorClassifier;
import org.whisperdog.error.TranscriptionException;
import org.whisperdog.validation.TranscriptionValidator;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class OpenAITranscribeClient {
    private static final Logger logger = LogManager.getLogger(OpenAITranscribeClient.class);
    private static final String API_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final long MAX_FILE_SIZE = 24 * 1024 * 1024; // 24 MB (leaving buffer under 25MB limit)
    private static final long MAX_COMPRESSED_FILE_SIZE = 26 * 1024 * 1024; // 26 MB hard limit for validation
    private static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final int SOCKET_TIMEOUT = 600000; // 10 minutes for large file processing
    private final ConfigManager configManager;

    public OpenAITranscribeClient(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Compresses the audio file to MP3 format using ffmpeg to reduce file size.
     * This is much more effective than downsampling - can reduce size by 10x or more.
     * Creates a temporary compressed file.
     *
     * @param originalFile The original audio file
     * @return The compressed MP3 file, or null if compression fails
     */
    private File compressAudioToMp3(File originalFile) {
        try {
            logger.info("Compressing audio file to MP3: {} (size: {} MB)",
                originalFile.getName(), originalFile.length() / (1024.0 * 1024.0));
            org.whisperdog.ConsoleLogger.getInstance().log(String.format(
                "Compressing audio file to MP3: %s (size: %.2f MB)",
                originalFile.getName(), originalFile.length() / (1024.0 * 1024.0)));

            // Create temporary MP3 file (cleanup in transcribe() finally block)
            File mp3File = File.createTempFile("whisperdog_compressed_", ".mp3");

            // Use ffmpeg to convert to MP3 with good compression
            // -y = overwrite output file
            // -i = input file
            // -codec:a libmp3lame = use LAME MP3 encoder
            // -q:a 4 = VBR quality (0-9, where 0 is best, 9 is worst; 4 is good for speech ~140kbps)
            // -ac 1 = mono audio (speech doesn't need stereo, halves file size)
            // -ar 16000 = 16kHz sample rate (good for speech recognition)
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", originalFile.getAbsolutePath(),
                "-codec:a", "libmp3lame",
                "-q:a", "4",
                "-ac", "1",
                "-ar", "16000",
                mp3File.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output to prevent blocking
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && mp3File.exists() && mp3File.length() > 0) {
                double compressionRatio = (double) originalFile.length() / mp3File.length();
                logger.info("Successfully compressed to MP3: {} (size: {} MB, compression ratio: {:.1f}x)",
                    mp3File.getName(),
                    mp3File.length() / (1024.0 * 1024.0),
                    compressionRatio);
                org.whisperdog.ConsoleLogger.getInstance().logSuccess(String.format(
                    "Successfully compressed to MP3: %.2f MB (%.1fx compression ratio)",
                    mp3File.length() / (1024.0 * 1024.0), compressionRatio));
                return mp3File;
            } else {
                logger.error("ffmpeg conversion failed with exit code: {}. Output: {}", exitCode, output);
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to compress audio file to MP3", e);
            return null;
        }
    }

    /**
     * Legacy compression method using downsampling.
     * Kept as fallback if ffmpeg is not available.
     *
     * @param originalFile The original audio file
     * @return The compressed audio file, or the original if compression fails
     */
    private File compressAudioFileByDownsampling(File originalFile) {
        try {
            logger.info("Compressing audio file by downsampling: {} (size: {} MB)",
                originalFile.getName(), originalFile.length() / (1024.0 * 1024.0));

            // Read the original audio file
            AudioInputStream originalStream = AudioSystem.getAudioInputStream(originalFile);
            AudioFormat originalFormat = originalStream.getFormat();

            // Create a new format with lower sample rate (16kHz is good for speech)
            float newSampleRate = 16000.0f;
            AudioFormat targetFormat = new AudioFormat(
                originalFormat.getEncoding(),
                newSampleRate,
                originalFormat.getSampleSizeInBits(),
                originalFormat.getChannels(),
                originalFormat.getFrameSize(),
                newSampleRate,
                originalFormat.isBigEndian()
            );

            // Convert to the new format
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);

            // Create a temporary file for the compressed audio (cleanup in transcribe() finally block)
            File compressedFile = File.createTempFile("whisperdog_compressed_", ".wav");

            // Write the converted audio to the temporary file
            AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, compressedFile);

            // Close streams
            convertedStream.close();
            originalStream.close();

            logger.info("Compressed audio file created: {} (size: {} MB)",
                compressedFile.getName(), compressedFile.length() / (1024.0 * 1024.0));

            return compressedFile;
        } catch (Exception e) {
            logger.error("Failed to compress audio file, using original", e);
            return originalFile;
        }
    }

    /**
     * Compresses the audio file to reduce size before uploading to OpenAI.
     * First tries MP3 compression via ffmpeg (10x+ compression).
     * Falls back to downsampling if ffmpeg is not available.
     *
     * @param originalFile The original audio file
     * @return The compressed audio file, or the original if compression fails
     */
    private File compressAudioFile(File originalFile) {
        // Try MP3 compression first (much better compression)
        File mp3File = compressAudioToMp3(originalFile);
        if (mp3File != null && mp3File.length() < originalFile.length()) {
            // Check if MP3 is still too large
            if (mp3File.length() > MAX_FILE_SIZE) {
                logger.warn("MP3 file still exceeds size limit ({} MB). File may be too long for OpenAI.",
                    mp3File.length() / (1024.0 * 1024.0));
            }
            return mp3File;
        }

        // Fall back to downsampling if ffmpeg failed or is not available
        logger.warn("MP3 compression failed or not available. Falling back to downsampling.");
        return compressAudioFileByDownsampling(originalFile);
    }

    public String transcribe(File audioFile) throws TranscriptionException {
        // Pre-submission validation: file type (ISS_00008)
        try {
            TranscriptionValidator.validateFileType(audioFile);
        } catch (TranscriptionException e) {
            logger.error("File type validation failed: {}", audioFile.getName());
            org.whisperdog.ConsoleLogger.getInstance().logError(e.getMessage());
            throw e;
        }

        // Check if file size exceeds limit and compress if necessary
        File fileToTranscribe = audioFile;
        File compressedFile = null;  // Track for cleanup
        if (audioFile.length() > MAX_FILE_SIZE) {
            logger.warn("Audio file size ({} MB) exceeds OpenAI limit (25 MB). Compressing...",
                audioFile.length() / (1024.0 * 1024.0));
            compressedFile = compressAudioFile(audioFile);
            fileToTranscribe = compressedFile;
        }

        // Pre-submission validation (ISS_00001, Task 0002)
        try {
            TranscriptionValidator.validateFileSize(fileToTranscribe);
        } catch (TranscriptionException e) {
            logger.error("File size validation failed: {} (size: {})",
                fileToTranscribe.getName(), TranscriptionValidator.getFileSizeDisplay(fileToTranscribe));
            org.whisperdog.ConsoleLogger.getInstance().logError(e.getMessage());
            throw e;
        }

        // Configure timeouts to prevent indefinite hanging
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT)
            .setSocketTimeout(SOCKET_TIMEOUT)
            .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
            .build();

        try {
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {
            HttpPost httpPost = new HttpPost(API_URL);
            httpPost.setHeader("Authorization", "Bearer " + configManager.getApiKey());

            // Determine content type based on file extension
            String fileName = fileToTranscribe.getName().toLowerCase();
            String contentType = "audio/wav"; // default
            if (fileName.endsWith(".mp3")) {
                contentType = "audio/mpeg";
            } else if (fileName.endsWith(".m4a")) {
                contentType = "audio/mp4";
            } else if (fileName.endsWith(".ogg")) {
                contentType = "audio/ogg";
            } else if (fileName.endsWith(".flac")) {
                contentType = "audio/flac";
            }

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", fileToTranscribe, ContentType.create(contentType), fileToTranscribe.getName());
            builder.addTextBody("model", "whisper-1");

            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity responseEntity = response.getEntity();
                String responseString = new String(responseEntity.getContent().readAllBytes(), StandardCharsets.UTF_8);

                if (statusCode != 200) {
                    logger.error("OpenAI API returned status code: {}. Response: {}", statusCode, responseString);

                    // Try to parse as JSON to get error message
                    String errorMessage;
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode jsonNode = objectMapper.readTree(responseString);
                        errorMessage = jsonNode.path("error").path("message").asText("Unknown error");
                    } catch (Exception jsonException) {
                        // Response is not valid JSON, use raw response
                        logger.error("Failed to parse error response as JSON", jsonException);
                        // Truncate very long responses
                        errorMessage = responseString.length() > 500
                            ? responseString.substring(0, 500) + "..."
                            : responseString;
                    }
                    // Throw TranscriptionException with HTTP status for categorization
                    throw new TranscriptionException(
                        "Error from OpenAI API (HTTP " + statusCode + "): " + errorMessage,
                        statusCode,
                        responseString
                    );
                }

                // Parse successful response
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(responseString);
                    String transcription = jsonNode.path("text").asText();
                    if (transcription == null || transcription.isEmpty()) {
                        logger.warn("OpenAI returned empty transcription");
                        // Empty response requires user action - they may want to retry
                        throw new TranscriptionException(
                            "No speech detected in recording",
                            200,
                            responseString
                        );
                    }
                    return transcription;
                } catch (TranscriptionException te) {
                    throw te; // Re-throw our own exceptions
                } catch (Exception jsonException) {
                    logger.error("Failed to parse successful response as JSON. Response: {}", responseString, jsonException);
                    // JSON parse error is transient (may succeed on retry)
                    throw new TranscriptionException(
                        "Failed to parse OpenAI response: " + jsonException.getMessage(),
                        jsonException,
                        true,  // isJsonError
                        false  // isNetworkError
                    );
                }
            }
            } catch (TranscriptionException te) {
                throw te; // Re-throw our own exceptions
            } catch (java.net.SocketTimeoutException e) {
                logger.error("Socket timeout during transcription", e);
                throw new TranscriptionException("Connection timed out", e, false, true);
            } catch (java.net.UnknownHostException e) {
                logger.error("Cannot reach OpenAI server", e);
                throw new TranscriptionException("Cannot reach server: " + e.getMessage(), e, false, true);
            } catch (java.net.ConnectException e) {
                logger.error("Connection to OpenAI failed", e);
                throw new TranscriptionException("Connection failed: " + e.getMessage(), e, false, true);
            } catch (IOException e) {
                logger.error("IO error during transcription", e);
                throw new TranscriptionException("Network error: " + e.getMessage(), e, false, true);
            }
        } finally {
            // Clean up internally-created compressed file to prevent temp file accumulation
            if (compressedFile != null && compressedFile.exists()) {
                if (!compressedFile.delete()) {
                    logger.debug("Could not delete temp compressed file: {}", compressedFile.getName());
                }
            }
        }
    }
}
