# Mic Test Screen - Execution Checklist

## Phase 1: Audio Analysis Utilities
- [ ] Create AudioAnalyzer class with RMS and peak calculation methods
- [ ] Create SilenceRemover class with silence detection and removal
- [ ] Unit test RMS calculation with known audio samples

## Phase 2: Mic Test Panel UI
- [ ] Create MicTestPanel with recording controls (Record/Stop buttons)
- [ ] Implement real-time RMS meter during recording
- [ ] Add metrics display (RMS, peak, silence ratio, non-silence duration)
- [ ] Add A/B playback buttons for original and filtered audio
- [ ] Create threshold adjustment sliders with live re-processing
- [ ] Add Apply and Reset Defaults buttons

## Phase 3: Integration
- [ ] Add "Test Microphone" button to Settings → Audio tab
- [ ] Open MicTestPanel in modal dialog
- [ ] Verify settings persist after Apply

## Phase 4: Testing & Verification
- [ ] Test recording starts/stops correctly
- [ ] Test RMS meter updates smoothly during recording
- [ ] Test auto-stop at 10 seconds
- [ ] Test playback of both original and filtered audio
- [ ] Test slider changes trigger re-processing
- [ ] Verify works in light and dark themes

---

**Note**: Full implementation code in `02_implementation_plan.md`
