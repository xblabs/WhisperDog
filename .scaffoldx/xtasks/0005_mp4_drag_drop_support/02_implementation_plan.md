# Implementation Plan: MP4 Drag-Drop Support

## Overview

Add video file drag-drop support to WhisperDog by detecting video files, extracting audio via ffmpeg, and feeding the extracted audio into the existing transcription pipeline.

## Phase 1: FFmpeg Utility Class

Create utility class for ffmpeg operations.

### File: `src/main/java/org/whisperdog/audio/FFmpegUtil.java`

```java
package org.whisperdog.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FFmpegUtil {

    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".mov", ".mkv", ".avi", ".webm"};

    /**
     * Check if ffmpeg is available in system PATH or bundled location
     */
    public static boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if file is a supported video format
     */
    public static boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : VIDEO_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract audio from video file asynchronously
     * @param videoFile Source video file
     * @param progressCallback Progress updates (0.0 - 1.0)
     * @return CompletableFuture with extracted audio file path
     */
    public static CompletableFuture<File> extractAudio(
            File videoFile,
            Consumer<Double> progressCallback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create temp file for extracted audio
                Path tempPath = Files.createTempFile("whisperdog_extract_", ".wav");
                File outputFile = tempPath.toFile();
                outputFile.deleteOnExit(); // Safety net

                // Build ffmpeg command
                ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",           // Overwrite output
                    "-i", videoFile.getAbsolutePath(),
                    "-vn",                     // No video
                    "-acodec", "pcm_s16le",   // 16-bit PCM
                    "-ar", "16000",           // 16kHz sample rate
                    "-ac", "1",               // Mono
                    outputFile.getAbsolutePath()
                );
                pb.redirectErrorStream(true);

                Process process = pb.start();

                // Read output for progress (ffmpeg outputs to stderr)
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );
                String line;
                while ((line = reader.readLine()) != null) {
                    // Parse progress from ffmpeg output if needed
                    // For now, just let it run
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("FFmpeg extraction failed with code: " + exitCode);
                }

                if (progressCallback != null) {
                    progressCallback.accept(1.0);
                }

                return outputFile;

            } catch (Exception e) {
                throw new RuntimeException("Failed to extract audio: " + e.getMessage(), e);
            }
        });
    }
}
```

## Phase 2: Drop Handler Enhancement

Modify existing drop handler to detect video files.

### File: `src/main/java/org/whisperdog/recording/RecorderForm.java`

Add video detection to existing drop handler:

```java
// In drop handler method
@Override
public void drop(DropTargetDropEvent event) {
    try {
        event.acceptDrop(DnDConstants.ACTION_COPY);
        List<File> files = (List<File>) event.getTransferable()
            .getTransferData(DataFlavor.javaFileListFlavor);

        if (!files.isEmpty()) {
            File file = files.get(0);

            if (FFmpegUtil.isVideoFile(file)) {
                handleVideoFile(file);
            } else {
                handleAudioFile(file);
            }
        }
    } catch (Exception e) {
        showError("Drop failed: " + e.getMessage());
    }
}

private void handleVideoFile(File videoFile) {
    // Check ffmpeg availability
    if (!FFmpegUtil.isFFmpegAvailable()) {
        showError(
            "Video transcription requires ffmpeg.\n" +
            "Install ffmpeg and ensure it's in your PATH.\n" +
            "Download: https://ffmpeg.org/download.html"
        );
        return;
    }

    // Show extraction status
    setStatus("Extracting audio from " + videoFile.getName() + "...");
    setControlsEnabled(false);

    // Extract audio async
    FFmpegUtil.extractAudio(videoFile, progress -> {
        // Update progress on EDT
        SwingUtilities.invokeLater(() -> {
            // Could update progress bar here
        });
    }).thenAccept(audioFile -> {
        SwingUtilities.invokeLater(() -> {
            setStatus("Extraction complete. Starting transcription...");
            // Feed extracted audio into existing pipeline
            handleAudioFile(audioFile);
            // Mark for cleanup after transcription
            registerTempFile(audioFile);
        });
    }).exceptionally(ex -> {
        SwingUtilities.invokeLater(() -> {
            setControlsEnabled(true);
            showError("Extraction failed: " + ex.getMessage());
        });
        return null;
    });
}
```

## Phase 3: Temp File Cleanup

Track and clean up extracted audio files.

```java
// In RecorderForm or dedicated cleanup manager
private List<File> tempFiles = new ArrayList<>();

private void registerTempFile(File file) {
    tempFiles.add(file);
}

private void cleanupTempFiles() {
    for (File file : tempFiles) {
        if (file.exists()) {
            file.delete();
        }
    }
    tempFiles.clear();
}

// Call after transcription completes
private void onTranscriptionComplete() {
    // ... existing code ...
    cleanupTempFiles();
}

// Also clean up on window close
@Override
public void dispose() {
    cleanupTempFiles();
    super.dispose();
}
```

## Testing Strategy

### Unit Tests
- `FFmpegUtil.isVideoFile()` with various extensions
- `FFmpegUtil.isFFmpegAvailable()` detection

### Integration Tests
- Drop video file → audio extracted → transcription runs
- Error handling when ffmpeg not installed
- Temp file cleanup after transcription

### Manual Testing
- Test with MP4, MOV, MKV files
- Test with video that has no audio track
- Test with very long video (performance)
- Test cancellation during extraction

## Dependencies

- FFmpeg must be installed and in PATH
- No new Java dependencies required
- Uses existing Swing drag-drop infrastructure
