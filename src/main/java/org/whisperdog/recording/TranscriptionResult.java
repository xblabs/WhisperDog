package org.whisperdog.recording;

import org.whisperdog.audio.SourceActivityTracker.TimestampedWord;

import java.util.Collections;
import java.util.List;

/**
 * Holds transcription results including optional word-level timestamps.
 * Used for accurate source attribution in dual-source (mic + system) recordings.
 */
public class TranscriptionResult {
    private final String text;
    private final List<TimestampedWord> words;
    private final boolean hasWordTimestamps;

    /**
     * Create a result with only text (no word timestamps).
     */
    public TranscriptionResult(String text) {
        this.text = text;
        this.words = Collections.emptyList();
        this.hasWordTimestamps = false;
    }

    /**
     * Create a result with text and word-level timestamps.
     */
    public TranscriptionResult(String text, List<TimestampedWord> words) {
        this.text = text;
        this.words = words != null ? words : Collections.emptyList();
        this.hasWordTimestamps = !this.words.isEmpty();
    }

    public String getText() {
        return text;
    }

    public List<TimestampedWord> getWords() {
        return words;
    }

    public boolean hasWordTimestamps() {
        return hasWordTimestamps;
    }
}
