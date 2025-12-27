# Implementation Plan: Mic Test Screen

## Phase 1: Audio Analysis Utilities

### 1.1 Create Audio Analyzer

**File**: `src/main/java/org/whisperdog/audio/AudioAnalyzer.java` (new)

```java
package org.whisperdog.audio;

public class AudioAnalyzer {

    public double calculateRMS(byte[] audioData) {
        if (audioData == null || audioData.length < 2) return 0;

        long sum = 0;
        int sampleCount = audioData.length / 2;

        for (int i = 0; i < audioData.length - 1; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xFF);
            sum += (long) sample * sample;
        }

        double rms = Math.sqrt((double) sum / sampleCount);
        return rms / 32768.0;
    }

    public double calculatePeak(byte[] audioData) {
        if (audioData == null || audioData.length < 2) return 0;

        int maxSample = 0;
        for (int i = 0; i < audioData.length - 1; i += 2) {
            int sample = Math.abs((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            if (sample > maxSample) maxSample = sample;
        }

        return maxSample / 32768.0;
    }
}
```

### 1.2 Create Silence Remover

**File**: `src/main/java/org/whisperdog/audio/SilenceRemover.java` (new)

```java
package org.whisperdog.audio;

import java.io.ByteArrayOutputStream;

public class SilenceRemover {

    private final double silenceThreshold;
    private final double minSilenceDuration;

    public SilenceRemover(double silenceThreshold, double minSilenceDuration) {
        this.silenceThreshold = silenceThreshold;
        this.minSilenceDuration = minSilenceDuration;
    }

    public Result removeSilence(byte[] audioData, float sampleRate) {
        AudioAnalyzer analyzer = new AudioAnalyzer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int samplesPerWindow = (int) (sampleRate * 0.02); // 20ms windows
        int bytesPerWindow = samplesPerWindow * 2;
        int windowCount = audioData.length / bytesPerWindow;

        int silentWindows = 0;

        for (int w = 0; w < windowCount; w++) {
            byte[] window = new byte[bytesPerWindow];
            System.arraycopy(audioData, w * bytesPerWindow, window, 0, bytesPerWindow);

            double windowRMS = analyzer.calculateRMS(window);

            if (windowRMS >= silenceThreshold) {
                output.write(window, 0, window.length);
            } else {
                silentWindows++;
            }
        }

        double totalDuration = audioData.length / 2.0 / sampleRate;
        double silenceRatio = (double) silentWindows / windowCount;
        double nonSilenceDuration = totalDuration * (1 - silenceRatio);

        return new Result(output.toByteArray(), silenceRatio, nonSilenceDuration);
    }

    public static class Result {
        public final byte[] audioData;
        public final double silenceRatio;
        public final double nonSilenceDuration;

        public Result(byte[] audioData, double silenceRatio, double nonSilenceDuration) {
            this.audioData = audioData;
            this.silenceRatio = silenceRatio;
            this.nonSilenceDuration = nonSilenceDuration;
        }
    }
}
```

## Phase 2: Mic Test Panel UI

### 2.1 Create MicTestPanel

**File**: `src/main/java/org/whisperdog/ui/MicTestPanel.java` (new)

