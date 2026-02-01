package org.whisperdog.recording;

import com.formdev.flatlaf.FlatClientProperties;
import org.whisperdog.Notificationmanager;
import org.whisperdog.ToastNotification;
import org.whisperdog.ui.IconLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Panel for browsing and managing retained recordings.
 */
public class RecordingsPanel extends JPanel {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy h:mm a");
    private static final int MIN_TRUNCATION_LENGTH = 40;
    private static final int DEFAULT_TRUNCATION_LENGTH = 80;

    private final RecordingRetentionManager retentionManager;
    private final InlineAudioPlayer audioPlayer;
    private final JPanel contentPanel;
    private final JLabel headerLabel;
    private final JLabel statusLabel;
    private Timer resizeTimer;
    private int currentTruncationLength = DEFAULT_TRUNCATION_LENGTH;

    public RecordingsPanel(RecordingRetentionManager retentionManager) {
        this.retentionManager = retentionManager;
        this.audioPlayer = new InlineAudioPlayer();

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(24, 24, 24, 24));

        // Header panel with title and buttons
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        headerLabel = new JLabel("Recordings (0)");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setIcon(IconLoader.loadButton("refresh", 14));
        refreshButton.addActionListener(e -> refresh());
        headerButtons.add(refreshButton);

        JButton openFolderButton = new JButton("Open Folder");
        openFolderButton.setIcon(IconLoader.loadButton("folder", 14));
        openFolderButton.addActionListener(e -> retentionManager.openInFileManager());
        headerButtons.add(openFolderButton);

        headerPanel.add(headerButtons, BorderLayout.EAST);

        // Status label for retention enabled/disabled
        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        statusLabel.setBorder(new EmptyBorder(5, 0, 0, 0));

        // Combine header and status into north panel
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        northPanel.add(headerPanel);
        northPanel.add(statusLabel);
        northPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        add(northPanel, BorderLayout.NORTH);

