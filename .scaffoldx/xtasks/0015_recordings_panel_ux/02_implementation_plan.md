# Implementation Plan: Recordings Panel UX Improvements

## Phase 1: Critical Fixes

### 1.1 Menu Icon Fix

**File**: `src/main/java/org/whisperdog/sidemenu/MenuItem.java`

**Current Code (line 62-70)**:
```java
private Icon getIcon() {
    Color lightColor = FlatUIUtils.getUIColor("Menu.icon.lightColor", Color.red);
    Color darkColor = FlatUIUtils.getUIColor("Menu.icon.darkColor", Color.red);
    FlatSVGIcon icon = new FlatSVGIcon("icon/" + menuIndex + ".svg", 24, 24);
    // ...
}
```

**Problem**: Icons loaded by position index, not semantic name.

**Solution Options**:

**Option A - Name-based icon mapping** (Recommended):
```java
private static final Map<String, String> MENU_ICONS = Map.of(
    "Record", "microphone",
    "Recordings", "recordings",  // Need new icon or use "list"
    "Settings", "settings",
    "PostPro Pipelines", "pipeline",
    "PostPro Unit Library", "library"
);

private Icon getIcon() {
    String menuName = menus[0];
    String iconName = MENU_ICONS.getOrDefault(menuName, "default");
    FlatSVGIcon icon = new FlatSVGIcon("icon/svg/" + iconName + ".svg", 24, 24);
    // ...
}
```

**Option B - Add recordings icon at correct index**:
- Create `icon/recordings.svg` or use existing icon
- Renumber existing icons: 1.svg→2.svg, 2.svg→3.svg, etc.
- Less maintainable, breaks on future menu changes

**Icon Options for Recordings**:
- Use existing `icon/svg/folder.svg`
- Use existing `icon/svg/list.svg`
- Request new icon from user (TODO: Henry to provide)

### 1.2 Content Padding Fix

**File**: `src/main/java/org/whisperdog/recording/RecordingsPanel.java`

**Current Code (line 32-33)**:
```java
setLayout(new BorderLayout());
setBorder(new EmptyBorder(10, 10, 10, 10));
```

**Change to**:
```java
setLayout(new BorderLayout());
setBorder(new EmptyBorder(24, 24, 24, 24));  // Increased padding
```

Also need to check if parent container adds padding - may need adjustment in MainFrame or content panel wrapper.

### 1.3 Manifest Filesystem Sync

**File**: `src/main/java/org/whisperdog/recording/RecordingManifest.java`

**New Method - scanAndReconcile()**:
```java
/**
 * Scans the recordings directory and reconciles manifest with actual files.
 * - Removes entries for files that no longer exist
 * - Adds entries for orphaned files (files not in manifest)
 */
public void scanAndReconcile() {
    File recordingsDir = this.directory;

    // Get all .wav files in directory
    File[] wavFiles = recordingsDir.listFiles((dir, name) ->
        name.toLowerCase().endsWith(".wav") &&
        name.startsWith("recording_"));

    if (wavFiles == null) return;

    Set<String> filesOnDisk = Arrays.stream(wavFiles)
        .map(File::getName)
        .collect(Collectors.toSet());

    Set<String> filesInManifest = recordings.stream()
        .map(RecordingEntry::getFilename)
        .collect(Collectors.toSet());

    // Remove manifest entries for missing files
    recordings.removeIf(entry -> !filesOnDisk.contains(entry.getFilename()));

    // Add entries for orphaned files (basic metadata only)
    for (String filename : filesOnDisk) {
        if (!filesInManifest.contains(filename)) {
            RecordingEntry orphan = createEntryFromFile(new File(recordingsDir, filename));
            if (orphan != null) {
                recordings.add(orphan);
            }
        }
    }

    save();
}

private RecordingEntry createEntryFromFile(File file) {
    // Parse timestamp from filename: recording_YYYYMMDD_HHmmss.wav
    // Create basic entry with file metadata
}
```

**File**: `src/main/java/org/whisperdog/recording/RecordingRetentionManager.java`

**Update refresh/reload**:
```java
public void reloadManifest() {
    initializeManifest();
    manifest.scanAndReconcile();  // Add reconciliation
}
```

