package org.whisperdog.audio;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Diagnostic test to analyze RMS levels in the realworld_manni recording.
 * Helps understand why the dominance ratio results are unexpected.
 */
class RmsDiagnosticTest {

    private static final String TEST_CORPUS_PATH =
        ".scaffoldx/xtasks/0008_dual_source_attribution_accuracy/artifacts/test_corpus";
    private static final double ACTIVITY_THRESHOLD = 0.005;
    private static final int SAMPLE_INTERVAL_MS = 100;

    @Test
    void analyzeRmsLevels() throws Exception {
        Path testCorpusDir = Paths.get(TEST_CORPUS_PATH);
        File micFile = testCorpusDir.resolve("realworld_manni_mic.wav").toFile();
        File sysFile = testCorpusDir.resolve("realworld_manni_system.wav").toFile();

        if (!micFile.exists() || !sysFile.exists()) {
            System.out.println("SKIP: Test corpus files not found");
            return;
        }

        double[] micRms = readRmsValues(micFile);
        double[] sysRms = readRmsValues(sysFile);

        int maxSamples = Math.max(micRms.length, sysRms.length);

        System.out.println("=== RMS LEVEL ANALYSIS ===\n");
        System.out.println("Total intervals: " + maxSamples);
        System.out.printf("Activity threshold: %.4f%n%n", ACTIVITY_THRESHOLD);

        // Categorize intervals
        int bothAbove = 0;
        int onlyMicAbove = 0;
        int onlySysAbove = 0;
        int neitherAbove = 0;

        // Track ratio distributions when both above
        int ratioAbove3 = 0;     // mic dominates (>= 3:1)
        int ratioBelow033 = 0;   // sys dominates (<= 1:3)
        int ratioBetween = 0;    // comparable (BOTH)

        // Sample some specific intervals for debugging
        System.out.println("Sample intervals (first 20 where both above threshold):");
        System.out.println("  Interval | Mic RMS  | Sys RMS  | Ratio    | Dom3.0 | Dom1000");
        System.out.println("  ---------|----------|----------|----------|--------|--------");

        int sampleCount = 0;
        for (int i = 0; i < maxSamples; i++) {
            double mic = i < micRms.length ? micRms[i] : 0.0;
            double sys = i < sysRms.length ? sysRms[i] : 0.0;

            boolean micActive = mic >= ACTIVITY_THRESHOLD;
            boolean sysActive = sys >= ACTIVITY_THRESHOLD;

            if (micActive && sysActive) {
                bothAbove++;
                double safeMic = Math.max(mic, 0.0001);
                double safeSys = Math.max(sys, 0.0001);
                double ratio = safeMic / safeSys;

                if (ratio >= 3.0) {
                    ratioAbove3++;
                } else if (ratio <= 0.333) {
                    ratioBelow033++;
                } else {
                    ratioBetween++;
                }

                // Sample output
                if (sampleCount < 20) {
                    String dom3 = ratio >= 3.0 ? "USER" : (ratio <= 0.333 ? "SYS" : "BOTH");
                    String dom1000 = ratio >= 1000.0 ? "USER" : (ratio <= 0.001 ? "SYS" : "BOTH");
                    System.out.printf("  %8d | %8.5f | %8.5f | %8.4f | %6s | %6s%n",
                        i, mic, sys, ratio, dom3, dom1000);
                    sampleCount++;
                }
            } else if (micActive) {
                onlyMicAbove++;
            } else if (sysActive) {
                onlySysAbove++;
            } else {
                neitherAbove++;
            }
        }

        System.out.println("\n=== INTERVAL CATEGORIZATION ===");
        System.out.printf("  Both above threshold:  %4d intervals%n", bothAbove);
        System.out.printf("  Only mic above:        %4d intervals%n", onlyMicAbove);
        System.out.printf("  Only sys above:        %4d intervals%n", onlySysAbove);
        System.out.printf("  Neither above:         %4d intervals%n", neitherAbove);

        System.out.println("\n=== RATIO DISTRIBUTION (when both above) ===");
        System.out.printf("  ratio >= 3.0 (USER):   %4d intervals (%.1f%%)%n",
            ratioAbove3, bothAbove > 0 ? ratioAbove3 * 100.0 / bothAbove : 0);
        System.out.printf("  ratio <= 0.33 (SYS):   %4d intervals (%.1f%%)%n",
            ratioBelow033, bothAbove > 0 ? ratioBelow033 * 100.0 / bothAbove : 0);
        System.out.printf("  0.33 < ratio < 3 (BOTH): %4d intervals (%.1f%%)%n",
            ratioBetween, bothAbove > 0 ? ratioBetween * 100.0 / bothAbove : 0);

        System.out.println("\n=== EXPECTED ATTRIBUTION ===");
        double totalActive = onlyMicAbove + onlySysAbove + bothAbove;

        // With dominance ratio = 3.0
        double user3 = onlyMicAbove + ratioAbove3;
        double sys3 = onlySysAbove + ratioBelow033;
        double both3 = ratioBetween;
        System.out.println("With dominance ratio = 3.0:");
        System.out.printf("  USER:   %.1f%% | SYSTEM: %.1f%% | BOTH: %.1f%%%n",
            user3 * 100 / totalActive, sys3 * 100 / totalActive, both3 * 100 / totalActive);

        // With dominance ratio = 1000.0 (effectively all BOTH when both active)
        double user1000 = onlyMicAbove + (bothAbove > 0 ? 0 : 0);  // Only mic-only intervals
        double sys1000 = onlySysAbove;  // Only sys-only intervals
        double both1000 = bothAbove;    // All both-above become BOTH
        System.out.println("With dominance ratio = 1000.0 (baseline):");
        System.out.printf("  USER:   %.1f%% | SYSTEM: %.1f%% | BOTH: %.1f%%%n",
            user1000 * 100 / totalActive, sys1000 * 100 / totalActive, both1000 * 100 / totalActive);
    }

