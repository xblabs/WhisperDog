package org.whisperdog.recording;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages the manifest of retained recordings.
 * Stores metadata about each recording in a JSON file.
 */
public class RecordingManifest {
    private static final Logger logger = LogManager.getLogger(RecordingManifest.class);
    private static final String MANIFEST_FILENAME = "manifest.json";

    private final File manifestFile;
    private final Gson gson;
    private List<RecordingEntry> entries;

    /**
     * Represents a single recording entry in the manifest.
     */
    public static class RecordingEntry {
        private String id;
        private String filename;
        private long timestamp;
        private long durationMs;
        private long fileSizeBytes;
        private String transcriptionPreview;
        private boolean dualSource;
        private String micChannelFile;
        private String systemChannelFile;

        public RecordingEntry() {
            // Default constructor - fields use default values
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public long getFileSizeBytes() {
            return fileSizeBytes;
        }

        public void setFileSizeBytes(long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
        }

        public String getTranscriptionPreview() {
            return transcriptionPreview;
        }

        public void setTranscriptionPreview(String transcriptionPreview) {
            this.transcriptionPreview = transcriptionPreview;
        }

        public boolean isDualSource() {
            return dualSource;
        }

        public void setDualSource(boolean dualSource) {
            this.dualSource = dualSource;
        }

        public String getMicChannelFile() {
            return micChannelFile;
        }

        public void setMicChannelFile(String micChannelFile) {
            this.micChannelFile = micChannelFile;
        }

        public String getSystemChannelFile() {
            return systemChannelFile;
        }

        public void setSystemChannelFile(String systemChannelFile) {
            this.systemChannelFile = systemChannelFile;
        }

    }

    /**
     * Creates a new RecordingManifest for the given recordings directory.
     *
     * @param recordingsDir The directory where recordings are stored
     */
    public RecordingManifest(File recordingsDir) {
        this.manifestFile = new File(recordingsDir, MANIFEST_FILENAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.entries = new ArrayList<>();
        load();
    }

    /**
     * Loads the manifest from disk.
     */
    public void load() {
        if (!manifestFile.exists()) {
            entries = new ArrayList<>();
            return;
        }

        try (Reader reader = new FileReader(manifestFile)) {
            ManifestData data = gson.fromJson(reader, ManifestData.class);
            if (data != null && data.recordings != null) {
                entries = new ArrayList<>(data.recordings);
            } else {
                entries = new ArrayList<>();
            }
            logger.info("Loaded manifest with {} recordings", entries.size());
        } catch (Exception e) {
            logger.error("Failed to load recording manifest", e);
            entries = new ArrayList<>();
        }
    }

    /**
     * Saves the manifest to disk using atomic write.
     */
    public void save() {
        File tempFile = new File(manifestFile.getParentFile(), MANIFEST_FILENAME + ".tmp");

        try (Writer writer = new FileWriter(tempFile)) {
            ManifestData data = new ManifestData();
            data.recordings = entries;
            data.version = 1;
            data.lastUpdated = System.currentTimeMillis();
            gson.toJson(data, writer);
            writer.flush();

            // Atomic move
            Files.move(tempFile.toPath(), manifestFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Saved manifest with {} recordings", entries.size());
        } catch (IOException e) {
            logger.error("Failed to save recording manifest", e);
            // Clean up temp file on failure
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Adds a recording entry to the manifest.
     *
     * @param entry The recording entry to add
     */
    public void addRecording(RecordingEntry entry) {
        entries.add(entry);
        save();
    }

    /**
     * Removes a recording entry from the manifest by ID.
     *
     * @param id The ID of the recording to remove
     * @return The removed entry, or null if not found
     */
    public RecordingEntry removeRecording(String id) {
        RecordingEntry removed = null;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(id)) {
                removed = entries.remove(i);
                break;
            }
        }
        if (removed != null) {
            save();
        }
        return removed;
    }

    /**
     * Prunes the manifest to keep only the specified number of most recent recordings.
     * Returns the entries that were removed.
     *
     * @param maxCount The maximum number of recordings to keep
     * @return List of entries that were removed (oldest first)
     */
    public List<RecordingEntry> pruneToCount(int maxCount) {
        List<RecordingEntry> removed = new ArrayList<>();

        if (entries.size() <= maxCount) {
            return removed;
        }

        // Sort by timestamp (oldest first)
        entries.sort(Comparator.comparingLong(RecordingEntry::getTimestamp));

        // Remove oldest entries until we're at the limit
        while (entries.size() > maxCount) {
            removed.add(entries.remove(0));
        }

        if (!removed.isEmpty()) {
            save();
            logger.info("Pruned {} old recordings", removed.size());
        }

        return removed;
    }

    /**
     * Gets all recording entries.
     *
     * @return List of all recording entries (sorted by timestamp, newest first)
     */
    public List<RecordingEntry> getRecordings() {
        List<RecordingEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingLong(RecordingEntry::getTimestamp).reversed());
        return sorted;
    }

    /**
     * Gets a recording entry by ID.
     *
     * @param id The recording ID
     * @return The entry, or null if not found
     */
    public RecordingEntry getRecordingById(String id) {
        return entries.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the count of recordings in the manifest.
     *
     * @return The number of recordings
     */
    public int getCount() {
        return entries.size();
    }

    /**
     * Internal class for JSON serialization.
     */
    private static class ManifestData {
        int version;
        long lastUpdated;
        List<RecordingEntry> recordings;
    }
}
