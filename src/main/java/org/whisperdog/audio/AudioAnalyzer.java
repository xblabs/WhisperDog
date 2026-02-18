package org.whisperdog.audio;

import java.io.ByteArrayOutputStream;

/**
 * Utility class for analyzing audio data.
 * Provides RMS and peak amplitude calculations for 16-bit PCM audio.
 */
public class AudioAnalyzer {

    /**
     * Calculates the RMS (Root Mean Square) amplitude of audio data.
     *
     * @param audioData 16-bit signed PCM audio bytes (little-endian)
     * @return RMS value normalized to 0.0-1.0 range
     */
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

    /**
     * Calculates the peak amplitude of audio data.
     *
     * @param audioData 16-bit signed PCM audio bytes (little-endian)
     * @return Peak value normalized to 0.0-1.0 range
     */
    public double calculatePeak(byte[] audioData) {
        if (audioData == null || audioData.length < 2) return 0;

        int maxSample = 0;
        for (int i = 0; i < audioData.length - 1; i += 2) {
            int sample = Math.abs((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            if (sample > maxSample) maxSample = sample;
        }

        return maxSample / 32768.0;
    }

    /**
     * Result of silence removal containing processed audio and metrics.
     */
    public static class SilenceRemovalResult {
        public final byte[] audioData;
        public final double silenceRatio;
        public final double nonSilenceDuration;

        public SilenceRemovalResult(byte[] audioData, double silenceRatio, double nonSilenceDuration) {
            this.audioData = audioData;
            this.silenceRatio = silenceRatio;
            this.nonSilenceDuration = nonSilenceDuration;
        }
    }

    /**
     * Removes silence from audio data based on RMS threshold.
     * Delegates to the duration-aware overload with 0ms minimum (removes all silence).
     *
     * @param audioData Raw 16-bit PCM audio bytes
     * @param sampleRate Sample rate in Hz (e.g., 16000)
     * @param silenceThreshold RMS threshold below which audio is considered silence (0.0-1.0)
     * @return Result containing filtered audio and metrics
     */
    public SilenceRemovalResult removeSilence(byte[] audioData, float sampleRate, double silenceThreshold) {
        return removeSilence(audioData, sampleRate, silenceThreshold, 0);
    }

    /**
     * Removes silence from audio data based on RMS threshold and minimum silence duration.
     * Only silence regions lasting at least minSilenceDurationMs are removed.
     * Matches the algorithm used by SilenceRemover for consistent behavior.
     *
     * @param audioData Raw 16-bit PCM audio bytes
     * @param sampleRate Sample rate in Hz (e.g., 16000)
     * @param silenceThreshold RMS threshold below which audio is considered silence (0.0-1.0)
     * @param minSilenceDurationMs Minimum consecutive silence duration (ms) to qualify for removal
     * @return Result containing filtered audio and metrics
     */
    public SilenceRemovalResult removeSilence(byte[] audioData, float sampleRate,
                                              double silenceThreshold, int minSilenceDurationMs) {
        if (audioData == null || audioData.length < 2) {
            return new SilenceRemovalResult(new byte[0], 1.0, 0.0);
        }

        // 100ms analysis windows (matches SilenceRemover for consistent calibration)
        int samplesPerWindow = (int) (sampleRate * 0.1);
        int bytesPerWindow = samplesPerWindow * 2;
        int windowCount = audioData.length / bytesPerWindow;
        int minSilenceWindows = Math.max(1, (int) (minSilenceDurationMs / 100.0));

        // First pass: classify each window as silent or not
        boolean[] isSilent = new boolean[windowCount];
        int silentWindows = 0;
        for (int w = 0; w < windowCount; w++) {
            int offset = w * bytesPerWindow;
            byte[] window = new byte[bytesPerWindow];
            System.arraycopy(audioData, offset, window, 0, bytesPerWindow);
            double windowRMS = calculateRMS(window);
            isSilent[w] = windowRMS < silenceThreshold;
            if (isSilent[w]) silentWindows++;
        }

        // Second pass: identify silence regions that meet the minimum duration,
        // and mark which windows to remove
        boolean[] remove = new boolean[windowCount];
        int regionStart = -1;
        for (int w = 0; w <= windowCount; w++) {
            boolean silent = (w < windowCount) && isSilent[w];
            if (silent) {
                if (regionStart == -1) regionStart = w;
            } else {
                if (regionStart != -1) {
                    int regionLength = w - regionStart;
                    if (regionLength >= minSilenceWindows) {
                        for (int r = regionStart; r < w; r++) {
                            remove[r] = true;
                        }
                    }
                    regionStart = -1;
                }
            }
        }

        // Third pass: write non-removed windows to output
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int removedWindows = 0;
        for (int w = 0; w < windowCount; w++) {
            if (remove[w]) {
                removedWindows++;
            } else {
                int offset = w * bytesPerWindow;
                output.write(audioData, offset, bytesPerWindow);
            }
        }

        double totalDuration = audioData.length / 2.0 / sampleRate;
        double silenceRatio = windowCount > 0 ? (double) silentWindows / windowCount : 0;
        double removedDuration = removedWindows * 0.1;
        double nonSilenceDuration = totalDuration - removedDuration;

        return new SilenceRemovalResult(output.toByteArray(), silenceRatio, nonSilenceDuration);
    }
}
