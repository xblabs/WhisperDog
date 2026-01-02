package org.whisperdog.recording;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog that warns users about large recordings with high silence content.
 * Shows when both conditions are met:
 * - Duration > 600 seconds (10 minutes)
 * - Silence ratio > 50%
 */
public class LargeRecordingWarningDialog extends JDialog {

    private boolean proceed = false;

    /**
     * Creates a new warning dialog.
     *
     * @param parent Parent window
     * @param analysis Silence analysis result with metrics
     */
    public LargeRecordingWarningDialog(Window parent, SilenceRemover.SilenceAnalysisResult analysis) {
        super(parent, "Large Recording Detected", ModalityType.APPLICATION_MODAL);

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

        JLabel titleLabel = new JLabel("Large Recording with High Silence");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(titleLabel);

        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Metrics section
        JPanel metricsPanel = createMetricsPanel(analysis);
        mainPanel.add(metricsPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Separator
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        mainPanel.add(separator);
        mainPanel.add(Box.createVerticalStrut(15));

        // Warning message
        JTextArea messageArea = new JTextArea(
            "This recording has an unusually high silence ratio.\n" +
            "Transcribing may use significant API tokens for minimal output.\n\n" +
            "This could happen if the recording was accidentally left running,\n" +
            "or if there were long pauses between speech."
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
        setMinimumSize(new Dimension(420, getHeight()));
    }

    /**
     * Creates the metrics display panel.
     */
    private JPanel createMetricsPanel(SilenceRemover.SilenceAnalysisResult analysis) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(10, 15, 10, 15)
        ));

        // Duration
        JLabel durationLabel = new JLabel("Duration: " + analysis.getFormattedDuration());
        durationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(durationLabel);
        panel.add(Box.createVerticalStrut(5));

        // Silence percentage with warning color
        JLabel silenceLabel = new JLabel("Silence: " + analysis.getFormattedSilencePercent());
        silenceLabel.setForeground(new Color(200, 80, 80));  // Soft red for warning
        silenceLabel.setFont(silenceLabel.getFont().deriveFont(Font.BOLD));
        silenceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(silenceLabel);
        panel.add(Box.createVerticalStrut(5));

        // Estimated useful audio
        JLabel usefulLabel = new JLabel("Estimated useful audio: " + analysis.getFormattedUsefulDuration());
        usefulLabel.setForeground(new Color(60, 140, 60));  // Green for useful content
        usefulLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(usefulLabel);

        return panel;
    }

    /**
     * Creates the button panel with Cancel and Transcribe Anyway buttons.
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            proceed = false;
            dispose();
        });

        JButton proceedButton = new JButton("Transcribe Anyway");
        proceedButton.addActionListener(e -> {
            proceed = true;
            dispose();
        });

        panel.add(cancelButton);
        panel.add(proceedButton);

        return panel;
    }

    /**
     * Shows the dialog and returns whether to proceed with transcription.
     *
     * @return true if user chose to proceed, false if cancelled
     */
    public boolean showAndGetResult() {
        setVisible(true);
        return proceed;
    }

    /**
     * Returns whether the user chose to proceed.
     */
    public boolean shouldProceed() {
        return proceed;
    }
}
