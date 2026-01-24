package org.whisperdog.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * Reusable search bar component with next/prev navigation.
 * Used for log search functionality.
 */
public class SearchBar extends JPanel {

    // Spacing constants (inline until Spacing utility exists)
    private static final int SPACING_XS = 4;
    private static final int SPACING_SM = 8;

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
        setLayout(new FlowLayout(FlowLayout.LEFT, SPACING_SM, SPACING_XS));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(SPACING_XS, SPACING_SM, SPACING_XS, SPACING_SM)
        ));

        // Search label and field
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
        prevButton = new JButton("▲");
        prevButton.setToolTipText("Previous match (Shift+Enter)");
        prevButton.setMargin(new Insets(2, 6, 2, 6));
        prevButton.addActionListener(e -> previousMatch());
        add(prevButton);

        nextButton = new JButton("▼");
        nextButton.setToolTipText("Next match (Enter)");
        nextButton.setMargin(new Insets(2, 6, 2, 6));
        nextButton.addActionListener(e -> nextMatch());
        add(nextButton);

        // Match count label
        matchCountLabel = new JLabel("0 of 0");
        matchCountLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(matchCountLabel);

        // Close button
        closeButton = new JButton("✕");
        closeButton.setToolTipText("Close (Escape)");
        closeButton.setMargin(new Insets(2, 6, 2, 6));
        closeButton.addActionListener(e -> close());
        add(closeButton);

        setVisible(false);
    }

    /**
     * Show the search bar and focus the search field.
     */
    public void showSearchBar() {
        setVisible(true);
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /**
     * Close the search bar.
     */
    public void close() {
        setVisible(false);
        searchField.setText("");
        setMatchCount(0, 0);
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
            updateMatchCountLabel();
        }
    }

    private void previousMatch() {
        if (onPrev != null && totalMatches > 0) {
            currentMatch = currentMatch > 1 ? currentMatch - 1 : totalMatches;
            onPrev.run();
            updateMatchCountLabel();
        }
    }

    /**
     * Update the match count display.
     * @param current Current match index (1-based)
     * @param total Total number of matches
     */
    public void setMatchCount(int current, int total) {
        this.currentMatch = current;
        this.totalMatches = total;
        updateMatchCountLabel();
    }

    private void updateMatchCountLabel() {
        if (totalMatches == 0) {
            if (searchField.getText().isEmpty()) {
                matchCountLabel.setText("0 of 0");
                matchCountLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            } else {
                matchCountLabel.setText("No matches");
                matchCountLabel.setForeground(new Color(198, 40, 40));
            }
        } else {
            matchCountLabel.setText(currentMatch + " of " + totalMatches);
            matchCountLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    // Event handlers
    public void setOnSearch(Consumer<String> handler) { this.onSearch = handler; }
    public void setOnPrev(Runnable handler) { this.onPrev = handler; }
    public void setOnNext(Runnable handler) { this.onNext = handler; }
    public void setOnClose(Runnable handler) { this.onClose = handler; }

    public String getSearchText() { return searchField.getText(); }

    public int getCurrentMatch() { return currentMatch; }
    public int getTotalMatches() { return totalMatches; }
}
