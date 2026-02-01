# Checklist: Synchronized Dual-Source Silence Removal

## Phase 1: Intersection-Based Silence Detection
- [ ] Add `SilenceRegion` data class to SilenceRemover (startFrame, endFrame, duration)
- [ ] Extract `detectSilenceRegions()` returning `List<SilenceRegion>` from existing logic
- [ ] Implement `intersectRegions()` to compute overlap between two region lists
- [ ] Implement `spliceFrames()` to remove frame ranges from a WAV byte array
- [ ] Implement `removeSynchronizedSilence(micFile, sysFile, ...)` entry point
- [ ] Update RecorderForm dual-source path to call synchronized removal before merge
- [ ] Add unit tests for region intersection (overlapping, non-overlapping, partial)
- [ ] Add unit tests for frame splicing (verify frame counts match in both outputs)
- [ ] Add integration test with synthetic dual-source audio
- [ ] Test with real dual-source recordings

## Phase 2: Zero-Crossing Alignment
- [ ] Implement zero-crossing snap for cut boundaries (within 5ms / ±110 samples window)
- [ ] Add listening test for click-free playback
- [ ] Verify source attribution still works on pruned+merged output
- [ ] DEFERRED: Crossfade at splice boundaries (out of scope for Phase 2 MVP)

## Validation & Release
- [ ] Run full test suite, ensure all pass
- [ ] Benchmark performance (< 100ms overhead for 60s recording)
- [ ] Verify backward compatibility with mic-only silence removal
- [ ] Test edge cases (no common silence, very short recordings, mismatched lengths)
