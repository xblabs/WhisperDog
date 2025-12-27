# Implementation Plan: UI/UX Improvements

## Phase 1: Icon System & Padding (WD-0002)

### 1.1 Create Spacing Utility

**File**: `src/main/java/org/whisperdog/ui/Spacing.java` (new)

```java
package org.whisperdog.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Centralized spacing constants based on 8px base unit.
 * Use these exclusively for consistent padding/margins.
 */
public final class Spacing {

    private Spacing() {} // Utility class

    // Base spacing scale
    public static final int XS = 4;   // Dense inline
    public static final int SM = 8;   // Standard inline
    public static final int MD = 12;  // Section padding
    public static final int LG = 16;  // Panel padding
    public static final int XL = 24;  // Major sections

    // Pre-built borders for common use cases
    public static Border listItem() {
        return BorderFactory.createEmptyBorder(SM, MD, SM, MD);
    }

    public static Border panel() {
        return BorderFactory.createEmptyBorder(LG, LG, LG, LG);
    }

    public static Border section() {
        return BorderFactory.createEmptyBorder(XL, MD, MD, MD);
    }

    public static Border button() {
        return BorderFactory.createEmptyBorder(SM, LG, SM, LG);
    }

    public static Border formField() {
        return BorderFactory.createEmptyBorder(XS, 0, SM, 0);
    }

    // Insets for layout managers
    public static Insets listItemInsets() {
        return new Insets(SM, MD, SM, MD);
    }

    public static Insets panelInsets() {
        return new Insets(LG, LG, LG, LG);
    }

    // Gap utilities for layouts
    public static int gap() { return SM; }
    public static int sectionGap() { return MD; }
    public static int panelGap() { return LG; }
}
```

### 1.2 Create Icon Manager

**File**: `src/main/java/org/whisperdog/ui/Icons.java` (new)

```java
package org.whisperdog.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized icon management using FlatSVGIcon.
 * All icons loaded from resources/icon/svg/ directory.
 */
public final class Icons {

    private Icons() {} // Utility class

    // Standard sizes
    public static final int SIZE_SM = 16;
    public static final int SIZE_MD = 20;
    public static final int SIZE_LG = 24;

    // Icon cache
    private static final Map<String, FlatSVGIcon> cache = new HashMap<>();

    // Pre-defined icons (lazy-loaded)
    public static FlatSVGIcon mic() { return get("mic", SIZE_MD); }
    public static FlatSVGIcon stop() { return get("stop", SIZE_MD); }
    public static FlatSVGIcon play() { return get("play", SIZE_SM); }
    public static FlatSVGIcon sliders() { return get("sliders", SIZE_MD); }
    public static FlatSVGIcon terminal() { return get("terminal", SIZE_MD); }
    public static FlatSVGIcon gitPullRequest() { return get("git-pull-request", SIZE_MD); }
    public static FlatSVGIcon box() { return get("box", SIZE_MD); }
    public static FlatSVGIcon chevronDown() { return get("chevron-down", SIZE_SM); }
    public static FlatSVGIcon chevronUp() { return get("chevron-up", SIZE_SM); }
    public static FlatSVGIcon edit() { return get("edit", SIZE_SM); }
    public static FlatSVGIcon trash() { return get("trash", SIZE_SM); }
    public static FlatSVGIcon dark() { return get("dark", SIZE_SM); }
    public static FlatSVGIcon search() { return get("search", SIZE_SM); }
    public static FlatSVGIcon copy() { return get("copy", SIZE_SM); }
    public static FlatSVGIcon folder() { return get("folder", SIZE_SM); }
    public static FlatSVGIcon x() { return get("x", SIZE_SM); }
    public static FlatSVGIcon clock() { return get("clock", SIZE_SM); }

    /**
     * Get icon by name and size, with caching.
     */
    public static FlatSVGIcon get(String name, int size) {
        String key = name + "_" + size;
        return cache.computeIfAbsent(key, k -> {
            try {
                return new FlatSVGIcon("icon/svg/" + name + ".svg", size, size);
            } catch (Exception e) {
                System.err.println("Failed to load icon: " + name);
                return null;
            }
        });
    }

    /**
     * Create a scaled version of an icon.
     */
    public static FlatSVGIcon scaled(String name, float scale) {
        return get(name, (int)(SIZE_MD * scale));
    }
}
```

