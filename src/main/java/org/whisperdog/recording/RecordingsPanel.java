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
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.whisperdog.ConfigManager;
import org.whisperdog.ui.RecoveryDialog;

/**
 * Panel for browsing and managing retained recordings.
 */
public class RecordingsPanel extends JPanel {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy h:mm a");
    private static final int MIN_TRUNCATION_LENGTH = 50;
    private static final int DEFAULT_TRUNCATION_LENGTH = 100;
    private static final int BUTTON_ICON_SIZE = 10; // 30% smaller than 14
    private static final int STACK_BUTTONS_THRESHOLD = 568; // Stack buttons when content width < this (768 - 200 sidebar)

    private final RecordingRetentionManager retentionManager;
    private final ConfigManager configManager;
    private final RecorderForm recorderForm;
    private final InlineAudioPlayer audioPlayer;
    private final JPanel contentPanel;
    private final JLabel headerLabel;
    private final JLabel statusLabel;
    private Timer resizeTimer;
    private int currentTruncationLength = DEFAULT_TRUNCATION_LENGTH;
    private boolean firstRun = true;
    private boolean buttonsStacked = false;
    private boolean windowListenerAdded = false;
    private final java.util.HashMap<String, Boolean> expandedStates = new java.util.HashMap<>();

    /**
     * Stops any currently playing audio. Called when navigating away from this panel.
     */
    public void stopPlayback() {
        audioPlayer.stop();
    }

    public RecordingsPanel(RecordingRetentionManager retentionManager,
                           ConfigManager configManager,
                           RecorderForm recorderForm) {
        this.retentionManager = retentionManager;
        this.configManager = configManager;
        this.recorderForm = recorderForm;
        this.audioPlayer = new InlineAudioPlayer();

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(48, 24, 24, 24));

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

