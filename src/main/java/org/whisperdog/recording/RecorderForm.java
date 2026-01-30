package org.whisperdog.recording;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;
import org.whisperdog.*;
import org.whisperdog.postprocessing.PostProcessingData;
import org.whisperdog.postprocessing.Pipeline;
import org.whisperdog.postprocessing.PostProcessingService;
import org.whisperdog.recording.clients.FasterWhisperTranscribeClient;
import org.whisperdog.recording.clients.OpenAITranscribeClient;
import org.whisperdog.recording.clients.OpenWebUITranscribeClient;
import org.whisperdog.error.ErrorCategory;
import org.whisperdog.error.ErrorClassifier;
import org.whisperdog.error.TranscriptionException;
import org.whisperdog.ui.TranscriptionErrorDialog;
import org.whisperdog.ui.IndeterminateProgressBar;
import org.whisperdog.ui.ProcessProgressPanel;
import org.whisperdog.recording.AudioFileAnalyzer;
import org.whisperdog.recording.AudioFileAnalyzer.AnalysisResult;
import org.whisperdog.recording.AudioFileAnalyzer.LargeFileOption;
import org.whisperdog.audio.FFmpegUtil;
import org.whisperdog.audio.SystemAudioCapture;
import org.whisperdog.audio.AudioCaptureManager;
import org.whisperdog.audio.AudioDeviceInfo;
import org.whisperdog.audio.SourceActivityTracker;
import org.whisperdog.recording.WavChunker;
import org.whisperdog.recording.FfmpegChunker;
import org.whisperdog.recording.FfmpegCompressor;
import org.whisperdog.recording.LargeFileOptionsDialog;
import org.whisperdog.recording.LargeRecordingWarningDialog;
import org.whisperdog.recording.ChunkedTranscriptionWorker;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;


public class RecorderForm extends javax.swing.JPanel {

    private final JTextArea processedText = new JTextArea(3, 20);
    private final JCheckBox enablePostProcessingCheckBox = new JCheckBox("Enable automatic post-processing");
    private final JCheckBox showPipelineSectionCheckBox = new JCheckBox("Show pipeline section");
    private JPanel postProcessingContainerPanel;
    private JLabel pipelineActiveIndicator;
    private IndeterminateProgressBar progressBar;
    private ProcessProgressPanel processProgressPanel;
    private final JButton recordButton;
    private final int baseIconSize = 40;  // Reduced from 200 for status indicator
    private final OpenAITranscribeClient whisperClient;
    private final ConfigManager configManager;
    private final FasterWhisperTranscribeClient fasterWhisperTranscribeClient;
    private final OpenWebUITranscribeClient openWebUITranscribeClient;
    private boolean isRecording = false;
    private boolean isTranscribing = false;  // Track transcription/conversion state
    private boolean isPostProcessing = false;  // Track post-processing state

    // Helper to set processing state and update tray icon
    private void setProcessingState(boolean processing) {
        setProcessingState(processing, IndeterminateProgressBar.Stage.TRANSCRIPTION);
    }

    // Overload with stage control for progress bar
    private void setProcessingState(boolean processing, IndeterminateProgressBar.Stage stage) {
        isTranscribing = processing && (stage == IndeterminateProgressBar.Stage.TRANSCRIPTION);
        isPostProcessing = processing && (stage == IndeterminateProgressBar.Stage.POST_PROCESSING);
        statusIndicatorPanel.repaint();
        TrayIconManager trayManager = AudioRecorderUI.getTrayIconManager();
        if (trayManager != null) {
            trayManager.setProcessing(processing);
        }

        // Control progress bar
        if (progressBar != null) {
            if (processing) {
                progressBar.start(stage);
            } else {
                progressBar.stop();
            }
        }
    }

    // Switch progress bar stage without stopping (for transitioning transcription -> post-processing)
    private void setProgressStage(IndeterminateProgressBar.Stage stage) {
        // Update main progress bar (for voice recording flow)
        if (progressBar != null && progressBar.isAnimating()) {
            progressBar.setStage(stage);
        }
        // Update ProcessProgressPanel progress bar (for file drop flow)
        if (processProgressPanel != null && processProgressPanel.isVisible()) {
            processProgressPanel.setProgressStage(stage);
        }
        // Update status indicator color
        isTranscribing = (stage == IndeterminateProgressBar.Stage.TRANSCRIPTION);
        isPostProcessing = (stage == IndeterminateProgressBar.Stage.POST_PROCESSING);
        statusIndicatorPanel.repaint();
    }
    private AudioRecorder recorder;
    private final JTextArea transcriptionTextArea;
    private final JPanel statusIndicatorPanel;  // Status circles instead of large logo
    private JButton copyButton;

    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(RecorderForm.class);


    private JComboBox<PostProcessingItem> postProcessingSelectComboBox;
    private List<Pipeline> pipelineList;
    private JTextArea consoleLogArea;
    private JTextField logSearchField;
    private JLabel searchResultLabel;
    private int currentSearchIndex = -1;
    private java.util.List<int[]> searchMatches = new ArrayList<>();
    private JButton runPipelineButton;
    private boolean isManualPipelineRunning = false;
    private final PipelineExecutionHistory pipelineHistory = new PipelineExecutionHistory();
    private HistoryPanel historyPanel;
    private boolean isPopulatingComboBox = false;  // Flag to prevent ItemListener firing during repopulation

    // System audio toggle and indicator
    private JCheckBox systemAudioToggle;
    private JPanel systemAudioIndicator;
    private AudioCaptureManager audioCaptureManager;

    // Device labels (showing current mic and system audio device)
    private JPanel deviceLabelsPanel;
    private JLabel microphoneLabel;
    private JLabel systemAudioLabel;

    // Recording warning timer (ISS_00007)
    private javax.swing.Timer recordingWarningTimer;
    private long recordingStartTime = 0;
    private boolean isRecordingWarningActive = false;
    private int warningPulseState = 0;  // For pulsing animation

    public RecorderForm(ConfigManager configManager) {
        this.configManager = configManager;
        this.whisperClient = new OpenAITranscribeClient(configManager);
        this.fasterWhisperTranscribeClient = new FasterWhisperTranscribeClient(configManager);
        this.openWebUITranscribeClient = new OpenWebUITranscribeClient(configManager);


        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(30, 50, 10, 50));

        // Create status indicator panel (small circles next to button)
        statusIndicatorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        statusIndicatorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status indicator (small circle that changes color)
        JPanel statusCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Status colors: green (ready), red (recording), orange-pulsing (warning), blue (transcribing), purple (post-processing)
                Color fillColor = new Color(144, 238, 144); // Light green - ready/idle
                if (isRecording) {
                    if (isRecordingWarningActive) {
                        // Pulsing orange-red warning color
                        int pulse = (warningPulseState % 10);
                        int intensity = pulse < 5 ? 200 + pulse * 11 : 255 - (pulse - 5) * 11;
                        fillColor = new Color(255, Math.max(50, 150 - pulse * 10), 0);
                    } else {
                        fillColor = new Color(255, 99, 71); // Tomato red - recording
                    }
                } else if (isPostProcessing) {
                    fillColor = new Color(138, 103, 201); // Bluish purple - post-processing
                } else if (isTranscribing) {
                    fillColor = new Color(100, 149, 237); // Cornflower blue - transcribing/converting
                }
                g2.setColor(fillColor);
                g2.fillOval(2, 2, 16, 16);

