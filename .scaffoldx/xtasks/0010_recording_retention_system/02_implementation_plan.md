# Implementation Plan: Recording Retention System

## Overview

This plan implements a recording retention system with three main components:
1. **Core**: `RecordingRetentionManager` + `RecordingManifest`
2. **Settings**: Configuration properties and settings UI
3. **Browser**: Recordings panel in side menu

## Phase 1: Configuration Settings

### File: `src/main/java/org/whisperdog/ConfigManager.java`

Add retention settings following existing patterns (reference: `isKeepCompressedFile()` at line ~340):

```java
// Location: After line ~380 (after other recording-related settings)

// ============================================================
// Recording Retention Settings
// ============================================================

/**
 * Check if recording retention is enabled.
 * When enabled, recordings are copied to persistent storage after transcription.
 */
public boolean isRecordingRetentionEnabled() {
    return Boolean.parseBoolean(properties.getProperty("recordingRetentionEnabled", "true"));
}

public void setRecordingRetentionEnabled(boolean enabled) {
    properties.setProperty("recordingRetentionEnabled", String.valueOf(enabled));
    saveConfig();
}

/**
 * Get the number of recordings to retain.
 * Default: 20, Range: 1-100
 */
public int getRecordingRetentionCount() {
    String count = properties.getProperty("recordingRetentionCount", "20");
    try {
        int parsed = Integer.parseInt(count);
        return Math.max(1, Math.min(parsed, 100)); // Clamp to 1-100
    } catch (NumberFormatException e) {
        logger.warn("Invalid recordingRetentionCount, using default 20");
        return 20;
    }
}

public void setRecordingRetentionCount(int count) {
    int clamped = Math.max(1, Math.min(count, 100));
    properties.setProperty("recordingRetentionCount", String.valueOf(clamped));
    saveConfig();
}

/**
 * Get the storage path for retained recordings.
 * Default: config directory + "/recordings"
 */
public String getRecordingStoragePath() {
    String path = properties.getProperty("recordingStoragePath", "");
    if (path.isEmpty()) {
        return getConfigDirectory() + File.separator + "recordings";
    }
    return path;
}

public void setRecordingStoragePath(String path) {
    properties.setProperty("recordingStoragePath", path != null ? path : "");
    saveConfig();
}

/**
 * Get the recordings directory as a File object, creating if necessary.
 */
public File getRecordingsDirectory() {
    File dir = new File(getRecordingStoragePath());
    if (!dir.exists()) {
        dir.mkdirs();
    }
    return dir;
}
```

## Phase 2: Data Model

### File: `src/main/java/org/whisperdog/recording/RecordingManifest.java`