### 1.3 Update Left Menu Icons

**File**: `src/main/java/org/whisperdog/ui/LeftMenuPanel.java` (modify)

```java
// Replace icon setup with Icons utility

// Before
// JButton logsButton = new JButton("📜 Logs");  // Unicode, may break

// After
import org.whisperdog.ui.Icons;

JButton logsButton = new JButton("Logs", Icons.terminal());
logsButton.setHorizontalAlignment(SwingConstants.LEFT);
logsButton.setBorder(Spacing.listItem());

JButton pipelinesButton = new JButton("Pipelines", Icons.gitPullRequest());
pipelinesButton.setHorizontalAlignment(SwingConstants.LEFT);
pipelinesButton.setBorder(Spacing.listItem());

JButton unitsButton = new JButton("Units", Icons.box());
unitsButton.setHorizontalAlignment(SwingConstants.LEFT);
unitsButton.setBorder(Spacing.listItem());

JButton settingsButton = new JButton("Options", Icons.sliders());
settingsButton.setHorizontalAlignment(SwingConstants.LEFT);
settingsButton.setBorder(Spacing.listItem());
```

### 1.4 Update List Padding

**File**: `src/main/java/org/whisperdog/ui/UnitLibraryListForm.java` (modify)

```java
// Add consistent padding to list renderer

class UnitListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus);

        // Apply consistent spacing
        label.setBorder(Spacing.listItem());

        return label;
    }
}
```

**File**: `src/main/java/org/whisperdog/ui/PipelineListForm.java` (modify)

```java
// Same pattern as UnitLibraryListForm
// Apply Spacing.listItem() border to list cell renderer
```

## Phase 2: Post-Processing UI (WD-0003)

### 2.1 Create Post-Processing Panel

**File**: `src/main/java/org/whisperdog/ui/PostProcessingPanel.java` (new)

