package org.whisperdog.ui;

import org.whisperdog.error.ErrorClassifier;
import org.whisperdog.error.TranscriptionException;
import org.whisperdog.retry.RetryState;
import org.whisperdog.validation.TranscriptionValidator;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogs for transcription errors, allowing users to retry or cancel.
 */
public class TranscriptionErrorDialog {

    /**
     * Shows dialog for empty transcription response, allowing user to retry or cancel.
     *
     * @param parent The parent component for dialog positioning
     * @param state The current retry state
     * @return true if user wants to retry, false to cancel
     */
    public static boolean showEmptyResponseDialog(Component parent, RetryState state) {
        String fileSize = TranscriptionValidator.getFileSizeDisplay(state.getCompressedAudio());

        String message = "<html><body style='width: 320px; font-family: sans-serif;'>" +
            "<h3 style='margin-top: 0;'>No Speech Detected</h3>" +
            "<p>Your recording was processed but no speech was detected.</p>" +
            "<p style='color: #666;'>This can happen when:</p>" +
            "<ul style='color: #666; margin-left: -15px;'>" +
            "<li>The audio contains only background noise</li>" +
            "<li>The microphone sensitivity was too low</li>" +
            "<li>The recording captured mostly silence</li>" +
            "</ul>" +
            "<hr style='border: none; border-top: 1px solid #ddd;'/>" +
            "<p><b>File:</b> " + state.getCompressedAudio().getName() + "</p>" +
            "<p><b>Size:</b> " + fileSize + "</p>" +
            "</body></html>";

        int result = JOptionPane.showOptionDialog(
            parent,
            message,
            "Transcription Result",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new String[]{"Retry Transcription", "Cancel"},
            "Retry Transcription"
        );

        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Shows error dialog for permanent failures (no retry option).
     *
     * @param parent The parent component
     * @param e The transcription exception
     */
    public static void showPermanentErrorDialog(Component parent, TranscriptionException e) {
        String userMessage = ErrorClassifier.getUserFriendlyMessage(e);

        String message = "<html><body style='width: 320px; font-family: sans-serif;'>" +
            "<h3 style='margin-top: 0; color: #c0392b;'>Transcription Failed</h3>" +
            "<p>" + userMessage + "</p>";

        if (e.getHttpStatus() > 0) {
            message += "<p style='color: #666; font-size: 11px;'>Error code: HTTP " + e.getHttpStatus() + "</p>";
        }

        message += "</body></html>";

        JOptionPane.showMessageDialog(
            parent,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Shows dialog when all automatic retries are exhausted.
     *
     * @param parent The parent component
     * @param state The current retry state
     * @return true if user wants to try again, false to give up
     */
    public static boolean showRetriesExhaustedDialog(Component parent, RetryState state) {
        String message = "<html><body style='width: 320px; font-family: sans-serif;'>" +
            "<h3 style='margin-top: 0; color: #c0392b;'>Retries Exhausted</h3>" +
            "<p>Transcription failed after " + state.getMaxAttempts() + " attempts.</p>" +
            "<p><b>Last error:</b><br/>" +
            "<span style='color: #666;'>" + state.getLastErrorMessage() + "</span></p>" +
            "<hr style='border: none; border-top: 1px solid #ddd;'/>" +
            "<p>Would you like to try again?</p>" +
            "</body></html>";

        int result = JOptionPane.showOptionDialog(
            parent,
            message,
            "Transcription Failed",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            new String[]{"Try Again", "Cancel"},
            "Cancel"
        );

        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Shows progress dialog for retry operations (non-modal).
     * Returns the dialog so caller can close it when done.
     *
     * @param parent The parent component
     * @param initialMessage Initial status message
     * @return The dialog for later dismissal
     */
    public static JDialog showRetryProgressDialog(Component parent, String initialMessage) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
            window instanceof Frame ? (Frame) window : null,
            "Transcribing...",
            false // Non-modal so it doesn't block
        );

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel statusLabel = new JLabel(initialMessage);
        statusLabel.setName("statusLabel"); // For finding later
        panel.add(statusLabel, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(300, 100));
        dialog.setLocationRelativeTo(parent);

        return dialog;
    }

    /**
     * Updates the status message in a retry progress dialog.
     *
     * @param dialog The dialog from showRetryProgressDialog
     * @param message The new status message
     */
    public static void updateProgressDialog(JDialog dialog, String message) {
        if (dialog == null) return;

        SwingUtilities.invokeLater(() -> {
            Component[] components = ((JPanel) dialog.getContentPane().getComponent(0)).getComponents();
            for (Component c : components) {
                if (c instanceof JLabel && "statusLabel".equals(c.getName())) {
                    ((JLabel) c).setText(message);
                    break;
                }
            }
        });
    }
}
