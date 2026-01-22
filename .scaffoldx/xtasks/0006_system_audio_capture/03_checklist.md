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
- [ ] Create `AudioCaptureManager` to coordinate mic + system
- [ ] Implement synchronized start with common timestamp
- [ ] Handle sample rate differences (resample if needed)
- [ ] Add temp file management for system audio track

---

## Cluster 3: Source Attribution Logic

- [ ] Create `SourceActivityTracker` class
- [ ] Implement RMS sampling at configurable interval (100ms default)
- [ ] Define activity threshold constant
- [ ] Generate activity timeline from dual tracks
- [ ] Handle overlapping activity (both sources active)
- [ ] Implement `labelTranscript()` to insert `[User]`/`[System]` prefixes
- [ ] Test labeling accuracy with sample recordings

---

## Cluster 4: UI Integration

- [ ] Add system audio toggle to recording panel
- [ ] Add visual indicator for system audio state
- [ ] Update status display to show active sources
- [ ] Implement on-the-fly toggle during recording
- [ ] Add "System Audio" section in Settings/Options
- [ ] Add default toggle state preference

---

## Cluster 5: Pipeline Integration

- [ ] Decide merge strategy (pre-transcription vs post)
- [ ] Implement track merging for transcription
- [ ] Handle silence removal interaction (skip or adjust timeline)
- [ ] Integrate labeled output into history panel
- [ ] Update transcript export to include source labels

---

## Cluster 6: Testing & Verification

- [ ] Test with YouTube video + commentary
- [ ] Test with voice message playback + response
- [ ] Test toggle during active recording
- [ ] Test long recording (30+ min)
- [ ] Test when system audio unavailable (graceful fallback)
- [ ] Verify temp files cleaned up properly
- [ ] Test in both light and dark themes

---

**Reference**: Full implementation details in `02_implementation_plan.md`

**Source**: Triaged from `WB 030 - WSPR - System Audio Capture Feature.md`
