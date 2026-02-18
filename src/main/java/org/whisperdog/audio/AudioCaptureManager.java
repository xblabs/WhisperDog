package org.whisperdog.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.ConfigManager;
import org.whisperdog.recording.AudioRecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages dual-source audio capture: microphone + system audio (WASAPI loopback).
 * Coordinates synchronized start/stop and provides access to both audio tracks.
 */
public class AudioCaptureManager {
    private static final Logger logger = LogManager.getLogger(AudioCaptureManager.class);

    private final ConfigManager configManager;

    // Capture components
    private AudioRecorder micRecorder;
    private SystemAudioCapture systemCapture;

    // Output files
    private File micTrackFile;
    private File systemTrackFile;

    // State tracking
    private final AtomicBoolean capturing = new AtomicBoolean(false);
    private final AtomicBoolean systemAudioEnabled = new AtomicBoolean(false);
    private final AtomicLong captureStartTimestamp = new AtomicLong(0);

    // Thread for mic recording (AudioRecorder.start() blocks)
    private Thread micRecordingThread;

    // Preferred loopback device name (partial match)
    private String preferredLoopbackDevice;

    public AudioCaptureManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Set the preferred loopback device for system audio capture.
     * @param deviceName Partial device name to match (e.g. "Sound Blaster", "Speakers")
     */
    public void setPreferredLoopbackDevice(String deviceName) {
        this.preferredLoopbackDevice = deviceName;
    }

    /**
     * Check if system audio capture is available on this platform.
     */
    public boolean isSystemAudioAvailable() {
        return SystemAudioCapture.isAvailable();
    }

    /**
     * Start dual-source capture (mic + optional system audio).
     * @param enableSystemAudio Whether to capture system audio alongside microphone
     * @throws Exception if capture cannot be started
     */
    public void startCapture(boolean enableSystemAudio) throws Exception {
        if (capturing.get()) {
            logger.warn("Capture already in progress");
            return;
        }

        // Generate timestamp for file naming
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String tempDir = ConfigManager.getTempDirectory().getPath();

        // Prepare mic track file (cleaned up by cleanupTempFiles() or AudioTranscriptionWorker)
        micTrackFile = new File(tempDir, "whisperdog_mic_" + timeStamp + ".wav");

        // Initialize mic recorder
        micRecorder = new AudioRecorder(micTrackFile, configManager);

        // Initialize system audio if requested and available
        systemAudioEnabled.set(false);
        systemTrackFile = null;
        if (enableSystemAudio && isSystemAudioAvailable()) {
            systemCapture = new SystemAudioCapture();
            boolean initialized = (preferredLoopbackDevice != null && !preferredLoopbackDevice.isEmpty())
                ? systemCapture.initialize(preferredLoopbackDevice)
                : systemCapture.initialize();  // Auto-detect default output device
            if (initialized) {
                systemAudioEnabled.set(true);
                logger.info("System audio capture initialized");
            } else {
                logger.warn("Failed to initialize system audio capture, continuing with mic only");
                systemCapture = null;
            }
        }

        // Record the synchronized start timestamp
        captureStartTimestamp.set(System.currentTimeMillis());
        capturing.set(true);

        // Start system audio capture first (non-blocking, uses WASAPI loopback)
        if (systemAudioEnabled.get() && systemCapture != null) {
            try {
                systemCapture.start();
                logger.info("System audio capture started");
            } catch (Exception e) {
                logger.error("Failed to start system audio capture: {}", e.getMessage(), e);
                systemAudioEnabled.set(false);
                systemCapture.dispose();
                systemCapture = null;
            }
        }

        // Start mic recording in separate thread (AudioRecorder.start() blocks)
        micRecordingThread = new Thread(() -> {
            try {
                micRecorder.start();
            } catch (Exception e) {
                logger.error("Mic recording error: {}", e.getMessage(), e);
            }
        }, "MicRecordingThread");
        micRecordingThread.start();

        logger.info("Capture started - mic: {}, system: {}",
            micTrackFile.getName(),
            systemAudioEnabled.get() ? "enabled" : "disabled");
    }

