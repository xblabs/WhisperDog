package org.whisperdog.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.ConfigManager;
import org.whisperdog.recording.AudioRecorder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
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
        String tempDir = System.getProperty("java.io.tmpdir");

        // Prepare mic track file
        micTrackFile = new File(tempDir, "whisperdog_mic_" + timeStamp + ".wav");
        micTrackFile.deleteOnExit();

        // Initialize mic recorder
        micRecorder = new AudioRecorder(micTrackFile, configManager);

        // Initialize system audio if requested and available
        systemAudioEnabled.set(false);
        if (enableSystemAudio && isSystemAudioAvailable()) {
            systemCapture = new SystemAudioCapture();
            boolean initialized = (preferredLoopbackDevice != null && !preferredLoopbackDevice.isEmpty())
                ? systemCapture.initialize(preferredLoopbackDevice)
                : systemCapture.initialize();  // Auto-detect default output device
            if (initialized) {
                systemTrackFile = new File(tempDir, "whisperdog_system_" + timeStamp + ".wav");
                systemTrackFile.deleteOnExit();
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
            systemAudioEnabled.get() ? systemTrackFile.getName() : "disabled");
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

        // Stop system audio and save to file
        if (systemAudioEnabled.get() && systemCapture != null) {
            try {
                logger.info("System audio diagnostics: {}", systemCapture.getDiagnostics());
                byte[] systemAudioData = systemCapture.stop();
                if (systemAudioData.length > 0) {
                    writeWavFile(systemTrackFile, systemAudioData, 16000, 1);
                    logger.info("System audio saved: {} bytes, {}s",
                        systemAudioData.length,
                        String.format("%.1f", systemAudioData.length / 2.0 / 16000));
                }
            } catch (Exception e) {
                logger.error("Failed to save system audio: {}", e.getMessage(), e);
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

            // Create new file if needed
            if (systemTrackFile == null) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String tempDir = System.getProperty("java.io.tmpdir");
                systemTrackFile = new File(tempDir, "whisperdog_system_" + timeStamp + ".wav");
                systemTrackFile.deleteOnExit();
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
                byte[] partialData = systemCapture.stop();
                systemCapture.dispose();
                systemCapture = null;
                // Note: partial data is discarded when disabled mid-recording
                logger.info("System audio capture disabled mid-recording ({} bytes captured)",
                    partialData.length);
            }
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
        return systemAudioEnabled.get() ? systemTrackFile : null;
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

    /**
     * Write PCM audio data to a WAV file.
     */
    private void writeWavFile(File file, byte[] pcmData, int sampleRate, int channels)
            throws IOException {
        int byteRate = sampleRate * channels * 2;
        int blockAlign = channels * 2;

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            // RIFF header
            out.writeBytes("RIFF");
            out.writeInt(Integer.reverseBytes(36 + pcmData.length));
            out.writeBytes("WAVE");

            // fmt chunk
            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16)); // chunk size
            out.writeShort(Short.reverseBytes((short) 1)); // PCM
            out.writeShort(Short.reverseBytes((short) channels));
            out.writeInt(Integer.reverseBytes(sampleRate));
            out.writeInt(Integer.reverseBytes(byteRate));
            out.writeShort(Short.reverseBytes((short) blockAlign));
            out.writeShort(Short.reverseBytes((short) 16)); // bits per sample

            // data chunk
            out.writeBytes("data");
            out.writeInt(Integer.reverseBytes(pcmData.length));
            out.write(pcmData);
        }
    }
}