    private double[] readRmsValues(File wavFile) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile)) {
            AudioFormat format = ais.getFormat();
            int sampleRate = (int) format.getSampleRate();
            int channels = format.getChannels();
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int frameSize = format.getFrameSize();

            int samplesPerInterval = (sampleRate * SAMPLE_INTERVAL_MS) / 1000;
            int bytesPerInterval = samplesPerInterval * frameSize;

            long totalFrames = ais.getFrameLength();
            int totalIntervals = (int) Math.ceil((double) totalFrames / samplesPerInterval);
            double[] rmsValues = new double[totalIntervals];

            byte[] buffer = new byte[bytesPerInterval];
            int intervalIndex = 0;

            while (true) {
                int bytesRead = ais.read(buffer);
                if (bytesRead <= 0) break;

                double rms = calculateRms(buffer, bytesRead, bytesPerSample, channels, format.isBigEndian());
                if (intervalIndex < rmsValues.length) {
                    rmsValues[intervalIndex] = rms;
                }
                intervalIndex++;
            }

            return rmsValues;
        }
    }

    private double calculateRms(byte[] buffer, int bytesRead, int bytesPerSample,
                                int channels, boolean bigEndian) {
        int frameSize = bytesPerSample * channels;
        int samplesRead = bytesRead / frameSize;
        if (samplesRead == 0) return 0.0;

        int bytesToProcess = samplesRead * frameSize;
        double sumSquares = 0.0;
        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, bytesToProcess);
        bb.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < samplesRead; i++) {
            double sample = 0.0;
            for (int ch = 0; ch < channels; ch++) {
                if (bytesPerSample == 2) {
                    sample += bb.getShort() / 32768.0;
                } else if (bytesPerSample == 1) {
                    sample += ((bb.get() & 0xFF) - 128) / 128.0;
                } else if (bytesPerSample == 4) {
                    sample += bb.getInt() / 2147483648.0;
                }
            }
            sample /= channels;
            sumSquares += sample * sample;
        }

        return Math.sqrt(sumSquares / samplesRead);
    }
}
