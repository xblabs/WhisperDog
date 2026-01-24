# MP4 Drag-Drop Support - Execution Checklist

## Phase 1: FFmpeg Utility
- [x] Create FFmpegUtil class with isFFmpegAvailable() method
- [x] Add isVideoFile() method with extension detection
- [x] Implement extractAudio() async method with progress callback
- [ ] Unit test ffmpeg detection and video file identification

## Phase 2: Drop Handler
- [x] Modify RecorderForm drop handler to detect video files
- [x] Add handleVideoFile() method for video-specific flow
- [x] Show "Extracting audio..." status during extraction
- [x] Disable controls during extraction
- [x] Feed extracted audio into existing transcription pipeline

## Phase 3: Error Handling
- [x] Handle missing ffmpeg with helpful error message
- [x] Handle extraction failures gracefully
- [x] Handle videos with no audio track

## Phase 4: Cleanup
- [x] Track temp files created during extraction
- [x] Clean up temp files after transcription completes
- [x] Clean up on application exit (safety net via deleteOnExit)
- [x] Clean up on extraction cancellation/failure

## Phase 5: Testing & Verification
- [ ] Test with MP4 file
- [ ] Test with MOV file
- [ ] Test with MKV file
- [ ] Test error when ffmpeg not installed
- [ ] Test with video that has no audio
- [ ] Verify temp files are cleaned up
- [ ] Test in both light and dark themes

---

**Note**: Full implementation code in `02_implementation_plan.md`