```java
package org.whisperdog.ui;

import org.whisperdog.ConfigManager;

import javax.swing.*;
import java.awt.*;

/**
 * Reorganized post-processing controls with separate visibility and auto-execution.
 */
public class PostProcessingPanel extends JPanel {

    private final ConfigManager configManager;

    private JCheckBox showSectionCheckbox;
    private JCheckBox autoExecuteCheckbox;
    private JCheckBox activateOnStartupCheckbox;
    private JComboBox<String> pipelineDropdown;
    private JPanel contentPanel;

    public PostProcessingPanel(ConfigManager configManager) {
        this.configManager = configManager;
        setLayout(new BorderLayout());
        initComponents();
        layoutComponents();
        loadSettings();
    }

    private void initComponents() {
        // Show section checkbox (controls visibility)
        showSectionCheckbox = new JCheckBox("Show pipeline processing section");
        showSectionCheckbox.addActionListener(e -> {
            boolean show = showSectionCheckbox.isSelected();
            contentPanel.setVisible(show);
            configManager.setBoolean("ui.show_pipeline_section", show);
            revalidate();
        });

        // Pipeline dropdown
        pipelineDropdown = new JComboBox<>();
        loadPipelines();

        // Auto-execute checkbox
        autoExecuteCheckbox = new JCheckBox("Enable automatic post-processing");
        autoExecuteCheckbox.addActionListener(e -> {
            boolean auto = autoExecuteCheckbox.isSelected();
            activateOnStartupCheckbox.setEnabled(auto);
            configManager.setBoolean("pipeline.auto_execute", auto);
            updateStatusIndicator();
        });

        // Activate on startup
        activateOnStartupCheckbox = new JCheckBox("Activate on application startup");
        activateOnStartupCheckbox.addActionListener(e -> {
            configManager.setBoolean("pipeline.activate_on_startup",
                activateOnStartupCheckbox.isSelected());
        });
    }

    private void layoutComponents() {
        // Top: Show section checkbox
        add(showSectionCheckbox, BorderLayout.NORTH);

        // Content panel (can be hidden)
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(Spacing.panel());

        // Pipeline selection row
        JPanel pipelineRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pipelineRow.add(new JLabel("Pipeline:"));
        pipelineRow.add(pipelineDropdown);
        contentPanel.add(pipelineRow);

        // Auto-execute section
        JPanel autoPanel = new JPanel();
        autoPanel.setLayout(new BoxLayout(autoPanel, BoxLayout.Y_AXIS));
        autoPanel.setBorder(BorderFactory.createEmptyBorder(Spacing.MD, 0, 0, 0));
        autoPanel.add(autoExecuteCheckbox);

        JPanel startupRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startupRow.setBorder(BorderFactory.createEmptyBorder(0, Spacing.LG, 0, 0));
        startupRow.add(activateOnStartupCheckbox);
        autoPanel.add(startupRow);

        contentPanel.add(autoPanel);

        add(contentPanel, BorderLayout.CENTER);
    }

    private void loadSettings() {
        showSectionCheckbox.setSelected(
            configManager.getBoolean("ui.show_pipeline_section", true));
        autoExecuteCheckbox.setSelected(
            configManager.getBoolean("pipeline.auto_execute", false));
        activateOnStartupCheckbox.setSelected(
            configManager.getBoolean("pipeline.activate_on_startup", false));

        contentPanel.setVisible(showSectionCheckbox.isSelected());
        activateOnStartupCheckbox.setEnabled(autoExecuteCheckbox.isSelected());

        updateStatusIndicator();
    }

    private void loadPipelines() {
        // Load available pipelines from config
        // pipelineDropdown.addItem("Default Pipeline");
    }

    /**
     * Shows subtle indicator when section hidden but auto-execute enabled.
     */
    private void updateStatusIndicator() {
        // This would update a status bar indicator
        // Implementation depends on main frame structure
        boolean hiddenButActive = !showSectionCheckbox.isSelected()
            && autoExecuteCheckbox.isSelected();

        // Fire event for status bar to update
        firePropertyChange("hiddenButActive", null, hiddenButActive);
    }

    public boolean isAutoExecuteEnabled() {
        return autoExecuteCheckbox.isSelected();
    }

    public String getSelectedPipeline() {
        return (String) pipelineDropdown.getSelectedItem();
    }
}
```

## Phase 3: Searchable Log (WD-0009)

### 3.1 Create Search Bar Component

**File**: `src/main/java/org/whisperdog/ui/SearchBar.java` (new)