        JButton recoverButton = new JButton("Recover");
        recoverButton.setToolTipText("Retry failed transcriptions");
        recoverButton.addActionListener(e -> {
            PreservedRecordingScanner scanner = new PreservedRecordingScanner(configManager);
            java.util.List<PreservedRecordingScanner.RecoverableSession> sessions =
                    scanner.scan(recorderForm.isRecording());
            if (sessions.isEmpty()) {
                Notificationmanager.getInstance().showNotification(
                        ToastNotification.Type.INFO, "No recoverable recordings found.");
            } else {
                Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
                new RecoveryDialog(parentFrame, sessions, retentionManager, configManager)
                        .setVisible(true);
                refresh();
            }
        });
        headerButtons.add(recoverButton);

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
        // Listen on both this panel AND the ancestor window (more reliable on some platforms)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize();
            }
        });

        // Also listen to ancestor window resize (componentResized may not fire on nested panels)
        addHierarchyListener(e -> {
            if (!windowListenerAdded &&
                (e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    windowListenerAdded = true;
                    window.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentResized(ComponentEvent evt) {
                            handleResize();
                        }
                    });
                }
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
     * Recalculates layout based on current panel width and refreshes if needed.
     */
    private void recalculateTruncation() {
        int newLength = calculateMaxChars();
        int availableWidth = getWidth() - 48; // Same calculation as calculateMaxChars
        boolean shouldStack = availableWidth < STACK_BUTTONS_THRESHOLD;

        boolean needsRefresh = false;
        if (newLength != currentTruncationLength) {
            currentTruncationLength = newLength;
            needsRefresh = true;
        }
        if (shouldStack != buttonsStacked) {
            buttonsStacked = shouldStack;
            needsRefresh = true;
        }

        if (needsRefresh) {
            refresh();
        }
    }

    /**
     * Calculates the maximum characters for truncation based on available width.
     * Returns chars for approximately 1 line (collapsed preview).
     */
    private int calculateMaxChars() {
        // Use RecordingsPanel width minus border padding (24px left + 24px right = 48px)
        // contentPanel.getWidth() is unreliable - BoxLayout sizes to content, not viewport
        int panelWidth = getWidth() - 48;
        if (panelWidth <= 0) {
            return DEFAULT_TRUNCATION_LENGTH;
        }
        // Approximate: 7 pixels per character at 13pt bold font
        // Subtract ~160px for button panel (140px) + card padding + [more] link
        int availableWidth = panelWidth - 160;
        return Math.max(MIN_TRUNCATION_LENGTH, availableWidth / 7);
    }

    /**
     * Refreshes the recordings list from the manager.
     */
    public void refresh() {
        // On first run, sync manifest with filesystem
        if (firstRun) {
            firstRun = false;
            retentionManager.reloadManifest();
        }

        contentPanel.removeAll();

        List<RecordingManifest.RecordingEntry> recordings = retentionManager.getRecordings();
        headerLabel.setText(String.format("Recordings (%d)", recordings.size()));

        // Only show warning when retention is disabled
        if (retentionManager.isRetentionEnabled()) {
            statusLabel.setVisible(false);
        } else {
            statusLabel.setText("Recording retention is disabled - recordings will not be saved");
            statusLabel.setForeground(new Color(192, 0, 0)); // Red
            statusLabel.setVisible(true);
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
     * Uses GridBagLayout to ensure button column stays fixed width.
     */
    private JPanel createRecordingCard(RecordingManifest.RecordingEntry entry) {
        // Outer card with border
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Main content panel using GridBagLayout for strict 2-column layout
        JPanel contentWrapper = new JPanel(new GridBagLayout());
        contentWrapper.setBorder(new EmptyBorder(10, 10, 12, 10));
        GridBagConstraints gbc = new GridBagConstraints();

        // Column 0: Info panel (fills available space)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // Line 1: Transcription preview (bold, truncated) with [more]/[less] toggle
        String preview = entry.getTranscriptionPreview();
        boolean hasMoreContent = false;
        String truncatedText = "";
        if (preview != null && !preview.isEmpty()) {
            truncatedText = preview.length() > currentTruncationLength
                ? truncate(preview, currentTruncationLength)
                : preview;
            hasMoreContent = preview.length() > currentTruncationLength
                || preview.endsWith("...")
                || entry.getTranscriptionFile() != null;
        }

        // Create title row with selectable text field, copy button, and [more]/[less] link
        JPanel titleRow = new JPanel();
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Selectable title text field
        JTextField titleField = new JTextField();
        titleField.setFont(titleField.getFont().deriveFont(Font.BOLD, 13f));
        titleField.setEditable(false);
        titleField.setBorder(null);
        titleField.setOpaque(false);

        // Copy button (icon only)
        JButton copyButton = new JButton();
        copyButton.setIcon(IconLoader.loadButton("copy", BUTTON_ICON_SIZE));
        copyButton.setToolTipText("Copy text");
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyButton.setBorderPainted(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setFocusPainted(false);
        copyButton.setMargin(new Insets(0, 4, 0, 4));
        copyButton.setVisible(false); // Will show if there's text

        // [more]/[less] link label
        JLabel moreLink = new JLabel();
        moreLink.setFont(moreLink.getFont().deriveFont(13f));
        moreLink.setForeground(Color.GRAY);
        moreLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        moreLink.setVisible(false); // Will show if there's more content

        // Line 3: Full transcription area (hidden by default, shown when expanded)
        // Using JTextArea instead of HTML JLabel to respect layout constraints
        JTextArea fullTextArea = new JTextArea();
        fullTextArea.setFont(fullTextArea.getFont().deriveFont(11f));
        fullTextArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        fullTextArea.setLineWrap(true);
        fullTextArea.setWrapStyleWord(true);
        fullTextArea.setEditable(false);
        fullTextArea.setOpaque(false);
        fullTextArea.setBorder(null);
        fullTextArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        fullTextArea.setVisible(false);

        if (preview != null && !preview.isEmpty()) {
            final String truncated = truncatedText;
            final String entryId = entry.getId();
            final String[] fullText = {null};

            titleField.setText(truncated);
            copyButton.setVisible(true);

            // Copy button action - copies full text if available, otherwise preview
            copyButton.addActionListener(e -> {
                String textToCopy = fullText[0] != null ? fullText[0] : retentionManager.getFullTranscription(entry);
                if (textToCopy == null) textToCopy = preview;
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(textToCopy), null);
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.SUCCESS, "Text copied");
            });

            if (hasMoreContent) {
                // Check if this entry was previously expanded (survives refresh)
                boolean wasExpanded = expandedStates.getOrDefault(entryId, false);

                moreLink.setVisible(true);

                // Set initial state based on stored expanded state
                if (wasExpanded) {
                    moreLink.setText("[less]");
                    fullText[0] = retentionManager.getFullTranscription(entry);
                    if (fullText[0] == null) fullText[0] = preview;
                    // Will set fullTextArea text after infoPanel is added to layout
                } else {
                    moreLink.setText("[more]");
                }

                moreLink.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        boolean isExpanded = !expandedStates.getOrDefault(entryId, false);
                        expandedStates.put(entryId, isExpanded);

                        if (isExpanded) {
                            // Load full text on first expand
                            if (fullText[0] == null) {
                                fullText[0] = retentionManager.getFullTranscription(entry);
                                if (fullText[0] == null) {
                                    fullText[0] = preview;
                                }
                            }
                            // Update link to show [less]
                            moreLink.setText("[less]");
                            // JTextArea respects container width for wrapping - no pixel width needed
                            fullTextArea.setText(fullText[0]);
                            fullTextArea.setVisible(true);
                        } else {
                            // Collapse: show [more], hide full text
                            moreLink.setText("[more]");
                            fullTextArea.setVisible(false);
                        }
                        // Re-layout the card
                        infoPanel.revalidate();
                        infoPanel.repaint();
                    }
                });

                // If was expanded, set the full text visible after adding to layout
                if (wasExpanded) {
                    fullTextArea.setVisible(true);
                }
            }

            // Assemble title row: text + copy button + [more] link
            titleRow.add(titleField);
            titleRow.add(copyButton);
            if (hasMoreContent) {
                titleRow.add(Box.createHorizontalStrut(4));
                titleRow.add(moreLink);
            }
            titleRow.add(Box.createHorizontalGlue()); // Push everything left

            infoPanel.add(titleRow);
            infoPanel.add(Box.createVerticalStrut(3));
        }

        // Line 2: Date + duration + size + source (combined, small, gray, selectable)
        String dateStr = DATE_FORMAT.format(new Date(entry.getTimestamp()));
        String durationStr = formatDuration(entry.getDurationMs());
        String sizeStr = formatFileSize(entry.getFileSizeBytes());
        String sourceStr = entry.isImported() ? "Imported" : (entry.isDualSource() ? "Dual source" : "Mic only");
        JTextField metaField = new JTextField(String.format("%s | %s | %s | %s", dateStr, durationStr, sizeStr, sourceStr));
        metaField.setFont(metaField.getFont().deriveFont(11f));
        metaField.setForeground(Color.GRAY);
        metaField.setEditable(false);
        metaField.setBorder(null);
        metaField.setOpaque(false);
        metaField.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(metaField);

        // Line 3: Full transcription (added below meta, hidden by default unless restored)
        if (hasMoreContent) {
            // If restoring expanded state, set the text now
            boolean wasExpanded = expandedStates.getOrDefault(entry.getId(), false);
            if (wasExpanded) {
                String fullText = retentionManager.getFullTranscription(entry);
                if (fullText == null) fullText = preview;
                // JTextArea handles wrapping based on container width - no pixel calc needed
                fullTextArea.setText(fullText);
            }
            infoPanel.add(Box.createVerticalStrut(5));
            infoPanel.add(fullTextArea);
        }

        // Add infoPanel to column 0 (fills horizontal space, anchored top)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 0, 10);
        contentWrapper.add(infoPanel, gbc);

        // Right side: buttons (column 1, fixed width - must stay visible)
        JPanel buttonPanel = new JPanel();
        int axis = buttonsStacked ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS;
        buttonPanel.setLayout(new BoxLayout(buttonPanel, axis));
        buttonPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        // Ensure buttons always have minimum space (prevents being pushed off-screen)
        buttonPanel.setMinimumSize(new Dimension(140, 30));

        // Progress bar for inline playback (initially hidden)
        JProgressBar progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(0, 3));
        progressBar.setVisible(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(UIManager.getColor("Panel.background"));
        progressBar.setForeground(InlineAudioPlayer.getProgressColor());

        JButton playButton = createStyledButton("Play", "play-2", BUTTON_ICON_SIZE);
        playButton.addActionListener(e -> handlePlayButton(entry, playButton, progressBar));
        buttonPanel.add(playButton);

        if (buttonsStacked) {
            buttonPanel.add(Box.createVerticalStrut(5));
        } else {
            buttonPanel.add(Box.createHorizontalStrut(5));
        }

        JButton deleteButton = createStyledButton("Delete", "delete");
        deleteButton.addActionListener(e -> deleteRecording(entry));
        buttonPanel.add(deleteButton);

        // Add buttonPanel to column 1 (fixed width, anchored top)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.insets = new Insets(0, 0, 0, 0);
        contentWrapper.add(buttonPanel, gbc);

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
        return createStyledButton(text, iconName, 14);
    }

    private JButton createStyledButton(String text, String iconName, int iconSize) {
        JButton button = new JButton(text);
        button.setIcon(IconLoader.loadButton(iconName, iconSize));
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
            return seconds > 0 ? String.format("%dmin %ds", minutes, seconds) : minutes + "min";
        }
        long hours = minutes / 60;
        minutes = minutes % 60;
        return seconds > 0 ? String.format("%dh %dmin %ds", hours, minutes, seconds)
                           : String.format("%dh %dmin", hours, minutes);
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
