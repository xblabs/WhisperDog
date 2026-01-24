package org.whisperdog.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Test the SourceActivityTracker with real dual-captured audio.
 * Run this to verify activity detection works correctly.
 *
 * Test procedure:
 * 1. Stay silent for first 2 seconds
 * 2. Speak into mic for 2 seconds (while no system audio)
 * 3. Play system audio for 2 seconds (while staying silent)
 * 4. Both: speak AND play system audio for 2 seconds
 * 5. Stay silent for final 2 seconds
 */
public class SourceActivityTrackerTest {

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE = 16;
    private static final int CHANNELS = 1;
    private static final int TEST_DURATION_SEC = 10;

    public static void main(String[] args) throws Exception {
        System.out.println("=== SourceActivityTracker Test ===\n");

        // Check availability
        if (!SystemAudioCapture.isAvailable()) {
            System.out.println("ERROR: WASAPI loopback not available.");
            return;
        }

        Mixer.Info micMixer = findMicrophone();
        if (micMixer == null) {
            System.out.println("ERROR: No microphone found.");
            return;
        }

        System.out.println("Test procedure (" + TEST_DURATION_SEC + " seconds total):");
        System.out.println("  0-2s:  Stay SILENT (no mic, no system audio)");
        System.out.println("  2-4s:  SPEAK into mic only");
        System.out.println("  4-6s:  Play SYSTEM AUDIO only (stay silent)");
        System.out.println("  6-8s:  BOTH: speak AND play system audio");
        System.out.println("  8-10s: Stay SILENT again");
        System.out.println("\nPress Enter to start...");
        System.in.read();

        // Initialize captures
        SystemAudioCapture systemCapture = new SystemAudioCapture();
        systemCapture.initialize();

        AudioFormat micFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, true, false);
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, micFormat);
        Mixer mic = AudioSystem.getMixer(micMixer);
        TargetDataLine micLine = (TargetDataLine) mic.getLine(micInfo);
        ByteArrayOutputStream micAudio = new ByteArrayOutputStream();

        // Start captures
        micLine.open(micFormat);
        micLine.start();
        systemCapture.start();

        // Mic capture thread
        Thread micThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            long endTime = System.currentTimeMillis() + (TEST_DURATION_SEC * 1000);
            while (System.currentTimeMillis() < endTime) {
                int read = micLine.read(buffer, 0, buffer.length);
                if (read > 0) {
                    synchronized (micAudio) {
                        micAudio.write(buffer, 0, read);
                    }
                }
            }
        });
        micThread.start();

        // Countdown with prompts
        String[] prompts = {
            "SILENCE...",
            "SILENCE...",
            ">>> SPEAK NOW <<<",
            ">>> SPEAK NOW <<<",
            ">>> PLAY SYSTEM AUDIO <<<",
            ">>> PLAY SYSTEM AUDIO <<<",
            ">>> SPEAK + SYSTEM AUDIO <<<",
            ">>> SPEAK + SYSTEM AUDIO <<<",
            "SILENCE...",
            "SILENCE..."
        };

        for (int i = 0; i < TEST_DURATION_SEC; i++) {
            System.out.println("  [" + i + "-" + (i+1) + "s] " + prompts[i]);
            Thread.sleep(1000);
        }

        // Stop captures
        micLine.stop();
        micLine.close();
        micThread.join();

        byte[] systemData = systemCapture.stop();
        byte[] micData;
        synchronized (micAudio) {
            micData = micAudio.toByteArray();
        }
        systemCapture.dispose();

        System.out.println("\nCapture complete!");
        System.out.println("  Mic: " + micData.length + " bytes");
        System.out.println("  Sys: " + systemData.length + " bytes");
        System.out.println("  Sys capture: " + systemCapture.getDiagnostics());

        // Save to temp files
        Path micFile = Files.createTempFile("tracker_test_mic_", ".wav");
        Path sysFile = Files.createTempFile("tracker_test_sys_", ".wav");
        writeWav(micFile.toFile(), micData);
        writeWav(sysFile.toFile(), systemData);

        System.out.println("\nSaved:");
        System.out.println("  " + micFile);
        System.out.println("  " + sysFile);

        // Diagnostic: compute RMS values manually to check levels
        System.out.println("\n=== RMS Diagnostics ===\n");
        printRmsDiagnostics("Mic", micFile.toFile());
        printRmsDiagnostics("Sys", sysFile.toFile());

        // Run SourceActivityTracker
        System.out.println("\n=== Activity Analysis ===\n");

        SourceActivityTracker tracker = new SourceActivityTracker();
        List<SourceActivityTracker.ActivitySegment> timeline =
            tracker.trackActivity(micFile.toFile(), sysFile.toFile());

        System.out.println("Timeline (" + timeline.size() + " segments):\n");
        System.out.println("Expected pattern:");
        System.out.println("  0-2s: SILENCE | 2-4s: USER | 4-6s: SYSTEM | 6-8s: BOTH | 8-10s: SILENCE\n");
        System.out.println("Detected:");

        for (SourceActivityTracker.ActivitySegment seg : timeline) {
            String bar = "=".repeat((int) Math.min(50, seg.getDurationMs() / 100));
            System.out.printf("  %5d - %5d ms  %-8s %s%n",
                seg.startMs, seg.endMs, seg.source, bar);
        }

        // Summary
        System.out.println("\n=== Summary ===");
        long silenceMs = 0, userMs = 0, systemMs = 0, bothMs = 0;
        for (SourceActivityTracker.ActivitySegment seg : timeline) {
            switch (seg.source) {
                case SILENCE -> silenceMs += seg.getDurationMs();
                case USER -> userMs += seg.getDurationMs();
                case SYSTEM -> systemMs += seg.getDurationMs();
                case BOTH -> bothMs += seg.getDurationMs();
            }
        }

        System.out.printf("  SILENCE: %5d ms (expected ~4000ms)%n", silenceMs);
        System.out.printf("  USER:    %5d ms (expected ~2000ms)%n", userMs);
        System.out.printf("  SYSTEM:  %5d ms (expected ~2000ms)%n", systemMs);
        System.out.printf("  BOTH:    %5d ms (expected ~2000ms)%n", bothMs);

        // Verdict
        System.out.println("\n=== Verdict ===");
        boolean hasUser = userMs > 500;
        boolean hasSystem = systemMs > 500;
        boolean hasBoth = bothMs > 500;
        boolean hasSilence = silenceMs > 1000;

        System.out.println("  User detection:    " + (hasUser ? "PASS" : "FAIL"));
        System.out.println("  System detection:  " + (hasSystem ? "PASS" : "FAIL"));
        System.out.println("  Both detection:    " + (hasBoth ? "PASS" : "FAIL"));
        System.out.println("  Silence detection: " + (hasSilence ? "PASS" : "FAIL"));

        if (hasUser && hasSystem && hasBoth && hasSilence) {
            System.out.println("\n  *** ALL TESTS PASSED ***");
        } else {
            System.out.println("\n  Some tests failed - check your test procedure");
        }
    }

    private static Mixer.Info findMicrophone() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                    String name = mixerInfo.getName().toLowerCase();
                    if (!name.contains("loopback") && !name.contains("output")) {
                        return mixerInfo;
                    }
                }
            } catch (Exception e) {
                // Skip
            }
        }
        return null;
    }

    private static void printRmsDiagnostics(String label, File wavFile) {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile)) {
            AudioFormat format = ais.getFormat();
            System.out.printf("  %s format: %.0fHz, %dch, %dbit, bigEndian=%b%n",
                label, format.getSampleRate(), format.getChannels(),
                format.getSampleSizeInBits(), format.isBigEndian());
            System.out.printf("  %s frameLength: %d, frameSize: %d%n",
                label, ais.getFrameLength(), format.getFrameSize());

            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int samplesPerInterval = ((int) format.getSampleRate() * 100) / 1000;
            int bytesPerInterval = samplesPerInterval * format.getFrameSize();
            byte[] buffer = new byte[bytesPerInterval];

            double peakRms = 0;
            int intervalsAboveThreshold = 0;
            int totalIntervals = 0;
            double threshold = SourceActivityTracker.DEFAULT_ACTIVITY_THRESHOLD;

            while (true) {
                int bytesRead = ais.read(buffer);
                if (bytesRead <= 0) break;
                totalIntervals++;

                // Calculate RMS
                int samples = bytesRead / (bytesPerSample * format.getChannels());
                double sumSq = 0;
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(buffer, 0, bytesRead);
                bb.order(format.isBigEndian() ? java.nio.ByteOrder.BIG_ENDIAN : java.nio.ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < samples; i++) {
                    double sample = 0;
                    for (int ch = 0; ch < format.getChannels(); ch++) {
                        if (bytesPerSample == 2) sample += bb.getShort() / 32768.0;
                    }
                    sample /= format.getChannels();
                    sumSq += sample * sample;
                }
                double rms = Math.sqrt(sumSq / samples);
                if (rms > peakRms) peakRms = rms;
                if (rms >= threshold) intervalsAboveThreshold++;
            }

            System.out.printf("  %s peak RMS: %.6f (threshold: %.4f)%n", label, peakRms, threshold);
            System.out.printf("  %s intervals above threshold: %d/%d%n",
                label, intervalsAboveThreshold, totalIntervals);
        } catch (Exception e) {
            System.out.println("  " + label + " diagnostics failed: " + e.getMessage());
        }
    }

    private static void writeWav(File file, byte[] pcmData) throws IOException {
        int byteRate = SAMPLE_RATE * CHANNELS * 2;
        int blockAlign = CHANNELS * 2;

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeBytes("RIFF");
            out.writeInt(Integer.reverseBytes(36 + pcmData.length));
            out.writeBytes("WAVE");
            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16));
            out.writeShort(Short.reverseBytes((short) 1));
            out.writeShort(Short.reverseBytes((short) CHANNELS));
            out.writeInt(Integer.reverseBytes(SAMPLE_RATE));
            out.writeInt(Integer.reverseBytes(byteRate));
            out.writeShort(Short.reverseBytes((short) blockAlign));
            out.writeShort(Short.reverseBytes((short) 16));
            out.writeBytes("data");
            out.writeInt(Integer.reverseBytes(pcmData.length));
            out.write(pcmData);
        }
    }
}