```java
package org.whisperdog.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * Reusable search bar component with next/prev navigation.
 */
public class SearchBar extends JPanel {

    private final JTextField searchField;
    private final JLabel matchCountLabel;
    private final JButton prevButton;
    private final JButton nextButton;
    private final JButton closeButton;

    private Consumer<String> onSearch;
    private Runnable onPrev;
    private Runnable onNext;
    private Runnable onClose;

    private int currentMatch = 0;
    private int totalMatches = 0;

    public SearchBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, Spacing.SM, Spacing.XS));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(Spacing.XS, Spacing.SM, Spacing.XS, Spacing.SM)
        ));

        // Search icon and field
        add(new JLabel("Search:"));

        searchField = new JTextField(20);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        previousMatch();
                    } else {
                        nextMatch();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    close();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER &&
                    e.getKeyCode() != KeyEvent.VK_ESCAPE) {
                    performSearch();
                }
            }
        });
        add(searchField);

        // Navigation buttons
        prevButton = new JButton(Icons.chevronUp());
        prevButton.setToolTipText("Previous match (Shift+Enter)");
        prevButton.addActionListener(e -> previousMatch());
        add(prevButton);

        nextButton = new JButton(Icons.chevronDown());
        nextButton.setToolTipText("Next match (Enter)");
        nextButton.addActionListener(e -> nextMatch());
        add(nextButton);

        // Match count
        matchCountLabel = new JLabel("0 of 0");
        matchCountLabel.setForeground(Color.GRAY);
        add(matchCountLabel);

        // Close button
        closeButton = new JButton(Icons.x());
        closeButton.setToolTipText("Close (Escape)");
        closeButton.addActionListener(e -> close());
        add(closeButton);

        setVisible(false);
    }

    public void show() {
        setVisible(true);
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    public void close() {
        setVisible(false);
        if (onClose != null) onClose.run();
    }

    private void performSearch() {
        String query = searchField.getText();
        if (onSearch != null) {
            onSearch.accept(query);
        }
    }

    private void nextMatch() {
        if (onNext != null && totalMatches > 0) {
            currentMatch = (currentMatch % totalMatches) + 1;
            onNext.run();
            updateMatchCount();
        }
    }

    private void previousMatch() {
        if (onPrev != null && totalMatches > 0) {
            currentMatch = currentMatch > 1 ? currentMatch - 1 : totalMatches;
            onPrev.run();
            updateMatchCount();
        }
    }

    public void setMatchCount(int current, int total) {
        this.currentMatch = current;
        this.totalMatches = total;
        updateMatchCount();
    }

    private void updateMatchCount() {
        if (totalMatches == 0) {
            matchCountLabel.setText("No matches");
            matchCountLabel.setForeground(Color.RED);
        } else {
            matchCountLabel.setText(currentMatch + " of " + totalMatches);
            matchCountLabel.setForeground(Color.GRAY);
        }
    }

    public void setOnSearch(Consumer<String> handler) { this.onSearch = handler; }
    public void setOnPrev(Runnable handler) { this.onPrev = handler; }
    public void setOnNext(Runnable handler) { this.onNext = handler; }
    public void setOnClose(Runnable handler) { this.onClose = handler; }

    public String getSearchText() { return searchField.getText(); }
}
```

### 3.2 Create Searchable Log Panel

**File**: `src/main/java/org/whisperdog/ui/SearchableLogPanel.java` (new)

