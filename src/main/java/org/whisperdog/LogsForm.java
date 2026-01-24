package org.whisperdog;

import org.whisperdog.ui.SearchBar;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

/**
 * Log viewer panel with search functionality.
 * Supports Ctrl+F search, highlighting, and match navigation.
 */
public class LogsForm extends JPanel {

    private final JTextArea logsTextArea;
    private final SearchBar searchBar;
    private final JScrollPane scrollPane;

    // Highlight colors
    private static final Color MATCH_COLOR = new Color(255, 245, 157); // Yellow
    private static final Color CURRENT_MATCH_COLOR = new Color(255, 183, 77); // Orange

    // Search state
    private List<int[]> matches = new ArrayList<>();
    private int currentMatchIndex = -1;
    private Highlighter.HighlightPainter matchPainter;
    private Highlighter.HighlightPainter currentMatchPainter;

    public LogsForm() {
        setLayout(new BorderLayout());

        // Initialize highlight painters
        matchPainter = new DefaultHighlighter.DefaultHighlightPainter(MATCH_COLOR);
        currentMatchPainter = new DefaultHighlighter.DefaultHighlightPainter(CURRENT_MATCH_COLOR);

        // Search bar (initially hidden)
        searchBar = new SearchBar();
        searchBar.setOnSearch(this::search);
        searchBar.setOnNext(this::nextMatch);
        searchBar.setOnPrev(this::prevMatch);
        searchBar.setOnClose(this::clearHighlights);
        add(searchBar, BorderLayout.NORTH);

        // Log text area
        logsTextArea = new JTextArea();
        logsTextArea.setEditable(false);
        logsTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Register with TextAreaAppender
        TextAreaAppender.setTextArea(logsTextArea);

        scrollPane = new JScrollPane(logsTextArea);
        add(scrollPane, BorderLayout.CENTER);

        // Ctrl+F keyboard shortcut
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        // Register Ctrl+F on the panel
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
            "openSearch"
        );
        getActionMap().put("openSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchBar.showSearchBar();
            }
        });

        // Also register on the text area for when it has focus
        logsTextArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
            "openSearch"
        );
        logsTextArea.getActionMap().put("openSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchBar.showSearchBar();
            }
        });
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

        String text = logsTextArea.getText();
        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        Highlighter highlighter = logsTextArea.getHighlighter();

        while (matcher.find()) {
            try {
                matches.add(new int[]{matcher.start(), matcher.end()});
                highlighter.addHighlight(matcher.start(), matcher.end(), matchPainter);
            } catch (BadLocationException e) {
                // Ignore highlighting errors
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

    /**
     * Navigate to next match.
     */
    private void nextMatch() {
        if (matches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + 1) % matches.size();
        highlightCurrentMatch();
        searchBar.setMatchCount(currentMatchIndex + 1, matches.size());
    }

    /**
     * Navigate to previous match.
     */
    private void prevMatch() {
        if (matches.isEmpty()) return;
        currentMatchIndex = currentMatchIndex > 0 ? currentMatchIndex - 1 : matches.size() - 1;
        highlightCurrentMatch();
        searchBar.setMatchCount(currentMatchIndex + 1, matches.size());
    }

    /**
     * Re-apply highlights with current match in different color.
     */
    private void highlightCurrentMatch() {
        Highlighter highlighter = logsTextArea.getHighlighter();
        highlighter.removeAllHighlights();

        try {
            for (int i = 0; i < matches.size(); i++) {
                int[] match = matches.get(i);
                Highlighter.HighlightPainter painter =
                    (i == currentMatchIndex) ? currentMatchPainter : matchPainter;
                highlighter.addHighlight(match[0], match[1], painter);
            }

            // Scroll to current match
            if (currentMatchIndex >= 0 && currentMatchIndex < matches.size()) {
                int[] current = matches.get(currentMatchIndex);
                logsTextArea.setCaretPosition(current[0]);

                // Scroll to make the match visible
                try {
                    Rectangle rect = logsTextArea.modelToView(current[0]);
                    if (rect != null) {
                        logsTextArea.scrollRectToVisible(rect);
                    }
                } catch (BadLocationException e) {
                    // Ignore scroll errors
                }
            }
        } catch (BadLocationException e) {
            // Ignore highlighting errors
        }
    }

    /**
     * Clear all search highlights.
     */
    private void clearHighlights() {
        logsTextArea.getHighlighter().removeAllHighlights();
        matches.clear();
        currentMatchIndex = -1;
    }

    /**
     * Get the underlying text area (for TextAreaAppender compatibility).
     */
    public JTextArea getTextArea() {
        return logsTextArea;
    }

    /**
     * Clear all log content.
     */
    public void clear() {
        logsTextArea.setText("");
        clearHighlights();
    }

    /**
     * Show the search bar programmatically.
     */
    public void showSearch() {
        searchBar.showSearchBar();
    }
}