```java
package org.whisperdog.recording;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manifest for retained recordings, stored as recordings.json.
 */
public class RecordingManifest {

    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .create();

    @SerializedName("version")
    private String version = "1.0.0";

    @SerializedName("recordings")
    private List<RecordingEntry> recordings = new ArrayList<>();

    // ========================================
    // Entry class
    // ========================================

    public static class RecordingEntry {
        @SerializedName("id")
        private String id;

        @SerializedName("filename")
        private String filename;

        @SerializedName("timestamp")
        private long timestamp;

        @SerializedName("durationMs")
        private long durationMs;

        @SerializedName("fileSizeBytes")
        private long fileSizeBytes;

        @SerializedName("transcriptionPreview")
        private String transcriptionPreview;

        @SerializedName("pipelineResult")
        private String pipelineResult;

        @SerializedName("executionLogSnippet")
        private String executionLogSnippet;

        @SerializedName("hasSystemAudio")
        private boolean hasSystemAudio;

        @SerializedName("micChannelFile")
        private String micChannelFile;

        @SerializedName("systemChannelFile")
        private String systemChannelFile;

        public RecordingEntry() {
            this.id = UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

        public long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

        public String getTranscriptionPreview() { return transcriptionPreview; }
        public void setTranscriptionPreview(String preview) {
            // Limit to 200 chars
            if (preview != null && preview.length() > 200) {
                this.transcriptionPreview = preview.substring(0, 197) + "...";
            } else {
                this.transcriptionPreview = preview;
            }
        }

        public String getPipelineResult() { return pipelineResult; }
        public void setPipelineResult(String result) { this.pipelineResult = result; }

        public String getExecutionLogSnippet() { return executionLogSnippet; }
        public void setExecutionLogSnippet(String snippet) { this.executionLogSnippet = snippet; }

        public boolean isHasSystemAudio() { return hasSystemAudio; }
        public void setHasSystemAudio(boolean hasSystemAudio) { this.hasSystemAudio = hasSystemAudio; }

        public String getMicChannelFile() { return micChannelFile; }
        public void setMicChannelFile(String micChannelFile) { this.micChannelFile = micChannelFile; }

        public String getSystemChannelFile() { return systemChannelFile; }
        public void setSystemChannelFile(String systemChannelFile) { this.systemChannelFile = systemChannelFile; }
    }

    // ========================================
    // Manifest operations
    // ========================================

    public List<RecordingEntry> getRecordings() {
        return recordings;
    }

    public void addRecording(RecordingEntry entry) {
        recordings.add(0, entry); // Add to front (newest first)
    }

    public void removeRecording(String id) {
        recordings.removeIf(e -> e.getId().equals(id));
    }

    public RecordingEntry getRecording(String id) {
        return recordings.stream()
            .filter(e -> e.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Prune recordings to keep only the specified count.
     * Returns list of removed entries (for file cleanup).
     */
    public List<RecordingEntry> pruneToCount(int maxCount) {
        List<RecordingEntry> removed = new ArrayList<>();
        while (recordings.size() > maxCount) {
            removed.add(recordings.remove(recordings.size() - 1)); // Remove oldest
        }
        return removed;
    }

    // ========================================
    // Serialization
    // ========================================

    public static RecordingManifest load(File file) throws IOException {
        if (!file.exists()) {
            return new RecordingManifest();
        }
        try (Reader reader = new FileReader(file)) {
            RecordingManifest manifest = gson.fromJson(reader, RecordingManifest.class);
            return manifest != null ? manifest : new RecordingManifest();
        }
    }

    public void save(File file) throws IOException {
        // Write to temp file first, then rename (atomic)
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer writer = new FileWriter(tempFile)) {
            gson.toJson(this, writer);
        }
        // Rename temp to actual (atomic on most filesystems)
        if (file.exists()) {
            file.delete();
        }
        tempFile.renameTo(file);
    }
}
```

## Phase 3: Retention Manager

### File: `src/main/java/org/whisperdog/recording/RecordingRetentionManager.java`

