# Mic Test Screen - Execution Checklist

## Phase 1: Audio Analysis Utilities
- [x] Create AudioAnalyzer class with RMS and peak calculation methods
- [x] Create SilenceRemover class with silence detection and removal
- [ ] Unit test RMS calculation with known audio samples

## Phase 2: Mic Test Panel UI
- [x] Create MicTestPanel with recording controls (Record/Stop buttons)
- [x] Implement real-time RMS meter during recording
- [x] Add metrics display (RMS, peak, silence ratio, non-silence duration)
- [x] Add A/B playback buttons for original and filtered audio
- [x] Create threshold adjustment sliders with live re-processing
- [x] Add Apply and Reset Defaults buttons

## Phase 3: Integration
- [x] Add "Test Microphone" button to Settings → Audio tab
- [x] Open MicTestPanel in modal dialog
- [x] Verify settings persist after Apply

## Phase 4: Testing & Verification
- [ ] Test recording starts/stops correctly
- [ ] Test RMS meter updates smoothly during recording
- [ ] Test auto-stop at 10 seconds
- [ ] Test playback of both original and filtered audio
- [ ] Test slider changes trigger re-processing
- [ ] Verify works in light and dark themes

---

**Note**: Full implementation code in `02_implementation_plan.md`
