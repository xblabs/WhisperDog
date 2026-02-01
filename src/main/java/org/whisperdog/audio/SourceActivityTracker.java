package org.whisperdog.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks audio activity in dual-source recordings to determine which source
 * (microphone/user or system audio) is active at any given time.
 *
 * Uses RMS (Root Mean Square) analysis to detect audio activity and generates
 * a timeline of activity segments for transcript labeling.
 */
public class SourceActivityTracker {
    private static final Logger logger = LogManager.getLogger(SourceActivityTracker.class);

    /** Default interval for RMS sampling in milliseconds */
    public static final int DEFAULT_SAMPLE_INTERVAL_MS = 100;

    /** Default RMS threshold for detecting activity (0.0-1.0 normalized scale) */
    public static final double DEFAULT_ACTIVITY_THRESHOLD = 0.005;

    /**
     * Default dominance ratio threshold. When both sources are above activity threshold,
     * if one source is this many times louder than the other, it is considered the sole
     * active source. Set to 3.0 meaning 3:1 ratio required for dominance.
     */
    public static final double DEFAULT_DOMINANCE_RATIO = 3.0;

    /**
     * Minimum RMS level to use as denominator in ratio calculations.
     * Prevents division by zero and handles near-silent signals.
     */
    public static final double MIN_RMS_FOR_RATIO = 0.0001;

    private final int sampleIntervalMs;
    private final double activityThreshold;
    private final double dominanceRatio;

    /**
     * Represents the active audio source.
     */
    public enum Source {
        SILENCE,   // No activity detected
        USER,      // Microphone/user audio active
        SYSTEM,    // System audio active
        BOTH       // Both sources active simultaneously
    }

    /**
     * Represents a segment of audio activity.
     */
    public static class ActivitySegment {
        public final long startMs;
        public final long endMs;
        public final Source source;

        public ActivitySegment(long startMs, long endMs, Source source) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.source = source;
        }

        public long getDurationMs() {
            return endMs - startMs;
        }