**File**: `src/main/java/org/whisperdog/recording/RecordingsPanel.java`

**Update refresh()**:
```java
public void refresh() {
    retentionManager.reloadManifest();  // This now includes reconciliation
    // ... rest of refresh logic
}
```

---

## Phase 2: Button Interactivity

**File**: `src/main/java/org/whisperdog/recording/RecordingsPanel.java`

**Current button creation**:
```java
JButton playButton = new JButton("Play");
playButton.setIcon(new FlatSVGIcon("icon/svg/play.svg", 14, 14));
```

**Enhanced button with hover/click states**:
```java
private JButton createStyledButton(String text, String iconPath) {
    JButton button = new JButton(text);
    button.setIcon(new FlatSVGIcon(iconPath, 14, 14));
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    button.setFocusPainted(false);

    // FlatLaf styling for hover/press states
    button.putClientProperty(FlatClientProperties.STYLE, ""
        + "arc:6;"
        + "margin:4,8,4,8;"
        + "background:$Button.background;"
        + "hoverBackground:$Button.hoverBackground;"
        + "pressedBackground:$Button.pressedBackground;");

    return button;
}
```

**Alternative - Custom ButtonUI for reusable styling**:
```java
// Create: src/main/java/org/whisperdog/ui/StyledButtonUI.java
public class StyledButtonUI extends BasicButtonUI {
    // Implement paintComponent with hover/press detection
    // Use MouseListener to track hover state
}
```

---

## Phase 3: Expandable Transcription

**File**: `src/main/java/org/whisperdog/recording/RecordingsPanel.java`

**Current implementation**:
```java
JLabel previewLabel = new JLabel("<html>" + truncate(preview, 80) + "</html>");
```

**New expandable implementation**:
```java
private JPanel createExpandablePreview(String fullText, int maxLength) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    boolean needsTruncation = fullText != null && fullText.length() > maxLength;
    String displayText = needsTruncation ? truncate(fullText, maxLength) : fullText;

    JLabel previewLabel = new JLabel("<html>" + displayText + "</html>");
    previewLabel.setFont(previewLabel.getFont().deriveFont(Font.ITALIC, 11f));
    previewLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

    if (needsTruncation) {
        previewLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final boolean[] expanded = {false};

        previewLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                expanded[0] = !expanded[0];
                previewLabel.setText("<html>" +
                    (expanded[0] ? fullText : truncate(fullText, maxLength)) +
                    "</html>");

                // Trigger re-layout
                contentPanel.revalidate();
                contentPanel.repaint();
            }
        });
    }

    panel.add(previewLabel, BorderLayout.CENTER);
    return panel;
}
```

---

## Phase 4: Responsive Ellipsis

**File**: `src/main/java/org/whisperdog/recording/RecordingsPanel.java`

**Add resize listener in constructor**:
```java
// In constructor, after creating contentPanel
addComponentListener(new ComponentAdapter() {
    private Timer resizeTimer;

    @Override
    public void componentResized(ComponentEvent e) {
        // Debounce resize events
        if (resizeTimer != null && resizeTimer.isRunning()) {
            resizeTimer.restart();
        } else {
            resizeTimer = new Timer(150, evt -> {
                recalculateTruncation();
                resizeTimer.stop();
            });
            resizeTimer.setRepeats(false);
            resizeTimer.start();
        }
    }
});
```

**Calculate truncation based on width**:
```java
private int calculateMaxChars() {
    int panelWidth = contentPanel.getWidth();
    // Approximate: 7 pixels per character at 11pt font
    int availableWidth = panelWidth - 200; // Subtract button panel width + padding
    int charsPerLine = Math.max(40, availableWidth / 7);
    return charsPerLine * 2; // Allow ~2 lines
}

private void recalculateTruncation() {
    // Would need to store references to labels and full text
    // Or trigger full refresh() which rebuilds everything
    refresh();
}
```

---

## Phase 5: Inline Audio Playback

### 5.1 Audio Player Component

**New File**: `src/main/java/org/whisperdog/recording/InlineAudioPlayer.java`

