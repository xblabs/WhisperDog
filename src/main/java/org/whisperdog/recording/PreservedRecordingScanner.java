package org.whisperdog.recording;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.ConfigManager;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans the WhisperDog temp directory for recoverable audio files
 * from failed transcriptions. Groups files into sessions by timestamp
 * and selects the best candidate for retry.
 */
public class PreservedRecordingScanner {
    private static final Logger logger = LogManager.getLogger(PreservedRecordingScanner.class);

    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long SIXTY_SECONDS_MS = 60 * 1000;

    private static final Pattern MIC_RAW_PATTERN =
            Pattern.compile("whisperdog_mic_(\\d{8}_\\d{6})\\.wav");
    private static final Pattern MIC_NOSILENCE_PATTERN =
            Pattern.compile("whisperdog_mic_(\\d{8}_\\d{6})_nosilence\\.wav");
    private static final Pattern COMPRESSED_PATTERN =
            Pattern.compile("whisperdog_compressed_.*\\.mp3");

    private static final DateTimeFormatter TODAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Immutable value object representing a group of files from one recording session.
     */
    public static class RecoverableSession {
        private final String timestampPrefix;
        private final File selectedFile;
        private final List<File> allFiles;
        private final String sourceLabel;

        public RecoverableSession(String timestampPrefix, File selectedFile,
                                  List<File> allFiles, String sourceLabel) {
            this.timestampPrefix = timestampPrefix;
            this.selectedFile = selectedFile;
            this.allFiles = Collections.unmodifiableList(new ArrayList<>(allFiles));
            this.sourceLabel = sourceLabel;
        }

        public String getTimestampPrefix() { return timestampPrefix; }
        public File getSelectedFile() { return selectedFile; }
        public List<File> getAllFiles() { return allFiles; }
        public String getSourceLabel() { return sourceLabel; }
    }

    private final ConfigManager configManager;

    public PreservedRecordingScanner(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Scan temp directory and return recoverable sessions.
     * Thread-safe. No side effects. May be called from any thread.
     *
     * @param isRecording true if a recording is currently in progress
     * @return list of recoverable sessions, empty if none found. Never null.
     */
    public List<RecoverableSession> scan(boolean isRecording) {
        File tempDir = ConfigManager.getTempDirectory();
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = tempDir.listFiles();
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        long cutoffOld = now - SEVEN_DAYS_MS;
        long cutoffRecent = now - SIXTY_SECONDS_MS;
        String todayPrefix = LocalDate.now().format(TODAY_FORMAT);

        // Collect matching files after applying filters
        Map<String, File> nosilenceByTimestamp = new LinkedHashMap<>();
        Map<String, File> rawByTimestamp = new LinkedHashMap<>();
        List<File> compressedFiles = new ArrayList<>();

        // Track all files per timestamp for session grouping
        Map<String, List<File>> filesByTimestamp = new LinkedHashMap<>();

        for (File file : files) {
            if (!file.isFile()) continue;

            long lastMod = file.lastModified();

            // Age filter: only files within 7 days
            if (lastMod < cutoffOld) continue;

            // Recency guard: exclude files modified within 60 seconds
            if (lastMod > cutoffRecent) continue;

            String name = file.getName();

            // Check nosilence pattern first (more specific)
            Matcher nosilenceMatcher = MIC_NOSILENCE_PATTERN.matcher(name);
            if (nosilenceMatcher.matches()) {
                String ts = nosilenceMatcher.group(1);
                if (isRecording && ts.startsWith(todayPrefix)) continue;
                nosilenceByTimestamp.put(ts, file);
                filesByTimestamp.computeIfAbsent(ts, k -> new ArrayList<>()).add(file);
                continue;
            }

            // Check raw mic pattern
            Matcher rawMatcher = MIC_RAW_PATTERN.matcher(name);
            if (rawMatcher.matches()) {
                String ts = rawMatcher.group(1);
                if (isRecording && ts.startsWith(todayPrefix)) continue;
                rawByTimestamp.put(ts, file);
                filesByTimestamp.computeIfAbsent(ts, k -> new ArrayList<>()).add(file);
                continue;
            }

            // Check compressed pattern (standalone, no timestamp grouping)
            if (COMPRESSED_PATTERN.matcher(name).matches()) {
                if (isRecording) continue; // Skip all compressed during recording
                compressedFiles.add(file);
            }
        }

        // Build sessions from grouped timestamps
        List<RecoverableSession> sessions = new ArrayList<>();
        Set<String> processedTimestamps = new LinkedHashSet<>();
        processedTimestamps.addAll(nosilenceByTimestamp.keySet());
        processedTimestamps.addAll(rawByTimestamp.keySet());

        for (String ts : processedTimestamps) {
            List<File> sessionFiles = filesByTimestamp.getOrDefault(ts, Collections.emptyList());
            File selected;
            String label;

            // Priority: nosilence > raw
            if (nosilenceByTimestamp.containsKey(ts)) {
                selected = nosilenceByTimestamp.get(ts);
                label = "Silence-removed";
            } else {
                selected = rawByTimestamp.get(ts);
                label = "Raw recording";
            }

            if (selected != null) {
                sessions.add(new RecoverableSession(ts, selected, sessionFiles, label));
            }
        }

        // Add compressed files as standalone sessions
        for (File compressed : compressedFiles) {
            sessions.add(new RecoverableSession(
                    null, compressed, Collections.singletonList(compressed), "Compressed"));
        }

        logger.info("Recovery scan found {} session(s) in {}", sessions.size(), tempDir.getAbsolutePath());
        return sessions;
    }
}
