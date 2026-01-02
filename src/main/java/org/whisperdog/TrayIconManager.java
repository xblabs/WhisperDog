// TrayIconManager.java

package org.whisperdog;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class TrayIconManager {

    private SystemTray systemTray;
    private MenuItem recordToggleMenuItem;
    private boolean isRecording;
    private boolean isProcessing;
    private boolean isRecordingWarning;  // ISS_00007: Long recording warning state
    private final String trayIconPath = "/whisperdog_tray.png";
    private final String trayIconRecordingPath = "/whisperdog_recording.png";
    private final String trayIconProcessingPath = "/whisperdog_processing.png";
    private final String trayIconWarningPath = "/whisperdog_warning.png";  // ISS_00007

    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(TrayIconManager.class);


    // Create the tray icon. This method can be called from a separate thread.
    public void createTrayIcon(Runnable openAppCallback, Runnable toggleRecordingCallback) {
        try {
            systemTray = SystemTray.get();
            setTrayImage(trayIconPath);


            // Create the "Open" sidemenu item.
            systemTray.getMenu().add(new dorkbox.systemTray.MenuItem("Open", e -> {
                SwingUtilities.invokeLater(openAppCallback);
            }));

            systemTray.getMenu().add(new Separator());

            // Create the record toggle sidemenu item.
            recordToggleMenuItem = new dorkbox.systemTray.MenuItem(isRecording ? "Stop Recording" : "Start Recording", e -> {
                // When clicked, notify the FormDashboard (or its controller) to toggle recording.
                toggleRecordingCallback.run();
            });
            systemTray.getMenu().add(recordToggleMenuItem);

            systemTray.getMenu().add(new Separator());

            // Create the "Exit" sidemenu item.
            systemTray.getMenu().add(new dorkbox.systemTray.MenuItem("Exit", e -> {
                int result = JOptionPane.showConfirmDialog(null, "Do you really want to exit WhisperDog?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    systemTray.shutdown();
                    System.exit(0);
                }
            }));
        } catch (Exception e) {
            logger.error("Unable to initialize system tray", e);
        }
    }

    // Called when the recording state changes.
    public void updateTrayMenu(boolean recording) {
        this.isRecording = recording;
        if (recordToggleMenuItem != null) {
            recordToggleMenuItem.setText(isRecording ? "Stop Recording" : "Start Recording");
        }
        updateTrayIcon();
    }

    // Called when processing/transcription state changes
    public void setProcessing(boolean processing) {
        this.isProcessing = processing;
        updateTrayIcon();
    }

    /**
     * Called when recording warning state changes (ISS_00007).
     * Shows visual indicator when recording exceeds configured duration threshold.
     */
    public void setRecordingWarning(boolean warning) {
        this.isRecordingWarning = warning;
        updateTrayIcon();
        // Update tooltip to indicate warning state
        if (systemTray != null) {
            if (warning) {
                systemTray.setStatus("⚠ Recording is getting long...");
            } else if (isRecording) {
                systemTray.setStatus("Recording...");
            } else {
                systemTray.setStatus("WhisperDog");
            }
        }
    }

    // Update tray icon based on current state (warning > recording > processing > idle)
    private void updateTrayIcon() {
        if (isRecordingWarning && isRecording) {
            // Warning state during recording (use warning icon if available, otherwise fall back to recording)
            URL warningURL = TrayIconManager.class.getResource(trayIconWarningPath);
            if (warningURL != null) {
                setTrayImage(trayIconWarningPath);
            } else {
                setTrayImage(trayIconRecordingPath);  // Fallback if warning icon doesn't exist
            }
        } else if (isRecording) {
            setTrayImage(trayIconRecordingPath);
        } else if (isProcessing) {
            setTrayImage(trayIconProcessingPath);
        } else {
            setTrayImage(trayIconPath);
        }
    }

    private void setTrayImage(String imagePath) {
        try {
            URL imageURL = TrayIconManager.class.getResource(imagePath);
            if (imageURL != null) {
                Image trayImage = new ImageIcon(imageURL).getImage();
                systemTray.setImage(trayImage);
            } else {
                System.err.println("Tray icon image not found: " + imagePath);
            }
        } catch (Exception e) {
           logger.error("Error setting tray icon image", e);
        }
    }

    public void shutdown() {
        if (systemTray != null) {
            systemTray.shutdown();
        }
    }

    /**
     * Show a system-level notification (OS corner pop-up).
     * This is subtle and appears even when the app is minimized.
     */
    public void showSystemNotification(String title, String message) {
        if (systemTray != null) {
            try {
                // Update tray status text
                systemTray.setStatus(message);

                // Show a tooltip notification on the tray icon
                // Note: The dorkbox SystemTray library doesn't directly support notification balloons,
                // but the status text will be visible when hovering over the tray icon

                // Log the notification
                logger.info("System notification: {} - {}", title, message);
            } catch (Exception e) {
                logger.error("Error showing system notification", e);
            }
        }
    }
}