    /**
     * Stop capture and finalize audio files.
     * @return The microphone track file (primary output)
     */
    public File stopCapture() {
        if (!capturing.get()) {
            logger.warn("No capture in progress");
            return null;
        }

        capturing.set(false);
        long captureEndTime = System.currentTimeMillis();
        long durationMs = captureEndTime - captureStartTimestamp.get();

        // Stop mic recording
        if (micRecorder != null) {
            micRecorder.stop();
            logger.info("Mic recording stopped");
        }

        // Stop system audio and capture resulting file
        if (systemAudioEnabled.get() && systemCapture != null) {
            try {
                logger.info("System audio diagnostics: {}", systemCapture.getDiagnostics());
                File capturedSystemFile = systemCapture.stop();
                if (capturedSystemFile != null && capturedSystemFile.exists()) {
                    systemTrackFile = capturedSystemFile;
                    long pcmBytes = Math.max(0, capturedSystemFile.length() - 44);
                    logger.info("System audio saved: {} bytes, {}s",
                        pcmBytes,
                        String.format("%.1f", pcmBytes / 2.0 / 16000));
                } else {
                    systemTrackFile = null;
                }

                if (systemCapture.hasWriteError()) {
                    logger.warn("System audio capture reported write error: {}",
                        systemCapture.getWriteErrorMessage());
                }
            } catch (Exception e) {
                logger.error("Failed to finalize system audio capture: {}", e.getMessage(), e);
            } finally {
                systemCapture.dispose();
                systemCapture = null;
            }
        }

        logger.info("Capture stopped - duration: {}ms", durationMs);
        return micTrackFile;
    }

    /**
     * Toggle system audio capture on/off during an active recording.
     * @param enabled Whether system audio should be enabled
     * @return true if toggle was successful
     */
    public boolean toggleSystemAudio(boolean enabled) {
        if (!capturing.get()) {
            logger.warn("Cannot toggle system audio - no capture in progress");
            return false;
        }

        if (enabled == systemAudioEnabled.get()) {
            // Already in requested state
            return true;
        }

        if (enabled) {
            // Enable system audio mid-recording
            if (!isSystemAudioAvailable()) {
                logger.warn("System audio not available on this platform");
                return false;
            }

            systemCapture = new SystemAudioCapture();
            boolean initialized = (preferredLoopbackDevice != null && !preferredLoopbackDevice.isEmpty())
                ? systemCapture.initialize(preferredLoopbackDevice)
                : systemCapture.initialize();
            if (!initialized) {
                logger.error("Failed to initialize system audio capture");
                systemCapture = null;
                return false;
            }

            try {
                systemCapture.start();
                systemAudioEnabled.set(true);
                logger.info("System audio capture enabled mid-recording");
                return true;
            } catch (Exception e) {
                logger.error("Failed to start system audio: {}", e.getMessage(), e);
                systemCapture.dispose();
                systemCapture = null;
                return false;
            }
        } else {
            // Disable system audio mid-recording
            if (systemCapture != null) {
                File partialFile = systemCapture.stop();
                systemCapture.dispose();
                systemCapture = null;
                long pcmBytes = (partialFile != null && partialFile.exists())
                    ? Math.max(0, partialFile.length() - 44)
                    : 0;
                // Note: partial data is discarded when disabled mid-recording.
                if (partialFile != null && partialFile.exists() && !partialFile.delete()) {
                    logger.debug("Could not delete partial system audio file: {}", partialFile.getAbsolutePath());
                }
                logger.info("System audio capture disabled mid-recording ({} bytes captured)",
                    pcmBytes);
            }
            systemTrackFile = null;
            systemAudioEnabled.set(false);
            return true;
        }
    }

    /**
     * Check if capture is currently in progress.
     */
    public boolean isCapturing() {
        return capturing.get();
    }

    /**
     * Check if system audio is currently being captured.
     */
    public boolean isSystemAudioEnabled() {
        return systemAudioEnabled.get();
    }

    /**
     * Get the timestamp when capture started.
     */
    public long getCaptureStartTimestamp() {
        return captureStartTimestamp.get();
    }

    /**
     * Get the microphone track file.
     * Only valid after stopCapture() is called.
     */
    public File getMicTrackFile() {
        return micTrackFile;
    }

    /**
     * Get the system audio track file.
     * Only valid after stopCapture() is called and system audio was enabled.
     */
    public File getSystemTrackFile() {
        return systemTrackFile;
    }

    /**
     * Clean up temporary audio files.
     */
    public void cleanupTempFiles() {
        try {
            if (micTrackFile != null && micTrackFile.exists()) {
                if (micTrackFile.delete()) {
                    logger.debug("Deleted temp mic file: {}", micTrackFile.getName());
                }
                micTrackFile = null;
            }
            if (systemTrackFile != null && systemTrackFile.exists()) {
                if (systemTrackFile.delete()) {
                    logger.debug("Deleted temp system file: {}", systemTrackFile.getName());
                }
                systemTrackFile = null;
            }
        } catch (Exception e) {
            logger.warn("Error cleaning up temp files: {}", e.getMessage());
        }
    }

}