```java
package org.whisperdog.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

/**
 * Log panel with search highlighting and navigation.
 */
public class SearchableLogPanel extends JPanel {

    private final JTextPane logPane;
    private final SearchBar searchBar;
    private final DefaultStyledDocument doc;

    private List<int[]> matches = new ArrayList<>();
    private int currentMatchIndex = -1;

    // Highlight colors
    private static final Color MATCH_COLOR = new Color(255, 245, 157); // Yellow
    private static final Color CURRENT_MATCH_COLOR = new Color(255, 183, 77); // Orange

    public SearchableLogPanel() {
        setLayout(new BorderLayout());

        // Search bar (initially hidden)
        searchBar = new SearchBar();
        searchBar.setOnSearch(this::search);
        searchBar.setOnNext(this::nextMatch);
        searchBar.setOnPrev(this::prevMatch);
        searchBar.setOnClose(this::clearHighlights);
        add(searchBar, BorderLayout.NORTH);

        // Log text pane
        doc = new DefaultStyledDocument();
        logPane = new JTextPane(doc);
        logPane.setEditable(false);
        logPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(logPane);
        add(scrollPane, BorderLayout.CENTER);

        // Ctrl+F keyboard shortcut
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
            "openSearch"
        );
        getActionMap().put("openSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchBar.show();
            }
        });
    }

    /**
     * Append a log entry.
     */
    public void appendLog(String text) {
        try {
            doc.insertString(doc.getLength(), text + "\n", null);
            logPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Search for text and highlight all matches.
     */
    private void search(String query) {
        clearHighlights();
        matches.clear();
        currentMatchIndex = -1;

        if (query == null || query.isEmpty()) {
            searchBar.setMatchCount(0, 0);
            return;
        }

        String text = logPane.getText();
        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        Highlighter highlighter = logPane.getHighlighter();

        while (matcher.find()) {
            try {
                matches.add(new int[]{matcher.start(), matcher.end()});
                highlighter.addHighlight(
                    matcher.start(),
                    matcher.end(),
                    new DefaultHighlighter.DefaultHighlightPainter(MATCH_COLOR)
                );
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        if (!matches.isEmpty()) {
            currentMatchIndex = 0;
            highlightCurrentMatch();
        }

        searchBar.setMatchCount(
            matches.isEmpty() ? 0 : 1,
            matches.size()
        );
    }

    private void nextMatch() {
        if (matches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + 1) % matches.size();
        highlightCurrentMatch();
        searchBar.setMatchCount(currentMatchIndex + 1, matches.size());
    }

    private void prevMatch() {
        if (matches.isEmpty()) return;
        currentMatchIndex = currentMatchIndex > 0 ? currentMatchIndex - 1 : matches.size() - 1;
        highlightCurrentMatch();
        searchBar.setMatchCount(currentMatchIndex + 1, matches.size());
    }

    private void highlightCurrentMatch() {
        // Re-apply highlights with current match in different color
        Highlighter highlighter = logPane.getHighlighter();
        highlighter.removeAllHighlights();

        try {
            for (int i = 0; i < matches.size(); i++) {
                int[] match = matches.get(i);
                Color color = (i == currentMatchIndex) ? CURRENT_MATCH_COLOR : MATCH_COLOR;
                highlighter.addHighlight(
                    match[0],
                    match[1],
                    new DefaultHighlighter.DefaultHighlightPainter(color)
                );
            }

            // Scroll to current match
            if (currentMatchIndex >= 0) {
                int[] current = matches.get(currentMatchIndex);
                logPane.setCaretPosition(current[0]);
                logPane.scrollRectToVisible(logPane.modelToView(current[0]));
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void clearHighlights() {
        logPane.getHighlighter().removeAllHighlights();
        matches.clear();
        currentMatchIndex = -1;
    }

    public void clear() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        clearHighlights();
    }
}
```

## Phase 4: Long-Running Process UX (WD-0011)

### 4.1 Create Progress Panel

**File**: `src/main/java/org/whisperdog/ui/ProcessProgressPanel.java` (new)

