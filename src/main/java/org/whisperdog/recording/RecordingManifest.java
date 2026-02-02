package org.whisperdog.recording;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manages the manifest of retained recordings.
 * Stores metadata about each recording in a JSON file.
 */
public class RecordingManifest {
    private static final Logger logger = LogManager.getLogger(RecordingManifest.class);
    private static final String MANIFEST_FILENAME = "manifest.json";
    private static final Pattern RECORDING_FILENAME_PATTERN = Pattern.compile("recording_(\\d{8}_\\d{6})\\.wav");

    private final File manifestFile;
    private final File recordingsDir;
    private final Gson gson;
    private List<RecordingEntry> entries;
    private final Object saveLock = new Object();

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
        private String transcriptionFile;
        private boolean dualSource;
        private boolean imported;  // True for files imported/recovered from disk (not recorded live)
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

        public String getTranscriptionFile() {
            return transcriptionFile;
        }

        public void setTranscriptionFile(String transcriptionFile) {
            this.transcriptionFile = transcriptionFile;
        }

        public boolean isDualSource() {
            return dualSource;
        }

        public void setDualSource(boolean dualSource) {
            this.dualSource = dualSource;
        }

        public boolean isImported() {
            return imported;
        }

        public void setImported(boolean imported) {
            this.imported = imported;
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
        this.recordingsDir = recordingsDir;
        this.manifestFile = new File(recordingsDir, MANIFEST_FILENAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.entries = new ArrayList<>();
        load();
    }

    /**
     * Loads the manifest from disk.
     */
    public void load() {
        synchronized (saveLock) {
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
    }

    /**
     * Saves the manifest to disk with synchronization and Windows-safe fallback.
     */
    public void save() {
        synchronized (saveLock) {
            File tempFile = new File(manifestFile.getParentFile(), MANIFEST_FILENAME + ".tmp");

            try (Writer writer = new FileWriter(tempFile)) {
                ManifestData data = new ManifestData();
                data.recordings = new ArrayList<>(entries); // Copy to avoid concurrent modification
                data.version = 1;
                data.lastUpdated = System.currentTimeMillis();
                gson.toJson(data, writer);
                writer.flush();
            } catch (IOException e) {
                logger.error("Failed to write temp manifest file", e);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                return;
            }

            // Try atomic move first, fall back to copy+delete for Windows file locking
            try {
                Files.move(tempFile.toPath(), manifestFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Saved manifest with {} recordings", entries.size());
            } catch (IOException e) {
                // Windows file locking fallback: copy content then delete temp
                logger.debug("Atomic move failed, using fallback save: {}", e.getMessage());
                try {
                    Files.copy(tempFile.toPath(), manifestFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    tempFile.delete();
                    logger.debug("Saved manifest with {} recordings (fallback)", entries.size());
                } catch (IOException e2) {
                    logger.error("Failed to save recording manifest", e2);
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                }
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
     * Scans the recordings directory and reconciles manifest with actual files.
     * - Removes entries for files that no longer exist on disk
     * - Adds entries for orphaned files (files not in manifest)
     *
     * @return true if any changes were made
     */
    public boolean scanAndReconcile() {
        if (!recordingsDir.exists() || !recordingsDir.isDirectory()) {
            return false;
        }

        // Get all main recording .wav files in directory (not channel files)
        File[] wavFiles = recordingsDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".wav") &&
                name.startsWith("recording_") &&
                !name.contains("_mic") &&
                !name.contains("_system"));

        if (wavFiles == null) {
            return false;
        }

        Set<String> filesOnDisk = Arrays.stream(wavFiles)
                .map(File::getName)
                .collect(Collectors.toSet());

        Set<String> filesInManifest = entries.stream()
                .map(RecordingEntry::getFilename)
                .collect(Collectors.toSet());

        boolean changed = false;

        // Remove manifest entries for missing files
        int removedCount = entries.size();
        entries.removeIf(entry -> !filesOnDisk.contains(entry.getFilename()));
        removedCount = removedCount - entries.size();
        if (removedCount > 0) {
            logger.info("Removed {} manifest entries for missing files", removedCount);
            changed = true;
        }

        // Add entries for orphaned files (files on disk not in manifest)
        int addedCount = 0;
        for (String filename : filesOnDisk) {
            if (!filesInManifest.contains(filename)) {
                RecordingEntry orphan = createEntryFromFile(new File(recordingsDir, filename));
                if (orphan != null) {
                    entries.add(orphan);
                    addedCount++;
                }
            }
        }
        if (addedCount > 0) {
            logger.info("Added {} manifest entries for orphaned files", addedCount);
            changed = true;
        }

        if (changed) {
            save();
        }

        return changed;
    }

    /**
     * Creates a recording entry from an orphaned file on disk.
     * Parses timestamp from filename and reads file metadata.
     */
    private RecordingEntry createEntryFromFile(File file) {
        Matcher matcher = RECORDING_FILENAME_PATTERN.matcher(file.getName());
        if (!matcher.matches()) {
            logger.warn("Cannot parse orphaned file: {}", file.getName());
            return null;
        }

        String timestampStr = matcher.group(1); // YYYYMMDD_HHmmss
        long timestamp;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
            timestamp = sdf.parse(timestampStr).getTime();
        } catch (java.text.ParseException e) {
            logger.warn("Cannot parse timestamp from filename: {}", file.getName());
            timestamp = file.lastModified();
        }

        RecordingEntry entry = new RecordingEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setFilename(file.getName());
        entry.setTimestamp(timestamp);
        entry.setFileSizeBytes(file.length());

        // Calculate duration from audio file
        String format = AudioFileAnalyzer.detectFormat(file);
        Float durationSeconds = AudioFileAnalyzer.estimateDuration(file, format);
        entry.setDurationMs(durationSeconds != null ? (long)(durationSeconds * 1000) : 0);

        entry.setTranscriptionPreview("(Recovered recording - no transcription)");
        entry.setDualSource(false); // Unknown
        entry.setImported(true);  // Mark as imported/recovered

        // Check if channel files exist
        String baseName = file.getName().replace(".wav", "");
        File micFile = new File(recordingsDir, baseName + "_mic.wav");
        File systemFile = new File(recordingsDir, baseName + "_system.wav");
        if (micFile.exists()) {
            entry.setMicChannelFile(micFile.getName());
        }
        if (systemFile.exists()) {
            entry.setSystemChannelFile(systemFile.getName());
            entry.setDualSource(true);
        }

        logger.debug("Created entry for orphaned file: {}", file.getName());
        return entry;
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
