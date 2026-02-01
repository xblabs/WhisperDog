package org.whisperdog.recording;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.Notificationmanager;
import org.whisperdog.ToastNotification;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Panel for browsing and managing retained recordings.
 */
public class RecordingsPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger(RecordingsPanel.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy h:mm a");

    private final RecordingRetentionManager retentionManager;
    private final JPanel contentPanel;
    private final JLabel headerLabel;

    public RecordingsPanel(RecordingRetentionManager retentionManager) {
        this.retentionManager = retentionManager;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header panel with title and buttons
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        headerLabel = new JLabel("Recordings (0)");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setIcon(new FlatSVGIcon("icon/svg/refresh.svg", 14, 14));
        refreshButton.addActionListener(e -> refresh());
        headerButtons.add(refreshButton);

        JButton openFolderButton = new JButton("Open Folder");
        openFolderButton.setIcon(new FlatSVGIcon("icon/svg/folder.svg", 14, 14));
        openFolderButton.addActionListener(e -> retentionManager.openInFileManager());
        headerButtons.add(openFolderButton);

        headerPanel.add(headerButtons, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Content panel with scroll
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Initial load
        refresh();
    }

    /**
     * Refreshes the recordings list from the manager.
     */
    public void refresh() {
        contentPanel.removeAll();

        List<RecordingManifest.RecordingEntry> recordings = retentionManager.getRecordings();
        headerLabel.setText(String.format("Recordings (%d)", recordings.size()));

        if (recordings.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            JLabel emptyLabel = new JLabel("No recordings yet");
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setFont(emptyLabel.getFont().deriveFont(14f));
            emptyPanel.add(emptyLabel);
            contentPanel.add(emptyPanel);
        } else {
            for (RecordingManifest.RecordingEntry entry : recordings) {
                contentPanel.add(createRecordingCard(entry));
                contentPanel.add(Box.createVerticalStrut(8));
            }
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Creates a card component for a single recording entry.
     */
    private JPanel createRecordingCard(RecordingManifest.RecordingEntry entry) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left side: info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // Date and time
        String dateStr = DATE_FORMAT.format(new Date(entry.getTimestamp()));
        JLabel dateLabel = new JLabel(dateStr);
        dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD, 13f));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(dateLabel);

        // Duration, size, and source type
        String durationStr = formatDuration(entry.getDurationMs());
        String sizeStr = formatFileSize(entry.getFileSizeBytes());
        String sourceStr = entry.isDualSource() ? "Dual source" : "Mic only";
        JLabel metaLabel = new JLabel(String.format("%s | %s | %s", durationStr, sizeStr, sourceStr));
        metaLabel.setFont(metaLabel.getFont().deriveFont(11f));
        metaLabel.setForeground(Color.GRAY);
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(metaLabel);

        // Transcription preview
        String preview = entry.getTranscriptionPreview();
        if (preview != null && !preview.isEmpty()) {
            JLabel previewLabel = new JLabel("<html>" + truncate(preview, 80) + "</html>");
            previewLabel.setFont(previewLabel.getFont().deriveFont(Font.ITALIC, 11f));
            previewLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            previewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(Box.createVerticalStrut(3));
            infoPanel.add(previewLabel);
        }

        card.add(infoPanel, BorderLayout.CENTER);

        // Right side: buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        JButton playButton = new JButton("Play");
        playButton.setIcon(new FlatSVGIcon("icon/svg/play.svg", 14, 14));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.addActionListener(e -> playRecording(entry));
        buttonPanel.add(playButton);

        buttonPanel.add(Box.createVerticalStrut(5));

        JButton deleteButton = new JButton("Delete");
        deleteButton.setIcon(new FlatSVGIcon("icon/svg/delete.svg", 14, 14));
        deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteButton.addActionListener(e -> deleteRecording(entry));
        buttonPanel.add(deleteButton);

        card.add(buttonPanel, BorderLayout.EAST);

        return card;
    }

    /**
     * Opens a recording in the default audio player.
     */
    private void playRecording(RecordingManifest.RecordingEntry entry) {
        File audioFile = retentionManager.getAudioFile(entry);
        if (audioFile == null || !audioFile.exists()) {
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                "Recording file not found");
            refresh();
            return;
        }

        try {
            Desktop.getDesktop().open(audioFile);
        } catch (Exception e) {
            logger.error("Failed to open recording", e);
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                "Failed to open recording: " + e.getMessage());
        }
    }

    /**
     * Deletes a recording after confirmation.
     */
    private void deleteRecording(RecordingManifest.RecordingEntry entry) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this recording?",
            "Delete Recording",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (retentionManager.deleteRecording(entry.getId())) {
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.SUCCESS,
                    "Recording deleted");
                refresh();
            } else {
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                    "Failed to delete recording");
            }
        }
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     */
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) {
            return String.format("%d:%02d", minutes, seconds);
        }
        long hours = minutes / 60;
        minutes = minutes % 60;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Formats a file size to a human-readable string.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    /**
     * Truncates a string to the specified length.
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
