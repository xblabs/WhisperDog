package org.whisperdog.ui;

import org.whisperdog.audio.AudioAnalyzer;
import org.whisperdog.ConfigManager;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Panel for testing microphone and calibrating silence detection thresholds.
 * Provides real-time RMS visualization, A/B playback comparison, and live threshold adjustment.
 */
public class MicTestPanel extends JPanel {

    private final ConfigManager configManager;
    private final AudioAnalyzer analyzer = new AudioAnalyzer();

    // UI Components
    private JButton recordButton;
    private JButton stopButton;
    private JLabel durationLabel;
    private JProgressBar rmsBar;
    private JProgressBar peakBar;
    private JLabel silenceRatioLabel;
    private JLabel nonSilenceLabel;
    private JButton playOriginalButton;
    private JButton playFilteredButton;
    private JSlider silenceThresholdSlider;
    private JSlider minDurationSlider;
    private JLabel thresholdValueLabel;
    private JLabel durationValueLabel;

    // Audio state
    private byte[] recordedAudio;
    private byte[] filteredAudio;
    private TargetDataLine microphone;
    private Clip playbackClip;
    private volatile boolean recording = false;
    private static final float SAMPLE_RATE = 16000f;
    private static final int MAX_RECORDING_SECONDS = 10;

    public MicTestPanel(ConfigManager configManager) {
        this.configManager = configManager;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        // Recording controls
        recordButton = new JButton("Record Test");
        recordButton.addActionListener(e -> startRecording());

        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopRecording());

        durationLabel = new JLabel("Duration: 0:00");

        // Metrics bars
        rmsBar = new JProgressBar(0, 100);
        rmsBar.setStringPainted(true);
        rmsBar.setString("--");

        peakBar = new JProgressBar(0, 100);
        peakBar.setStringPainted(true);
        peakBar.setString("--");

        // Metrics labels
        silenceRatioLabel = new JLabel("Silence: --%");
        nonSilenceLabel = new JLabel("Non-silence: --s");

        // Playback buttons
        playOriginalButton = new JButton("Play Original");
        playOriginalButton.setEnabled(false);
        playOriginalButton.addActionListener(e -> playAudio(recordedAudio));

        playFilteredButton = new JButton("Play Filtered");
        playFilteredButton.setEnabled(false);
        playFilteredButton.addActionListener(e -> playAudio(filteredAudio));

        // Threshold slider using dB scale (-60dB to -20dB)
        // dB provides logarithmic control which matches human perception
        // -60dB = 0.001 RMS, -40dB = 0.01 RMS, -20dB = 0.1 RMS
        int initialDb = rmsToDb(configManager.getSilenceThreshold());
        silenceThresholdSlider = new JSlider(-60, -20, Math.max(-60, Math.min(-20, initialDb)));
        silenceThresholdSlider.setMajorTickSpacing(10);
        silenceThresholdSlider.setMinorTickSpacing(5);
        silenceThresholdSlider.setPaintTicks(true);
        silenceThresholdSlider.setPaintLabels(true);
        silenceThresholdSlider.addChangeListener(e -> {
            updateThresholdLabel();
            if (!silenceThresholdSlider.getValueIsAdjusting()) {
                reprocessAudio();
            }
        });
        thresholdValueLabel = new JLabel();

        // Min duration slider (500ms to 5000ms)
        int initialDuration = configManager.getMinSilenceDuration();
        minDurationSlider = new JSlider(500, 5000, Math.max(500, Math.min(5000, initialDuration)));
        minDurationSlider.setMajorTickSpacing(1000);
        minDurationSlider.setMinorTickSpacing(500);
        minDurationSlider.setPaintTicks(true);
        minDurationSlider.addChangeListener(e -> {
            updateDurationLabel();
            if (!minDurationSlider.getValueIsAdjusting()) {
                reprocessAudio();
            }
        });
        durationValueLabel = new JLabel();