```java
package org.whisperdog.recording;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class InlineAudioPlayer {
    private static final Color PROGRESS_COLOR = new Color(100, 181, 246); // Light blue

    private Clip currentClip;
    private JButton currentButton;
    private JProgressBar currentProgressBar;
    private Timer progressTimer;

    public void play(File audioFile, JButton playButton, JProgressBar progressBar) {
        // Stop any current playback
        stop();

        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            currentClip = AudioSystem.getClip();
            currentClip.open(audioStream);

            currentButton = playButton;
            currentProgressBar = progressBar;

            // Update button to Stop
            playButton.setText("Stop");
            playButton.setIcon(new FlatSVGIcon("icon/svg/stop.svg", 14, 14));

            // Show and configure progress bar
            progressBar.setVisible(true);
            progressBar.setMaximum((int) (currentClip.getMicrosecondLength() / 1000));
            progressBar.setValue(0);

            // Start progress timer (update every 100ms)
            progressTimer = new Timer(100, e -> {
                if (currentClip != null && currentClip.isRunning()) {
                    int position = (int) (currentClip.getMicrosecondPosition() / 1000);
                    progressBar.setValue(position);
                }
            });
            progressTimer.start();

            // Handle playback completion
            currentClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    SwingUtilities.invokeLater(this::resetPlaybackState);
                }
            });

            currentClip.start();

        } catch (Exception e) {
            logger.error("Failed to play audio", e);
            resetPlaybackState();
        }
    }

    public void stop() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
        }
        resetPlaybackState();
    }

    private void resetPlaybackState() {
        if (progressTimer != null) {
            progressTimer.stop();
            progressTimer = null;
        }

        if (currentClip != null) {
            currentClip.close();
            currentClip = null;
        }

        if (currentButton != null) {
            currentButton.setText("Play");
            currentButton.setIcon(new FlatSVGIcon("icon/svg/play.svg", 14, 14));
            currentButton = null;
        }

        if (currentProgressBar != null) {
            currentProgressBar.setVisible(false);
            currentProgressBar.setValue(0);
            currentProgressBar = null;
        }
    }

    public boolean isPlaying() {
        return currentClip != null && currentClip.isRunning();
    }
}
```

### 5.2 Progress Bar Styling

**In RecordingsPanel.createRecordingCard()**:
```java
// Create thin progress bar below card content
JProgressBar progressBar = new JProgressBar();
progressBar.setPreferredSize(new Dimension(0, 3));
progressBar.setVisible(false);
progressBar.setBorderPainted(false);
progressBar.setBackground(UIManager.getColor("Panel.background"));
progressBar.setForeground(new Color(100, 181, 246)); // Light blue

card.add(progressBar, BorderLayout.SOUTH);
```

### 5.3 Integration with Play Button

```java
// Field in RecordingsPanel
private final InlineAudioPlayer audioPlayer = new InlineAudioPlayer();

// In createRecordingCard():
playButton.addActionListener(e -> {
    if (audioPlayer.isPlaying() && /* check if this entry is playing */) {
        audioPlayer.stop();
    } else {
        File audioFile = retentionManager.getAudioFile(entry);
        if (audioFile != null && audioFile.exists()) {
            audioPlayer.play(audioFile, playButton, progressBar);
        } else {
            Notificationmanager.getInstance().showNotification(
                ToastNotification.Type.ERROR, "Recording file not found");
        }
    }
});
```

---

## File Summary

| File | Changes |
|------|---------|
| `MenuItem.java` | Change icon loading to name-based mapping |
| `RecordingsPanel.java` | Padding, button styling, expandable text, resize listener, audio player |
| `RecordingManifest.java` | Add `scanAndReconcile()` method |
| `RecordingRetentionManager.java` | Call reconcile on reload |
| `InlineAudioPlayer.java` | New file for audio playback |

---

## Testing Checklist

- [ ] Menu icons match their menu items after recompilation
- [ ] Content area has visible padding, no overlap
- [ ] Buttons change appearance on hover
- [ ] Cursor changes to hand over buttons
- [ ] Long transcriptions can be expanded/collapsed
- [ ] Panel resize adjusts text truncation
- [ ] Audio plays inline with progress bar
- [ ] Stop button appears during playback
- [ ] Playback completes and resets UI state
- [ ] Multiple recordings don't play simultaneously
- [ ] Refresh syncs manifest with filesystem
- [ ] Orphaned files appear after refresh
- [ ] Missing file entries are removed after refresh
