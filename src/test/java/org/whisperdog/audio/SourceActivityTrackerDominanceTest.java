package org.whisperdog.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SourceActivityTracker dominance ratio feature (Task 0008).
 * Tests the fix for false [User+System] attribution when one source dominates.
 */
class SourceActivityTrackerDominanceTest {

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_INTERVAL_MS = 100;
    private static final double ACTIVITY_THRESHOLD = 0.005;

    @TempDir
    Path tempDir;

    private SourceActivityTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new SourceActivityTracker(SAMPLE_INTERVAL_MS, ACTIVITY_THRESHOLD);
    }

    @Nested
    @DisplayName("Dominance Ratio Constants")
    class DominanceRatioConstants {

        @Test
        @DisplayName("Default dominance ratio should be 3.0")
        void defaultDominanceRatio() {
            assertEquals(3.0, SourceActivityTracker.DEFAULT_DOMINANCE_RATIO);
        }

        @Test
        @DisplayName("Min RMS for ratio should prevent division by zero")
        void minRmsForRatio() {
            assertTrue(SourceActivityTracker.MIN_RMS_FOR_RATIO > 0);
            assertEquals(0.0001, SourceActivityTracker.MIN_RMS_FOR_RATIO);
        }
    }

    @Nested
    @DisplayName("System Dominates (mic has ambient noise)")
    class SystemDominates {

        @Test
        @DisplayName("System at 0.1 RMS, mic at 0.006 RMS should be SYSTEM only")
        void systemClearlyDominates() throws Exception {
            // System: loud (0.1 RMS) - clearly audible system audio
            // Mic: ambient noise just above threshold (0.006 RMS)
            // Expected: SYSTEM only (not BOTH)

            File micFile = createConstantRmsWav("mic.wav", 0.006, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.1, 1000);

            List<SourceActivityTracker.ActivitySegment> timeline =
                tracker.trackActivity(micFile, sysFile);

            // Should have single SYSTEM segment, NOT BOTH
            assertFalse(timeline.isEmpty());
            long systemMs = sumDuration(timeline, SourceActivityTracker.Source.SYSTEM);
            long bothMs = sumDuration(timeline, SourceActivityTracker.Source.BOTH);

            assertTrue(systemMs > 800, "Expected mostly SYSTEM, got " + systemMs + "ms");
            assertEquals(0, bothMs, "Expected no BOTH segments, got " + bothMs + "ms");
        }

        @Test
        @DisplayName("System at 0.05 RMS, mic at 0.008 RMS should be SYSTEM only")
        void systemModeratelyDominates() throws Exception {
            // Ratio: 0.05 / 0.008 = 6.25 (> 3.0 threshold)
            // Should attribute to system

            File micFile = createConstantRmsWav("mic.wav", 0.008, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.05, 1000);

            List<SourceActivityTracker.ActivitySegment> timeline =
                tracker.trackActivity(micFile, sysFile);

            long systemMs = sumDuration(timeline, SourceActivityTracker.Source.SYSTEM);
            long bothMs = sumDuration(timeline, SourceActivityTracker.Source.BOTH);

            assertTrue(systemMs > 800, "Expected SYSTEM dominance");
            assertEquals(0, bothMs, "Expected no BOTH when system dominates");
        }
    }

    @Nested
    @DisplayName("User Dominates (system has low-level audio)")
    class UserDominates {

        @Test
        @DisplayName("Mic at 0.1 RMS, system at 0.006 RMS should be USER only")
        void userClearlyDominates() throws Exception {
            // User speaking loudly, system has faint background
            File micFile = createConstantRmsWav("mic.wav", 0.1, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.006, 1000);

            List<SourceActivityTracker.ActivitySegment> timeline =
                tracker.trackActivity(micFile, sysFile);

            long userMs = sumDuration(timeline, SourceActivityTracker.Source.USER);
            long bothMs = sumDuration(timeline, SourceActivityTracker.Source.BOTH);

            assertTrue(userMs > 800, "Expected mostly USER");
            assertEquals(0, bothMs, "Expected no BOTH when user dominates");
        }

        @Test
        @DisplayName("Mic at 0.04 RMS, system at 0.01 RMS should be USER only")
        void userModeratelyDominates() throws Exception {
            // Ratio: 0.04 / 0.01 = 4.0 (> 3.0 threshold)
            File micFile = createConstantRmsWav("mic.wav", 0.04, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.01, 1000);

            List<SourceActivityTracker.ActivitySegment> timeline =
                tracker.trackActivity(micFile, sysFile);

            long userMs = sumDuration(timeline, SourceActivityTracker.Source.USER);
            long bothMs = sumDuration(timeline, SourceActivityTracker.Source.BOTH);

            assertTrue(userMs > 800, "Expected USER dominance");
            assertEquals(0, bothMs, "Expected no BOTH when user dominates");
        }
    }

    @Nested
    @DisplayName("Genuine Crosstalk (comparable levels)")
    class GenuineCrosstalk {

        @Test
        @DisplayName("Both at 0.05 RMS should be BOTH")
        void equalLevels() throws Exception {
            // Both sources at same level - genuine crosstalk
            File micFile = createConstantRmsWav("mic.wav", 0.05, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.05, 1000);

            List<SourceActivityTracker.ActivitySegment> timeline =
                tracker.trackActivity(micFile, sysFile);

            long bothMs = sumDuration(timeline, SourceActivityTracker.Source.BOTH);
            assertTrue(bothMs > 800, "Expected BOTH for equal levels, got " + bothMs + "ms");
        }

        @Test
        @DisplayName("Ratio of 2:1 should still be BOTH (below 3:1 threshold)")
        void twoToOneRatioIsBoth() throws Exception {
            // Ratio: 0.04 / 0.02 = 2.0 (< 3.0 threshold)
            File micFile = createConstantRmsWav("mic.wav", 0.04, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.02, 1000);

            List<SourceActivityTracker.ActivitySegment> timeline =
                tracker.trackActivity(micFile, sysFile);

            long bothMs = sumDuration(timeline, SourceActivityTracker.Source.BOTH);
            assertTrue(bothMs > 800, "2:1 ratio should be BOTH, got " + bothMs + "ms");
        }

        @Test
        @DisplayName("Ratio of 2.9:1 should still be BOTH (just under threshold)")
        void justUnderThresholdIsBoth() throws Exception {
            // Ratio: 0.029 / 0.01 = 2.9 (< 3.0 threshold)
            File micFile = createConstantRmsWav("mic.wav", 0.029, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.01, 1000);

            List<SourceActivityTracker.ActivitySegment> timeline =
                tracker.trackActivity(micFile, sysFile);

            long bothMs = sumDuration(timeline, SourceActivityTracker.Source.BOTH);
            assertTrue(bothMs > 500, "2.9:1 ratio should be BOTH");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Near-zero system level should not cause division error")
        void nearZeroSystemLevel() throws Exception {
            // Mic active, system at near-zero (below MIN_RMS_FOR_RATIO)
            File micFile = createConstantRmsWav("mic.wav", 0.05, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.00001, 1000);

            // Should not throw, should attribute to USER
            List<SourceActivityTracker.ActivitySegment> timeline =
                assertDoesNotThrow(() -> tracker.trackActivity(micFile, sysFile));

            // System is below activity threshold, so this should be USER only
            long userMs = sumDuration(timeline, SourceActivityTracker.Source.USER);
            assertTrue(userMs > 800, "Expected USER when system near-zero");
        }

        @Test
        @DisplayName("Near-zero mic level should not cause division error")
        void nearZeroMicLevel() throws Exception {
            // System active, mic at near-zero
            File micFile = createConstantRmsWav("mic.wav", 0.00001, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.05, 1000);

            List<SourceActivityTracker.ActivitySegment> timeline =
                assertDoesNotThrow(() -> tracker.trackActivity(micFile, sysFile));

            // Mic is below activity threshold, so this should be SYSTEM only
            long systemMs = sumDuration(timeline, SourceActivityTracker.Source.SYSTEM);
            assertTrue(systemMs > 800, "Expected SYSTEM when mic near-zero");
        }

        @Test
        @DisplayName("Custom dominance ratio of 5.0 should require higher disparity")
        void customDominanceRatio() throws Exception {
            // With 5:1 ratio requirement, 4:1 should be BOTH
            SourceActivityTracker strictTracker =
                new SourceActivityTracker(SAMPLE_INTERVAL_MS, ACTIVITY_THRESHOLD, 5.0);

            File micFile = createConstantRmsWav("mic.wav", 0.04, 1000);
            File sysFile = createConstantRmsWav("sys.wav", 0.01, 1000); // 4:1 ratio

            List<SourceActivityTracker.ActivitySegment> timeline =
                strictTracker.trackActivity(micFile, sysFile);

            long bothMs = sumDuration(timeline, SourceActivityTracker.Source.BOTH);
            assertTrue(bothMs > 500, "4:1 with 5.0 threshold should be BOTH");
        }
    }

    @Nested
    @DisplayName("Transitions")
    class Transitions {

        @Test
        @DisplayName("User speaking then system takes over should transition cleanly")
        void userToSystemTransition() throws Exception {
            // First 500ms: user dominant, next 500ms: system dominant
            File micFile = createVariableRmsWav("mic.wav", new double[]{0.1, 0.1, 0.1, 0.1, 0.1, 0.006, 0.006, 0.006, 0.006, 0.006});
            File sysFile = createVariableRmsWav("sys.wav", new double[]{0.006, 0.006, 0.006, 0.006, 0.006, 0.1, 0.1, 0.1, 0.1, 0.1});

            List<SourceActivityTracker.ActivitySegment> timeline =
                tracker.trackActivity(micFile, sysFile);

            // Should have USER segment followed by SYSTEM segment
            long userMs = sumDuration(timeline, SourceActivityTracker.Source.USER);
            long systemMs = sumDuration(timeline, SourceActivityTracker.Source.SYSTEM);

            assertTrue(userMs > 300, "Expected USER period");
            assertTrue(systemMs > 300, "Expected SYSTEM period");
        }
    }

    // ========== Helper Methods ==========

    private File createConstantRmsWav(String filename, double targetRms, int durationMs) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        int samples = (SAMPLE_RATE * durationMs) / 1000;

        // Generate samples that produce target RMS
        // For a sine wave, amplitude = RMS * sqrt(2)
        double amplitude = targetRms * Math.sqrt(2) * 32767;
        short[] pcm = new short[samples];

        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            pcm[i] = (short) (amplitude * Math.sin(2 * Math.PI * 440 * t));
        }

        writeWav(file, pcm);
        return file;
    }

    private File createVariableRmsWav(String filename, double[] rmsPerInterval) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        int samplesPerInterval = (SAMPLE_RATE * SAMPLE_INTERVAL_MS) / 1000;
        int totalSamples = samplesPerInterval * rmsPerInterval.length;
        short[] pcm = new short[totalSamples];

        for (int interval = 0; interval < rmsPerInterval.length; interval++) {
            double targetRms = rmsPerInterval[interval];
            double amplitude = targetRms * Math.sqrt(2) * 32767;

            int start = interval * samplesPerInterval;
            for (int i = 0; i < samplesPerInterval; i++) {
                double t = (double) i / SAMPLE_RATE;
                pcm[start + i] = (short) (amplitude * Math.sin(2 * Math.PI * 440 * t));
            }
        }

        writeWav(file, pcm);
        return file;
    }

    private void writeWav(File file, short[] pcm) throws IOException {
        int byteRate = SAMPLE_RATE * 2; // 16-bit mono
        int dataSize = pcm.length * 2;

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            // RIFF header
            out.writeBytes("RIFF");
            out.writeInt(Integer.reverseBytes(36 + dataSize));
            out.writeBytes("WAVE");
            // fmt chunk
            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16)); // chunk size
            out.writeShort(Short.reverseBytes((short) 1)); // PCM
            out.writeShort(Short.reverseBytes((short) 1)); // mono
            out.writeInt(Integer.reverseBytes(SAMPLE_RATE));
            out.writeInt(Integer.reverseBytes(byteRate));
            out.writeShort(Short.reverseBytes((short) 2)); // block align
            out.writeShort(Short.reverseBytes((short) 16)); // bits per sample
            // data chunk
            out.writeBytes("data");
            out.writeInt(Integer.reverseBytes(dataSize));
            for (short sample : pcm) {
                out.writeShort(Short.reverseBytes(sample));
            }
        }
    }

    private long sumDuration(List<SourceActivityTracker.ActivitySegment> timeline,
                             SourceActivityTracker.Source source) {
        return timeline.stream()
            .filter(seg -> seg.source == source)
            .mapToLong(SourceActivityTracker.ActivitySegment::getDurationMs)
            .sum();
    }
}