```java
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

/**
 * Manages retention of audio recordings with metadata.
 * Keeps the last N recordings in a configurable location.
 */
public class RecordingRetentionManager {

    private static final Logger logger = LogManager.getLogger(RecordingRetentionManager.class);
    private static final String MANIFEST_FILE = "recordings.json";
    private static final SimpleDateFormat FILENAME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final ConfigManager configManager;
    private File storageDirectory;
    private RecordingManifest manifest;

    public RecordingRetentionManager(ConfigManager configManager) {
        this.configManager = configManager;
        initializeStorage();
    }

    /**
     * Initialize storage directory and load manifest.
     */
    private void initializeStorage() {
        storageDirectory = configManager.getRecordingsDirectory();
        loadManifest();
    }

    private void loadManifest() {
        try {
            File manifestFile = new File(storageDirectory, MANIFEST_FILE);
            manifest = RecordingManifest.load(manifestFile);
            logger.debug("Loaded {} recordings from manifest", manifest.getRecordings().size());
        } catch (IOException e) {
            logger.warn("Failed to load manifest, starting fresh: {}", e.getMessage());
            manifest = new RecordingManifest();
        }
    }

    private void saveManifest() {
        try {
            File manifestFile = new File(storageDirectory, MANIFEST_FILE);
            manifest.save(manifestFile);
            logger.debug("Saved manifest with {} recordings", manifest.getRecordings().size());
        } catch (IOException e) {
            logger.error("Failed to save manifest: {}", e.getMessage());
        }
    }

    /**
     * Retain a recording after transcription completes.
     *
     * @param sourceAudioFile The audio file that was transcribed (merged file)
     * @param micChannelFile The microphone channel file (may be null if not retaining channels)
     * @param systemChannelFile The system audio channel file (may be null if single-source or not retaining channels)
     * @param transcription The transcription text
     * @param pipelineResult The pipeline result (if any)
     * @param hasSystemAudio Whether dual-source recording was used
     * @param durationMs Recording duration in milliseconds
     * @return The created recording entry, or null if retention failed
     */
    public RecordingManifest.RecordingEntry retainRecording(
            File sourceAudioFile,
            File micChannelFile,
            File systemChannelFile,
            String transcription,
            String pipelineResult,
            boolean hasSystemAudio,
            long durationMs) {

        if (!configManager.isRecordingRetentionEnabled()) {
            logger.debug("Recording retention is disabled");
            return null;
        }

        if (sourceAudioFile == null || !sourceAudioFile.exists()) {
            logger.warn("Source audio file does not exist, cannot retain");
            return null;
        }

        try {
            // Refresh storage directory in case settings changed
            storageDirectory = configManager.getRecordingsDirectory();

            // Generate filename with timestamp
            String timestamp = FILENAME_FORMAT.format(new Date());
            String extension = getExtension(sourceAudioFile.getName());
            String filename = "recording_" + timestamp + extension;
            File destFile = new File(storageDirectory, filename);

            // Handle duplicate filenames (rare, but possible within same second)
            int counter = 1;
            while (destFile.exists()) {
                filename = "recording_" + timestamp + "_" + counter + extension;
                destFile = new File(storageDirectory, filename);
                counter++;
            }

            // Copy merged file to recordings directory
            Files.copy(sourceAudioFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Retained recording: {}", filename);

            // Copy channel files if enabled and available
            String micChannelFilename = null;
            String systemChannelFilename = null;
            if (configManager.isRetainChannelFilesEnabled()) {
                // Copy mic channel file (always exists for dual-source)
                if (micChannelFile != null && micChannelFile.exists()) {
                    String micFilename = "recording_" + timestamp + "_mic" + extension;
                    File micDestFile = new File(storageDirectory, micFilename);
                    Files.copy(micChannelFile.toPath(), micDestFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    micChannelFilename = micFilename;
                    logger.debug("Retained mic channel: {}", micFilename);
                }
                // Copy system channel file (only exists for dual-source)
                if (systemChannelFile != null && systemChannelFile.exists()) {
                    String sysFilename = "recording_" + timestamp + "_system" + extension;
                    File sysDestFile = new File(storageDirectory, sysFilename);
                    Files.copy(systemChannelFile.toPath(), sysDestFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    systemChannelFilename = sysFilename;
                    logger.debug("Retained system channel: {}", sysFilename);
                }
            }

            // Create manifest entry
            RecordingManifest.RecordingEntry entry = new RecordingManifest.RecordingEntry();
            entry.setFilename(filename);
            entry.setDurationMs(durationMs);
            entry.setFileSizeBytes(destFile.length());
            entry.setTranscriptionPreview(transcription);
            entry.setPipelineResult(pipelineResult);
            entry.setHasSystemAudio(hasSystemAudio);
            entry.setMicChannelFile(micChannelFilename);      // null if not retained
            entry.setSystemChannelFile(systemChannelFilename); // null if not retained or single-source
            // executionLogSnippet can be set separately if needed

            // Add to manifest
            manifest.addRecording(entry);

            // Prune old recordings
            int maxCount = configManager.getRecordingRetentionCount();
            List<RecordingManifest.RecordingEntry> removed = manifest.pruneToCount(maxCount);
            for (RecordingManifest.RecordingEntry old : removed) {
                // Delete merged file
                File oldFile = new File(storageDirectory, old.getFilename());
                if (oldFile.exists() && oldFile.delete()) {
                    logger.debug("Pruned old recording: {}", old.getFilename());
                }
                // Delete channel files if they exist
                if (old.getMicChannelFile() != null) {
                    File micFile = new File(storageDirectory, old.getMicChannelFile());
                    if (micFile.exists()) micFile.delete();
                }
                if (old.getSystemChannelFile() != null) {
                    File sysFile = new File(storageDirectory, old.getSystemChannelFile());
                    if (sysFile.exists()) sysFile.delete();
                }
            }

            // Save manifest
            saveManifest();

            return entry;

        } catch (IOException e) {
            logger.error("Failed to retain recording: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get all retained recordings (newest first).
     */
    public List<RecordingManifest.RecordingEntry> getRecordings() {
        return manifest.getRecordings();
    }

    /**
     * Get a specific recording by ID.
     */
    public RecordingManifest.RecordingEntry getRecording(String id) {
        return manifest.getRecording(id);
    }

    /**
     * Get the audio file for a recording entry.
     */
    public File getAudioFile(RecordingManifest.RecordingEntry entry) {
        if (entry == null) return null;
        return new File(storageDirectory, entry.getFilename());
    }

    /**
     * Delete a specific recording.
     */
    public boolean deleteRecording(String id) {
        RecordingManifest.RecordingEntry entry = manifest.getRecording(id);
        if (entry == null) return false;

        // Delete merged file
        File file = new File(storageDirectory, entry.getFilename());
        if (file.exists()) {
            file.delete();
        }

        // Delete channel files if they exist
        if (entry.getMicChannelFile() != null) {
            File micFile = new File(storageDirectory, entry.getMicChannelFile());
            if (micFile.exists()) micFile.delete();
        }
        if (entry.getSystemChannelFile() != null) {
            File sysFile = new File(storageDirectory, entry.getSystemChannelFile());
            if (sysFile.exists()) sysFile.delete();
        }

        // Remove from manifest
        manifest.removeRecording(id);
        saveManifest();

        logger.info("Deleted recording: {}", entry.getFilename());
        return true;
    }

    /**
     * Open recordings directory in system file manager.
     */
    public void openInFileManager() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(storageDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to open recordings folder: {}", e.getMessage());
        }
    }

    /**
     * Get the storage directory.
     */
    public File getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Refresh storage directory (call after settings change).
     */
    public void refreshStorageDirectory() {
        storageDirectory = configManager.getRecordingsDirectory();
        loadManifest();
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".wav";
    }
}
```

