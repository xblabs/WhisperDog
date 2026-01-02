package org.whisperdog.recording;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog that warns users when detected speech is below the minimum threshold.
 * Allows user to discard the recording or proceed with transcription anyway.
 */
public class MinSpeechDurationDialog extends JDialog {

    private boolean proceed = false;

    /**
     * Creates a new minimum speech duration warning dialog.
     *
     * @param parent Parent window
     * @param detectedSpeechSeconds The amount of detected speech in seconds
     * @param thresholdSeconds The minimum threshold setting
     */
    public MinSpeechDurationDialog(Window parent, float detectedSpeechSeconds, float thresholdSeconds) {
        super(parent, "Insufficient Speech Detected", ModalityType.APPLICATION_MODAL);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Warning icon and title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel("\u26A0");  // Warning sign
        iconLabel.setFont(iconLabel.getFont().deriveFont(24f));
        iconLabel.setForeground(new Color(220, 160, 50));  // Amber warning color
        headerPanel.add(iconLabel);

        JLabel titleLabel = new JLabel("Very Little Speech Detected");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(titleLabel);

        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Metrics section
        JPanel metricsPanel = createMetricsPanel(detectedSpeechSeconds, thresholdSeconds);
        mainPanel.add(metricsPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Separator
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        mainPanel.add(separator);
        mainPanel.add(Box.createVerticalStrut(15));

        // Warning message
        JTextArea messageArea = new JTextArea(
            "This recording contains very little detected speech.\n" +
            "Transcribing may produce empty or minimal results.\n\n" +
            "This could happen from accidental recording triggering,\n" +
            "or recording only ambient noise."
        );
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageArea.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
        mainPanel.add(messageArea);
        mainPanel.add(Box.createVerticalStrut(20));

        // Buttons
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);

        // Set minimum size
        setMinimumSize(new Dimension(380, getHeight()));
    }

    /**
     * Creates the metrics display panel.
     */
    private JPanel createMetricsPanel(float detectedSpeechSeconds, float thresholdSeconds) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(10, 15, 10, 15)
        ));

        // Detected speech with warning color
        String speechFormatted = String.format("%.1f seconds", detectedSpeechSeconds);
        JLabel speechLabel = new JLabel("Detected speech: " + speechFormatted);
        speechLabel.setForeground(new Color(200, 80, 80));  // Soft red for warning
        speechLabel.setFont(speechLabel.getFont().deriveFont(Font.BOLD));
        speechLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(speechLabel);
        panel.add(Box.createVerticalStrut(5));

        // Minimum threshold
        String thresholdFormatted = String.format("%.1f seconds", thresholdSeconds);
        JLabel thresholdLabel = new JLabel("Minimum required: " + thresholdFormatted);
        thresholdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(thresholdLabel);

        return panel;
    }

    /**
     * Creates the button panel with Discard and Transcribe Anyway buttons.
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton discardButton = new JButton("Discard");
        discardButton.addActionListener(e -> {
            proceed = false;
            dispose();
        });

        JButton proceedButton = new JButton("Transcribe Anyway");
        proceedButton.addActionListener(e -> {
            proceed = true;
            dispose();
        });

        panel.add(discardButton);
        panel.add(proceedButton);

        return panel;
    }

    /**
     * Shows the dialog and returns whether to proceed with transcription.
     *
     * @return true if user chose to proceed, false if discarded
     */
    public boolean showAndGetResult() {
        setVisible(true);
        return proceed;
    }
}