```java
package org.whisperdog.ui;

import org.whisperdog.audio.AudioAnalyzer;
import org.whisperdog.audio.SilenceRemover;
import org.whisperdog.ConfigManager;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;

public class MicTestPanel extends JPanel {

    private final ConfigManager configManager;
    private final AudioAnalyzer analyzer = new AudioAnalyzer();

    private JButton recordButton, stopButton;
    private JLabel durationLabel;
    private JProgressBar rmsBar, peakBar;
    private JLabel silenceRatioLabel, nonSilenceLabel;
    private JButton playOriginalButton, playFilteredButton;
    private JSlider silenceThresholdSlider, minDurationSlider;
    private JLabel thresholdValueLabel, durationValueLabel;

    private byte[] recordedAudio;
    private byte[] filteredAudio;
    private TargetDataLine microphone;
    private Clip playbackClip;
    private volatile boolean recording = false;
    private static final float SAMPLE_RATE = 16000f;

    public MicTestPanel(ConfigManager configManager) {
        this.configManager = configManager;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        recordButton = new JButton("● Record Test");
        recordButton.addActionListener(e -> startRecording());

        stopButton = new JButton("■ Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopRecording());

        durationLabel = new JLabel("Duration: 0:00");

        rmsBar = new JProgressBar(0, 100);
        rmsBar.setStringPainted(true);
        rmsBar.setString("--");

        peakBar = new JProgressBar(0, 100);
        peakBar.setStringPainted(true);
        peakBar.setString("--");

        silenceRatioLabel = new JLabel("Silence: --%");
        nonSilenceLabel = new JLabel("Non-silence: --s");

        playOriginalButton = new JButton("▶ Play Original");
        playOriginalButton.setEnabled(false);
        playOriginalButton.addActionListener(e -> playAudio(recordedAudio));

        playFilteredButton = new JButton("▶ Play Filtered");
        playFilteredButton.setEnabled(false);
        playFilteredButton.addActionListener(e -> playAudio(filteredAudio));

        int initialThreshold = (int) (configManager.getDouble("audio.silence_threshold", 0.05) * 1000);
        silenceThresholdSlider = new JSlider(10, 200, initialThreshold);
        silenceThresholdSlider.addChangeListener(e -> {
            updateThresholdLabel();
            if (!silenceThresholdSlider.getValueIsAdjusting()) reprocessAudio();
        });
        thresholdValueLabel = new JLabel();

        int initialDuration = (int) (configManager.getDouble("audio.min_silence_duration", 0.3) * 10);
        minDurationSlider = new JSlider(1, 10, initialDuration);
        minDurationSlider.addChangeListener(e -> {
            updateDurationLabel();
            if (!minDurationSlider.getValueIsAdjusting()) reprocessAudio();
        });
        durationValueLabel = new JLabel();

        updateThresholdLabel();
        updateDurationLabel();
    }

    private void layoutComponents() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Recording controls
        JPanel recordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        recordPanel.add(recordButton);
        recordPanel.add(stopButton);
        recordPanel.add(Box.createHorizontalStrut(16));
        recordPanel.add(durationLabel);
        content.add(recordPanel);

        content.add(new JSeparator());

        // Metrics
        JPanel metricsPanel = new JPanel(new GridBagLayout());
        metricsPanel.setBorder(BorderFactory.createTitledBorder("Metrics"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        metricsPanel.add(new JLabel("RMS Level:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        metricsPanel.add(rmsBar, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        metricsPanel.add(new JLabel("Peak Level:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        metricsPanel.add(peakBar, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        metricsPanel.add(silenceRatioLabel, gbc);
        gbc.gridx = 1;
        metricsPanel.add(nonSilenceLabel, gbc);

        content.add(metricsPanel);
        content.add(new JSeparator());

        // Playback
        JPanel playbackPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        playbackPanel.setBorder(BorderFactory.createTitledBorder("Playback"));
        playbackPanel.add(new JLabel("Original:"));
        playbackPanel.add(playOriginalButton);
        playbackPanel.add(new JLabel("Filtered:"));
        playbackPanel.add(playFilteredButton);
        content.add(playbackPanel);

        content.add(new JSeparator());

        // Threshold settings
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Threshold Settings"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        settingsPanel.add(new JLabel("Silence RMS:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        settingsPanel.add(silenceThresholdSlider, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        settingsPanel.add(thresholdValueLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        settingsPanel.add(new JLabel("Min Duration:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        settingsPanel.add(minDurationSlider, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        settingsPanel.add(durationValueLabel, gbc);

        content.add(settingsPanel);
        add(content, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> applySettings());
        JButton resetButton = new JButton("Reset Defaults");
        resetButton.addActionListener(e -> resetDefaults());
        buttonPanel.add(applyButton);
        buttonPanel.add(resetButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void startRecording() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                JOptionPane.showMessageDialog(this, "Microphone not supported", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            recording = true;
            recordButton.setEnabled(false);
            stopButton.setEnabled(true);
            playOriginalButton.setEnabled(false);
            playFilteredButton.setEnabled(false);

            new Thread(this::recordLoop).start();
        } catch (LineUnavailableException e) {
            JOptionPane.showMessageDialog(this, "Could not access microphone: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recordLoop() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        long startTime = System.currentTimeMillis();

        while (recording && (System.currentTimeMillis() - startTime) < 10000) {
            int read = microphone.read(chunk, 0, chunk.length);
            if (read > 0) {
                buffer.write(chunk, 0, read);
                double rms = analyzer.calculateRMS(chunk);
                final long elapsed = System.currentTimeMillis() - startTime;

                SwingUtilities.invokeLater(() -> {
                    rmsBar.setValue((int) (rms * 100));
                    rmsBar.setString(String.format("%.3f", rms));
                    durationLabel.setText(String.format("Duration: %d:%02d", elapsed / 60000, (elapsed / 1000) % 60));
                });
            }
        }

        final byte[] audio = buffer.toByteArray();
        SwingUtilities.invokeLater(() -> {
            stopRecording();
            recordedAudio = audio;
            processRecording();
        });
    }

    private void stopRecording() {
        recording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        recordButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void processRecording() {
        if (recordedAudio == null || recordedAudio.length == 0) return;

        double threshold = silenceThresholdSlider.getValue() / 1000.0;
        double minDuration = minDurationSlider.getValue() / 10.0;

        double rms = analyzer.calculateRMS(recordedAudio);
        double peak = analyzer.calculatePeak(recordedAudio);

        SilenceRemover remover = new SilenceRemover(threshold, minDuration);
        SilenceRemover.Result result = remover.removeSilence(recordedAudio, SAMPLE_RATE);
        filteredAudio = result.audioData;

        rmsBar.setValue((int) (rms * 100));
        rmsBar.setString(String.format("%.3f", rms));
        peakBar.setValue((int) (peak * 100));
        peakBar.setString(String.format("%.3f", peak));
        silenceRatioLabel.setText(String.format("Silence: %.0f%%", result.silenceRatio * 100));
        nonSilenceLabel.setText(String.format("Non-silence: %.1fs", result.nonSilenceDuration));

        playOriginalButton.setEnabled(true);
        playFilteredButton.setEnabled(filteredAudio.length > 0);
    }

    private void reprocessAudio() {
        if (recordedAudio != null) processRecording();
    }

    private void playAudio(byte[] audio) {
        if (audio == null || audio.length == 0) return;
        if (playbackClip != null && playbackClip.isRunning()) {
            playbackClip.stop();
            playbackClip.close();
        }
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            AudioInputStream ais = new AudioInputStream(new java.io.ByteArrayInputStream(audio), format, audio.length / format.getFrameSize());
            playbackClip = AudioSystem.getClip();
            playbackClip.open(ais);
            playbackClip.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Playback error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateThresholdLabel() {
        thresholdValueLabel.setText(String.format("%.3f", silenceThresholdSlider.getValue() / 1000.0));
    }

    private void updateDurationLabel() {
        durationValueLabel.setText(String.format("%.1fs", minDurationSlider.getValue() / 10.0));
    }

    private void applySettings() {
        configManager.setDouble("audio.silence_threshold", silenceThresholdSlider.getValue() / 1000.0);
        configManager.setDouble("audio.min_silence_duration", minDurationSlider.getValue() / 10.0);
        JOptionPane.showMessageDialog(this, "Settings applied!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetDefaults() {
        silenceThresholdSlider.setValue(50);
        minDurationSlider.setValue(3);
        updateThresholdLabel();
        updateDurationLabel();
        reprocessAudio();
    }
}
```