## Phase 4: Integration with RecorderForm

### File: `src/main/java/org/whisperdog/recording/RecorderForm.java`

#### 4.1 Add field declarations

**Location**: After line ~120 (with other manager fields like `audioCaptureManager`)

```java
// Recording retention
private RecordingRetentionManager recordingRetentionManager;
private String lastRetainedRecordingId;  // Track for pipeline result update
```

#### 4.2 Initialize in constructor

**Location**: After `configManager` initialization (search for `this.configManager = configManager`)

```java
// Initialize recording retention
recordingRetentionManager = new RecordingRetentionManager(configManager);
```

#### 4.3 Add getter for retention manager

**Location**: With other public getters

```java
public RecordingRetentionManager getRecordingRetentionManager() {
    return recordingRetentionManager;
}
```

#### 4.4 Modify AudioTranscriptionWorker.done()

**Exact Location**: Lines 2273-2278 (the `finally` block)

**Find this exact code**:
```java
            } finally {
                isRecording = false;
                // Clean up temp audio files to prevent accumulation in long-running sessions
                cleanupTempAudioFile(audioFile);
                cleanupTempAudioFile(systemTrackFile);
            }
```

**Replace with**:
```java
            } finally {
                isRecording = false;

                // Retain recording before cleanup if enabled
                if (transcript != null && !transcript.trim().isEmpty()) {
                    try {
                        // Calculate duration from transcription start time
                        long durationMs = System.currentTimeMillis() - transcriptionStartTime;

                        // Capture channel files before they're deleted
                        // audioFile = mic channel (always exists)
                        // systemTrackFile = system channel (null if single-source)
                        File micChannel = audioFile;
                        File systemChannel = systemTrackFile;

                        RecordingManifest.RecordingEntry entry = recordingRetentionManager.retainRecording(
                            fileToTranscribe,  // The merged/processed file
                            micChannel,        // Mic channel file
                            systemChannel,     // System channel file (may be null)
                            transcript,
                            null,  // pipelineResult updated in PostProcessingWorker.done()
                            systemTrackFile != null && systemTrackFile.exists(),
                            durationMs
                        );
                        // Store ID for pipeline result update
                        if (entry != null) {
                            lastRetainedRecordingId = entry.getId();
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to retain recording: {}", e.getMessage());
                    }
                }

                // Then cleanup temp files as usual
                cleanupTempAudioFile(audioFile);
                cleanupTempAudioFile(systemTrackFile);
                if (fileToTranscribe != audioFile) {
                    cleanupTempAudioFile(fileToTranscribe);
                }
            }
```