```java
package org.whisperdog.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

/**
 * Progress panel for long-running operations with file access and cancel/retry.
 */
public class ProcessProgressPanel extends JPanel {

    private final JLabel titleLabel;
    private final JProgressBar progressBar;
    private final JLabel stageLabel;
    private final JLabel filePathLabel;
    private final JButton copyPathButton;
    private final JButton openFolderButton;
    private final JButton cancelButton;
    private final JButton retryButton;
    private final JButton dismissButton;

    private File currentFile;
    private Runnable onCancel;
    private Runnable onRetry;
    private Runnable onDismiss;

    public ProcessProgressPanel() {
        setLayout(new BorderLayout(Spacing.SM, Spacing.SM));
        setBorder(Spacing.panel());

        // Title
        titleLabel = new JLabel("Processing...");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(titleLabel, BorderLayout.NORTH);

        // Center content
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
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

        filePathLabel = new JLabel();
        filePathLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        fileRow.add(new JLabel("File:"));
        fileRow.add(filePathLabel);

        copyPathButton = new JButton(Icons.copy());
        copyPathButton.setToolTipText("Copy path to clipboard");
        copyPathButton.addActionListener(e -> copyPath());
        fileRow.add(copyPathButton);

        openFolderButton = new JButton(Icons.folder());
        openFolderButton.setToolTipText("Open containing folder");
        openFolderButton.addActionListener(e -> openFolder());
        fileRow.add(openFolderButton);

        centerPanel.add(fileRow);
        add(centerPanel, BorderLayout.CENTER);

        // Button row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            if (onCancel != null) onCancel.run();
        });
        buttonPanel.add(cancelButton);

        retryButton = new JButton("Retry");
        retryButton.setVisible(false);
        retryButton.addActionListener(e -> {
            if (onRetry != null) onRetry.run();
        });
        buttonPanel.add(retryButton);

        dismissButton = new JButton("Dismiss");
        dismissButton.setVisible(false);
        dismissButton.addActionListener(e -> {
            if (onDismiss != null) onDismiss.run();
        });
        buttonPanel.add(dismissButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setStage(String stage) {
        stageLabel.setText(stage);
    }

    public void setProgress(int percent) {
        progressBar.setValue(percent);
        progressBar.setString(percent + "%");
    }

    public void setFile(File file) {
        this.currentFile = file;
        if (file != null) {
            String path = file.getAbsolutePath();
            if (path.length() > 50) {
                path = "..." + path.substring(path.length() - 47);
            }
            filePathLabel.setText(path);
            filePathLabel.setToolTipText(file.getAbsolutePath());
        } else {
            filePathLabel.setText("");
            filePathLabel.setToolTipText(null);
        }
    }

    public void showError(String errorMessage) {
        titleLabel.setText("⚠ Processing Failed");
        titleLabel.setForeground(new Color(198, 40, 40));
        stageLabel.setText("Error: " + errorMessage);
        progressBar.setVisible(false);
        cancelButton.setVisible(false);
        retryButton.setVisible(true);
        dismissButton.setVisible(true);
    }

    public void showSuccess() {
        titleLabel.setText("✓ Complete");
        titleLabel.setForeground(new Color(46, 125, 50));
        progressBar.setValue(100);
        cancelButton.setVisible(false);
    }

    public void reset() {
        titleLabel.setText("Processing...");
        titleLabel.setForeground(null);
        stageLabel.setText("Initializing...");
        progressBar.setVisible(true);
        progressBar.setValue(0);
        cancelButton.setVisible(true);
        retryButton.setVisible(false);
        dismissButton.setVisible(false);
    }

    private void copyPath() {
        if (currentFile != null) {
            StringSelection selection = new StringSelection(currentFile.getAbsolutePath());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

            // Visual feedback
            copyPathButton.setToolTipText("Copied!");
            Timer timer = new Timer(1500, e -> copyPathButton.setToolTipText("Copy path to clipboard"));
            timer.setRepeats(false);
            timer.start();
        }
    }

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

    public void setOnCancel(Runnable handler) { this.onCancel = handler; }
    public void setOnRetry(Runnable handler) { this.onRetry = handler; }
    public void setOnDismiss(Runnable handler) { this.onDismiss = handler; }
}
```

## File Summary

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/org/whisperdog/ui/Spacing.java` | Create | Centralized spacing constants |
| `src/main/java/org/whisperdog/ui/Icons.java` | Create | Icon manager with caching |
| `src/main/java/org/whisperdog/ui/PostProcessingPanel.java` | Create | Reorganized post-processing UI |
| `src/main/java/org/whisperdog/ui/SearchBar.java` | Create | Reusable search bar component |
| `src/main/java/org/whisperdog/ui/SearchableLogPanel.java` | Create | Log panel with search |
| `src/main/java/org/whisperdog/ui/ProcessProgressPanel.java` | Create | Progress panel with file access |
| `src/main/java/org/whisperdog/ui/LeftMenuPanel.java` | Modify | Use Icons utility |
| `src/main/java/org/whisperdog/ui/UnitLibraryListForm.java` | Modify | Apply Spacing |
| `src/main/java/org/whisperdog/ui/PipelineListForm.java` | Modify | Apply Spacing |
| `src/main/java/org/whisperdog/recording/RecorderForm.java` | Modify | Integrate new panels |

## Testing Checklist

- [ ] All icons load without errors in both light and dark themes
- [ ] Padding is consistent across all screens (visual inspection)
- [ ] Post-processing checkboxes work independently
- [ ] Hidden-but-active indicator displays correctly
- [ ] Ctrl+F opens search bar in log screen
- [ ] Search highlights all matches with correct colors
- [ ] Next/Previous navigation works correctly
- [ ] Copy Path and Open Folder buttons work
- [ ] Cancel button aborts operations
- [ ] Retry button re-attempts failed operations
