package org.whisperdog.recording;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.ConfigManager;
import org.whisperdog.recording.clients.FasterWhisperTranscribeClient;
import org.whisperdog.recording.clients.OpenAITranscribeClient;
import org.whisperdog.recording.clients.OpenWebUITranscribeClient;

import javax.swing.*;
import java.io.File;
import java.util.function.Consumer;

/**
 * SwingWorker that transcribes a single recovered audio file and retains the result.
 * Does NOT reuse AudioTranscriptionWorker because that class depends on RecorderForm
 * state (active recording context, pre-flight dialogs, dual-source merge logic).
 */
public class RecoveryTranscriptionWorker extends SwingWorker<String, String> {
    private static final Logger logger = LogManager.getLogger(RecoveryTranscriptionWorker.class);

    private final PreservedRecordingScanner.RecoverableSession session;
    private final RecordingRetentionManager retentionManager;
    private final ConfigManager configManager;
    private final Consumer<String> onSuccess;
    private final Consumer<String> onFailure;

    public RecoveryTranscriptionWorker(
            PreservedRecordingScanner.RecoverableSession session,
            RecordingRetentionManager retentionManager,
            ConfigManager configManager,
            Consumer<String> onSuccess,
            Consumer<String> onFailure) {
        this.session = session;
        this.retentionManager = retentionManager;
        this.configManager = configManager;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    protected String doInBackground() throws Exception {
        File selectedFile = session.getSelectedFile();
        logger.info("Recovery transcription starting for: {}", selectedFile.getName());

        String server = configManager.getWhisperServer();
        switch (server) {
            case "Faster-Whisper":
                FasterWhisperTranscribeClient fwClient = new FasterWhisperTranscribeClient(configManager);
                return fwClient.transcribe(selectedFile);
            case "Open WebUI":
                OpenWebUITranscribeClient owuiClient = new OpenWebUITranscribeClient(configManager);
                return owuiClient.transcribeAudio(selectedFile);
            default:
                OpenAITranscribeClient oaiClient = new OpenAITranscribeClient(configManager);
                return oaiClient.transcribe(selectedFile);
        }
    }

    @Override
    protected void done() {
        try {
            String transcription = get();
            logger.info("Recovery transcription succeeded for: {}", session.getSelectedFile().getName());

            // Retain the recovered recording
            retentionManager.retainRecoveredRecording(session.getSelectedFile(), transcription);

            // Delete all session files on success
            for (File file : session.getAllFiles()) {
                if (file.exists()) {
                    if (file.delete()) {
                        logger.debug("Deleted recovered session file: {}", file.getName());
                    } else {
                        logger.warn("Failed to delete recovered session file: {}", file.getName());
                    }
                }
            }

            if (onSuccess != null) {
                onSuccess.accept(transcription);
            }
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            logger.error("Recovery transcription failed for: {}", session.getSelectedFile().getName(), e);
            // Leave files in place on failure
            if (onFailure != null) {
                onFailure.accept(errorMsg);
            }
        }
    }
}