#### 4.5 Update PostProcessingWorker.done() with pipeline result

**Exact Location**: Lines 2357-2398 (inside `PostProcessingWorker` class)

**Find this code block** (inside the `try` block of `done()`):
```java
                // Add result to history
                pipelineHistory.addResult(pipeline.uuid, pipeline.title, processedResult, executionTime);
```

**Add AFTER that line**:
```java
                // Update retained recording with pipeline result
                if (lastRetainedRecordingId != null) {
                    recordingRetentionManager.updatePipelineResult(lastRetainedRecordingId, processedResult);
                    lastRetainedRecordingId = null;  // Clear after update
                }
```

#### 4.6 Add updatePipelineResult method to RecordingRetentionManager

**Add this method to RecordingRetentionManager.java**:
```java
/**
 * Update the pipeline result for an existing recording entry.
 * Called after post-processing completes.
 */
public void updatePipelineResult(String recordingId, String pipelineResult) {
    RecordingManifest.RecordingEntry entry = manifest.getRecording(recordingId);
    if (entry != null) {
        entry.setPipelineResult(pipelineResult);
        saveManifest();
        logger.debug("Updated pipeline result for recording: {}", recordingId);
    }
}
```

## Phase 5: Settings UI

### File: `src/main/java/org/whisperdog/settings/SettingsForm.java`

#### 5.1 Add field declarations

**Location**: After line ~51 (with other JCheckBox/JSlider fields like `keepCompressedSwitch`)

```java
// Recording retention settings
private JCheckBox retentionEnabledCheckBox;
private JSpinner retentionCountSpinner;
private JTextField retentionPathField;
```

#### 5.2 Add retention settings panel

**Exact Location**: After line 666 (after the system audio `if` block closes, BEFORE `JPanel apiSettingsPanel`)

**Find this code**:
```java
            row++;
        }

        JPanel apiSettingsPanel = new JPanel(new GridBagLayout());
```

