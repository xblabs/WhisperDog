package org.whisperdog.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test simultaneous capture of microphone and system audio.
 * Verifies there are no conflicts between the two capture methods.
 */
public class DualCaptureTest {

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE = 16;
    private static final int CHANNELS = 1;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Dual Capture Test ===\n");

        // Check system audio availability
        if (!SystemAudioCapture.isAvailable()) {
            System.out.println("ERROR: WASAPI loopback not available.");
            return;
        }
        System.out.println("OK: WASAPI loopback available.\n");

        // Find microphone
        Mixer.Info micMixer = findMicrophone();
        if (micMixer == null) {
            System.out.println("ERROR: No microphone found.");
            return;
        }
        System.out.println("OK: Microphone found: " + micMixer.getName() + "\n");

        // Initialize system audio capture
        SystemAudioCapture systemCapture = new SystemAudioCapture();
        if (!systemCapture.initialize()) {
            System.out.println("ERROR: Failed to initialize system audio capture.");
            return;
        }

        // Prepare microphone capture
        AudioFormat micFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, true, false);
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, micFormat);
        Mixer mic = AudioSystem.getMixer(micMixer);

        if (!mic.isLineSupported(micInfo)) {
            System.out.println("ERROR: Microphone doesn't support required format.");
            systemCapture.dispose();
            return;
        }

        TargetDataLine micLine = (TargetDataLine) mic.getLine(micInfo);
        ByteArrayOutputStream micAudio = new ByteArrayOutputStream();

        System.out.println("Starting 5-second dual capture...");
        System.out.println("  - Speak into microphone AND");
        System.out.println("  - Play some audio on your computer\n");

        // Start both captures
        micLine.open(micFormat);
        micLine.start();
        systemCapture.start();

        // Capture for 5 seconds
        Thread micThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            long endTime = System.currentTimeMillis() + 5000;
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

        for (int i = 5; i > 0; i--) {
            System.out.println("  Capturing... " + i + "s remaining");
            Thread.sleep(1000);
        }

        // Stop both captures
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
        System.out.println("  Microphone: " + micData.length + " bytes (" +
            String.format("%.2f", micData.length / 2.0 / SAMPLE_RATE) + " sec)");
        System.out.println("  System:     " + systemData.length + " bytes (" +
            String.format("%.2f", systemData.length / 2.0 / SAMPLE_RATE) + " sec)");

        // Check both captured something
        boolean micOk = micData.length > 0;
        boolean sysOk = systemData.length > 0;

        System.out.println("\nResults:");
        System.out.println("  Microphone capture: " + (micOk ? "SUCCESS" : "FAILED"));
        System.out.println("  System capture:     " + (sysOk ? "SUCCESS" : "FAILED"));
        System.out.println("  Simultaneous:       " + ((micOk && sysOk) ? "SUCCESS" : "FAILED"));

        // Save files for verification
        if (micOk) {
            Path micFile = Files.createTempFile("dual_mic_", ".wav");
            writeWav(micFile.toFile(), micData);
            System.out.println("\n  Mic saved: " + micFile);
        }
        if (sysOk) {
            Path sysFile = Files.createTempFile("dual_sys_", ".wav");
            writeWav(sysFile.toFile(), systemData);
            System.out.println("  Sys saved: " + sysFile);
        }
    }

    private static Mixer.Info findMicrophone() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                    // Skip loopback devices
                    String name = mixerInfo.getName().toLowerCase();
                    if (!name.contains("loopback") && !name.contains("output")) {
                        return mixerInfo;
                    }
                }
            } catch (Exception e) {
                // Skip problematic mixers
            }
        }
        return null;
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
