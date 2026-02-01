package org.whisperdog.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Measurement tests for Task 0008: Dual-Source Attribution Accuracy.
 *
 * Analyzes test corpus recordings and calculates false_both_rate to validate
 * the dominance ratio implementation against gate check thresholds.
 */
class AttributionMeasurementTest {

    private static final String TEST_CORPUS_PATH =
        ".scaffoldx/xtasks/0008_dual_source_attribution_accuracy/artifacts/test_corpus";

    private Path testCorpusDir;

    @BeforeEach
    void setUp() {
        testCorpusDir = Paths.get(TEST_CORPUS_PATH);
    }

    @Test
    @DisplayName("Measure realworld_manni with dominance ratio DISABLED (baseline)")
    void measureBaselineWithoutDominanceRatio() {
        File micFile = testCorpusDir.resolve("realworld_manni_mic.wav").toFile();
        File sysFile = testCorpusDir.resolve("realworld_manni_system.wav").toFile();

        if (!micFile.exists() || !sysFile.exists()) {
            System.out.println("SKIP: Test corpus files not found at " + testCorpusDir);
            System.out.println("  Expected: realworld_manni_mic.wav, realworld_manni_system.wav");
            return;
        }

        // Use very high dominance ratio to effectively disable the feature (baseline)
        SourceActivityTracker tracker = new SourceActivityTracker(
            SourceActivityTracker.DEFAULT_SAMPLE_INTERVAL_MS,
            SourceActivityTracker.DEFAULT_ACTIVITY_THRESHOLD,
            1000.0  // Effectively disabled - requires 1000:1 ratio
        );

        MeasurementResult result = measureAttribution(tracker, micFile, sysFile);

        System.out.println("\n=== BASELINE MEASUREMENT (Dominance Ratio DISABLED) ===");
        printResult(result);
        System.out.println("Expected baseline false_both_rate: ~15-25%");
    }

    @Test
    @DisplayName("Measure realworld_manni with dominance ratio ENABLED (Phase 1 fix)")
    void measurePhase1WithDominanceRatio() {
        File micFile = testCorpusDir.resolve("realworld_manni_mic.wav").toFile();
        File sysFile = testCorpusDir.resolve("realworld_manni_system.wav").toFile();

        if (!micFile.exists() || !sysFile.exists()) {
            System.out.println("SKIP: Test corpus files not found at " + testCorpusDir);
            System.out.println("  Expected: realworld_manni_mic.wav, realworld_manni_system.wav");
            return;
        }

        // Use default tracker with dominance ratio enabled
        SourceActivityTracker tracker = new SourceActivityTracker();

        MeasurementResult result = measureAttribution(tracker, micFile, sysFile);

        System.out.println("\n=== PHASE 1 MEASUREMENT (Dominance Ratio = 3.0) ===");
        printResult(result);
        System.out.println("Gate check: false_both_rate <= 5.0%");
        System.out.println("Result: " + (result.falseBothRate <= 5.0 ? "PASS" : "FAIL"));

        // Assertion for CI/automated testing
        assertTrue(result.falseBothRate <= 5.0,
            "Phase 1 gate check failed: false_both_rate=" + result.falseBothRate + "% (max 5.0%)");
    }