**Insert BEFORE `JPanel apiSettingsPanel`**:
```java
            row++;
        }

        // ============================================================
        // Recording Retention Section
        // ============================================================
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(new JLabel("Keep recent recordings:"), gbc);

        retentionEnabledCheckBox = new JCheckBox();
        retentionEnabledCheckBox.setSelected(configManager.isRecordingRetentionEnabled());
        retentionEnabledCheckBox.setToolTipText("Save recordings to disk for later playback");
        retentionEnabledCheckBox.addActionListener(e -> {
            boolean enabled = retentionEnabledCheckBox.isSelected();
            configManager.setRecordingRetentionEnabled(enabled);
            retentionCountSpinner.setEnabled(enabled);
            retentionPathField.setEnabled(enabled);
            settingsDirty = true;
        });
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(retentionEnabledCheckBox, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(new JLabel("Recordings to keep:"), gbc);

        SpinnerNumberModel countModel = new SpinnerNumberModel(
            configManager.getRecordingRetentionCount(), 1, 100, 1);
        retentionCountSpinner = new JSpinner(countModel);
        retentionCountSpinner.setEnabled(configManager.isRecordingRetentionEnabled());
        retentionCountSpinner.addChangeListener(e -> {
            configManager.setRecordingRetentionCount((Integer) retentionCountSpinner.getValue());
            settingsDirty = true;
        });
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(retentionCountSpinner, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(new JLabel("Storage location:"), gbc);

        JPanel retentionPathPanel = new JPanel(new BorderLayout(5, 0));
        retentionPathField = new JTextField(configManager.getRecordingStoragePath(), 20);
        retentionPathField.setEnabled(configManager.isRecordingRetentionEnabled());
        retentionPathField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                configManager.setRecordingStoragePath(retentionPathField.getText());
                settingsDirty = true;
            }
        });
        retentionPathPanel.add(retentionPathField, BorderLayout.CENTER);

        JPanel retentionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        JButton browseRetentionButton = new JButton("Browse...");
        browseRetentionButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(configManager.getRecordingStoragePath()));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                retentionPathField.setText(path);
                configManager.setRecordingStoragePath(path);
                settingsDirty = true;
            }
        });
        retentionButtonPanel.add(browseRetentionButton);

        JButton openRetentionFolderButton = new JButton("Open");
        openRetentionFolderButton.addActionListener(e -> {
            try {
                File dir = new File(configManager.getRecordingStoragePath());
                if (!dir.exists()) dir.mkdirs();
                Desktop.getDesktop().open(dir);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Could not open folder: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        retentionButtonPanel.add(openRetentionFolderButton);
        retentionPathPanel.add(retentionButtonPanel, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(retentionPathPanel, gbc);

        row++;

        JPanel apiSettingsPanel = new JPanel(new GridBagLayout());
```

## Phase 6: Recordings Browser Panel

### File: `src/main/java/org/whisperdog/ui/RecordingsPanel.java`

```java
package org.whisperdog.ui;

import org.whisperdog.ConfigManager;
import org.whisperdog.recording.RecordingManifest;
import org.whisperdog.recording.RecordingRetentionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Panel for browsing retained recordings.
 */
public class RecordingsPanel extends JPanel {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy h:mm:ss a");

    private final RecordingRetentionManager retentionManager;
    private JPanel listPanel;

    public RecordingsPanel(RecordingRetentionManager retentionManager) {
        this.retentionManager = retentionManager;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Recordings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton openFolderButton = new JButton("Open Folder");
        openFolderButton.addActionListener(e -> retentionManager.openInFileManager());
        headerPanel.add(openFolderButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // List panel (scrollable)
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        footerPanel.add(refreshButton);
        add(footerPanel, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        listPanel.removeAll();

        java.util.List<RecordingManifest.RecordingEntry> recordings = retentionManager.getRecordings();

        if (recordings.isEmpty()) {
            JLabel emptyLabel = new JLabel("No recordings yet");
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(emptyLabel);
        } else {
            for (RecordingManifest.RecordingEntry entry : recordings) {
                listPanel.add(createRecordingCard(entry));
                listPanel.add(Box.createVerticalStrut(5));
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createRecordingCard(RecordingManifest.RecordingEntry entry) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            new EmptyBorder(10, 10, 10, 10)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left: Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        // Timestamp
        String dateStr = DATE_FORMAT.format(new Date(entry.getTimestamp()));
        JLabel dateLabel = new JLabel(dateStr);
        dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD));
        infoPanel.add(dateLabel);

        // Duration and size
        String duration = formatDuration(entry.getDurationMs());
        String size = formatSize(entry.getFileSizeBytes());
        String dualSource = entry.isHasSystemAudio() ? " | Dual-source" : "";
        JLabel metaLabel = new JLabel("Duration: " + duration + " | Size: " + size + dualSource);
        metaLabel.setForeground(Color.GRAY);
        infoPanel.add(metaLabel);

        // Preview
        String preview = entry.getTranscriptionPreview();
        if (preview != null && !preview.isEmpty()) {
            JLabel previewLabel = new JLabel("\"" + preview + "\"");
            previewLabel.setFont(previewLabel.getFont().deriveFont(Font.ITALIC));
            infoPanel.add(previewLabel);
        }

        card.add(infoPanel, BorderLayout.CENTER);

        // Right: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> playRecording(entry));
        buttonPanel.add(playButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                "Delete this recording?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                retentionManager.deleteRecording(entry.getId());
                refresh();
            }
        });
        buttonPanel.add(deleteButton);

        card.add(buttonPanel, BorderLayout.EAST);

        return card;
    }

    private void playRecording(RecordingManifest.RecordingEntry entry) {
        File file = retentionManager.getAudioFile(entry);
        if (file != null && file.exists()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Could not open recording: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
```

