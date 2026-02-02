package org.whisperdog.recording;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whisperdog.ui.IconLoader;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Handles inline audio playback within the RecordingsPanel.
 * Manages single playback at a time with progress visualization.
 */
public class InlineAudioPlayer {
    private static final Logger logger = LogManager.getLogger(InlineAudioPlayer.class);
    private static final Color PROGRESS_COLOR = new Color(100, 181, 246); // Light blue
    private static final int BUTTON_ICON_SIZE = 10; // 30% smaller than 14

    private Clip currentClip;
    private JButton currentButton;
    private JProgressBar currentProgressBar;
    private Timer progressTimer;
    private String currentEntryId;

    /**
     * Plays an audio file with inline progress visualization.
     * Stops any currently playing audio first.
     *
     * @param audioFile   The audio file to play
     * @param entryId     The ID of the recording entry (for tracking)
     * @param playButton  The play button to toggle to Stop
     * @param progressBar The progress bar to update
     */
    public void play(File audioFile, String entryId, JButton playButton, JProgressBar progressBar) {
        // Stop any current playback
        stop();

        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            currentClip = AudioSystem.getClip();
            currentClip.open(audioStream);

            currentButton = playButton;
            currentProgressBar = progressBar;
            currentEntryId = entryId;

            // Update button to Stop
            playButton.setText("Stop");
            FlatSVGIcon stopIcon = IconLoader.loadButton("stop", BUTTON_ICON_SIZE);
            if (stopIcon != null) {
                playButton.setIcon(stopIcon);
            }

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
            logger.debug("Started playback: {}", audioFile.getName());

        } catch (UnsupportedAudioFileException e) {
            logger.error("Unsupported audio format: {}", audioFile.getName(), e);
            resetPlaybackState();
        } catch (LineUnavailableException e) {
            logger.error("Audio line unavailable", e);
            resetPlaybackState();
        } catch (Exception e) {
            logger.error("Failed to play audio: {}", audioFile.getName(), e);
            resetPlaybackState();
        }
    }

    /**
     * Stops the current playback.
     */
    public void stop() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
            logger.debug("Stopped playback");
        }
        resetPlaybackState();
    }

    /**
     * Resets the playback state and UI elements.
     */
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
            FlatSVGIcon playIcon = IconLoader.loadButton("play-2", BUTTON_ICON_SIZE);
            if (playIcon != null) {
                currentButton.setIcon(playIcon);
            }
            currentButton = null;
        }

        if (currentProgressBar != null) {
            currentProgressBar.setVisible(false);
            currentProgressBar.setValue(0);
            currentProgressBar = null;
        }

        currentEntryId = null;
    }

    /**
     * Checks if audio is currently playing.
     *
     * @return true if audio is playing
     */
    public boolean isPlaying() {
        return currentClip != null && currentClip.isRunning();
    }

    /**
     * Checks if the specified entry is currently playing.
     *
     * @param entryId The entry ID to check
     * @return true if this entry is currently playing
     */
    public boolean isPlayingEntry(String entryId) {
        return isPlaying() && entryId != null && entryId.equals(currentEntryId);
    }

    /**
     * Gets the progress color used for the progress bar.
     *
     * @return The progress bar foreground color
     */
    public static Color getProgressColor() {
        return PROGRESS_COLOR;
    }
}
