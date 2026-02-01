# System Audio Capture - Execution Checklist

> **Context Recovery**: Find first unchecked `[ ]` item → resume from there.

---

## Cluster 1: Platform Research & Feasibility

- [x] Research WASAPI loopback capture mode documentation
- [x] Identify Java library for WASAPI access (JNA vs native)
- [x] Create minimal PoC: capture 10s of system audio
- [x] Verify audio format compatibility with existing pipeline
- [x] Test mic + system capture simultaneously (no conflicts)
- [x] Document findings and chosen approach

---

## Cluster 2: Dual-Track Capture Infrastructure

- [x] Create `SystemAudioCapture` class with WASAPI integration
- [x] Implement `isAvailable()` to check platform support
- [x] Implement `start()`, `pause()`, `resume()`, `stop()` methods
- [x] Create `AudioCaptureManager` to coordinate mic + system
- [x] Implement synchronized start with common timestamp
- [x] Handle sample rate differences (resample if needed)
- [x] Add temp file management for system audio track

---

## Cluster 3: Source Attribution Logic

- [x] Create `SourceActivityTracker` class
- [x] Implement RMS sampling at configurable interval (100ms default)
- [x] Define activity threshold constant
- [x] Generate activity timeline from dual tracks
- [x] Handle overlapping activity (both sources active)
- [x] Implement `labelTranscript()` to insert `[User]`/`[System]` prefixes
- [x] Test labeling accuracy with sample recordings

---

## Cluster 4: UI Integration

- [x] Add system audio toggle to recording panel
- [x] Add visual indicator for system audio state
- [x] Update status display to show active sources
- [x] Implement on-the-fly toggle during recording
- [x] Add "System Audio" section in Settings/Options
- [x] Add default toggle state preference

---

## Cluster 5: Pipeline Integration

- [x] Decide merge strategy (pre-transcription vs post)
- [x] Implement track merging for transcription
- [x] Handle silence removal interaction (skip or adjust timeline)
- [x] Integrate labeled output into history panel
- [x] Update transcript export to include source labels

---

## Cluster 6: Testing & Verification

- [x] Test with YouTube video + commentary
- [x] Test with voice message playback + response
- [x] Test toggle during active recording
- [ ] Test long recording (30+ min)
- [x] Test when system audio unavailable (graceful fallback)
- [x] Verify temp files cleaned up properly
- [x] Test in dark theme (light theme removed)

---

**Reference**: Full implementation details in `02_implementation_plan.md`

**Source**: Triaged from `WB 030 - WSPR - System Audio Capture Feature.md`
