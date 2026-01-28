package org.whisperdog.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.whisperdog.ConfigManager;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for ffmpeg operations, primarily for extracting audio from video files.
 * Supports MP4, MOV, MKV, AVI, and WEBM formats.
 */
public class FFmpegUtil {

    private static final org.apache.logging.log4j.Logger logger =
        org.apache.logging.log4j.LogManager.getLogger(FFmpegUtil.class);

    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".mov", ".mkv", ".avi", ".webm"};

    // Pattern to parse ffmpeg duration output: Duration: 00:05:23.45
    private static final Pattern DURATION_PATTERN =
        Pattern.compile("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})");

    // Pattern to parse ffmpeg progress: time=00:01:23.45
    private static final Pattern TIME_PATTERN =
        Pattern.compile("time=(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})");

    /**
     * Result of an audio extraction operation.
     */
    public static class ExtractionResult {
        public final boolean success;
        public final File audioFile;
        public final String errorMessage;
        public final boolean noAudioTrack;

        private ExtractionResult(boolean success, File audioFile, String errorMessage, boolean noAudioTrack) {
            this.success = success;
            this.audioFile = audioFile;
            this.errorMessage = errorMessage;
            this.noAudioTrack = noAudioTrack;
        }

        public static ExtractionResult success(File audioFile) {
            return new ExtractionResult(true, audioFile, null, false);
        }

        public static ExtractionResult failure(String errorMessage) {
            return new ExtractionResult(false, null, errorMessage, false);
        }

        public static ExtractionResult noAudio() {
            return new ExtractionResult(false, null, "This video contains no audio track.", true);
        }
    }

    /**
     * Check if ffmpeg is available in system PATH or bundled location.
     * @return true if ffmpeg is available and working
     */
    public static boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output to prevent blocking
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Consume output
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.debug("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if file is a supported video format based on extension.
     * @param file The file to check
     * @return true if file has a supported video extension
     */
    public static boolean isVideoFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        String name = file.getName().toLowerCase();
        for (String ext : VIDEO_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if video file contains an audio track using ffprobe.
     * @param videoFile The video file to check
     * @return true if video has at least one audio stream
     */
    public static boolean hasAudioTrack(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "a",
                "-show_entries", "stream=codec_type",
                "-of", "csv=p=0",
                videoFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            process.waitFor();
            // If output contains "audio", there's an audio track
            return output.toString().contains("audio");
        } catch (Exception e) {
            logger.warn("Could not check audio track: {}", e.getMessage());
            // Assume there's audio and let extraction fail if not
            return true;
        }
    }

    /**
     * Extract audio from video file synchronously.
     * @param videoFile Source video file
     * @param progressCallback Progress updates (0.0 - 1.0), can be null
     * @return ExtractionResult with success status and audio file or error
     */
    public static ExtractionResult extractAudio(File videoFile, Consumer<Double> progressCallback) {
        try {
            // First check if video has audio
            if (!hasAudioTrack(videoFile)) {
                return ExtractionResult.noAudio();
            }

            // Create temp file for extracted audio (caller responsible for cleanup via cleanupTempAudioFile)
            File outputFile = ConfigManager.createTempFile("whisperdog_extract_", ".wav");

            logger.info("Extracting audio from {} to {}", videoFile.getName(), outputFile.getName());

            // Get video duration for progress calculation
            double durationSeconds = getVideoDuration(videoFile);

            // Build ffmpeg command
            // -y: Overwrite output
            // -i: Input file
            // -vn: No video
            // -acodec pcm_s16le: 16-bit PCM (WhisperDog standard)
            // -ar 16000: 16kHz sample rate (OpenAI optimal)
            // -ac 1: Mono audio
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", videoFile.getAbsolutePath(),
                "-vn",
                "-acodec", "pcm_s16le",
                "-ar", "16000",
                "-ac", "1",
                "-progress", "pipe:1",  // Output progress to stdout
                outputFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output and parse progress
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (progressCallback != null && durationSeconds > 0) {
                        // Try to parse time from progress output
                        if (line.startsWith("out_time=")) {
                            String timeStr = line.substring(9);
                            double currentSeconds = parseTimeToSeconds(timeStr);
                            if (currentSeconds >= 0) {
                                double progress = Math.min(currentSeconds / durationSeconds, 1.0);
                                progressCallback.accept(progress);
                            }
                        }
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                logger.error("FFmpeg extraction failed with exit code: {}", exitCode);
                // Clean up failed output
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                return ExtractionResult.failure("FFmpeg extraction failed with code: " + exitCode);
            }

            // Verify output file exists and has content
            if (!outputFile.exists() || outputFile.length() == 0) {
                logger.error("Extraction produced empty or missing file");
                return ExtractionResult.failure("Extraction produced empty file");
            }

            if (progressCallback != null) {
                progressCallback.accept(1.0);
            }

            logger.info("Successfully extracted audio: {} bytes", outputFile.length());
            return ExtractionResult.success(outputFile);

        } catch (Exception e) {
            logger.error("Failed to extract audio: {}", e.getMessage(), e);
            return ExtractionResult.failure("Failed to extract audio: " + e.getMessage());
        }
    }

    /**
     * Extract audio from video file asynchronously.
     * @param videoFile Source video file
     * @param progressCallback Progress updates (0.0 - 1.0), can be null
     * @return CompletableFuture with ExtractionResult
     */
    public static CompletableFuture<ExtractionResult> extractAudioAsync(
            File videoFile, Consumer<Double> progressCallback) {
        return CompletableFuture.supplyAsync(() -> extractAudio(videoFile, progressCallback));
    }

    /**
     * Get video duration in seconds using ffprobe.
     * @param videoFile The video file
     * @return Duration in seconds, or -1 if unable to determine
     */
    private static double getVideoDuration(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                videoFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line.trim());
                }
            }

            process.waitFor();
            return Double.parseDouble(output.toString());
        } catch (Exception e) {
            logger.debug("Could not determine video duration: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Parse ffmpeg time string (HH:MM:SS.ms or seconds) to seconds.
     * @param timeStr Time string like "00:01:23.456" or "83.456"
     * @return Time in seconds, or -1 if parsing fails
     */
    private static double parseTimeToSeconds(String timeStr) {
        try {
            if (timeStr.contains(":")) {
                // Format: HH:MM:SS.ms
                String[] parts = timeStr.split(":");
                if (parts.length == 3) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    double seconds = Double.parseDouble(parts[2]);
                    return hours * 3600 + minutes * 60 + seconds;
                }
            } else {
                // Format: seconds
                return Double.parseDouble(timeStr);
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        return -1;
    }

    /**
     * Merge two audio tracks (mic + system) into a single WAV file.
     * Uses ffmpeg amix filter to combine both sources.
     * Output is 16kHz mono 16-bit PCM (standard for transcription).
     *
     * @param micTrack Microphone audio file
     * @param systemTrack System audio file
     * @return Merged audio file, or null if merge fails
     */
    public static File mergeAudioTracks(File micTrack, File systemTrack) {
        try {
            // Caller responsible for cleanup (files match whisperdog_* pattern)
            File outputFile = ConfigManager.createTempFile("whisperdog_merged_", ".wav");

            logger.info("Merging audio tracks: {} + {} -> {}",
                micTrack.getName(), systemTrack.getName(), outputFile.getName());

            // Requires ffmpeg 4.4+ for normalize=0 (preserves original levels)
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", micTrack.getAbsolutePath(),
                "-i", systemTrack.getAbsolutePath(),
                "-filter_complex", "[0:a][1:a]amix=inputs=2:duration=longest:normalize=0",
                "-acodec", "pcm_s16le",
                "-ar", "16000",
                "-ac", "1",
                outputFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Consume output in daemon thread to prevent blocking on waitFor
            Thread outputDrain = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    while (reader.readLine() != null) {
                        // Consume
                    }
                } catch (Exception e) {
                    // Process destroyed or stream closed
                }
            });
            outputDrain.setDaemon(true);
            outputDrain.start();

            boolean finished = process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                logger.error("FFmpeg merge timed out after 120s, killing process");
                process.destroyForcibly();
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                return null;
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                logger.error("FFmpeg merge failed with exit code: {} (requires ffmpeg 4.4+)", exitCode);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                return null;
            }

            if (!outputFile.exists() || outputFile.length() == 0) {
                logger.error("Merge produced empty or missing file");
                return null;
            }

            logger.info("Successfully merged audio tracks: {} bytes", outputFile.length());
            return outputFile;

        } catch (Exception e) {
            logger.error("Failed to merge audio tracks: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get supported video extensions as a formatted string for display.
     * @return String like "MP4, MOV, MKV, AVI, WEBM"
     */
    public static String getSupportedFormatsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < VIDEO_EXTENSIONS.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(VIDEO_EXTENSIONS[i].substring(1).toUpperCase());
        }
        return sb.toString();
    }
}