    @Test
    @DisplayName("Compare baseline vs Phase 1 improvement")
    void compareBaselineVsPhase1() {
        File micFile = testCorpusDir.resolve("realworld_manni_mic.wav").toFile();
        File sysFile = testCorpusDir.resolve("realworld_manni_system.wav").toFile();

        if (!micFile.exists() || !sysFile.exists()) {
            System.out.println("SKIP: Test corpus files not found at " + testCorpusDir);
            return;
        }

        // Baseline (disabled)
        SourceActivityTracker baselineTracker = new SourceActivityTracker(
            SourceActivityTracker.DEFAULT_SAMPLE_INTERVAL_MS,
            SourceActivityTracker.DEFAULT_ACTIVITY_THRESHOLD,
            1000.0
        );
        MeasurementResult baseline = measureAttribution(baselineTracker, micFile, sysFile);

        // Phase 1 (enabled)
        SourceActivityTracker phase1Tracker = new SourceActivityTracker();
        MeasurementResult phase1 = measureAttribution(phase1Tracker, micFile, sysFile);

        System.out.println("\n=== COMPARISON: BASELINE vs PHASE 1 ===");
        System.out.println("\nBASELINE (dominance ratio disabled):");
        printResult(baseline);
        System.out.println("\nPHASE 1 (dominance ratio = 3.0):");
        printResult(phase1);

        double improvement = baseline.falseBothRate - phase1.falseBothRate;
        System.out.println("\n=== IMPROVEMENT ===");
        System.out.printf("  false_both_rate reduced by: %.2f percentage points%n", improvement);
        System.out.printf("  Relative improvement: %.1f%%%n",
            baseline.falseBothRate > 0 ? (improvement / baseline.falseBothRate * 100) : 0);
    }

    private MeasurementResult measureAttribution(SourceActivityTracker tracker,
                                                  File micFile, File sysFile) {
        List<SourceActivityTracker.ActivitySegment> timeline =
            tracker.trackActivity(micFile, sysFile);

        long totalMs = 0;
        long silenceMs = 0;
        long userMs = 0;
        long systemMs = 0;
        long bothMs = 0;

        for (SourceActivityTracker.ActivitySegment seg : timeline) {
            long duration = seg.getDurationMs();
            totalMs += duration;
            switch (seg.source) {
                case SILENCE -> silenceMs += duration;
                case USER -> userMs += duration;
                case SYSTEM -> systemMs += duration;
                case BOTH -> bothMs += duration;
            }
        }

        // Calculate percentages (excluding silence for false_both_rate)
        long activeMs = userMs + systemMs + bothMs;
        double userPct = activeMs > 0 ? (userMs * 100.0 / activeMs) : 0;
        double systemPct = activeMs > 0 ? (systemMs * 100.0 / activeMs) : 0;
        double bothPct = activeMs > 0 ? (bothMs * 100.0 / activeMs) : 0;

        return new MeasurementResult(
            timeline.size(),
            totalMs,
            silenceMs,
            userMs,
            systemMs,
            bothMs,
            userPct,
            systemPct,
            bothPct
        );
    }

    private void printResult(MeasurementResult r) {
        System.out.println("  Total segments: " + r.totalSegments);
        System.out.println("  Total duration: " + r.totalMs + "ms");
        System.out.println("  Breakdown:");
        System.out.printf("    SILENCE: %6dms%n", r.silenceMs);
        System.out.printf("    USER:    %6dms (%.1f%% of active)%n", r.userMs, r.userPct);
        System.out.printf("    SYSTEM:  %6dms (%.1f%% of active)%n", r.systemMs, r.systemPct);
        System.out.printf("    BOTH:    %6dms (%.1f%% of active)%n", r.bothMs, r.falseBothRate);
        System.out.println();
        System.out.printf("  >>> false_both_rate: %.2f%% <<<%n", r.falseBothRate);
    }

    private static class MeasurementResult {
        final int totalSegments;
        final long totalMs;
        final long silenceMs;
        final long userMs;
        final long systemMs;
        final long bothMs;
        final double userPct;
        final double systemPct;
        final double falseBothRate; // bothPct

        MeasurementResult(int totalSegments, long totalMs, long silenceMs,
                         long userMs, long systemMs, long bothMs,
                         double userPct, double systemPct, double bothPct) {
            this.totalSegments = totalSegments;
            this.totalMs = totalMs;
            this.silenceMs = silenceMs;
            this.userMs = userMs;
            this.systemMs = systemMs;
            this.bothMs = bothMs;
            this.userPct = userPct;
            this.systemPct = systemPct;
            this.falseBothRate = bothPct;
        }
    }
}
