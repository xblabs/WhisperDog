package org.whisperdog.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

/**
 * Progress panel for long-running operations with file access and cancel/retry.
 * Shows current file being processed, progress stage, and action buttons.
 */
public class ProcessProgressPanel extends JPanel {

    private final JLabel titleLabel;
    private final JLabel stageLabel;
    private final JLabel filePathLabel;
    private final JButton copyPathButton;
    private final JButton openFolderButton;
    private final JButton cancelButton;
    private final JButton retryButton;
    private final JButton dismissButton;
    private final IndeterminateProgressBar progressBar;

    private File currentFile;
    private Runnable onCancel;
    private Runnable onRetry;
    private Runnable onDismiss;

    // Colors for different states
    private static final Color ERROR_COLOR = new Color(198, 40, 40);
    private static final Color SUCCESS_COLOR = new Color(46, 125, 50);

    public ProcessProgressPanel() {
        setLayout(new BorderLayout(Spacing.SM, Spacing.SM));
        setBorder(Spacing.panel());

        // Title row (north)
        JPanel titlePanel = new JPanel(new BorderLayout());
        titleLabel = new JLabel("Processing...");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        add(titlePanel, BorderLayout.NORTH);

        // Center content
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Progress bar (indeterminate)
        progressBar = new IndeterminateProgressBar();
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(progressBar);
        centerPanel.add(Box.createVerticalStrut(Spacing.SM));

        // Stage label
        stageLabel = new JLabel("Initializing...");
        stageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(stageLabel);
        centerPanel.add(Box.createVerticalStrut(Spacing.MD));

        // File path row
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, 0));
        fileRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fileLabel = new JLabel("File:");
        fileLabel.setForeground(Color.GRAY);
        fileRow.add(fileLabel);

        filePathLabel = new JLabel();
        filePathLabel.setFont(FontUtil.getMonospacedFont(Font.PLAIN, 11));
        fileRow.add(filePathLabel);

        // Copy Path button
        copyPathButton = createIconButton(Icons.copy(), "Copy path to clipboard");
        copyPathButton.addActionListener(e -> copyPath());
        fileRow.add(copyPathButton);

        // Open Folder button
        openFolderButton = createIconButton(Icons.folder(), "Open containing folder");
        openFolderButton.addActionListener(e -> openFolder());
        fileRow.add(openFolderButton);

        centerPanel.add(fileRow);
        add(centerPanel, BorderLayout.CENTER);

        // Button row (south)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.SM, 0));

        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel the current operation");
        cancelButton.addActionListener(e -> {
            if (onCancel != null) onCancel.run();
        });
        buttonPanel.add(cancelButton);

        retryButton = new JButton("Retry");
        retryButton.setToolTipText("Retry the failed operation");
        retryButton.setVisible(false);
        retryButton.addActionListener(e -> {
            if (onRetry != null) onRetry.run();
        });
        buttonPanel.add(retryButton);

        dismissButton = new JButton("Dismiss");
        dismissButton.setToolTipText("Close this panel");
        dismissButton.setVisible(false);
        dismissButton.addActionListener(e -> {
            if (onDismiss != null) onDismiss.run();
        });
        buttonPanel.add(dismissButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Initially hidden
        setVisible(false);
    }

    /**
     * Creates a small icon button with consistent styling.
     */
    private JButton createIconButton(Icon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setMargin(new Insets(2, 4, 2, 4));
        button.setFocusPainted(false);
        return button;
    }

    /**
     * Sets the title text displayed at the top of the panel.
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
        titleLabel.setForeground(null); // Reset to default color
    }

    /**
     * Sets the current processing stage text.
     */
    public void setStage(String stage) {
        stageLabel.setText(stage);
    }

    /**
     * Sets the current file being processed.
     */
    public void setFile(File file) {
        this.currentFile = file;
        if (file != null) {
            String path = file.getAbsolutePath();
            // Truncate long paths for display
            if (path.length() > 50) {
                path = "..." + path.substring(path.length() - 47);
            }
            filePathLabel.setText(path);
            filePathLabel.setToolTipText(file.getAbsolutePath());
            copyPathButton.setEnabled(true);
            openFolderButton.setEnabled(true);
        } else {
            filePathLabel.setText("");
            filePathLabel.setToolTipText(null);
            copyPathButton.setEnabled(false);
            openFolderButton.setEnabled(false);
        }
    }

    /**
     * Starts the progress panel with the given stage.
     */
    public void start(IndeterminateProgressBar.Stage stage, String stageText) {
        reset();
        setStage(stageText);
        progressBar.start(stage);
        setVisible(true);
    }

    /**
     * Switches the progress bar stage (e.g., from transcription to post-processing).
     */
    public void setProgressStage(IndeterminateProgressBar.Stage stage) {
        if (progressBar.isAnimating()) {
            progressBar.setStage(stage);
        }
    }

    /**
     * Shows an error state with retry and dismiss buttons.
     */
    public void showError(String errorMessage) {
        titleLabel.setText("⚠ Processing Failed");
        titleLabel.setForeground(ERROR_COLOR);
        stageLabel.setText("Error: " + errorMessage);
        progressBar.stop();
        cancelButton.setVisible(false);
        retryButton.setVisible(true);
        dismissButton.setVisible(true);
    }

    /**
     * Shows success state.
     */
    public void showSuccess() {
        titleLabel.setText("✓ Complete");
        titleLabel.setForeground(SUCCESS_COLOR);
        progressBar.stop();
        cancelButton.setVisible(false);
        // Keep panel visible briefly to show success, then can be dismissed
        dismissButton.setVisible(true);
    }

    /**
     * Resets the panel to its initial state.
     */
    public void reset() {
        titleLabel.setText("Processing...");
        titleLabel.setForeground(null);
        stageLabel.setText("Initializing...");
        progressBar.stop();
        cancelButton.setVisible(true);
        retryButton.setVisible(false);
        dismissButton.setVisible(false);
        currentFile = null;
        filePathLabel.setText("");
        filePathLabel.setToolTipText(null);
    }

    /**
     * Hides and resets the panel.
     * Note: Named hidePanel() to avoid conflict with deprecated Component.hide()
     */
    public void hidePanel() {
        setVisible(false);
        reset();
    }

    /**
     * Copies the current file path to the clipboard.
     */
    private void copyPath() {
        if (currentFile != null) {
            StringSelection selection = new StringSelection(currentFile.getAbsolutePath());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

            // Visual feedback via tooltip
            String originalTooltip = copyPathButton.getToolTipText();
            copyPathButton.setToolTipText("Copied!");
            Timer timer = new Timer(1500, e -> copyPathButton.setToolTipText(originalTooltip));
            timer.setRepeats(false);
            timer.start();
        }
    }

    /**
     * Opens the containing folder of the current file.
     */
    private void openFolder() {
        if (currentFile != null && currentFile.getParentFile() != null) {
            try {
                Desktop.getDesktop().open(currentFile.getParentFile());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Could not open folder: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Callback setters
    public void setOnCancel(Runnable handler) { this.onCancel = handler; }
    public void setOnRetry(Runnable handler) { this.onRetry = handler; }
    public void setOnDismiss(Runnable handler) { this.onDismiss = handler; }

    // Getters for state inspection
    public File getCurrentFile() { return currentFile; }
    public boolean isProcessing() { return progressBar.isAnimating(); }
}
