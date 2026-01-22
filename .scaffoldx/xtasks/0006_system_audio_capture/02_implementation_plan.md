# Implementation Plan: System Audio Capture

## Overview

Implement dual-source recording (mic + system audio) with source-level attribution. Users can toggle system audio capture on-the-fly, and transcripts are labeled with `[User]` or `[System]` based on audio source.

---

## Cluster 1: Platform Research & Feasibility

Research WASAPI for Windows system audio capture.

### 1.1 WASAPI Loopback Investigation

- Research WASAPI loopback capture mode
- Identify Java libraries for WASAPI access (JNA, or native wrapper)
- Test basic system audio capture in isolation
- Document API requirements and limitations

### 1.2 Audio Library Selection

Options:
- **JNA + WASAPI**: Direct Windows API access
- **Java Sound API**: Limited, may not support loopback
- **TarsosDSP**: Audio processing, unclear on loopback
- **Native JNI wrapper**: Custom C++ bridge

Decision criteria: Reliability, maintenance burden, Java 17 compatibility

### 1.3 Proof of Concept

- Capture 10 seconds of system audio
- Verify format compatibility with existing pipeline
- Test alongside microphone capture (no conflicts)

---

## Cluster 2: Dual-Track Capture Infrastructure

Build the core recording infrastructure for two audio sources.

### 2.1 AudioCaptureManager Class

New class to manage dual-source capture:

```java
public class AudioCaptureManager {
    private MicrophoneCapture micCapture;
    private SystemAudioCapture systemCapture;

    public void startDualCapture();
    public void stopDualCapture();
    public void toggleSystemAudio(boolean enabled);

    public File getMicTrack();
    public File getSystemTrack();
}
```

### 2.2 SystemAudioCapture Class

Encapsulate WASAPI loopback capture:

```java
public class SystemAudioCapture {
    public boolean isAvailable();  // Check WASAPI support
    public void start();
    public void pause();
    public void resume();
    public void stop();
    public File getRecordedFile();
}
```

### 2.3 Track Synchronization

- Start both captures with synchronized timestamp
- Use common clock reference for alignment
- Handle different sample rates (resample if needed)

---

## Cluster 3: Source Attribution Logic

Implement `[User]`/`[System]` labeling based on audio source activity.

### 3.1 RMS Activity Detection

Sample RMS (root mean square) at intervals to detect active source:

```java
public class SourceActivityTracker {
    private static final int SAMPLE_INTERVAL_MS = 100;
    private static final double ACTIVITY_THRESHOLD = 0.02;

    public List<ActivitySegment> trackActivity(
        File micTrack,
        File systemTrack
    );
}

public class ActivitySegment {
    long startMs;
    long endMs;
    Source activeSource;  // USER, SYSTEM, BOTH, SILENCE
}
```

### 3.2 Activity Timeline Generation

- Process both tracks in parallel
- Generate timeline of active sources
- Handle overlapping activity (both speaking)

### 3.3 Transcript Labeling

Map activity timeline to transcript segments:

```java
public String labelTranscript(
    String rawTranscript,
    List<ActivitySegment> activityTimeline
) {
    // Insert [User] or [System] prefixes
}
```

---

## Cluster 4: UI Integration

Add controls for system audio toggle.

### 4.1 Recording Panel Updates

- Add system audio toggle button/checkbox
- Visual indicator for system audio state (active/paused/off)
- Update status display to show both sources

### 4.2 Settings Panel

- Add "System Audio" section in Options
- Enable/disable feature globally
- Default toggle state preference

### 4.3 On-the-Fly Toggle

- Allow toggling during active recording
- Pause/resume system capture without stopping mic
- Visual feedback on state change

---

## Cluster 5: Pipeline Integration

Integrate dual-track capture with existing transcription flow.

### 5.1 Pre-Transcription Merge

Options:
- **Merge tracks before transcription**: Single audio file with both sources
- **Transcribe separately**: Two transcripts, interleave by timestamp

Recommended: Merge tracks, transcribe once, then apply source labels.

### 5.2 Silence Removal Consideration

Existing silence removal breaks timestamp alignment. Options:
- Skip silence removal for dual-source recordings
- Apply silence removal to merged track, adjust activity timeline
- Make silence removal optional/configurable

### 5.3 Output Format

Final transcript format:

```
[User]: My thoughts on this video...
[System]: Welcome to today's presentation about...
[User]: Interesting point there.
```

---

## Cluster 6: Testing & Verification

### 6.1 Unit Tests

- RMS activity detection accuracy
- Source labeling correctness
- Track synchronization

### 6.2 Integration Tests

- Full recording → transcription → labeled output flow
- Toggle during recording
- Error handling (system audio unavailable)

### 6.3 Manual Testing

- YouTube video + commentary session
- Voice message playback + response
- Long recording (30+ minutes)
- Edge cases: silence, simultaneous speech

---

## Dependencies

- WASAPI access (Windows-specific, needs JNA or native code)
- No new external services required for basic functionality
- Optional: Gemini API for enhanced attribution (future)

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| WASAPI complexity | Start with PoC, validate feasibility early |
| Track sync drift | Use common timestamp reference |
| Attribution accuracy | Conservative thresholds, user can edit labels |
| Performance | Async processing, efficient RMS sampling |