## Phase 7: Side Menu Integration

### File: `src/main/java/org/whisperdog/sidemenu/Menu.java`

The menu uses a 2D string array at line 17-24 to define menu items.

**Find this exact code** (lines 17-24):
```java
    private final String menuItems[][] = {
            {"~~"},
            {"Record"},
            {"Settings", "Options", "Logs"},
            {"PostPro Pipelines", "PostPro Pipelines", "Create/Edit Pipeline"},
            {"PostPro Unit Library", "PostPro Unit Library", "Create/Edit Unit"},

    };
```

**Replace with** (add "Recordings" entry):
```java
    private final String menuItems[][] = {
            {"~~"},
            {"Record"},
            {"Recordings"},
            {"Settings", "Options", "Logs"},
            {"PostPro Pipelines", "PostPro Pipelines", "Create/Edit Pipeline"},
            {"PostPro Unit Library", "PostPro Unit Library", "Create/Edit Unit"},

    };
```

### File: `src/main/java/org/whisperdog/sidemenu/MenuEvent.java`

The `MenuEvent` interface handles menu clicks. The index passed to `selected()` corresponds to the position in `menuItems` (excluding title rows starting with `~`).

**After adding "Recordings"**:
- Index 0 = Record
- Index 1 = Recordings (NEW)
- Index 2 = Settings
- Index 3 = PostPro Pipelines
- Index 4 = PostPro Unit Library

### File: Where MenuEvent.selected() is handled

**Search for**: `implements MenuEvent` or `new MenuEvent`

The handler must route index 1 to show `RecordingsPanel`:

```java
// In the MenuEvent handler (likely in MainFrame or AudioRecorderUI):
@Override
public void selected(int index, int subIndex) {
    switch (index) {
        case 0: showRecordPanel(); break;
        case 1: showRecordingsPanel(); break;  // NEW
        case 2: showSettingsPanel(subIndex); break;
        case 3: showPipelinesPanel(subIndex); break;
        case 4: showUnitsPanel(subIndex); break;
    }
}

// Add this method:
private RecordingsPanel recordingsPanel;

private void showRecordingsPanel() {
    if (recordingsPanel == null) {
        recordingsPanel = new RecordingsPanel(
            recorderForm.getRecordingRetentionManager()
        );
    }
    recordingsPanel.refresh();
    showForm(recordingsPanel);  // Use existing panel-switching method
}
```

**IMPORTANT**: Verify the exact location by searching for `MenuEvent` usage in the codebase. The switch statement indices must be updated if "Recordings" is inserted.

## Testing

### Manual Testing Steps