## Phase 3: Integration

### 3.1 Add to Settings Form

**File**: `src/main/java/org/whisperdog/settings/SettingsForm.java` (modify)

```java
// In the Audio settings tab:
JButton micTestButton = new JButton("Test Microphone...");
micTestButton.addActionListener(e -> {
    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Microphone Test", Dialog.ModalityType.APPLICATION_MODAL);
    dialog.setContentPane(new MicTestPanel(configManager));
    dialog.pack();
    dialog.setMinimumSize(new Dimension(400, 500));
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
});
audioPanel.add(micTestButton);
```

## File Summary

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/org/whisperdog/audio/AudioAnalyzer.java` | Create | RMS/peak calculation |
| `src/main/java/org/whisperdog/audio/SilenceRemover.java` | Create | Silence removal with metrics |
| `src/main/java/org/whisperdog/ui/MicTestPanel.java` | Create | Main mic test UI |
| `src/main/java/org/whisperdog/settings/SettingsForm.java` | Modify | Add "Test Microphone" button |

## Testing Checklist

- [ ] Record button starts recording, RMS meter updates in real-time
- [ ] Recording auto-stops at 10 seconds
- [ ] Stop button ends recording early
- [ ] Metrics display correctly after recording
- [ ] Original and filtered audio play back correctly
- [ ] Changing threshold slider re-processes without re-recording
- [ ] Apply saves settings to config
- [ ] Reset restores default values