        // Content panel with scroll
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Add resize listener with debouncing for responsive truncation
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize();
            }
        });

        // Initial load
        refresh();
    }

    /**
     * Handles panel resize with debouncing to avoid excessive refreshes.
     */
    private void handleResize() {
        if (resizeTimer != null && resizeTimer.isRunning()) {
            resizeTimer.restart();
        } else {
            resizeTimer = new Timer(150, e -> {
                recalculateTruncation();
                resizeTimer.stop();
            });
            resizeTimer.setRepeats(false);
            resizeTimer.start();
        }
    }

    /**
     * Recalculates the truncation length based on current panel width and refreshes.
     */
    private void recalculateTruncation() {
        int newLength = calculateMaxChars();
        if (newLength != currentTruncationLength) {
            currentTruncationLength = newLength;
            refresh();
        }
    }

    /**
     * Calculates the maximum characters for truncation based on available width.
     */
    private int calculateMaxChars() {
        int panelWidth = contentPanel.getWidth();
        if (panelWidth <= 0) {
            return DEFAULT_TRUNCATION_LENGTH;
        }
        // Approximate: 7 pixels per character at 11pt font
        // Subtract ~200px for button panel and padding
        int availableWidth = panelWidth - 200;
        int charsPerLine = Math.max(MIN_TRUNCATION_LENGTH, availableWidth / 7);
        // Allow approximately 2 lines
        return Math.min(charsPerLine * 2, 300);
    }

    /**
     * Refreshes the recordings list from the manager.
     */
    public void refresh() {
        contentPanel.removeAll();

        List<RecordingManifest.RecordingEntry> recordings = retentionManager.getRecordings();
        headerLabel.setText(String.format("Recordings (%d)", recordings.size()));

        // Update retention status indicator
        if (retentionManager.isRetentionEnabled()) {
            statusLabel.setText("Recording retention is enabled");
            statusLabel.setForeground(new Color(0, 128, 0)); // Green
        } else {
            statusLabel.setText("Recording retention is disabled - recordings will not be saved");
            statusLabel.setForeground(new Color(192, 0, 0)); // Red
        }

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
        // Outer card with border
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Main content panel
        JPanel contentWrapper = new JPanel(new BorderLayout(10, 5));
        contentWrapper.setBorder(new EmptyBorder(10, 10, 10, 10));

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

        // Transcription preview (expandable, responsive)
        String preview = entry.getTranscriptionPreview();
        if (preview != null && !preview.isEmpty()) {
            infoPanel.add(Box.createVerticalStrut(3));
            infoPanel.add(createExpandablePreview(preview, currentTruncationLength));
        }

        contentWrapper.add(infoPanel, BorderLayout.CENTER);

        // Right side: buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // Progress bar for inline playback (initially hidden)
        JProgressBar progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(0, 3));
        progressBar.setVisible(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(UIManager.getColor("Panel.background"));
        progressBar.setForeground(InlineAudioPlayer.getProgressColor());

        JButton playButton = createStyledButton("Play", "play-2");
        playButton.addActionListener(e -> handlePlayButton(entry, playButton, progressBar));
        buttonPanel.add(playButton);

        buttonPanel.add(Box.createVerticalStrut(5));

        JButton deleteButton = createStyledButton("Delete", "delete");
        deleteButton.addActionListener(e -> deleteRecording(entry));
        buttonPanel.add(deleteButton);

        contentWrapper.add(buttonPanel, BorderLayout.EAST);

        card.add(contentWrapper, BorderLayout.CENTER);
        card.add(progressBar, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Handles play button click - toggles between play and stop.
     */
    private void handlePlayButton(RecordingManifest.RecordingEntry entry, JButton playButton, JProgressBar progressBar) {
        if (audioPlayer.isPlayingEntry(entry.getId())) {
            // Currently playing this entry - stop it
            audioPlayer.stop();
        } else {
            // Not playing this entry - start playback
            File audioFile = retentionManager.getAudioFile(entry);
            if (audioFile == null || !audioFile.exists()) {
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                        "Recording file not found");
                refresh();
                return;
            }
            audioPlayer.play(audioFile, entry.getId(), playButton, progressBar);
        }
    }

    /**
     * Creates a styled button with hand cursor and FlatLaf hover/press states.
     */
    private JButton createStyledButton(String text, String iconName) {
        JButton button = new JButton(text);
        button.setIcon(IconLoader.loadButton(iconName, 14));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);

        // FlatLaf styling for consistent hover/press states
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 6; " +
                "margin: 4,8,4,8;");

        return button;
    }

    /**
     * Creates an expandable/collapsible transcription preview label.
     */
    private JLabel createExpandablePreview(String fullText, int maxLength) {
        boolean needsTruncation = fullText.length() > maxLength;
        String truncatedText = needsTruncation ? truncate(fullText, maxLength) : fullText;

        JLabel previewLabel = new JLabel();
        previewLabel.setFont(previewLabel.getFont().deriveFont(Font.ITALIC, 11f));
        previewLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        previewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (needsTruncation) {
            // Add expand indicator and make clickable
            previewLabel.setText("<html>" + truncatedText + " <u style='color:#666'>[more]</u></html>");
            previewLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            previewLabel.setToolTipText("Click to expand/collapse");

            final boolean[] expanded = {false};
            previewLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    expanded[0] = !expanded[0];
                    if (expanded[0]) {
                        previewLabel.setText("<html>" + fullText + " <u style='color:#666'>[less]</u></html>");
                    } else {
                        previewLabel.setText("<html>" + truncatedText + " <u style='color:#666'>[more]</u></html>");
                    }
                    // Trigger re-layout
                    contentPanel.revalidate();
                    contentPanel.repaint();
                }
            });
        } else {
            previewLabel.setText("<html>" + fullText + "</html>");
        }

        return previewLabel;
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