1. **Basic Retention**
   - Enable retention in settings (should be default)
   - Make a recording and transcribe
   - Check `%APPDATA%\WhisperDog\recordings\` for:
     - `recordings.json` manifest file
     - `recording_YYYYMMDD_HHmmss.wav` audio file

2. **Pruning**
   - Set retention count to 3
   - Make 5 recordings
   - Verify only 3 recordings remain (oldest 2 pruned)

3. **Settings UI**
   - Toggle enable/disable checkbox
   - Change retention count
   - Change storage path
   - Click "Open Folder"

4. **Recordings Browser**
   - Open recordings from side menu
   - Verify all recordings listed with correct metadata
   - Click "Play" to open in default player
   - Click "Delete" to remove a recording
   - Click "Refresh" to reload list

5. **Edge Cases**
   - Disable retention, make recording, verify no file copied
   - Set custom path, verify files go there
   - Make recordings in quick succession (same second)

6. **Error Scenarios**
   - Set invalid storage path (network drive offline), verify graceful failure
   - Fill disk, verify error logged and transcription still works
   - Corrupt `recordings.json` manually, verify recovery on next load

## Appendix A: Error Handling

### A.1 Storage Path Failures

**In `RecordingRetentionManager.initializeStorage()`**:

```java
private void initializeStorage() {
    try {
        storageDirectory = configManager.getRecordingsDirectory();
        if (!storageDirectory.exists() && !storageDirectory.mkdirs()) {
            logger.error("Cannot create recordings directory: {}", storageDirectory);
            storageDirectory = null;  // Mark as unavailable
            return;
        }
        if (!storageDirectory.canWrite()) {
            logger.error("Recordings directory not writable: {}", storageDirectory);
            storageDirectory = null;
            return;
        }
        loadManifest();
    } catch (Exception e) {
        logger.error("Failed to initialize recording storage: {}", e.getMessage());
        storageDirectory = null;
    }
}
```

**In `retainRecording()`**, add at the start:

```java
if (storageDirectory == null) {
    logger.warn("Recording storage unavailable, skipping retention");
    return null;
}
```

### A.2 Manifest Corruption Recovery

**In `RecordingManifest.load()`**:

```java
public static RecordingManifest load(File file) throws IOException {
    if (!file.exists()) {
        return new RecordingManifest();
    }
    try (Reader reader = new FileReader(file)) {
        RecordingManifest manifest = gson.fromJson(reader, RecordingManifest.class);
        if (manifest == null || manifest.recordings == null) {
            throw new IOException("Invalid manifest structure");
        }
        return manifest;
    } catch (Exception e) {
        // Backup corrupt file
        File backup = new File(file.getParent(), file.getName() + ".corrupt." + System.currentTimeMillis());
        file.renameTo(backup);
        LogManager.getLogger(RecordingManifest.class)
            .warn("Manifest corrupted, backed up to: {}. Starting fresh.", backup.getName());
        return new RecordingManifest();
    }
}
```

### A.3 Orphan File Detection

**Add to `RecordingsPanel.refresh()`**:

```java
public void refresh() {
    // Prune orphan entries (files deleted externally)
    List<RecordingManifest.RecordingEntry> recordings = retentionManager.getRecordings();
    List<String> orphanIds = new ArrayList<>();
    for (RecordingManifest.RecordingEntry entry : recordings) {
        File file = retentionManager.getAudioFile(entry);
        if (file == null || !file.exists()) {
            orphanIds.add(entry.getId());
        }
    }
    for (String id : orphanIds) {
        retentionManager.removeOrphan(id);  // Add this method to manager
    }

    // Continue with existing refresh logic...
}
```

**Add to `RecordingRetentionManager`**:

```java
/**
 * Remove an orphan entry (file deleted externally).
 */
public void removeOrphan(String id) {
    manifest.removeRecording(id);
    saveManifest();
    logger.info("Removed orphan recording entry: {}", id);
}
```

### A.4 Disk Full Handling

**In `retainRecording()`, wrap the file copy**:

```java
try {
    Files.copy(sourceAudioFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
} catch (IOException e) {
    if (e.getMessage() != null && e.getMessage().contains("space")) {
        logger.error("Disk full, cannot retain recording");
        // Optionally notify user via console
        ConsoleLogger.getInstance().logError("Cannot save recording: disk full");
    }
    throw e;  // Re-throw to trigger outer catch
}
```
