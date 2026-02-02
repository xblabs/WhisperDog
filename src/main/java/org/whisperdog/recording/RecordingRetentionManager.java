package org.whisperdog.recording;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.ConfigManager;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Manages retention of audio recordings.
 * Copies recordings to a persistent location and maintains a manifest.
 */
public class RecordingRetentionManager {
    private static final Logger logger = LogManager.getLogger(RecordingRetentionManager.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final ConfigManager configManager;
    private RecordingManifest manifest;

    /**
     * Creates a new RecordingRetentionManager.
     *
     * @param configManager The configuration manager
     */
    public RecordingRetentionManager(ConfigManager configManager) {
        this.configManager = configManager;
        initializeManifest();
    }

    /**
     * Initializes or reloads the manifest.
     */
    private void initializeManifest() {
        File recordingsDir = configManager.getRecordingsDirectory();
        this.manifest = new RecordingManifest(recordingsDir);
    }

    /**
     * Retains a recording by copying it to the persistent storage location.
     *
     * @param mergedAudioFile The merged audio file to retain
     * @param micChannelFile The microphone channel file (may be null for single-source)
     * @param systemChannelFile The system audio channel file (may be null for single-source)
     * @param durationMs The recording duration in milliseconds
     * @param fullTranscription The full transcription text
     * @param isDualSource Whether this is a dual-source recording
     * @return The recording entry, or null if retention is disabled or failed
     */
    public synchronized RecordingManifest.RecordingEntry retainRecording(
            File mergedAudioFile,
            File micChannelFile,
            File systemChannelFile,
            long durationMs,
            String fullTranscription,
            boolean isDualSource) {

        if (!configManager.isRecordingRetentionEnabled()) {
            logger.debug("Recording retention is disabled");
            return null;
        }

        if (mergedAudioFile == null || !mergedAudioFile.exists()) {
            logger.warn("Cannot retain recording: merged audio file is null or does not exist");
            return null;
        }

        try {
            File recordingsDir = configManager.getRecordingsDirectory();
            String timestamp = DATE_FORMAT.format(new Date());
            String baseFilename = "recording_" + timestamp;
            String id = UUID.randomUUID().toString();

            // Copy merged file
            String mergedFilename = baseFilename + ".wav";
            File destMergedFile = new File(recordingsDir, mergedFilename);
            Files.copy(mergedAudioFile.toPath(), destMergedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Retained recording: {}", destMergedFile.getAbsolutePath());

            // Save full transcription to text file
            String transcriptionFilename = null;
            if (fullTranscription != null && !fullTranscription.isEmpty()) {
                transcriptionFilename = baseFilename + ".txt";
                File transcriptionFile = new File(recordingsDir, transcriptionFilename);
                Files.writeString(transcriptionFile.toPath(), fullTranscription);
                logger.debug("Saved transcription: {}", transcriptionFile.getAbsolutePath());
            }

            // Create manifest entry
            RecordingManifest.RecordingEntry entry = new RecordingManifest.RecordingEntry();
            entry.setId(id);
            entry.setFilename(mergedFilename);
            entry.setTimestamp(System.currentTimeMillis());
            entry.setDurationMs(durationMs);
            entry.setFileSizeBytes(destMergedFile.length());
            entry.setTranscriptionPreview(truncatePreview(fullTranscription, 300));
            entry.setTranscriptionFile(transcriptionFilename);
            entry.setDualSource(isDualSource);

            // Copy channel files if enabled and they exist
            if (configManager.isRetainChannelFilesEnabled()) {
                // Copy mic channel file
                if (micChannelFile != null && micChannelFile.exists()) {
                    String micFilename = baseFilename + "_mic.wav";
                    File destMicFile = new File(recordingsDir, micFilename);
                    Files.copy(micChannelFile.toPath(), destMicFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    entry.setMicChannelFile(micFilename);
                    logger.debug("Retained mic channel file: {}", destMicFile.getAbsolutePath());
                }

                // Copy system channel file
                if (systemChannelFile != null && systemChannelFile.exists()) {
                    String systemFilename = baseFilename + "_system.wav";
                    File destSystemFile = new File(recordingsDir, systemFilename);
                    Files.copy(systemChannelFile.toPath(), destSystemFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    entry.setSystemChannelFile(systemFilename);
                    logger.debug("Retained system channel file: {}", destSystemFile.getAbsolutePath());
                }
            }

            // Add to manifest
            manifest.addRecording(entry);

            // Prune old recordings
            pruneOldRecordings();

            return entry;

        } catch (IOException e) {
            logger.error("Failed to retain recording", e);
            return null;
        }
    }

    /**
     * Prunes old recordings when the count exceeds the configured limit.
     */
    private void pruneOldRecordings() {
        int maxCount = configManager.getRecordingRetentionCount();
        List<RecordingManifest.RecordingEntry> removed = manifest.pruneToCount(maxCount);

        // Delete the actual files for removed entries
        File recordingsDir = configManager.getRecordingsDirectory();
        for (RecordingManifest.RecordingEntry entry : removed) {
            deleteRecordingFiles(recordingsDir, entry);
        }
    }

    /**
     * Deletes a recording by ID.
     *
     * @param id The ID of the recording to delete
     * @return true if deleted successfully
     */
    public boolean deleteRecording(String id) {
        RecordingManifest.RecordingEntry entry = manifest.removeRecording(id);
        if (entry == null) {
            logger.warn("Recording not found for deletion: {}", id);
            return false;
        }

        File recordingsDir = configManager.getRecordingsDirectory();
        deleteRecordingFiles(recordingsDir, entry);
        return true;
    }

    /**
     * Deletes the files associated with a recording entry.
     */
    private void deleteRecordingFiles(File recordingsDir, RecordingManifest.RecordingEntry entry) {
        // Delete merged file
        if (entry.getFilename() != null) {
            File mergedFile = new File(recordingsDir, entry.getFilename());
            if (mergedFile.exists()) {
                if (mergedFile.delete()) {
                    logger.debug("Deleted recording file: {}", mergedFile.getAbsolutePath());
                } else {
                    logger.warn("Failed to delete recording file: {}", mergedFile.getAbsolutePath());
                }
            }
        }

        // Delete mic channel file (null-safe)
        if (entry.getMicChannelFile() != null) {
            File micFile = new File(recordingsDir, entry.getMicChannelFile());
            if (micFile.exists()) {
                if (micFile.delete()) {
                    logger.debug("Deleted mic channel file: {}", micFile.getAbsolutePath());
                } else {
                    logger.warn("Failed to delete mic channel file: {}", micFile.getAbsolutePath());
                }
            }
        }

        // Delete system channel file (null-safe)
        if (entry.getSystemChannelFile() != null) {
            File systemFile = new File(recordingsDir, entry.getSystemChannelFile());
            if (systemFile.exists()) {
                if (systemFile.delete()) {
                    logger.debug("Deleted system channel file: {}", systemFile.getAbsolutePath());
                } else {
                    logger.warn("Failed to delete system channel file: {}", systemFile.getAbsolutePath());
                }
            }
        }

        // Delete transcription file (null-safe)
        if (entry.getTranscriptionFile() != null) {
            File transcriptionFile = new File(recordingsDir, entry.getTranscriptionFile());
            if (transcriptionFile.exists()) {
                if (transcriptionFile.delete()) {
                    logger.debug("Deleted transcription file: {}", transcriptionFile.getAbsolutePath());
                } else {
                    logger.warn("Failed to delete transcription file: {}", transcriptionFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Opens the recordings folder in the system file manager.
     */
    public void openInFileManager() {
        File recordingsDir = configManager.getRecordingsDirectory();
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(recordingsDir);
            } else {
                logger.warn("Desktop is not supported on this platform");
            }
        } catch (IOException e) {
            logger.error("Failed to open recordings folder", e);
        }
    }

    /**
     * Gets all recordings from the manifest.
     *
     * @return List of recording entries (sorted by timestamp, newest first)
     */
    public List<RecordingManifest.RecordingEntry> getRecordings() {
        return manifest.getRecordings();
    }

    /**
     * Gets the audio file for a recording entry.
     *
     * @param entry The recording entry
     * @return The audio file, or null if it doesn't exist
     */
    public File getAudioFile(RecordingManifest.RecordingEntry entry) {
        if (entry == null || entry.getFilename() == null) {
            return null;
        }
        File recordingsDir = configManager.getRecordingsDirectory();
        File audioFile = new File(recordingsDir, entry.getFilename());
        return audioFile.exists() ? audioFile : null;
    }

    /**
     * Gets the mic channel file for a recording entry.
     *
     * @param entry The recording entry
     * @return The mic channel file, or null if it doesn't exist
     */
    public File getMicChannelFile(RecordingManifest.RecordingEntry entry) {
        if (entry == null || entry.getMicChannelFile() == null) {
            return null;
        }
        File recordingsDir = configManager.getRecordingsDirectory();
        File micFile = new File(recordingsDir, entry.getMicChannelFile());
        return micFile.exists() ? micFile : null;
    }

    /**
     * Gets the system channel file for a recording entry.
     *
     * @param entry The recording entry
     * @return The system channel file, or null if it doesn't exist
     */
    public File getSystemChannelFile(RecordingManifest.RecordingEntry entry) {
        if (entry == null || entry.getSystemChannelFile() == null) {
            return null;
        }
        File recordingsDir = configManager.getRecordingsDirectory();
        File systemFile = new File(recordingsDir, entry.getSystemChannelFile());
        return systemFile.exists() ? systemFile : null;
    }

    /**
     * Gets the full transcription text for a recording entry.
     *
     * @param entry The recording entry
     * @return The full transcription text, or null if not available
     */
    public String getFullTranscription(RecordingManifest.RecordingEntry entry) {
        if (entry == null || entry.getTranscriptionFile() == null) {
            return null;
        }
        File recordingsDir = configManager.getRecordingsDirectory();
        File transcriptionFile = new File(recordingsDir, entry.getTranscriptionFile());
        if (!transcriptionFile.exists()) {
            return null;
        }
        try {
            return Files.readString(transcriptionFile.toPath());
        } catch (IOException e) {
            logger.error("Failed to read transcription file: {}", transcriptionFile.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Gets the count of retained recordings.
     *
     * @return The number of recordings
     */
    public int getRecordingCount() {
        return manifest.getCount();
    }

    /**
     * Checks if recording retention is currently enabled.
     *
     * @return true if retention is enabled
     */
    public boolean isRetentionEnabled() {
        return configManager.isRecordingRetentionEnabled();
    }

    /**
     * Reloads the manifest from disk and reconciles with filesystem.
     * Call this if the storage path has changed or to sync with actual files.
     */
    public synchronized void reloadManifest() {
        // Reload existing manifest instead of creating new instance to preserve synchronization
        manifest.load();
        manifest.scanAndReconcile();
        // Regenerate previews to use updated length (300 chars)
        regeneratePreviews();
    }

    /**
     * Regenerates metadata for all recordings: previews (300 chars) and missing durations.
     */
    private void regeneratePreviews() {
        boolean changed = false;
        for (RecordingManifest.RecordingEntry entry : manifest.getRecordings()) {
            // Regenerate preview from full transcription
            String fullText = getFullTranscription(entry);
            if (fullText != null && !fullText.isEmpty()) {
                String newPreview = truncatePreview(fullText, 300);
                String oldPreview = entry.getTranscriptionPreview();
                if (!newPreview.equals(oldPreview)) {
                    entry.setTranscriptionPreview(newPreview);
                    changed = true;
                }
            }

            // Fix missing duration (0 = unknown, recalculate from audio file)
            if (entry.getDurationMs() == 0) {
                File audioFile = getAudioFile(entry);
                if (audioFile != null && audioFile.exists()) {
                    String format = AudioFileAnalyzer.detectFormat(audioFile);
                    Float durationSeconds = AudioFileAnalyzer.estimateDuration(audioFile, format);
                    if (durationSeconds != null && durationSeconds > 0) {
                        entry.setDurationMs((long)(durationSeconds * 1000));
                        changed = true;
                        logger.debug("Fixed duration for {}: {}s", entry.getFilename(), durationSeconds);
                    }
                }
            }
        }
        if (changed) {
            manifest.save();
            logger.info("Regenerated recording metadata");
        }
    }

    /**
     * Truncates a preview string to the specified length.
     */
    private String truncatePreview(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
