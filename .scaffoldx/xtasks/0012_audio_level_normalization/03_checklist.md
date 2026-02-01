# Task 0012: Audio Level Normalization - Implementation Checklist

## Phase 1: Analysis & Core Implementation

### Research & Design
- [ ] Review existing audio capture flow in RecorderForm.java
- [ ] Identify integration point between capture and merge
- [ ] Review SourceActivityTracker RMS calculation for reuse

### Core Classes
- [ ] Create `AudioLevelAnalyzer` class in `org.whisperdog.audio`
  - [ ] Implement RMS calculation from WAV bytes (16-bit PCM)
  - [ ] Implement peak detection
  - [ ] Add clipping detection (peak >= CLIPPING_THRESHOLD_DB = -0.1 dB)
  - [ ] Add silence detection (RMS < -60 dB)
  - [ ] Add short recording detection (durationMs < 1000)
  - [ ] Return `TrackAnalysis` result object

- [ ] Create `AudioNormalizer` class in `org.whisperdog.audio`
  - [ ] Define constants: CLIPPING_THRESHOLD_DB, MAX_GAIN_DB (20), HEADROOM_DB (3)
  - [ ] Implement gain computation with headroom protection and ±20 dB clamp
  - [ ] Implement `applyGain()` that modifies WAV samples
  - [ ] Add entry point `normalizeForMerge(micFile, sysFile, ...)`
  - [ ] Handle stereo input (analyze/normalize both channels together)
  - [ ] Return `NormalizationResult` with temp files

### Integration
- [ ] Add normalization step in RecorderForm dual-source path
- [ ] Wire normalized files to merge/attribution pipeline
- [ ] Add temp file cleanup after merge completes
- [ ] Add logging for gain applied and warnings

## Phase 2: Configuration & Testing

### Settings
- [ ] Add `audio.normalization.enabled` to ConfigManager (default: true)
- [ ] Add `audio.normalization.targetDb` to ConfigManager (default: -20.0)
- [ ] Add getter `isNormalizationEnabled()` to ConfigManager
- [ ] Add getter `getNormalizationTarget()` to ConfigManager
- [ ] Add UI controls in Options (toggle, target level slider: -30 to -10 dB)

### Testing
- [ ] Unit test: RMS/peak calculation with known-level audio
- [ ] Unit test: Gain computation edge cases
- [ ] Unit test: Headroom protection prevents clipping
- [ ] Integration test: Full pipeline with imbalanced test files
- [ ] Verify attribution accuracy improves with normalization
- [ ] Manual test: Various device combinations (headset/speakers/etc.)

### Documentation
- [ ] Add user-facing description of normalization feature
- [ ] Document expected behavior in changelog

## Completion Criteria

- [ ] Normalization applied before merge in dual-source recordings
- [ ] No clipping introduced by normalization
- [ ] Attribution accuracy maintained or improved
- [ ] User can enable/disable feature
- [ ] Clipped input handled gracefully with warning