        @Override
        public String toString() {
            return String.format("[%d-%dms: %s]", startMs, endMs, source);
        }
    }

    /**
     * Create tracker with default settings.
     */
    public SourceActivityTracker() {
        this(DEFAULT_SAMPLE_INTERVAL_MS, DEFAULT_ACTIVITY_THRESHOLD, DEFAULT_DOMINANCE_RATIO);
    }

    /**
     * Create tracker with custom settings.
     * @param sampleIntervalMs Interval between RMS samples in milliseconds
     * @param activityThreshold RMS threshold for detecting activity (0.0-1.0)
     */
    public SourceActivityTracker(int sampleIntervalMs, double activityThreshold) {
        this(sampleIntervalMs, activityThreshold, DEFAULT_DOMINANCE_RATIO);
    }

    /**
     * Create tracker with full custom settings including dominance ratio.
     * @param sampleIntervalMs Interval between RMS samples in milliseconds
     * @param activityThreshold RMS threshold for detecting activity (0.0-1.0)
     * @param dominanceRatio Ratio threshold for single-source attribution when both active
     */
    public SourceActivityTracker(int sampleIntervalMs, double activityThreshold, double dominanceRatio) {
        this.sampleIntervalMs = sampleIntervalMs;
        this.activityThreshold = activityThreshold;
        this.dominanceRatio = dominanceRatio;
    }

    /**
     * Analyze both audio tracks and generate an activity timeline.
     * @param micTrack Microphone audio file (WAV format)
     * @param systemTrack System audio file (WAV format), may be null
     * @return List of activity segments covering the entire duration
     */
    public List<ActivitySegment> trackActivity(File micTrack, File systemTrack) {
        List<ActivitySegment> timeline = new ArrayList<>();

        try {
            // Read RMS values from both tracks
            double[] micRms = readRmsValues(micTrack);
            double[] systemRms = systemTrack != null ? readRmsValues(systemTrack) : null;

            // Determine the longer track's length
            int maxSamples = micRms.length;
            if (systemRms != null && systemRms.length > maxSamples) {
                maxSamples = systemRms.length;
            }

            // Build timeline by analyzing each sample interval
            Source currentSource = Source.SILENCE;
            long segmentStartMs = 0;

            for (int i = 0; i < maxSamples; i++) {
                long timeMs = (long) i * sampleIntervalMs;

                // Get RMS values for this interval
                double micLevel = i < micRms.length ? micRms[i] : 0.0;
                double sysLevel = systemRms != null && i < systemRms.length ? systemRms[i] : 0.0;

                // Check activity in each track
                boolean micActive = micLevel >= activityThreshold;
                boolean systemActive = sysLevel >= activityThreshold;

                // Determine source for this interval
                Source source;
                if (micActive && systemActive) {
                    // Both above threshold - use dominance ratio to determine true source
                    source = determineSourceByDominance(micLevel, sysLevel);
                } else if (micActive) {
                    source = Source.USER;
                } else if (systemActive) {
                    source = Source.SYSTEM;
                } else {
                    source = Source.SILENCE;
                }

                // Check for source change
                if (source != currentSource) {
                    // Close previous segment if not at start
                    if (i > 0) {
                        timeline.add(new ActivitySegment(segmentStartMs, timeMs, currentSource));
                    }
                    // Start new segment
                    currentSource = source;
                    segmentStartMs = timeMs;
                }
            }

            // Close final segment
            long endMs = (long) maxSamples * sampleIntervalMs;
            timeline.add(new ActivitySegment(segmentStartMs, endMs, currentSource));

            // Merge very short segments (debounce)
            timeline = mergeShortSegments(timeline, sampleIntervalMs * 2);

            logger.info("Generated activity timeline with {} segments", timeline.size());

        } catch (Exception e) {
            logger.error("Failed to track activity: {}", e.getMessage(), e);
            // Return single segment covering entire duration as fallback
            timeline.add(new ActivitySegment(0, Long.MAX_VALUE, Source.USER));
        }

        return timeline;
    }

    /**
     * Determine source when both mic and system are above activity threshold.
     * Uses dominance ratio to attribute to single source when one is clearly louder.
     *
     * @param micLevel RMS level from microphone
     * @param sysLevel RMS level from system audio
     * @return USER if mic dominates, SYSTEM if system dominates, BOTH if comparable
     */
    private Source determineSourceByDominance(double micLevel, double sysLevel) {
        // Guard against division by zero - use minimum floor for denominator
        double safeMicLevel = Math.max(micLevel, MIN_RMS_FOR_RATIO);
        double safeSysLevel = Math.max(sysLevel, MIN_RMS_FOR_RATIO);

        double ratio = safeMicLevel / safeSysLevel;

        if (ratio >= dominanceRatio) {
            // Mic is significantly louder - attribute to user only
            return Source.USER;
        } else if (ratio <= 1.0 / dominanceRatio) {
            // System is significantly louder - attribute to system only
            return Source.SYSTEM;
        } else {
            // Levels are comparable - genuine crosstalk
            return Source.BOTH;
        }
    }

    /**
     * Read WAV file and compute RMS values at sample intervals.
     * @return Array of RMS values, one per interval
     */
    private double[] readRmsValues(File wavFile) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile)) {
            AudioFormat format = ais.getFormat();
            int sampleRate = (int) format.getSampleRate();
            int channels = format.getChannels();
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int frameSize = format.getFrameSize();

            // Calculate samples per interval
            int samplesPerInterval = (sampleRate * sampleIntervalMs) / 1000;
            int bytesPerInterval = samplesPerInterval * frameSize;

            // Estimate total intervals
            long totalFrames = ais.getFrameLength();
            int totalIntervals = (int) Math.ceil((double) totalFrames / samplesPerInterval);
            double[] rmsValues = new double[totalIntervals];

            byte[] buffer = new byte[bytesPerInterval];
            int intervalIndex = 0;

            while (true) {
                int bytesRead = ais.read(buffer);
                if (bytesRead <= 0) break;

                // Calculate RMS for this interval
                double rms = calculateRms(buffer, bytesRead, bytesPerSample, channels, format.isBigEndian());
                if (intervalIndex < rmsValues.length) {
                    rmsValues[intervalIndex] = rms;
                }
                intervalIndex++;
            }

            return rmsValues;
        }
    }

    /**
     * Calculate RMS (Root Mean Square) of audio samples.
     * Returns normalized value between 0.0 and 1.0.
     */
    private double calculateRms(byte[] buffer, int bytesRead, int bytesPerSample,
            int channels, boolean bigEndian) {

        int frameSize = bytesPerSample * channels;
        int samplesRead = bytesRead / frameSize;
        if (samplesRead == 0) return 0.0;

        // Wrap only the bytes we'll actually read to prevent underflow
        int bytesToProcess = samplesRead * frameSize;
        double sumSquares = 0.0;
        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, bytesToProcess);
        bb.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < samplesRead; i++) {
            double sample = 0.0;

            // Read and average all channels
            for (int ch = 0; ch < channels; ch++) {
                if (bytesPerSample == 2) {
                    // 16-bit samples
                    sample += bb.getShort() / 32768.0;
                } else if (bytesPerSample == 1) {
                    // 8-bit samples (unsigned)
                    sample += ((bb.get() & 0xFF) - 128) / 128.0;
                } else if (bytesPerSample == 4) {
                    // 32-bit samples (assume int)
                    sample += bb.getInt() / 2147483648.0;
                }
            }
            sample /= channels;

            sumSquares += sample * sample;
        }

        return Math.sqrt(sumSquares / samplesRead);
    }

    /**
     * Merge segments shorter than the minimum duration with adjacent segments.
     * Helps reduce noise from brief fluctuations.
     *
     * When merging segments with different sources, uses the source of the longer
     * neighbor to avoid falsely inflating BOTH attribution.
     */
    private List<ActivitySegment> mergeShortSegments(List<ActivitySegment> segments,
            long minDurationMs) {
        if (segments.size() <= 1) return segments;

        List<ActivitySegment> merged = new ArrayList<>();
        ActivitySegment current = segments.get(0);

        for (int i = 1; i < segments.size(); i++) {
            ActivitySegment next = segments.get(i);

            // If current segment is too short, merge with neighbor
            if (current.getDurationMs() < minDurationMs && merged.isEmpty()) {
                // Very short first segment - extend next to cover it
                current = new ActivitySegment(current.startMs, next.endMs, next.source);
            } else if (current.getDurationMs() < minDurationMs) {
                // Short middle segment - merge with neighbors
                ActivitySegment prev = merged.remove(merged.size() - 1);

                // Determine source: use the longer neighbor's source to avoid false BOTH
                Source mergedSource;
                if (prev.source == next.source) {
                    mergedSource = prev.source;
                } else if (prev.source == Source.BOTH || next.source == Source.BOTH) {
                    // If either neighbor is BOTH, preserve that (genuine crosstalk context)
                    mergedSource = Source.BOTH;
                } else {
                    // Different single sources - use the longer neighbor's source
                    mergedSource = prev.getDurationMs() >= next.getDurationMs()
                        ? prev.source : next.source;
                }
                current = new ActivitySegment(prev.startMs, next.endMs, mergedSource);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    /**
     * Label a transcript with source prefixes based on the activity timeline.
     * Distributes words across timeline segments to keep source attribution coherent.
     *
     * @param rawTranscript The raw transcript text with timestamps
     * @param timeline Activity timeline from trackActivity()
     * @return Labeled transcript with [User]/[System] prefixes
     */
    public String labelTranscript(String rawTranscript, List<ActivitySegment> timeline) {
        if (timeline == null || timeline.isEmpty()) {
            return rawTranscript;
        }

        long totalDurationMs = timeline.get(timeline.size() - 1).endMs;
        if (totalDurationMs <= 0) {
            return rawTranscript;
        }

        String[] words = rawTranscript.split("\\s+");
        if (words.length == 0) {
            return rawTranscript;
        }

        // Calculate average words per millisecond for proportional distribution
        double wordsPerMs = (double) words.length / totalDurationMs;

        StringBuilder labeled = new StringBuilder();
        Source lastLabeledSource = null;
        int wordIndex = 0;

        // Distribute words across timeline segments
        for (ActivitySegment segment : timeline) {
            if (wordIndex >= words.length) {
                break;
            }

            Source segmentSource = segment.source;
            if (segmentSource == Source.SILENCE) {
                continue; // Skip silence segments
            }

            long segmentDurationMs = segment.endMs - segment.startMs;

            // Estimate words in this segment based on duration ratio
            int estimatedWordsInSegment = Math.max(1,
                (int) Math.round(segmentDurationMs * wordsPerMs));

            // Add label if source changed
            if (segmentSource != lastLabeledSource) {
                if (labeled.length() > 0) {
                    labeled.append("\n");
                }
                String label = switch (segmentSource) {
                    case USER -> "[User]";
                    case SYSTEM -> "[System]";
                    case BOTH -> "[User+System]";
                    default -> "";
                };
                if (!label.isEmpty()) {
                    labeled.append(label).append(": ");
                }
                lastLabeledSource = segmentSource;
            }

            // Add words for this segment
            int endWordIndex = Math.min(wordIndex + estimatedWordsInSegment, words.length);
            for (int i = wordIndex; i < endWordIndex; i++) {
                labeled.append(words[i]);
                if (i < words.length - 1) {
                    labeled.append(" ");
                }
            }

            wordIndex = endWordIndex;
        }

        // Add any remaining words to last source
        if (wordIndex < words.length) {
            if (labeled.length() > 0) {
                labeled.append(" ");
            }
            for (int i = wordIndex; i < words.length; i++) {
                labeled.append(words[i]);
                if (i < words.length - 1) {
                    labeled.append(" ");
                }
            }
        }

        return labeled.toString().trim();
    }

    /**
     * Enhanced label method that uses word-level timestamps if available.
     *
     * @param words List of transcribed words with timestamps
     * @param timeline Activity timeline
     * @return Labeled transcript
     */
    public String labelTranscriptWithTimestamps(List<TimestampedWord> words,
            List<ActivitySegment> timeline) {
        if (words == null || words.isEmpty()) {
            return "";
        }
        if (timeline == null || timeline.isEmpty()) {
            // No timeline - return unlabeled
            StringBuilder sb = new StringBuilder();
            for (TimestampedWord word : words) {
                sb.append(word.text).append(" ");
            }
            return sb.toString().trim();
        }

        StringBuilder result = new StringBuilder();
        Source currentSource = null;

        for (TimestampedWord word : words) {
            // Find which segment this word belongs to
            Source wordSource = getSourceAtTime(timeline, word.startMs);

            // Add label on source change
            if (wordSource != currentSource && wordSource != Source.SILENCE) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                String label = switch (wordSource) {
                    case USER -> "[User]";
                    case SYSTEM -> "[System]";
                    case BOTH -> "[User+System]";
                    default -> "";
                };
                result.append(label).append(": ");
                currentSource = wordSource;
            }

            result.append(word.text).append(" ");
        }

        return result.toString().trim();
    }

    /**
     * Get the active source at a specific timestamp.
     */
    public Source getSourceAtTime(List<ActivitySegment> timeline, long timeMs) {
        for (ActivitySegment seg : timeline) {
            if (timeMs >= seg.startMs && timeMs < seg.endMs) {
                return seg.source;
            }
        }
        return Source.SILENCE;
    }

    /**
     * Represents a word with timestamp information.
     */
    public static class TimestampedWord {
        public final String text;
        public final long startMs;
        public final long endMs;

        public TimestampedWord(String text, long startMs, long endMs) {
            this.text = text;
            this.startMs = startMs;
            this.endMs = endMs;
        }
    }
}