        updateThresholdLabel();
        updateDurationLabel();
    }

    private void layoutComponents() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Recording controls panel
        JPanel recordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        recordPanel.add(recordButton);
        recordPanel.add(stopButton);
        recordPanel.add(Box.createHorizontalStrut(16));
        recordPanel.add(durationLabel);
        content.add(recordPanel);

        content.add(Box.createVerticalStrut(8));
        content.add(new JSeparator());
        content.add(Box.createVerticalStrut(8));

        // Metrics panel
        JPanel metricsPanel = new JPanel(new GridBagLayout());
        metricsPanel.setBorder(BorderFactory.createTitledBorder("Audio Metrics"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        metricsPanel.add(new JLabel("RMS Level:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        metricsPanel.add(rmsBar, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        metricsPanel.add(new JLabel("Peak Level:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        metricsPanel.add(peakBar, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        metricsPanel.add(silenceRatioLabel, gbc);
        gbc.gridx = 1;
        metricsPanel.add(nonSilenceLabel, gbc);

        content.add(metricsPanel);

        content.add(Box.createVerticalStrut(8));
        content.add(new JSeparator());
        content.add(Box.createVerticalStrut(8));

        // Playback panel
        JPanel playbackPanel = new JPanel(new GridBagLayout());
        playbackPanel.setBorder(BorderFactory.createTitledBorder("A/B Comparison"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.5;
        playbackPanel.add(playOriginalButton, gbc);
        gbc.gridx = 1;
        playbackPanel.add(playFilteredButton, gbc);

        content.add(playbackPanel);

        content.add(Box.createVerticalStrut(8));
        content.add(new JSeparator());
        content.add(Box.createVerticalStrut(8));

        // Threshold settings panel
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Threshold Settings"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        settingsPanel.add(new JLabel("Silence RMS:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        settingsPanel.add(silenceThresholdSlider, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        settingsPanel.add(thresholdValueLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        settingsPanel.add(new JLabel("Min Silence:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        settingsPanel.add(minDurationSlider, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        settingsPanel.add(durationValueLabel, gbc);

        // Hint
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 3;
        JLabel hint = new JLabel("<html><i>Adjust sliders to see filtered audio change without re-recording</i></html>");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 10f));
        hint.setForeground(Color.GRAY);
        settingsPanel.add(hint, gbc);

        content.add(settingsPanel);

        add(content, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> applySettings());
        JButton resetButton = new JButton("Reset Defaults");
        resetButton.addActionListener(e -> resetDefaults());
        buttonPanel.add(resetButton);
        buttonPanel.add(applyButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void startRecording() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                JOptionPane.showMessageDialog(this,
                    "Microphone format not supported",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Use configured microphone if available
            String selectedMic = configManager.getProperty("selectedMicrophone");
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            TargetDataLine line = null;

            if (selectedMic != null && !selectedMic.isEmpty()) {
                for (Mixer.Info mixerInfo : mixers) {
                    if (selectedMic.startsWith(mixerInfo.getName())) {
                        Mixer mixer = AudioSystem.getMixer(mixerInfo);
                        if (mixer.isLineSupported(info)) {
                            line = (TargetDataLine) mixer.getLine(info);
                            break;
                        }
                    }
                }
            }

            if (line == null) {
                line = (TargetDataLine) AudioSystem.getLine(info);
            }

            microphone = line;
            microphone.open(format);
            microphone.start();

            recording = true;
            recordButton.setEnabled(false);
            stopButton.setEnabled(true);
            playOriginalButton.setEnabled(false);
            playFilteredButton.setEnabled(false);

            // Reset metrics display
            rmsBar.setValue(0);
            rmsBar.setString("--");
            peakBar.setValue(0);
            peakBar.setString("--");
            silenceRatioLabel.setText("Silence: --%");
            nonSilenceLabel.setText("Non-silence: --s");

            new Thread(this::recordLoop).start();

        } catch (LineUnavailableException e) {
            JOptionPane.showMessageDialog(this,
                "Could not access microphone: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recordLoop() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        long startTime = System.currentTimeMillis();
        long maxDurationMs = MAX_RECORDING_SECONDS * 1000L;

        while (recording && (System.currentTimeMillis() - startTime) < maxDurationMs) {
            int read = microphone.read(chunk, 0, chunk.length);
            if (read > 0) {
                buffer.write(chunk, 0, read);

                // Calculate real-time RMS for display
                byte[] chunkCopy = new byte[read];
                System.arraycopy(chunk, 0, chunkCopy, 0, read);
                double rms = analyzer.calculateRMS(chunkCopy);
                final long elapsed = System.currentTimeMillis() - startTime;

                SwingUtilities.invokeLater(() -> {
                    int rmsPercent = Math.min(100, (int) (rms * 100));
                    rmsBar.setValue(rmsPercent);
                    rmsBar.setString(String.format("%.3f (%ddB)", rms, rmsToDb(rms)));
                    durationLabel.setText(String.format("Duration: %d:%02d",
                        elapsed / 60000, (elapsed / 1000) % 60));
                });
            }
        }

        final byte[] audio = buffer.toByteArray();
        SwingUtilities.invokeLater(() -> {
            stopRecordingInternal();
            recordedAudio = audio;
            processRecording();
        });
    }

    private void stopRecording() {
        recording = false;
    }

    private void stopRecordingInternal() {
        recording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }
        recordButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void processRecording() {
        if (recordedAudio == null || recordedAudio.length == 0) {
            return;
        }

        // Convert slider dB value to linear RMS
        double threshold = dbToRms(silenceThresholdSlider.getValue());

        // Calculate overall metrics
        double rms = analyzer.calculateRMS(recordedAudio);
        double peak = analyzer.calculatePeak(recordedAudio);

        // Remove silence (respecting minimum silence duration from slider)
        int minSilenceMs = minDurationSlider.getValue();
        AudioAnalyzer.SilenceRemovalResult result = analyzer.removeSilence(
            recordedAudio, SAMPLE_RATE, threshold, minSilenceMs);
        filteredAudio = result.audioData;

        // Update UI with both linear and dB values
        int rmsPercent = Math.min(100, (int) (rms * 100));
        rmsBar.setValue(rmsPercent);
        rmsBar.setString(String.format("%.3f (%ddB)", rms, rmsToDb(rms)));

        int peakPercent = Math.min(100, (int) (peak * 100));
        peakBar.setValue(peakPercent);
        peakBar.setString(String.format("%.3f (%ddB)", peak, rmsToDb(peak)));

        silenceRatioLabel.setText(String.format("Silence: %.0f%%", result.silenceRatio * 100));
        nonSilenceLabel.setText(String.format("Non-silence: %.1fs", result.nonSilenceDuration));

        playOriginalButton.setEnabled(true);
        playFilteredButton.setEnabled(filteredAudio != null && filteredAudio.length > 0);
    }

    private void reprocessAudio() {
        if (recordedAudio != null && recordedAudio.length > 0) {
            processRecording();
        }
    }

    private void playAudio(byte[] audio) {
        if (audio == null || audio.length == 0) {
            return;
        }

        // Stop any current playback
        if (playbackClip != null && playbackClip.isRunning()) {
            playbackClip.stop();
            playbackClip.close();
        }

        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(audio),
                format,
                audio.length / format.getFrameSize()
            );
            playbackClip = AudioSystem.getClip();
            playbackClip.open(ais);
            playbackClip.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Playback error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateThresholdLabel() {
        int db = silenceThresholdSlider.getValue();
        double rms = dbToRms(db);
        thresholdValueLabel.setText(String.format("%ddB (%.3f)", db, rms));
    }

    private void updateDurationLabel() {
        durationValueLabel.setText(minDurationSlider.getValue() + "ms");
    }

    private void applySettings() {
        // Convert dB slider value to linear RMS for storage
        float threshold = (float) dbToRms(silenceThresholdSlider.getValue());
        int minDuration = minDurationSlider.getValue();

        configManager.setSilenceThreshold(threshold);
        configManager.setMinSilenceDuration(minDuration);
        configManager.saveConfig();

        JOptionPane.showMessageDialog(this,
            String.format("Settings applied!\nSilence threshold: %ddB (%.4f RMS)",
                silenceThresholdSlider.getValue(), threshold),
            "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetDefaults() {
        silenceThresholdSlider.setValue(-46); // ~0.005 RMS (-46dB)
        minDurationSlider.setValue(2000);     // 2000ms
        updateThresholdLabel();
        updateDurationLabel();
        reprocessAudio();
    }

    // ========== dB / RMS Conversion Utilities ==========

    /**
     * Convert linear RMS value to decibels.
     * dB = 20 * log10(rms)
     */
    private static int rmsToDb(double rms) {
        if (rms <= 0) return -60; // Floor at -60dB
        int db = (int) Math.round(20 * Math.log10(rms));
        return Math.max(-60, Math.min(0, db)); // Clamp to -60..0
    }

    /**
     * Convert decibels to linear RMS value.
     * rms = 10^(dB/20)
     */
    private static double dbToRms(int db) {
        return Math.pow(10, db / 20.0);
    }

    /**
     * Cleanup resources when panel is closed.
     */
    public void cleanup() {
        recording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        if (playbackClip != null) {
            playbackClip.stop();
            playbackClip.close();
        }
    }
}