                // Border - thicker when warning active
                if (isRecordingWarningActive && isRecording) {
                    g2.setColor(new Color(255, 69, 0)); // Orange-red border
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(1, 1, 18, 18);
                } else {
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawOval(2, 2, 16, 16);
                }
                g2.dispose();
            }
        };
        statusCircle.setPreferredSize(new Dimension(20, 20));
        statusCircle.setOpaque(false);

        recordButton = new JButton("Start Recording");
        recordButton.addActionListener(e -> {
            toggleRecording();
        });

        statusIndicatorPanel.add(statusCircle);
        statusIndicatorPanel.add(recordButton);

        // System audio indicator (small circle showing system capture state)
        systemAudioIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fillColor;
                if (isRecording) {
                    fillColor = new Color(0, 191, 255); // Deep sky blue - system audio active
                } else {
                    fillColor = new Color(100, 200, 200); // Teal - system audio enabled, idle
                }
                g2.setColor(fillColor);
                g2.fillOval(2, 2, 12, 12);
                g2.setColor(Color.DARK_GRAY);
                g2.drawOval(2, 2, 12, 12);
                g2.dispose();
            }
        };
        systemAudioIndicator.setPreferredSize(new Dimension(16, 16));
        systemAudioIndicator.setOpaque(false);
        systemAudioIndicator.setToolTipText("System audio capture active");
        systemAudioIndicator.setVisible(configManager.isSystemAudioEnabled());

        // System audio toggle - only visible when WASAPI loopback is available
        systemAudioToggle = new JCheckBox("System Audio");
        systemAudioToggle.setToolTipText("Include system audio (what you hear) in the recording");
        systemAudioToggle.setSelected(configManager.isSystemAudioEnabled());
        systemAudioToggle.addActionListener(e -> {
            boolean enabled = systemAudioToggle.isSelected();
            configManager.setSystemAudioEnabled(enabled);
            systemAudioIndicator.setVisible(enabled);

            // On-the-fly toggle during active recording
            if (isRecording && audioCaptureManager != null) {
                try {
                    boolean success = audioCaptureManager.toggleSystemAudio(enabled);
                    if (!success) {
                        // Revert toggle if operation failed
                        systemAudioToggle.setSelected(!enabled);
                        configManager.setSystemAudioEnabled(!enabled);
                        systemAudioIndicator.setVisible(!enabled);
                        logger.warn("Failed to toggle system audio during recording");
                    }
                } catch (Exception ex) {
                    // Catch any exceptions to prevent UI crash
                    logger.error("Exception while toggling system audio: {}", ex.getMessage(), ex);
                    // Revert toggle
                    systemAudioToggle.setSelected(!enabled);
                    configManager.setSystemAudioEnabled(!enabled);
                    systemAudioIndicator.setVisible(!enabled);
                }
            }

            statusIndicatorPanel.revalidate();
            statusIndicatorPanel.repaint();
            refreshDeviceLabels();  // Update device labels visibility
        });
        if (SystemAudioCapture.isAvailable()) {
            statusIndicatorPanel.add(systemAudioIndicator);
            statusIndicatorPanel.add(systemAudioToggle);
        }

        // Device labels panel (shows current mic and system audio device)
        deviceLabelsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 2));
        deviceLabelsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        microphoneLabel = new JLabel();
        microphoneLabel.setForeground(Color.GRAY);
        microphoneLabel.setFont(microphoneLabel.getFont().deriveFont(Font.PLAIN, 11f));
        microphoneLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        microphoneLabel.setToolTipText("Click to open audio settings");
        microphoneLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                navigateToSettings();
            }
        });

        systemAudioLabel = new JLabel();
        systemAudioLabel.setForeground(Color.GRAY);
        systemAudioLabel.setFont(systemAudioLabel.getFont().deriveFont(Font.PLAIN, 11f));
        systemAudioLabel.setVisible(false); // Only shown when system audio enabled

        deviceLabelsPanel.add(microphoneLabel);
        deviceLabelsPanel.add(systemAudioLabel);

        JPanel transcriptionPanel = new JPanel();
        transcriptionPanel.setLayout(new BoxLayout(transcriptionPanel, BoxLayout.Y_AXIS));

        transcriptionTextArea = new JTextArea(3, 20);
        transcriptionTextArea.setLineWrap(true);
        transcriptionTextArea.setWrapStyleWord(true);
        transcriptionTextArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Add document listener to update Run Pipeline button state
        transcriptionTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateRunPipelineButtonState(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateRunPipelineButtonState(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateRunPipelineButtonState(); }
        });

        JScrollPane transcriptionTextScrollPane = new JScrollPane(transcriptionTextArea);
        transcriptionTextScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        transcriptionTextScrollPane.setMinimumSize(new Dimension(600, transcriptionTextArea.getPreferredSize().height + 10));

        transcriptionPanel.add(transcriptionTextScrollPane);

        copyButton = new JButton("Copy");
        copyButton.setToolTipText("Copy transcription to clipboard");
        copyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        copyButton.addActionListener(e -> copyTranscriptionToClipboard(transcriptionTextArea.getText()));

        // Add components to center panel with proper spacing
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(statusIndicatorPanel);  // Status indicator + record button
        centerPanel.add(Box.createVerticalStrut(5));   // Small gap before device labels
        centerPanel.add(deviceLabelsPanel);            // Device labels (mic + system audio)
        centerPanel.add(Box.createVerticalStrut(15));  // Section spacing
        centerPanel.add(transcriptionPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(copyButton);
        centerPanel.add(Box.createVerticalStrut(15));

        // Drag & drop hint
        JLabel dragDropLabel = new JLabel("Drag & drop an audio or video file here.");
        dragDropLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        dragDropLabel.setForeground(Color.GRAY);
        centerPanel.add(dragDropLabel);
        centerPanel.add(Box.createVerticalStrut(20));  // Section spacing
        centerPanel.add(Box.createVerticalGlue());

        // TransferHandler is set up in setupDragAndDrop() at the end of constructor


        // Indeterminate progress bar for transcription/post-processing
        progressBar = new IndeterminateProgressBar();
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Process progress panel for file-based operations (drag & drop, chunked transcription)
        processProgressPanel = new ProcessProgressPanel();
        processProgressPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        processProgressPanel.setOnCancel(this::cancelCurrentOperation);
        processProgressPanel.setOnRetry(this::retryCurrentOperation);
        processProgressPanel.setOnDismiss(() -> processProgressPanel.hidePanel());

        // Pipeline section header with visibility toggle and status indicator
        JPanel pipelineSectionHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pipelineSectionHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        pipelineSectionHeader.setBorder(new EmptyBorder(0, 50, 0, 50));

        showPipelineSectionCheckBox.setToolTipText(
            "<html>Show or hide the pipeline processing section.<br>" +
            "Auto-processing can still run when section is hidden.</html>");
        showPipelineSectionCheckBox.setSelected(configManager.isShowPipelineSection());

        pipelineActiveIndicator = new JLabel("\u26A1 Auto-pipeline active");
        pipelineActiveIndicator.setForeground(new Color(255, 165, 0)); // Orange color
        pipelineActiveIndicator.setToolTipText("Automatic post-processing is enabled but section is hidden");
        pipelineActiveIndicator.setVisible(false);

        pipelineSectionHeader.add(showPipelineSectionCheckBox);
        pipelineSectionHeader.add(pipelineActiveIndicator);

        postProcessingContainerPanel = new JPanel();
        postProcessingContainerPanel.setLayout(new BoxLayout(postProcessingContainerPanel, BoxLayout.Y_AXIS));
        postProcessingContainerPanel.setBorder(new EmptyBorder(10, 50, 50, 50));
        postProcessingContainerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Responsive options panel with FlowLayout (flexbox-style)
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // New checkbox to control auto-paste from clipboard
        JCheckBox autoPasteCheckBox = new JCheckBox("Paste from clipboard (Ctrl+V)");
        autoPasteCheckBox.setSelected(configManager.isAutoPasteEnabled());
        autoPasteCheckBox.addActionListener(e -> {
            configManager.setAutoPasteEnabled(autoPasteCheckBox.isSelected());
        });

        // enablePostProcessingCheckBox controls automatic post-processing after each recording
        // The dropdown and Run Pipeline button allow manual execution even when this is unchecked
        enablePostProcessingCheckBox.setToolTipText(
            "<html>When enabled, automatically runs the selected pipeline after each recording.<br>" +
            "You can still manually run pipelines using the 'Run Pipeline' button when this is off.</html>");

        // Show pipeline section checkbox controls visibility of the container panel
        showPipelineSectionCheckBox.addActionListener(e -> {
            boolean show = showPipelineSectionCheckBox.isSelected();
            configManager.setShowPipelineSection(show);
            postProcessingContainerPanel.setVisible(show);
            updatePipelineActiveIndicator();
        });

        JCheckBox loadOnStartupCheckBox = new JCheckBox("Activate on startup");
        loadOnStartupCheckBox.setVisible(false); // initially hidden
        loadOnStartupCheckBox.addActionListener(e -> {
            if (loadOnStartupCheckBox.isSelected()) {
                configManager.setPostProcessingOnStartup(true);
            } else {
                configManager.setPostProcessingOnStartup(false);
            }
        });

        JLabel selectLabel = new JLabel("Pipeline:");
        selectLabel.setToolTipText("Select a pipeline for automatic or manual execution");

        postProcessingSelectComboBox = new JComboBox<>();
        postProcessingSelectComboBox.setToolTipText("Select a pipeline to use");
        populatePostProcessingComboBox();
        postProcessingSelectComboBox.addItemListener(e -> {
            // Ignore events during programmatic repopulation to prevent corrupting saved selection
            if (isPopulatingComboBox) return;

            if (e.getStateChange() == ItemEvent.SELECTED) {
                PostProcessingItem selectedItem = (PostProcessingItem) e.getItem();
                if (selectedItem != null) {
                    // Save selection to remember across refreshes and sessions
                    configManager.setLastUsedPipelineUUID(selectedItem.uuid);
                }
                updateRunPipelineButtonState();
            }
        });

        // Pipeline selection panel - always visible so user can run pipelines manually
        JPanel pipelineSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pipelineSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pipelineSelectionPanel.add(selectLabel);
        pipelineSelectionPanel.add(postProcessingSelectComboBox);

        // Run Pipeline button for manual pipeline execution
        runPipelineButton = new JButton("Run Pipeline", new FlatSVGIcon("icon/svg/play.svg", 14, 14));
        runPipelineButton.setToolTipText("Run selected pipeline on transcription text");
        runPipelineButton.setEnabled(false);
        runPipelineButton.addActionListener(e -> runManualPipeline());
        pipelineSelectionPanel.add(runPipelineButton);

        // Add controls to responsive options panel
        optionsPanel.add(autoPasteCheckBox);
        optionsPanel.add(enablePostProcessingCheckBox);
        optionsPanel.add(loadOnStartupCheckBox);

        postProcessingContainerPanel.add(optionsPanel);
        postProcessingContainerPanel.add(Box.createVerticalStrut(10));
        postProcessingContainerPanel.add(pipelineSelectionPanel);


        processedText.setLineWrap(true);
        processedText.setWrapStyleWord(true);
        processedText.setRows(3);
        processedText.setMinimumSize(new Dimension(Integer.MAX_VALUE, processedText.getPreferredSize().height));
        processedText.setAlignmentX(Component.LEFT_ALIGNMENT);
        //processedText.setVisible(false);

        JScrollPane processedTextScrollPane = new JScrollPane(processedText);
        processedTextScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        processedTextScrollPane.setMinimumSize(new Dimension(600, processedText.getPreferredSize().height + 10));

        processedTextScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel additionalTextLabel = new JLabel("Post Processed text:");
        additionalTextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        postProcessingContainerPanel.add(additionalTextLabel);
        postProcessingContainerPanel.add(processedTextScrollPane);


        JButton copyProcessedTextButton = new JButton("Copy");
        JPanel copyButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        copyButtonPanel.add(copyProcessedTextButton);
        copyButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyProcessedTextButton.addActionListener(e -> copyTranscriptionToClipboard(processedText.getText()));
        postProcessingContainerPanel.add(Box.createVerticalStrut(10));
        postProcessingContainerPanel.add(copyButtonPanel);

        // History panel for pipeline execution results
        historyPanel = new HistoryPanel();
        historyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        postProcessingContainerPanel.add(Box.createVerticalStrut(5));
        postProcessingContainerPanel.add(historyPanel);

        // Load saved state for post-processing checkbox
        enablePostProcessingCheckBox.setSelected(configManager.isPostProcessingEnabled());

        enablePostProcessingCheckBox.addActionListener(e -> {
            boolean selected = enablePostProcessingCheckBox.isSelected();
            // Save state when changed
            configManager.setPostProcessingEnabled(selected);
            // Show/hide the "Activate on startup" checkbox
            loadOnStartupCheckBox.setVisible(selected);
            updatePipelineActiveIndicator();
            postProcessingContainerPanel.revalidate();
            postProcessingContainerPanel.repaint();
        });

        // Trigger initial UI setup based on loaded state
        if (enablePostProcessingCheckBox.isSelected()) {
            loadOnStartupCheckBox.setVisible(true);
        }

        if (configManager.isPostProcessingOnStartup()) {
            loadOnStartupCheckBox.setSelected(true);
            if (!enablePostProcessingCheckBox.isSelected()) {
                enablePostProcessingCheckBox.doClick();
            }
        }

        // Set initial visibility of pipeline section based on saved config
        boolean showSection = configManager.isShowPipelineSection();
        postProcessingContainerPanel.setVisible(showSection);
        updatePipelineActiveIndicator();

        // Console log panel
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBorder(BorderFactory.createTitledBorder("Execution Log"));
        consoleLogArea = new JTextArea(15, 20);  // Increased from 8 to 15 rows for better visibility
        consoleLogArea.setEditable(false);
        consoleLogArea.setLineWrap(true);
        consoleLogArea.setWrapStyleWord(true);
        consoleLogArea.setFont(org.whisperdog.ui.FontUtil.getMonospacedFont(Font.PLAIN, 11));
        JScrollPane consoleScrollPane = new JScrollPane(consoleLogArea);
        consoleScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);

        // Add search and clear buttons for console log
        JPanel consoleButtonPanel = new JPanel(new BorderLayout(5, 0));

        // Search panel on the left
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        logSearchField = new JTextField(15);
        logSearchField.setToolTipText("Search in logs (Enter to find next)");
        logSearchField.addActionListener(e -> findNextInLog());
        logSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { performLogSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { performLogSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { performLogSearch(); }
        });

        JButton findPrevButton = new JButton(new FlatSVGIcon("icon/svg/chevron-up.svg", 14, 14));
        findPrevButton.setToolTipText("Find previous (Shift+Enter)");
        findPrevButton.setMargin(new Insets(2, 4, 2, 4));
        findPrevButton.addActionListener(e -> findPreviousInLog());

        JButton findNextButton = new JButton(new FlatSVGIcon("icon/svg/chevron-down.svg", 14, 14));
        findNextButton.setToolTipText("Find next (Enter)");
        findNextButton.setMargin(new Insets(2, 4, 2, 4));
        findNextButton.addActionListener(e -> findNextInLog());

        searchResultLabel = new JLabel("");
        searchResultLabel.setForeground(Color.GRAY);

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(logSearchField);
        searchPanel.add(findPrevButton);
        searchPanel.add(findNextButton);
        searchPanel.add(searchResultLabel);

        // Clear button on the right
        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.setToolTipText("Clear the execution log");
        clearLogButton.addActionListener(e -> {
            consoleLogArea.setText("");
            clearLogSearch();
        });
        clearPanel.add(clearLogButton);

        consoleButtonPanel.add(searchPanel, BorderLayout.WEST);
        consoleButtonPanel.add(clearPanel, BorderLayout.EAST);
        consolePanel.add(consoleButtonPanel, BorderLayout.SOUTH);

        // Register console with ConsoleLogger singleton
        ConsoleLogger.getInstance().setConsoleArea(consoleLogArea);

        checkSettings();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(centerPanel)
                        .addComponent(progressBar)
                        .addComponent(processProgressPanel)
                        .addComponent(pipelineSectionHeader)
                        .addComponent(postProcessingContainerPanel)
                        .addComponent(consolePanel)
        );

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(centerPanel)
                        .addGap(5)
                        .addComponent(progressBar, 3, 3, 3)  // Fixed 3px height
                        .addGap(5)
                        .addComponent(processProgressPanel)  // Shows during file-based operations
                        .addGap(5)
                        .addComponent(pipelineSectionHeader)
                        .addGap(5)
                        .addComponent(postProcessingContainerPanel)
                        .addGap(10)  // Reduced spacing between sections (was 20)
                        .addComponent(consolePanel, 250, 300, 350)  // Increased height for taller log (was 150)
                        .addContainerGap(10, Short.MAX_VALUE)  // Reduced bottom margin
        );

        // Enable drag & drop for audio files
        setupDragAndDrop(centerPanel);

        // Initialize device labels with current settings
        refreshDeviceLabels();

        // Refresh device labels when app window gains focus (e.g., after changing Windows audio settings)
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.PARENT_CHANGED) != 0) {
                // When added to window hierarchy, attach window focus listener
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
                        @Override
                        public void windowGainedFocus(java.awt.event.WindowEvent e) {
                            if (isShowing()) {
                                AudioDeviceInfo.clearCache();
                                refreshDeviceLabels();
                            }
                        }
                        @Override
                        public void windowLostFocus(java.awt.event.WindowEvent e) {}
                    });
                }
            }
        });
    }

    /**
     * Sets up drag and drop support for audio files.
     */
    private void setupDragAndDrop(JPanel panel) {
        panel.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                // Check if the data flavor is supported - this is safe to call during drag-over
                // Note: Do NOT access getTransferData() here as it causes InvalidDnDOperationException
                // during the drag-over phase. Only checking the flavor type is safe and enables
                // proper cursor feedback when dragging files over the drop zone.
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (files != null && files.size() == 1) {
                        File file = files.get(0);
                        String fileName = file.getName().toLowerCase();

                        // Check for video files first (MP4, MOV, MKV, AVI, WEBM)
                        if (FFmpegUtil.isVideoFile(file)) {
                            handleDroppedVideoFile(file);
                            return true;
                        }

                        // Accept audio files: WAV, MP3, OGG, M4A, FLAC
                        if (fileName.endsWith(".wav") || fileName.endsWith(".mp3") ||
                            fileName.endsWith(".ogg") || fileName.endsWith(".m4a") ||
                            fileName.endsWith(".flac")) {
                            handleDroppedAudioFile(file);
                            return true;
                        } else {
                            Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                                    "Unsupported file type. Please drop audio (WAV, MP3, OGG, M4A, FLAC) or video (" +
                                    FFmpegUtil.getSupportedFormatsDisplay() + ") files.");
                            return false;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error importing dropped file", e);
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                            "Error importing file: " + e.getMessage());
                }
                return false;
            }
        });
    }

    /**
     * Handles a dropped audio file by converting if necessary and transcribing.
     * Includes pre-flight analysis for large files and chunking support.
     */
    private void handleDroppedAudioFile(File file) {
        logger.info("Dropped file: " + file.getName());
        ConsoleLogger console = ConsoleLogger.getInstance();

        // Update UI state (don't start main progress bar - ProcessProgressPanel has its own)
        isTranscribing = true;
        statusIndicatorPanel.repaint();
        TrayIconManager trayManager = AudioRecorderUI.getTrayIconManager();
        if (trayManager != null) {
            trayManager.setProcessing(true);
        }
        recordButton.setText("Analyzing...");
        recordButton.setEnabled(false);

        // Show progress panel for file-based operations (has its own progress bar)
        showProgressPanel(file, "Analyzing audio file...", IndeterminateProgressBar.Stage.TRANSCRIPTION);

        // Pre-flight analysis
        AnalysisResult analysis = AudioFileAnalyzer.analyze(file);

        // Check if file exceeds API limit and we're using OpenAI
        if (analysis.exceedsApiLimit && "OpenAI".equals(configManager.getWhisperServer())) {
            handleLargeFile(file, analysis);
            return;
        }

        // Normal flow for files within limits
        processAudioFile(file);
    }

    /**
     * Handles a dropped video file by extracting audio via ffmpeg and transcribing.
     * Supports MP4, MOV, MKV, AVI, and WEBM formats.
     */
    private void handleDroppedVideoFile(File videoFile) {
        logger.info("Dropped video file: " + videoFile.getName());
        ConsoleLogger console = ConsoleLogger.getInstance();

        // Check ffmpeg availability first
        if (!FFmpegUtil.isFFmpegAvailable()) {
            logger.warn("FFmpeg not available for video extraction");
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                    "Video transcription requires ffmpeg.\n" +
                    "Install ffmpeg and ensure it's in your PATH.\n" +
                    "Download: https://ffmpeg.org/download.html");
            console.logError("FFmpeg not available - cannot extract audio from video");
            console.log("Install ffmpeg from: https://ffmpeg.org/download.html");
            return;
        }

        // Update UI state (but don't start main progress bar - ProcessProgressPanel has its own)
        isTranscribing = true;
        statusIndicatorPanel.repaint();
        TrayIconManager trayManager = AudioRecorderUI.getTrayIconManager();
        if (trayManager != null) {
            trayManager.setProcessing(true);
        }
        recordButton.setText("Extracting audio...");
        recordButton.setEnabled(false);

        // Show progress panel for extraction (has its own progress bar)
        showProgressPanel(videoFile, "Extracting audio from video...", IndeterminateProgressBar.Stage.TRANSCRIPTION);

        console.separator();
        console.log("Extracting audio from video: " + videoFile.getName());

        // Run extraction asynchronously
        FFmpegUtil.extractAudioAsync(videoFile, progress -> {
            // Update progress on EDT
            SwingUtilities.invokeLater(() -> {
                int percent = (int) (progress * 100);
                updateProgressPanelStage("Extracting audio... " + percent + "%");
            });
        }).thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                if (result.success) {
                    console.logSuccess("Audio extracted successfully");
                    console.log("Extracted file: " + result.audioFile.getName() +
                               " (" + (result.audioFile.length() / 1024) + " KB)");

                    // Register temp file for cleanup
                    registerExtractedTempFile(result.audioFile);

                    // Update status and proceed with transcription
                    updateProgressPanelStage("Starting transcription...");
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.INFO,
                            "Audio extracted. Starting transcription...");

                    // Feed extracted audio into existing pipeline
                    handleDroppedAudioFile(result.audioFile);
                } else if (result.noAudioTrack) {
                    console.logError("Video has no audio track");
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                            "This video contains no audio track.");
                    processProgressPanel.showError("No audio track in video");
                    resetUIAfterTranscription();
                } else {
                    console.logError("Extraction failed: " + result.errorMessage);
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                            "Failed to extract audio: " + result.errorMessage);
                    processProgressPanel.showError("Extraction failed");
                    resetUIAfterTranscription();
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                logger.error("Video extraction failed", ex);
                console.logError("Extraction failed: " + ex.getMessage());
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                        "Failed to extract audio from video.");
                processProgressPanel.showError("Extraction failed");
                resetUIAfterTranscription();
            });
            return null;
        });
    }

    // ========== Video Extraction Temp File Management ==========

    private final java.util.List<File> extractedTempFiles = new java.util.ArrayList<>();

    /**
     * Registers an extracted audio file for cleanup after transcription.
     */
    private void registerExtractedTempFile(File file) {
        extractedTempFiles.add(file);
    }

    /**
     * Cleans up all extracted temp files.
     * Called after transcription completes or on application exit.
     */
    private void cleanupExtractedTempFiles() {
        for (File file : extractedTempFiles) {
            if (file != null && file.exists()) {
                try {
                    if (file.delete()) {
                        logger.debug("Cleaned up temp file: " + file.getName());
                    } else {
                        logger.warn("Could not delete temp file: " + file.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.warn("Error deleting temp file: " + e.getMessage());
                }
            }
        }
        extractedTempFiles.clear();
    }

    /**
     * Handles a large audio file that exceeds the API limit.
     * Shows options dialog or uses saved preference.
     */
    private void handleLargeFile(File file, AnalysisResult analysis) {
        ConsoleLogger console = ConsoleLogger.getInstance();
        console.log("File exceeds 25MB API limit - showing options...");

        // Check for saved preference
        String savedOption = configManager.getLargeFileDefaultOption();
        LargeFileOption selectedOption = null;

        if (!savedOption.isEmpty()) {
            try {
                selectedOption = LargeFileOption.valueOf(savedOption);
                console.log("Using saved preference: " + selectedOption.getDescription());
            } catch (IllegalArgumentException e) {
                // Invalid saved option, will show dialog
                configManager.clearLargeFileDefaultOption();
            }
        }

        // Show dialog if no valid saved preference
        if (selectedOption == null) {
            LargeFileOptionsDialog dialog = new LargeFileOptionsDialog(
                SwingUtilities.getWindowAncestor(this),
                analysis,
                configManager
            );
            selectedOption = dialog.showAndGetSelection();
        }

        // Handle the selected option
        switch (selectedOption) {
            case SPLIT_AND_TRANSCRIBE:
                handleChunkedTranscription(file, analysis);
                break;
            case COMPRESS_FIRST:
                handleCompressAndTranscribe(file);
                break;
            case USE_LOCAL_WHISPER:
                handleLocalWhisperTranscription(file);
                break;
            case CANCEL:
            default:
                console.log("Large file handling cancelled");
                resetUIAfterTranscription();
                break;
        }
    }

    /**
     * Processes a normal audio file (within API limits).
     */
    private void processAudioFile(File file) {
        ConsoleLogger console = ConsoleLogger.getInstance();

        recordButton.setText("Converting...");
        updateProgressPanelStage("Preparing audio file...");

        File fileToTranscribe = file;

        // Check if OGG file and convert to WAV (FLAC is natively supported by OpenAI, no conversion needed)
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".ogg")) {
            logger.info("Converting OGG file to WAV...");
            console.log("Converting OGG file to WAV using ffmpeg...");
            updateProgressPanelStage("Converting OGG to WAV...");
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.INFO,
                    "Converting OGG to WAV...");
            fileToTranscribe = convertOggToWav(file);
            if (fileToTranscribe == null) {
                console.logError("Failed to convert OGG file");
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                        "Failed to convert OGG file. Please convert to WAV manually.");
                processProgressPanel.showError("Failed to convert OGG file");
                resetUIAfterTranscription();
                return;
            }
            console.logSuccess("Successfully converted OGG to WAV");
        } else if (fileName.endsWith(".flac")) {
            console.log("FLAC file detected - using directly (no conversion needed)");
        }

        // Transcribe the file
        logger.info("Transcribing audio using " + configManager.getWhisperServer());
        console.separator();
        console.log("Starting transcription using " + configManager.getWhisperServer());
        console.log("Audio file: " + fileToTranscribe.getName());
        updateProgressPanelStage("Transcribing with " + configManager.getWhisperServer() + "...");
        Notificationmanager.getInstance().showNotification(ToastNotification.Type.INFO,
                "Transcribing audio file...");
        new AudioTranscriptionWorker(fileToTranscribe).execute();
    }

    /**
     * Handles chunked transcription for large files.
     * Splits the file into chunks and transcribes each sequentially.
     */
    private void handleChunkedTranscription(File file, AnalysisResult analysis) {
        ConsoleLogger console = ConsoleLogger.getInstance();

        console.separator();
        console.log("Starting chunked transcription for large file...");
        recordButton.setText("Splitting file...");

        // Run chunking in background to not block UI
        new SwingWorker<java.util.List<File>, Void>() {
            @Override
            protected java.util.List<File> doInBackground() {
                // Choose chunking method based on format
                if (analysis.canSplitNatively && "wav".equals(analysis.format)) {
                    console.log("Using native WAV chunking (no FFmpeg needed)");
                    WavChunker.ChunkResult result = WavChunker.splitWavFileBySize(
                        file, AudioFileAnalyzer.TARGET_CHUNK_SIZE);
                    if (result.success) {
                        return result.chunks;
                    } else {
                        console.logError("WAV chunking failed: " + result.errorMessage);
                        return null;
                    }
                } else if (analysis.ffmpegAvailable) {
                    console.log("Using FFmpeg chunking");
                    FfmpegChunker.ChunkResult result = FfmpegChunker.splitBySize(
                        file, AudioFileAnalyzer.TARGET_CHUNK_SIZE);
                    if (result.success) {
                        return result.chunks;
                    } else {
                        console.logError("FFmpeg chunking failed: " + result.errorMessage);
                        return null;
                    }
                } else {
                    console.logError("No chunking method available (FFmpeg not installed)");
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    java.util.List<File> chunks = get();
                    if (chunks == null || chunks.isEmpty()) {
                        Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                            "Failed to split file. Check logs for details.");
                        resetUIAfterTranscription();
                        return;
                    }

                    // Start chunked transcription
                    startChunkedTranscription(chunks);

                } catch (Exception e) {
                    logger.error("Error during file chunking", e);
                    console.logError("Chunking failed: " + e.getMessage());
                    resetUIAfterTranscription();
                }
            }
        }.execute();
    }

    /**
     * Starts transcription of chunked audio files.
     */
    private void startChunkedTranscription(java.util.List<File> chunks) {
        ConsoleLogger console = ConsoleLogger.getInstance();

        console.log("Starting transcription of " + chunks.size() + " chunks...");
        recordButton.setText("Transcribing 1/" + chunks.size() + "...");

        ChunkedTranscriptionWorker worker = new ChunkedTranscriptionWorker(
            chunks, configManager, new ChunkedTranscriptionWorker.Callback() {
                @Override
                public void onProgress(ChunkedTranscriptionWorker.Progress progress) {
                    recordButton.setText(String.format("Transcribing %d/%d...",
                        progress.currentChunk + 1, progress.totalChunks));
                }

                @Override
                public void onComplete(String fullTranscript) {
                    transcriptionTextArea.setText(fullTranscript);

                    // Start new history session for this transcription
                    pipelineHistory.startNewSession(fullTranscript);
                    processedText.setText("");
                    historyPanel.updateResults(pipelineHistory.getResults());

                    console.logSuccess("Chunked transcription completed");
                    console.logTranscript(fullTranscript);
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.SUCCESS,
                        "Transcription completed!");

                    // Handle post-processing if enabled
                    handlePostTranscriptionActions(fullTranscript);

                    resetUIAfterTranscription();
                    updateTrayMenu();
                }

                @Override
                public void onError(String errorMessage) {
                    console.logError("Chunked transcription failed: " + errorMessage);
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                        "Transcription failed. See logs for details.");
                    resetUIAfterTranscription();
                }

                @Override
                public void onCancelled() {
                    console.log("Chunked transcription cancelled");
                    resetUIAfterTranscription();
                }
            }
        );

        worker.execute();
    }

    /**
     * Handles compression and transcription for large files.
     * Compresses the file first, then transcribes if within limits.
     */
    private void handleCompressAndTranscribe(File file) {
        ConsoleLogger console = ConsoleLogger.getInstance();

        console.separator();
        console.log("Compressing file before transcription...");
        recordButton.setText("Compressing...");

        new SwingWorker<FfmpegCompressor.CompressionResult, Void>() {
            @Override
            protected FfmpegCompressor.CompressionResult doInBackground() {
                return FfmpegCompressor.compress(file);
            }

            @Override
            protected void done() {
                try {
                    FfmpegCompressor.CompressionResult result = get();
                    if (!result.success) {
                        console.logError("Compression failed: " + result.errorMessage);
                        Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                            "Compression failed: " + result.errorMessage);
                        resetUIAfterTranscription();
                        return;
                    }

                    if (!result.withinLimit) {
                        // Still too large - need to split
                        console.log("Compressed file still exceeds limit - switching to chunking");
                        AnalysisResult newAnalysis = AudioFileAnalyzer.analyze(result.compressedFile);
                        handleChunkedTranscription(result.compressedFile, newAnalysis);
                    } else {
                        // Compressed file is within limits - proceed with normal transcription
                        console.logSuccess("Compression successful - proceeding with transcription");
                        processAudioFile(result.compressedFile);
                    }

                } catch (Exception e) {
                    logger.error("Error during compression", e);
                    console.logError("Compression failed: " + e.getMessage());
                    resetUIAfterTranscription();
                }
            }
        }.execute();
    }

    /**
     * Handles transcription using local Whisper (Faster-Whisper).
     * Temporarily switches the server setting and transcribes.
     */
    private void handleLocalWhisperTranscription(File file) {
        ConsoleLogger console = ConsoleLogger.getInstance();

        console.separator();
        console.log("Using Faster-Whisper for large file (no size limit)");

        // Note: We don't change the global setting, just use the local client directly
        recordButton.setText("Transcribing (local)...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return fasterWhisperTranscribeClient.transcribe(file);
                } catch (Exception e) {
                    logger.error("Faster-Whisper transcription failed", e);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    String transcript = get();
                    if (transcript != null && !transcript.isEmpty()) {
                        transcriptionTextArea.setText(transcript);

                        // Start new history session
                        pipelineHistory.startNewSession(transcript);
                        processedText.setText("");
                        historyPanel.updateResults(pipelineHistory.getResults());

                        console.logSuccess("Transcription completed");
                        console.logTranscript(transcript);
                        Notificationmanager.getInstance().showNotification(ToastNotification.Type.SUCCESS,
                            "Transcription completed!");

                        handlePostTranscriptionActions(transcript);
                    } else {
                        console.logError("Faster-Whisper returned empty transcript");
                        Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                            "Transcription failed or returned empty result");
                    }
                } catch (Exception e) {
                    logger.error("Error getting transcription result", e);
                    console.logError("Transcription failed: " + e.getMessage());
                }
                resetUIAfterTranscription();
                updateTrayMenu();
            }
        }.execute();
    }

    /**
     * Handles post-transcription actions like auto post-processing and clipboard copy.
     */
    private void handlePostTranscriptionActions(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return;
        }

        // Run post-processing if enabled
        if (enablePostProcessingCheckBox.isSelected() && postProcessingSelectComboBox.getSelectedItem() != null) {
            PostProcessingItem selectedItem = (PostProcessingItem) postProcessingSelectComboBox.getSelectedItem();
            if (selectedItem != null && selectedItem.uuid != null) {
                Pipeline pipeline = configManager.getPipelineByUuid(selectedItem.uuid);
                if (pipeline != null) {
                    setProgressStage(IndeterminateProgressBar.Stage.POST_PROCESSING);  // Switch to orange
                    new PostProcessingWorker(transcript, pipeline).execute();
                    return;  // PostProcessingWorker will handle clipboard
                }
            }
        }

        // No post-processing - handle clipboard directly
        if (configManager.isAutoPasteEnabled()) {
            transcriptionTextArea.transferFocus();
            copyTranscriptionToClipboard(transcript);
            pasteFromClipboard();
        }
        playFinishSound();
    }

    /**
     * Converts an OGG file to WAV format using ffmpeg.
     * Falls back to AudioSystem if ffmpeg is not available.
     */
    private File convertOggToWav(File oggFile) {
        // Try ffmpeg first (much more reliable for OGG files)
        File wavFile = convertOggToWavUsingFfmpeg(oggFile);
        if (wavFile != null) {
            return wavFile;
        }

        // Fall back to AudioSystem (may not work well)
        logger.warn("ffmpeg conversion failed or not available. Falling back to AudioSystem.");
        return convertOggToWavUsingAudioSystem(oggFile);
    }

    /**
     * Converts an OGG file to WAV format using ffmpeg.
     * This is the preferred method as it handles OGG files properly.
     */
    private File convertOggToWavUsingFfmpeg(File oggFile) {
        try {
            logger.info("Converting OGG file to WAV using ffmpeg in background...");

            // Create temporary WAV file (cleanup via cleanupTempAudioFile after transcription)
            File wavFile = ConfigManager.createTempFile("whisperdog_converted_", ".wav");

            // Use ffmpeg to convert OGG to WAV
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", oggFile.getAbsolutePath(),
                "-acodec", "pcm_s16le",
                "-ar", "16000",
                "-ac", "1",
                wavFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output to prevent blocking
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && wavFile.exists() && wavFile.length() > 0) {
                logger.info("Successfully converted OGG to WAV using ffmpeg");
                return wavFile;
            } else {
                logger.error("ffmpeg conversion failed with exit code: {}. Output: {}", exitCode, output);
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to convert OGG using ffmpeg", e);
            return null;
        }
    }

    /**
     * Converts an OGG file to WAV format using javax.sound.sampled.
     * This is a fallback method and may not work well with OGG files.
     */
    private File convertOggToWavUsingAudioSystem(File oggFile) {
        try {
            // Create temporary WAV file (cleanup via cleanupTempAudioFile after transcription)
            File wavFile = ConfigManager.createTempFile("whisperdog_converted_", ".wav");

            // Note: javax.sound.sampled doesn't natively support OGG
            // This will attempt to read using AudioSystem but may fail
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(oggFile);
                javax.sound.sampled.AudioSystem.write(audioStream,
                        javax.sound.sampled.AudioFileFormat.Type.WAVE, wavFile);
                audioStream.close();
                logger.info("Successfully converted OGG to WAV using AudioSystem");
                return wavFile;
            } catch (Exception e) {
                logger.error("Failed to convert OGG using AudioSystem. OGG codec may not be installed.", e);
                // Return null to indicate conversion failed
                return null;
            }
        } catch (Exception e) {
            logger.error("Error creating temp file for conversion", e);
            return null;
        }
    }

    private static class PostProcessingItem {
        private final String title;
        private final String uuid;

        public PostProcessingItem(String title, String uuid) {
            this.title = title;
            this.uuid = uuid;
        }

        // Return only title for display in the ComboBox.
        @Override
        public String toString() {
            return title;
        }
    }

    private void populatePostProcessingComboBox() {
        // Set flag to prevent ItemListener from corrupting the saved selection during repopulation
        isPopulatingComboBox = true;
        try {
            postProcessingSelectComboBox.removeAllItems();
            // Get the list of pipelines (only show enabled ones)
            pipelineList = configManager.getPipelines();
            String lastUsedPipelineUUID = configManager.getLastUsedPipelineUUID();
            Integer lastUsedIndex = null;
            for (int index = 0; index < pipelineList.size(); index++) {
                Pipeline pipeline = pipelineList.get(index);
                // Only show enabled pipelines in the dropdown
                if (pipeline.enabled) {
                    PostProcessingItem item = new PostProcessingItem(pipeline.title, pipeline.uuid);
                    if (pipeline.uuid.equals(lastUsedPipelineUUID)) {
                        lastUsedIndex = postProcessingSelectComboBox.getItemCount(); // Index in filtered list
                    }
                    postProcessingSelectComboBox.addItem(item);
                }
            }

            if (lastUsedIndex != null) {
                postProcessingSelectComboBox.setSelectedIndex(lastUsedIndex);
            }
        } finally {
            isPopulatingComboBox = false;
        }
        // Always update button state after repopulation since ItemListener is blocked
        updateRunPipelineButtonState();
    }

    /**
     * Refreshes the pipeline dropdown when navigating back to the recorder screen.
     * Call this when the form becomes visible to ensure new pipelines appear.
     */
    public void refreshPipelines() {
        populatePostProcessingComboBox();
    }

    /**
     * Refresh the device labels with current configuration.
     * Shows current microphone and system audio device names.
     */
    public void refreshDeviceLabels() {
        SwingUtilities.invokeLater(() -> {
            // Microphone label
            CharSequence micNameSeq = configManager.getMicrophone();
            String micName = micNameSeq != null ? micNameSeq.toString() : null;
            if (micName == null || micName.isEmpty()) {
                microphoneLabel.setText("Mic: No microphone selected");
                microphoneLabel.setFont(microphoneLabel.getFont().deriveFont(Font.ITALIC, 11f));
                microphoneLabel.setForeground(Color.GRAY);
            } else {
                String displayName = AudioDeviceInfo.formatDeviceNameForDisplay(
                    AudioDeviceInfo.extractDeviceName(micName), 35);
                microphoneLabel.setText("Mic: " + displayName);
                microphoneLabel.setFont(microphoneLabel.getFont().deriveFont(Font.PLAIN, 11f));

                // Check availability
                if (!AudioDeviceInfo.isMicrophoneAvailable(micName)) {
                    microphoneLabel.setText("Mic: " + displayName + " (unavailable)");
                    microphoneLabel.setForeground(new Color(180, 100, 100)); // Muted red
                } else {
                    microphoneLabel.setForeground(Color.GRAY);
                }
            }

            // System audio label - only visible when enabled and available
            boolean systemAudioEnabled = configManager.isSystemAudioEnabled();
            boolean systemAudioAvailable = SystemAudioCapture.isAvailable();
            systemAudioLabel.setVisible(systemAudioEnabled && systemAudioAvailable);

            if (systemAudioEnabled && systemAudioAvailable) {
                String sysDevice = configManager.getSystemAudioDevice();
                if (sysDevice == null || sysDevice.isEmpty()) {
                    // Using default - detect actual device name
                    String defaultOutput = AudioDeviceInfo.getDefaultOutputDeviceName();
                    String display = defaultOutput != null
                        ? AudioDeviceInfo.formatDeviceNameForDisplay(defaultOutput, 35)
                        : "Default";
                    systemAudioLabel.setText("System: " + display);
                } else {
                    systemAudioLabel.setText("System: " +
                        AudioDeviceInfo.formatDeviceNameForDisplay(sysDevice, 35));
                }
            }

            deviceLabelsPanel.revalidate();
            deviceLabelsPanel.repaint();
        });
    }

    /**
     * Navigate to the Settings form via MainForm.
     */
    private void navigateToSettings() {
        Container parent = getParent();
        while (parent != null && !(parent instanceof MainForm)) {
            parent = parent.getParent();
        }
        if (parent instanceof MainForm) {
            ((MainForm) parent).setSelectedMenu(1, 1);
        }
    }

    private boolean isToggleInProgress = false;
    private volatile AudioTranscriptionWorker activeTranscriptionWorker;

    public void toggleRecording() {


        if (isToggleInProgress) {
            logger.info("Toggle in progress. Ignoring.");
            return;
        }
        // Allow cancellation during transcription/conversion
        if (isStoppingInProgress && activeTranscriptionWorker != null) {
            logger.info("Cancelling transcription worker");
            activeTranscriptionWorker.cancel(true);
            activeTranscriptionWorker = null;
            ConsoleLogger.getInstance().log("Transcription cancelled by user");
            resetUIAfterTranscription();
            return;
        }
        if (isStoppingInProgress) {
            return;
        }
        if (!isRecording) {
            if (!checkSettings()) return;
            startRecording();
            updateUIForRecordingStart();
            updateTrayMenu();

        } else {
            stopRecording(false);
        }
    }

    private void startRecording() {
        try {
            isRecording = true;
            isRecordingWarningActive = false;
            warningPulseState = 0;
            recordingStartTime = System.currentTimeMillis();

            boolean useSystemAudio = systemAudioToggle.isSelected() && SystemAudioCapture.isAvailable();

            if (useSystemAudio) {
                // Use AudioCaptureManager for dual-source recording
                audioCaptureManager = new AudioCaptureManager(configManager);
                String preferredDevice = configManager.getSystemAudioDevice();
                if (preferredDevice != null && !preferredDevice.isEmpty()) {
                    audioCaptureManager.setPreferredLoopbackDevice(preferredDevice);
                }
                audioCaptureManager.startCapture(true);
                recorder = null;
                logger.info("Recording started with system audio capture");
            } else {
                // Standard mic-only recording
                audioCaptureManager = null;
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File audioFile = new File(ConfigManager.getTempDirectory(), "whisperdog_mic_" + timeStamp + ".wav");
                recorder = new AudioRecorder(audioFile, configManager);
                new Thread(recorder::start).start();
                logger.info("Recording started: " + audioFile.getPath());
            }
            recordButton.setText("Stop Recording");

            // Start recording warning timer (ISS_00007)
            startRecordingWarningTimer();
        } catch (Exception e) {
            logger.error("An error occurred while starting the recording", e);
            isRecording = false;
            stopRecordingWarningTimer();
        }
    }

    /**
     * Starts the recording warning timer. Checks every 500ms if recording exceeds threshold.
     */
    private void startRecordingWarningTimer() {
        int warningDuration = configManager.getRecordingWarningDuration();
        if (warningDuration <= 0) {
            // Feature disabled
            return;
        }

        if (recordingWarningTimer != null) {
            recordingWarningTimer.stop();
        }

        recordingWarningTimer = new javax.swing.Timer(500, e -> {
            if (!isRecording) {
                stopRecordingWarningTimer();
                return;
            }

            long elapsedSeconds = (System.currentTimeMillis() - recordingStartTime) / 1000;
            int threshold = configManager.getRecordingWarningDuration();

            if (elapsedSeconds >= threshold && !isRecordingWarningActive) {
                // Trigger warning
                isRecordingWarningActive = true;
                logger.info("Recording warning triggered at " + elapsedSeconds + " seconds");
                ConsoleLogger.getInstance().log("⚠ Recording exceeds " + formatDurationForLog(threshold));

                // Update tray icon to warning state
                TrayIconManager trayManager = AudioRecorderUI.getTrayIconManager();
                if (trayManager != null) {
                    trayManager.setRecordingWarning(true);
                }
            }

            if (isRecordingWarningActive) {
                // Animate pulse
                warningPulseState++;
                statusIndicatorPanel.repaint();

                // Update button text with duration
                long minutes = elapsedSeconds / 60;
                long seconds = elapsedSeconds % 60;
                recordButton.setText(String.format("Stop (%d:%02d) ⚠", minutes, seconds));
            }
        });
        recordingWarningTimer.start();
    }

    /**
     * Stops the recording warning timer and resets warning state.
     */
    private void stopRecordingWarningTimer() {
        if (recordingWarningTimer != null) {
            recordingWarningTimer.stop();
            recordingWarningTimer = null;
        }
        isRecordingWarningActive = false;
        warningPulseState = 0;

        // Reset tray icon warning state
        TrayIconManager trayManager = AudioRecorderUI.getTrayIconManager();
        if (trayManager != null) {
            trayManager.setRecordingWarning(false);
        }
    }

    /**
     * Formats duration in seconds to a human-readable string for log messages.
     */
    private String formatDurationForLog(int seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds % 60 == 0) {
            return (seconds / 60) + " minute" + (seconds >= 120 ? "s" : "");
        } else {
            return String.format("%.1f minutes", seconds / 60.0);
        }
    }

    private boolean isStoppingInProgress = false;

    public void stopRecording(boolean cancelledRecording) {
        // Stop the recording warning timer first (ISS_00007)
        stopRecordingWarningTimer();

        updateUIForRecordingStop();  // This already sets isTranscribing = true and repaints
        isStoppingInProgress = true;
        recordButton.setText("Converting. Please wait...");

        if (audioCaptureManager != null) {
            // Dual-source recording - stop via AudioCaptureManager
            File micFile = audioCaptureManager.stopCapture();
            File sysFile = audioCaptureManager.getSystemTrackFile();
            logger.info("Recording stopped (dual-source)");
            if (!cancelledRecording && micFile != null) {
                activeTranscriptionWorker = new RecorderForm.AudioTranscriptionWorker(micFile, sysFile);
                activeTranscriptionWorker.execute();
            } else {
                logger.info("Recording cancelled");
                audioCaptureManager.cleanupTempFiles();
                setProcessingState(false);
                updateTrayMenu();
            }
            audioCaptureManager = null;
        } else if (recorder != null) {
            recorder.stop();
            logger.info("Recording stopped");
            if (!cancelledRecording) {
                activeTranscriptionWorker = new RecorderForm.AudioTranscriptionWorker(recorder.getOutputFile());
                activeTranscriptionWorker.execute();
            } else {
                logger.info("Recording cancelled");
                setProcessingState(false);
                updateTrayMenu();
            }
        }
    }

    public void stopRecording(File audioFile) {
        isStoppingInProgress = true;

        // Set transcribing state (blue indicator) - same as dropped files
        setProcessingState(true);

        recordButton.setText("Cancel");
        recordButton.setEnabled(true);
        activeTranscriptionWorker = new RecorderForm.AudioTranscriptionWorker(audioFile);
        activeTranscriptionWorker.execute();
    }

    public void playFinishSound() {
        if (configManager.isFinishSoundEnabled()) {
            new Thread(() -> {
                try {
                    InputStream audioSrc = getClass().getResourceAsStream("/stop.wav");
                    InputStream bufferedIn = new BufferedInputStream(audioSrc);
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    clip.start();
                } catch (Exception e) {
                    logger.error(e);
                }
            }).start();
        }
    }

    private void copyTranscriptionToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    private void pasteFromClipboard() {
        if (!configManager.isAutoPasteEnabled()) {
            return;
        }
        try {
            Robot robot = new Robot();
            robot.delay(500);
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
        } catch (AWTException e) {
            logger.error("An error occurred while pasting from clipboard", e);
        }
    }

    private void updateUIForRecordingStart() {

        processedText.setFocusable(false);
        processedText.setFocusable(true);
        transcriptionTextArea.setFocusable(false);
        transcriptionTextArea.setFocusable(true);

        // Disable system audio toggle during recording (avoid mid-recording initialization crashes)
        systemAudioToggle.setEnabled(false);

        // Repaint status indicator to show recording state (red circle + system audio indicator)
        statusIndicatorPanel.repaint();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                isToggleInProgress = true;
                recordButton.setEnabled(false);
                Thread.sleep(1000);
                return null;
            }

            @Override
            protected void done() {
                isToggleInProgress = false;
                recordButton.setEnabled(true);
                recordButton.setText("Stop Recording");
            }
        };
        worker.execute();
    }

    private void updateUIForRecordingStop() {
        // Stop recording and start transcribing (blue indicator)
        isRecording = false;  // Must set this BEFORE isTranscribing, or circle stays red!
        setProcessingState(true);

        recordButton.setText("Cancel");
        recordButton.setEnabled(true);
    }

    private void resetUIAfterTranscription() {
        isStoppingInProgress = false;
        setProcessingState(false);  // Reset to idle state (green indicator) and update tray

        recordButton.setText("Start Recording");
        recordButton.setEnabled(true);

        // Re-enable system audio toggle (was disabled during recording)
        systemAudioToggle.setEnabled(SystemAudioCapture.isAvailable());

        // Hide progress panel for file-based operations
        hideProgressPanel();

        // Clean up any extracted temp files from video processing
        cleanupExtractedTempFiles();

        // Update Run Pipeline button state now that transcription is complete
        updateRunPipelineButtonState();
    }

    private boolean checkSettings() {
        boolean settingsSet = true;
        if ((configManager.getWhisperServer().equals("OpenAI") || configManager.getWhisperServer().isEmpty()) && (configManager.getApiKey() == null || configManager.getApiKey().length() == 0)) {
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.WARNING,
                    "API Key must be set in options.");
            settingsSet = false;
        }
        if (configManager.getMicrophone() == null || configManager.getMicrophone().length() == 0) {
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.WARNING,
                    "Microphone must be set in options.");
            settingsSet = false;
        }
        return settingsSet;
    }


    private void updateTrayMenu() {
        TrayIconManager manager = AudioRecorderUI.getTrayIconManager();
        if (manager != null) {
            manager.updateTrayMenu(isRecording);
        }
    }


    /**
     * Transcribes audio using OpenAI with automatic retry for transient errors.
     * Implements exponential backoff (1s, 2s, 4s) for up to 3 attempts.
     *
     * @param audioFile The audio file to transcribe
     * @param console Console logger for progress updates
     * @return The transcription result, or null if user cancelled
     * @throws Exception If transcription fails after all retries
     */
    private String transcribeWithRetry(File audioFile, ConsoleLogger console) throws Exception {
        final int MAX_RETRIES = 3;
        int attempt = 0;
        TranscriptionException lastException = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                if (attempt > 1) {
                    console.log(String.format("Retry attempt %d/%d...", attempt, MAX_RETRIES));
                }
                return whisperClient.transcribe(audioFile);
            } catch (TranscriptionException e) {
                lastException = e;
                console.log("Error: " + ErrorClassifier.getUserFriendlyMessage(e));

                // Handle based on error category
                if (e.getCategory() == ErrorCategory.PERMANENT) {
                    // Don't retry permanent errors
                    console.logError("Permanent error - cannot retry");
                    throw e;
                }

                if (e.getCategory() == ErrorCategory.USER_ACTION) {
                    // Empty response - ask user if they want to retry
                    console.log("No speech detected - asking user...");
                    java.util.concurrent.atomic.AtomicBoolean userRetry =
                        new java.util.concurrent.atomic.AtomicBoolean(false);

                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            // Create a simple retry state for the dialog
                            org.whisperdog.retry.RetryState state =
                                new org.whisperdog.retry.RetryState(null, audioFile);
                            state.recordAttempt(e.getMessage(), e.getHttpStatus());
                            userRetry.set(TranscriptionErrorDialog.showEmptyResponseDialog(
                                RecorderForm.this, state));
                        });
                    } catch (Exception dialogEx) {
                        logger.error("Error showing empty response dialog", dialogEx);
                    }

                    if (userRetry.get()) {
                        // User wants to retry - reset attempt counter for fresh tries
                        attempt = 0;
                        console.log("User chose to retry transcription");
                        continue;
                    } else {
                        console.log("User cancelled transcription");
                        return null;  // User cancelled
                    }
                }

                // Transient error - check if we can retry
                if (attempt >= MAX_RETRIES) {
                    console.logError("Max retries exhausted");

                    // Ask user if they want to try again
                    java.util.concurrent.atomic.AtomicBoolean userRetry =
                        new java.util.concurrent.atomic.AtomicBoolean(false);

                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            org.whisperdog.retry.RetryState state =
                                new org.whisperdog.retry.RetryState(null, audioFile);
                            for (int i = 0; i < MAX_RETRIES; i++) {
                                state.recordAttempt(e.getMessage(), e.getHttpStatus());
                            }
                            userRetry.set(TranscriptionErrorDialog.showRetriesExhaustedDialog(
                                RecorderForm.this, state));
                        });
                    } catch (Exception dialogEx) {
                        logger.error("Error showing retries exhausted dialog", dialogEx);
                    }

                    if (userRetry.get()) {
                        attempt = 0;  // Reset for fresh attempts
                        console.log("User chose to try again");
                        continue;
                    } else {
                        throw e;
                    }
                }

                // Wait before retry with exponential backoff (1s, 2s, 4s)
                long delaySeconds = (long) Math.pow(2, attempt - 1);
                console.log(String.format("Retrying in %d seconds...", delaySeconds));
                Thread.sleep(delaySeconds * 1000);
            }
        }

        // Should not reach here, but just in case
        if (lastException != null) {
            throw lastException;
        }
        throw new Exception("Transcription failed after max retries");
    }

    /**
     * Transcribes audio using OpenAI with word-level timestamps and automatic retry.
     * Used for dual-source recordings where accurate source attribution is needed.
     *
     * @param audioFile The audio file to transcribe
     * @param console Console logger for progress updates
     * @return TranscriptionResult with text and word timestamps
     * @throws Exception If transcription fails after all retries
     */
    private TranscriptionResult transcribeWithTimestampsAndRetry(File audioFile, ConsoleLogger console) throws Exception {
        final int MAX_RETRIES = 3;
        int attempt = 0;
        TranscriptionException lastException = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                if (attempt > 1) {
                    console.log(String.format("Retry attempt %d/%d...", attempt, MAX_RETRIES));
                }
                return whisperClient.transcribeWithTimestamps(audioFile);
            } catch (TranscriptionException e) {
                lastException = e;
                console.log("Error: " + ErrorClassifier.getUserFriendlyMessage(e));

                // Handle based on error category
                if (e.getCategory() == ErrorCategory.PERMANENT) {
                    console.logError("Permanent error - cannot retry");
                    throw e;
                }

                if (e.getCategory() == ErrorCategory.USER_ACTION) {
                    console.log("No speech detected - asking user...");
                    java.util.concurrent.atomic.AtomicBoolean userRetry =
                        new java.util.concurrent.atomic.AtomicBoolean(false);

                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            org.whisperdog.retry.RetryState state =
                                new org.whisperdog.retry.RetryState(null, audioFile);
                            state.recordAttempt(e.getMessage(), e.getHttpStatus());
                            userRetry.set(TranscriptionErrorDialog.showEmptyResponseDialog(
                                RecorderForm.this, state));
                        });
                    } catch (Exception dialogEx) {
                        logger.error("Error showing empty response dialog", dialogEx);
                    }

                    if (userRetry.get()) {
                        attempt = 0;
                        console.log("User chose to retry transcription");
                        continue;
                    } else {
                        console.log("User cancelled transcription");
                        return null;
                    }
                }

                // Transient error - check if we can retry
                if (attempt >= MAX_RETRIES) {
                    console.logError("Max retries exhausted");

                    java.util.concurrent.atomic.AtomicBoolean userRetry =
                        new java.util.concurrent.atomic.AtomicBoolean(false);

                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            org.whisperdog.retry.RetryState state =
                                new org.whisperdog.retry.RetryState(null, audioFile);
                            for (int i = 0; i < MAX_RETRIES; i++) {
                                state.recordAttempt(e.getMessage(), e.getHttpStatus());
                            }
                            userRetry.set(TranscriptionErrorDialog.showRetriesExhaustedDialog(
                                RecorderForm.this, state));
                        });
                    } catch (Exception dialogEx) {
                        logger.error("Error showing retries exhausted dialog", dialogEx);
                    }

                    if (userRetry.get()) {
                        attempt = 0;
                        console.log("User chose to try again");
                        continue;
                    } else {
                        throw e;
                    }
                }

                // Wait before retry with exponential backoff
                long delaySeconds = (long) Math.pow(2, attempt - 1);
                console.log(String.format("Retrying in %d seconds...", delaySeconds));
                Thread.sleep(delaySeconds * 1000);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new Exception("Transcription failed after max retries");
    }

    private class AudioTranscriptionWorker extends SwingWorker<String, Void> {
        private final File audioFile;
        private final File systemTrackFile;  // null for mic-only recordings
        private volatile boolean cancelledByUser = false;  // Track if user cancelled via warning dialog

        public AudioTranscriptionWorker(File audioFile) {
            this(audioFile, null);
        }

        public AudioTranscriptionWorker(File audioFile, File systemTrackFile) {
            this.audioFile = audioFile;
            this.systemTrackFile = systemTrackFile;
        }

        @Override
        protected String doInBackground() {
            ConsoleLogger console = ConsoleLogger.getInstance();
            // Emergency logging to catch silent failures on second recording
            String workerThreadName = Thread.currentThread().getName();
            System.err.println("[EMERGENCY] Worker started: " + workerThreadName);
            System.err.println("[EMERGENCY] Audio file: " + audioFile);
            System.err.println("[EMERGENCY] System file: " + systemTrackFile);

            try {
                logger.info("AudioTranscriptionWorker started on thread: {}", workerThreadName);
                logger.info("Audio file to analyze: {}", audioFile.getAbsolutePath());

                // Pre-flight validation
                if (!audioFile.exists()) {
                    String msg = "Audio file does not exist: " + audioFile.getAbsolutePath();
                    logger.error(msg);
                    console.logError(msg);
                    return null;
                }

                // Check if system track file is valid (if provided)
                File validatedSystemTrack = systemTrackFile;
                if (systemTrackFile != null && !systemTrackFile.exists()) {
                    logger.warn("System track file does not exist, proceeding with mic only");
                    validatedSystemTrack = null;
                }

                // Pre-flight analysis for speech content and silence detection
                // Runs for both min speech check and large recording warning
                console.separator();
                console.log("Analyzing audio...");

                SilenceRemover.SilenceAnalysisResult analysis = SilenceRemover.analyzeForSilence(
                    audioFile,
                    configManager.getSilenceThreshold(),
                    configManager.getMinSilenceDuration()
                );

                // Large recording warning (only when silence removal is enabled)
                if (configManager.isSilenceRemovalEnabled() && analysis != null && analysis.exceedsWarningThreshold) {
                    console.log(String.format("⚠ Large recording detected: %s, %s silence",
                        analysis.getFormattedDuration(), analysis.getFormattedSilencePercent()));

                    // Show warning dialog on EDT and wait for user decision
                    java.util.concurrent.atomic.AtomicBoolean userProceed =
                        new java.util.concurrent.atomic.AtomicBoolean(false);

                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            LargeRecordingWarningDialog dialog = new LargeRecordingWarningDialog(
                                SwingUtilities.getWindowAncestor(RecorderForm.this),
                                analysis
                            );
                            userProceed.set(dialog.showAndGetResult());
                        });
                    } catch (Exception e) {
                        logger.error("Error showing warning dialog", e);
                        // Default to proceeding if dialog fails
                        userProceed.set(true);
                    }

                    if (!userProceed.get()) {
                        console.log("Transcription cancelled by user");
                        cancelledByUser = true;
                        return null;  // User cancelled
                    }

                    console.log("User chose to proceed with transcription");
                }

                // Analyze system track content (needed for merge decision and min speech check)
                float micUsefulSeconds = analysis != null ? analysis.estimatedUsefulSeconds : 0;
                float systemUsefulSeconds = 0;
                boolean systemTrackHasContent = false;

                if (validatedSystemTrack != null && validatedSystemTrack.exists()) {
                    // Use a lower threshold for system audio (pre-processed, normalized audio)
                    // System audio doesn't have the same noise floor as mic input
                    float systemThreshold = (float) Math.max(0.001, configManager.getSilenceThreshold() * 0.5);
                    SilenceRemover.SilenceAnalysisResult systemAnalysis = SilenceRemover.analyzeForSilence(
                        validatedSystemTrack,
                        systemThreshold,
                        configManager.getMinSilenceDuration()
                    );
                    if (systemAnalysis != null) {
                        systemUsefulSeconds = systemAnalysis.estimatedUsefulSeconds;
                        // Only consider system track as having content if > 0.5s of audio
                        systemTrackHasContent = systemUsefulSeconds > 0.5f;
                        console.log(String.format("Audio content: mic=%.1fs, system=%.1fs",
                            micUsefulSeconds, systemUsefulSeconds));
                    }
                }

                // Check minimum speech duration threshold (ISS_00011: works regardless of silence removal setting)
                float minSpeechDuration = configManager.getMinSpeechDuration();
                if (minSpeechDuration > 0 && analysis != null) {
                    // Use the maximum of both sources - if either has content, proceed
                    float estimatedSpeech = Math.max(micUsefulSeconds, systemUsefulSeconds);

                    if (estimatedSpeech < minSpeechDuration) {
                        final float speechForDialog = estimatedSpeech;
                        console.log(String.format("⚠ Insufficient speech: %.1fs detected (minimum: %.1fs)",
                            speechForDialog, minSpeechDuration));

                        java.util.concurrent.atomic.AtomicBoolean userProceed =
                            new java.util.concurrent.atomic.AtomicBoolean(false);

                        try {
                            SwingUtilities.invokeAndWait(() -> {
                                MinSpeechDurationDialog dialog = new MinSpeechDurationDialog(
                                    SwingUtilities.getWindowAncestor(RecorderForm.this),
                                    speechForDialog,
                                    minSpeechDuration
                                );
                                userProceed.set(dialog.showAndGetResult());
                            });
                        } catch (Exception e) {
                            logger.error("Error showing min speech dialog", e);
                            userProceed.set(true);
                        }

                        if (!userProceed.get()) {
                            console.log("Recording discarded by user (insufficient speech)");
                            cancelledByUser = true;
                            return null;
                        }

                        console.log("User chose to proceed with transcription despite short speech");
                    }
                }

                // Prepare file for transcription
                File fileToTranscribe = audioFile;

                if (systemTrackHasContent) {
                    // Dual-source recording: merge mic + system tracks
                    // Only merge if system track has actual content (> 0.5s)
                    console.log("Merging mic + system audio tracks...");
                    File mergedFile = FFmpegUtil.mergeAudioTracks(audioFile, validatedSystemTrack);
                    if (mergedFile != null) {
                        fileToTranscribe = mergedFile;
                        console.log("Audio tracks merged successfully");
                    } else {
                        console.log("Track merge failed, using mic track only");
                    }
                    // Skip silence removal for dual-source (breaks timeline alignment)
                } else if (configManager.isSilenceRemovalEnabled()) {
                    // Mic-only: apply silence removal as usual
                    fileToTranscribe = SilenceRemover.removeSilence(
                        audioFile,
                        configManager.getSilenceThreshold(),
                        configManager.getMinSilenceDuration(),
                        configManager.isKeepCompressedFile(),
                        configManager.getMinRecordingDurationForSilenceRemoval()
                    );
                }

                String server = configManager.getWhisperServer();
                console.separator();
                console.log("Starting transcription using " + server);
                console.log("Audio file: " + fileToTranscribe.getName());

                long transcriptionStartTime = System.currentTimeMillis();
                String result = null;
                // Word timestamps for accurate source attribution (OpenAI only, dual-source recordings)
                List<SourceActivityTracker.TimestampedWord> wordTimestamps = null;

                if (server.equals("OpenAI")) {
                    logger.info("Transcribing audio using OpenAI");
                    // Use word timestamps for dual-source recordings for accurate attribution
                    if (systemTrackHasContent) {
                        console.log("Requesting word-level timestamps for source attribution...");
                        TranscriptionResult tsResult = transcribeWithTimestampsAndRetry(fileToTranscribe, console);
                        if (tsResult != null) {
                            result = tsResult.getText();
                            if (tsResult.hasWordTimestamps()) {
                                wordTimestamps = tsResult.getWords();
                                console.log(String.format("Received %d word timestamps", wordTimestamps.size()));
                            }
                        }
                    } else {
                        result = transcribeWithRetry(fileToTranscribe, console);
                    }
                } else if (server.equals("Faster-Whisper")) {
                    logger.info("Transcribing audio using Faster-Whisper");
                    result = fasterWhisperTranscribeClient.transcribe(fileToTranscribe);
                } else if (server.equals("Open WebUI")) {
                    logger.info("Transcribing audio using Open WebUI");
                    result = openWebUITranscribeClient.transcribeAudio(fileToTranscribe);
                } else {
                    logger.error("Unknown Whisper server: " + server);
                    console.logError("Unknown Whisper server: " + server);
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                            "Unknown Whisper server: " + server);
                    return null;
                }

                long transcriptionTime = System.currentTimeMillis() - transcriptionStartTime;
                console.log(String.format("Transcription took %dms", transcriptionTime));

                // Apply source attribution labels for dual-source recordings
                if (result != null && systemTrackHasContent) {
                    try {
                        console.log("Applying source attribution...");
                        SourceActivityTracker tracker = new SourceActivityTracker();
                        List<SourceActivityTracker.ActivitySegment> timeline =
                            tracker.trackActivity(audioFile, systemTrackFile);

                        // Only label if both sources have activity — single source is implied
                        boolean hasUserActivity = timeline.stream()
                            .anyMatch(s -> s.source == SourceActivityTracker.Source.USER || s.source == SourceActivityTracker.Source.BOTH);
                        boolean hasSystemActivity = timeline.stream()
                            .anyMatch(s -> s.source == SourceActivityTracker.Source.SYSTEM || s.source == SourceActivityTracker.Source.BOTH);

                        if (hasUserActivity && hasSystemActivity) {
                            // Use accurate word-level attribution if timestamps available
                            if (wordTimestamps != null && !wordTimestamps.isEmpty()) {
                                result = tracker.labelTranscriptWithTimestamps(wordTimestamps, timeline);
                                console.log(String.format("Source attribution complete with word timestamps (%d segments)", timeline.size()));
                            } else {
                                // Fall back to proportional distribution
                                result = tracker.labelTranscript(result, timeline);
                                console.log(String.format("Source attribution complete (proportional, %d segments)", timeline.size()));
                            }
                        } else {
                            console.log("Single source detected, skipping attribution labels");
                        }
                    } catch (Exception e) {
                        logger.warn("Source attribution failed, returning unlabeled transcript: {}", e.getMessage());
                        console.log("Source attribution skipped: " + e.getMessage());
                    }
                }

                return result;
            } catch (Exception e) {
                logger.error("Error during transcription", e);
                ConsoleLogger.getInstance().logError("Transcription failed: " + e.getMessage());
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                        "Error during transcription. See logs.");
                return null;
            }
        }

        @Override
        protected void done() {
            activeTranscriptionWorker = null;
            if (isCancelled()) {
                return;  // User cancelled — UI already reset by toggleRecording()
            }
            ConsoleLogger console = ConsoleLogger.getInstance();
            String transcript = null;
            try {
                transcript = get();
                if (transcript != null) {
                    logger.info("Transcribed text: " + transcript);
                    transcriptionTextArea.setText(transcript);

                    // Start new history session for this transcription
                    pipelineHistory.startNewSession(transcript);
                    processedText.setText("");  // Clear previous post-processed text
                    historyPanel.updateResults(pipelineHistory.getResults());  // Reset history panel

                    console.logSuccess("Transcription completed");
                    console.logTranscript(transcript);
                    console.log("Transcript length: " + transcript.length() + " characters");
                    // Show success notification
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.SUCCESS,
                            "Transcription completed!");
                    // Show system-level notification
                    TrayIconManager trayManager = AudioRecorderUI.getTrayIconManager();
                    if (trayManager != null) {
                        trayManager.showSystemNotification("WhisperDog", "Transcription completed");
                    }
                } else if (cancelledByUser) {
                    // User cancelled via large recording warning dialog - not an error
                    logger.info("Transcription cancelled by user (large recording warning)");
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.INFO,
                            "Transcription cancelled");
                } else {
                    logger.warn("Transcription resulted in null");
                    console.logError("Transcription returned null");
                }
            } catch (Exception e) {
                logger.error("An error occurred while finishing the transcription", e);
                console.logError("Error finishing transcription: " + e.getMessage());
            } finally {
                isRecording = false;
                // Clean up temp audio files to prevent accumulation in long-running sessions
                cleanupTempAudioFile(audioFile);
                cleanupTempAudioFile(systemTrackFile);
            }

            // Run post-processing asynchronously if enabled
            if (transcript != null && !transcript.trim().isEmpty() &&
                enablePostProcessingCheckBox.isSelected() && postProcessingSelectComboBox.getSelectedItem() != null) {
                    PostProcessingItem selectedItem = (PostProcessingItem) postProcessingSelectComboBox.getSelectedItem();
                    if (selectedItem != null && selectedItem.uuid != null) {
                        Pipeline pipeline = configManager.getPipelineByUuid(selectedItem.uuid);
                        if (pipeline != null) {
                            // Switch progress bar to post-processing stage (orange)
                            setProgressStage(IndeterminateProgressBar.Stage.POST_PROCESSING);
                            // Run post-processing in separate worker to avoid blocking UI
                            new PostProcessingWorker(transcript, pipeline).execute();
                        } else {
                            logger.error("Pipeline not found for UUID: " + selectedItem.uuid);
                            console.logError("Pipeline not found: " + selectedItem.uuid);
                            resetUIAfterTranscription();
                            updateTrayMenu();
                        }
                    } else {
                        resetUIAfterTranscription();
                    }
            } else if (transcript != null && !transcript.trim().isEmpty()) {
                // No post-processing, just copy raw transcript if auto-paste enabled
                if (configManager.isAutoPasteEnabled()) {
                    // Remove focus from transcription area to prevent pasting into itself
                    transcriptionTextArea.transferFocus();
                    copyTranscriptionToClipboard(transcript);
                    pasteFromClipboard();
                }
                playFinishSound();
                resetUIAfterTranscription();
                updateTrayMenu();
            } else {
                resetUIAfterTranscription();
            }
        }

        /**
         * Clean up a temp audio file if it's in the system temp directory.
         * Only deletes files with whisperdog prefix to avoid deleting user files.
         */
        private void cleanupTempAudioFile(File file) {
            if (file == null || !file.exists()) return;
            String tempDir = ConfigManager.getTempDirectory().getPath();
            if (file.getParent() != null && file.getParent().startsWith(tempDir)
                    && file.getName().startsWith("whisperdog_")) {
                try {
                    if (file.delete()) {
                        logger.debug("Cleaned up temp file: {}", file.getName());
                    }
                } catch (Exception e) {
                    logger.debug("Failed to clean up temp file: {}", file.getName());
                }
            }
        }
    }

    /**
     * Worker for running post-processing pipelines asynchronously
     */
    private class PostProcessingWorker extends SwingWorker<String, Void> {
        private final String inputText;
        private final Pipeline pipeline;
        private final long startTime;

        public PostProcessingWorker(String inputText, Pipeline pipeline) {
            this.inputText = inputText;
            this.pipeline = pipeline;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        protected String doInBackground() {
            PostProcessingService ppService = new PostProcessingService(configManager);
            return ppService.applyPipeline(inputText, pipeline);
        }

        @Override
        protected void done() {
            try {
                String processedResult = get();
                int executionTime = (int) (System.currentTimeMillis() - startTime);

                // Add result to history
                pipelineHistory.addResult(pipeline.uuid, pipeline.title, processedResult, executionTime);

                // Update history panel
                historyPanel.updateResults(pipelineHistory.getResults());

                RecorderForm.this.processedText.setText(processedResult);

                // Show pipeline completion toast
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.SUCCESS,
                        "Post-processing completed!");

                // Show system-level notification
                TrayIconManager trayManager = AudioRecorderUI.getTrayIconManager();
                if (trayManager != null) {
                    trayManager.showSystemNotification("WhisperDog", "Post-processing completed");
                }

                playFinishSound();

                // Remove focus from text areas to prevent pasting into them
                transcriptionTextArea.transferFocus();
                RecorderForm.this.processedText.transferFocus();

                copyTranscriptionToClipboard(processedResult);
                pasteFromClipboard();

                // Remember the last used pipeline
                configManager.setLastUsedPipelineUUID(pipeline.uuid);
            } catch (Exception e) {
                logger.error("Error during post-processing", e);
                ConsoleLogger.getInstance().logError("Post-processing failed: " + e.getMessage());
            } finally {
                resetUIAfterTranscription();
                updateTrayMenu();
            }
        }
    }

    /**
     * Updates the Run Pipeline button enabled state based on:
     * - Transcription text field has content
     * - A pipeline is selected
     * - No manual pipeline is currently running
     */
    private void updateRunPipelineButtonState() {
        if (runPipelineButton == null) {
            return; // Button not yet initialized
        }

        boolean hasTranscription = transcriptionTextArea.getText() != null
                && !transcriptionTextArea.getText().trim().isEmpty();
        boolean hasPipelineSelected = postProcessingSelectComboBox.getSelectedItem() != null;
        boolean canRun = hasTranscription && hasPipelineSelected && !isManualPipelineRunning && !isTranscribing;

        runPipelineButton.setEnabled(canRun);

        // Update tooltip based on state
        if (!hasTranscription) {
            runPipelineButton.setToolTipText("Record audio first");
        } else if (!hasPipelineSelected) {
            runPipelineButton.setToolTipText("Select a pipeline");
        } else if (isManualPipelineRunning || isTranscribing) {
            runPipelineButton.setToolTipText("Pipeline is running...");
        } else {
            runPipelineButton.setToolTipText("Run selected pipeline on transcription text");
        }
    }

    /**
     * Updates the "Auto-pipeline active" indicator visibility.
     * Shows when section is hidden but automatic post-processing is enabled.
     */
    private void updatePipelineActiveIndicator() {
        if (pipelineActiveIndicator == null) {
            return; // Not yet initialized
        }

        boolean sectionHidden = !showPipelineSectionCheckBox.isSelected();
        boolean autoEnabled = enablePostProcessingCheckBox.isSelected();

        // Show indicator only when section is hidden but auto-processing is enabled
        pipelineActiveIndicator.setVisible(sectionHidden && autoEnabled);
    }

    /**
     * Runs the selected pipeline manually on the current transcription text.
     * Works regardless of "Enable Post Processing" checkbox state.
     */
    private void runManualPipeline() {
        String transcript = transcriptionTextArea.getText();
        if (transcript == null || transcript.trim().isEmpty()) {
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.WARNING,
                    "No transcription text to process");
            return;
        }

        PostProcessingItem selectedItem = (PostProcessingItem) postProcessingSelectComboBox.getSelectedItem();
        if (selectedItem == null || selectedItem.uuid == null) {
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.WARNING,
                    "Please select a pipeline");
            return;
        }

        Pipeline pipeline = configManager.getPipelineByUuid(selectedItem.uuid);
        if (pipeline == null) {
            Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                    "Pipeline not found: " + selectedItem.title);
            return;
        }

        // Update UI state
        isManualPipelineRunning = true;
        runPipelineButton.setText("Running...");
        runPipelineButton.setEnabled(false);
        setProcessingState(true, IndeterminateProgressBar.Stage.POST_PROCESSING);  // Orange progress bar

        ConsoleLogger.getInstance().separator();
        ConsoleLogger.getInstance().log("Manual pipeline run: " + pipeline.title);

        // Run pipeline in worker
        new ManualPipelineWorker(transcript, pipeline).execute();
    }

    /**
     * Worker for running manual pipeline executions asynchronously
     */
    private class ManualPipelineWorker extends SwingWorker<String, Void> {
        private final String inputText;
        private final Pipeline pipeline;
        private final long startTime;
        private final String previousResult;  // Capture current result before running

        public ManualPipelineWorker(String inputText, Pipeline pipeline) {
            this.inputText = inputText;
            this.pipeline = pipeline;
            this.startTime = System.currentTimeMillis();
            // Capture the current post-processed text before we run
            this.previousResult = processedText.getText();
        }

        @Override
        protected String doInBackground() {
            PostProcessingService ppService = new PostProcessingService(configManager);
            return ppService.applyPipeline(inputText, pipeline);
        }

        @Override
        protected void done() {
            try {
                String result = get();
                int executionTime = (int) (System.currentTimeMillis() - startTime);

                // Result stacking: save previous result to history if it exists
                if (previousResult != null && !previousResult.trim().isEmpty()) {
                    // The previous result was from some pipeline run, we need to save it
                    // Note: We don't have the previous pipeline info, so we mark it as "Previous result"
                    // This is a limitation - in Phase 4 we'll track this properly
                    ConsoleLogger.getInstance().log("Previous result saved to history");
                    Notificationmanager.getInstance().showNotification(ToastNotification.Type.INFO,
                            "Previous result saved to history");
                }

                // Add new result to history
                pipelineHistory.addResult(pipeline.uuid, pipeline.title, result, executionTime);

                // Update history panel
                historyPanel.updateResults(pipelineHistory.getResults());

                // Update the display
                processedText.setText(result);

                // Post-processed text area is now always visible

                ConsoleLogger.getInstance().logSuccess("Manual pipeline completed: " + pipeline.title +
                        " (" + executionTime + "ms)");

                Notificationmanager.getInstance().showNotification(ToastNotification.Type.SUCCESS,
                        "Pipeline completed: " + pipeline.title);

                // Remember the last used pipeline
                configManager.setLastUsedPipelineUUID(pipeline.uuid);

            } catch (Exception e) {
                logger.error("Error during manual pipeline execution", e);
                ConsoleLogger.getInstance().logError("Pipeline failed: " + e.getMessage());
                Notificationmanager.getInstance().showNotification(ToastNotification.Type.ERROR,
                        "Pipeline failed: " + e.getMessage());
            } finally {
                // Reset UI state
                isManualPipelineRunning = false;
                setProcessingState(false);  // Reset indicator and tray
                runPipelineButton.setText("Run Pipeline");
                updateRunPipelineButtonState();
            }
        }
    }

    // ========== Log Search Functionality ==========

    /**
     * Performs a search in the log text and highlights all matches.
     * Called when the search text changes.
     */
    private void performLogSearch() {
        String searchText = logSearchField.getText();
        searchMatches.clear();
        currentSearchIndex = -1;

        // Remove any existing highlights
        consoleLogArea.getHighlighter().removeAllHighlights();

        if (searchText == null || searchText.isEmpty()) {
            searchResultLabel.setText("");
            return;
        }

        String logText = consoleLogArea.getText().toLowerCase();
        String searchLower = searchText.toLowerCase();

        // Find all matches
        int index = 0;
        while ((index = logText.indexOf(searchLower, index)) != -1) {
            searchMatches.add(new int[]{index, index + searchText.length()});
            index += searchText.length();
        }

        if (searchMatches.isEmpty()) {
            searchResultLabel.setText("No matches");
            searchResultLabel.setForeground(new Color(200, 80, 80));
        } else {
            searchResultLabel.setText(searchMatches.size() + " matches");
            searchResultLabel.setForeground(Color.GRAY);
            highlightAllMatches();
            // Automatically go to first match
            currentSearchIndex = 0;
            scrollToMatch(currentSearchIndex);
        }
    }

    /**
     * Highlights all search matches in the log.
     */
    private void highlightAllMatches() {
        javax.swing.text.Highlighter highlighter = consoleLogArea.getHighlighter();
        javax.swing.text.Highlighter.HighlightPainter painter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255, 255, 150));

        for (int[] match : searchMatches) {
            try {
                highlighter.addHighlight(match[0], match[1], painter);
            } catch (Exception e) {
                logger.debug("Could not highlight match", e);
            }
        }
    }

    /**
     * Scrolls to a specific match and highlights it more prominently.
     */
    private void scrollToMatch(int matchIndex) {
        if (matchIndex < 0 || matchIndex >= searchMatches.size()) {
            return;
        }

        int[] match = searchMatches.get(matchIndex);
        try {
            // Scroll to the match position
            consoleLogArea.setCaretPosition(match[0]);
            consoleLogArea.moveCaretPosition(match[1]);

            // Update result label
            searchResultLabel.setText((matchIndex + 1) + " of " + searchMatches.size());
            searchResultLabel.setForeground(Color.GRAY);
        } catch (Exception e) {
            logger.debug("Could not scroll to match", e);
        }
    }

    /**
     * Moves to the next search match.
     */
    private void findNextInLog() {
        if (searchMatches.isEmpty()) {
            performLogSearch();
            return;
        }

        currentSearchIndex++;
        if (currentSearchIndex >= searchMatches.size()) {
            currentSearchIndex = 0;  // Wrap around
        }
        scrollToMatch(currentSearchIndex);
    }

    /**
     * Moves to the previous search match.
     */
    private void findPreviousInLog() {
        if (searchMatches.isEmpty()) {
            performLogSearch();
            return;
        }

        currentSearchIndex--;
        if (currentSearchIndex < 0) {
            currentSearchIndex = searchMatches.size() - 1;  // Wrap around
        }
        scrollToMatch(currentSearchIndex);
    }

    /**
     * Clears the search state.
     */
    private void clearLogSearch() {
        logSearchField.setText("");
        searchMatches.clear();
        currentSearchIndex = -1;
        searchResultLabel.setText("");
        consoleLogArea.getHighlighter().removeAllHighlights();
    }

    // ========== Process Progress Panel Support ==========

    private File currentProcessingFile;  // Track file for retry functionality
    private SwingWorker<?, ?> currentWorker;  // Track current worker for cancellation

    /**
     * Cancels the current file processing operation.
     */
    private void cancelCurrentOperation() {
        ConsoleLogger.getInstance().log("Operation cancelled by user");
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
        processProgressPanel.hidePanel();
        resetUIAfterTranscription();
        updateTrayMenu();
    }

    /**
     * Retries the last failed file processing operation.
     */
    private void retryCurrentOperation() {
        if (currentProcessingFile != null && currentProcessingFile.exists()) {
            ConsoleLogger.getInstance().log("Retrying operation on: " + currentProcessingFile.getName());
            processProgressPanel.reset();
            handleDroppedAudioFile(currentProcessingFile);
        } else {
            ConsoleLogger.getInstance().logError("Cannot retry - no file available");
            processProgressPanel.hidePanel();
        }
    }

    /**
     * Shows the process progress panel for file-based operations.
     */
    private void showProgressPanel(File file, String stage, IndeterminateProgressBar.Stage progressStage) {
        currentProcessingFile = file;
        processProgressPanel.setFile(file);
        processProgressPanel.start(progressStage, stage);
    }

    /**
     * Updates the process progress panel stage text.
     */
    private void updateProgressPanelStage(String stage) {
        processProgressPanel.setStage(stage);
    }

    /**
     * Hides the process progress panel.
     */
    private void hideProgressPanel() {
        processProgressPanel.hidePanel();
        currentProcessingFile = null;
        currentWorker = null;
    }

}
