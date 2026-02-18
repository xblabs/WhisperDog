package org.whisperdog.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.ConfigManager;
import org.whisperdog.recording.PreservedRecordingScanner.RecoverableSession;
import org.whisperdog.recording.RecordingRetentionManager;
import org.whisperdog.recording.RecoveryTranscriptionWorker;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Modal dialog displaying recoverable recording sessions with action buttons.
 * Allows users to retry failed transcriptions, discard files, or open the temp folder.
 */
public class RecoveryDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger(RecoveryDialog.class);
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final List<RecoverableSession> sessions;
    private final RecordingRetentionManager retentionManager;
    private final ConfigManager configManager;
    private final JLabel[] statusLabels;
    private final JButton retryAllButton;
    private final JButton discardAllButton;
    private int completedCount = 0;
    private boolean retryInProgress = false;

    public enum SessionStatus { PENDING, TRANSCRIBING, DONE, FAILED }

    public RecoveryDialog(Frame owner,
                          List<RecoverableSession> sessions,
                          RecordingRetentionManager retentionManager,
                          ConfigManager configManager) {
        super(owner, "Recover Failed Transcriptions", true);
        this.sessions = sessions;
        this.retentionManager = retentionManager;
        this.configManager = configManager;
        this.statusLabels = new JLabel[sessions.size()];

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(520, 300));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header
        JLabel headerLabel = new JLabel(
                sessions.size() + " recoverable recording" + (sessions.size() != 1 ? "s" : "") + " found");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Session list
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        for (int i = 0; i < sessions.size(); i++) {
            listPanel.add(createSessionRow(sessions.get(i), i));
            if (i < sessions.size() - 1) {
                listPanel.add(Box.createVerticalStrut(6));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button bar
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.setBorder(new EmptyBorder(10, 0, 0, 0));

        retryAllButton = createStyledButton("Retry All");
        retryAllButton.addActionListener(e -> retryAll());
        buttonBar.add(retryAllButton);

        discardAllButton = createStyledButton("Discard All");
        discardAllButton.addActionListener(e -> discardAll());
        buttonBar.add(discardAllButton);

        JButton openFolderButton = createStyledButton("Open Folder");
        openFolderButton.addActionListener(e -> openFolder());
        buttonBar.add(openFolderButton);

        JButton closeButton = createStyledButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonBar.add(closeButton);

        mainPanel.add(buttonBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setSize(Math.max(getWidth(), 560), Math.min(Math.max(getHeight(), 320), 500));
        setLocationRelativeTo(owner);
    }

    private JPanel createSessionRow(RecoverableSession session, int index) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                new EmptyBorder(8, 10, 8, 10)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Info column
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        File selectedFile = session.getSelectedFile();
        String fileName = selectedFile.getName();
        String sizeStr = formatFileSize(selectedFile.length());
        String dateStr = TIMESTAMP_FORMAT.format(new Date(selectedFile.lastModified()));

        JLabel nameLabel = new JLabel(fileName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(nameLabel);

        JLabel metaLabel = new JLabel(sizeStr + " | " + dateStr + " | " + session.getSourceLabel());
        metaLabel.setFont(metaLabel.getFont().deriveFont(11f));
        metaLabel.setForeground(Color.GRAY);
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(metaLabel);

        row.add(infoPanel, BorderLayout.CENTER);

        // Status label (right side)
        JLabel statusLabel = new JLabel("Pending");
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setPreferredSize(new Dimension(100, 30));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabels[index] = statusLabel;
        row.add(statusLabel, BorderLayout.EAST);

        return row;
    }

    /**
     * Called to update per-row status. Must be called on EDT.
     */
    public void updateSessionStatus(int sessionIndex, SessionStatus status, String detail) {
        if (sessionIndex < 0 || sessionIndex >= statusLabels.length) return;

        JLabel label = statusLabels[sessionIndex];
        switch (status) {
            case PENDING:
                label.setText("Pending");
                label.setForeground(Color.GRAY);
                break;
            case TRANSCRIBING:
                label.setText("Transcribing...");
                label.setForeground(new Color(59, 130, 246)); // Blue
                break;
            case DONE:
                label.setText("Done");
                label.setForeground(new Color(34, 197, 94)); // Green
                break;
            case FAILED:
                String failText = detail != null ? "Failed: " + truncate(detail, 40) : "Failed";
                label.setText(failText);
                label.setForeground(new Color(239, 68, 68)); // Red
                label.setToolTipText(detail);
                break;
        }
    }

    private void retryAll() {
        if (retryInProgress) return;
        retryInProgress = true;
        completedCount = 0;
        retryAllButton.setEnabled(false);
        discardAllButton.setEnabled(false);

        for (int i = 0; i < sessions.size(); i++) {
            final int idx = i;
            updateSessionStatus(idx, SessionStatus.TRANSCRIBING, null);

            RecoveryTranscriptionWorker worker = new RecoveryTranscriptionWorker(
                    sessions.get(idx),
                    retentionManager,
                    configManager,
                    transcription -> SwingUtilities.invokeLater(() -> {
                        updateSessionStatus(idx, SessionStatus.DONE, null);
                        onWorkerComplete();
                    }),
                    errorMsg -> SwingUtilities.invokeLater(() -> {
                        updateSessionStatus(idx, SessionStatus.FAILED, errorMsg);
                        onWorkerComplete();
                    })
            );
            worker.execute();
        }
    }

    private void onWorkerComplete() {
        completedCount++;
        if (completedCount >= sessions.size()) {
            retryInProgress = false;
            retryAllButton.setEnabled(true);
            discardAllButton.setEnabled(true);
        }
    }

    private void discardAll() {
        int totalFiles = sessions.stream().mapToInt(s -> s.getAllFiles().size()).sum();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete " + totalFiles + " file" + (totalFiles != 1 ? "s" : "") + " permanently?",
                "Discard Recordings",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        for (RecoverableSession session : sessions) {
            for (File file : session.getAllFiles()) {
                if (file.exists()) {
                    if (file.delete()) {
                        logger.debug("Discarded: {}", file.getName());
                    } else {
                        logger.warn("Failed to delete: {}", file.getName());
                    }
                }
            }
        }
        dispose();
    }

    private void openFolder() {
        try {
            File tempDir = ConfigManager.getTempDirectory();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempDir);
            }
        } catch (Exception e) {
            logger.error("Failed to open temp folder", e);
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 6; " +
                "margin: 4,8,4,8;");
        return button;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
