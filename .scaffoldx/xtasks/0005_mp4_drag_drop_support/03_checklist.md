# MP4 Drag-Drop Support - Execution Checklist

## Phase 1: FFmpeg Utility
- [ ] Create FFmpegUtil class with isFFmpegAvailable() method
- [ ] Add isVideoFile() method with extension detection
- [ ] Implement extractAudio() async method with progress callback
- [ ] Unit test ffmpeg detection and video file identification

## Phase 2: Drop Handler
- [ ] Modify RecorderForm drop handler to detect video files
- [ ] Add handleVideoFile() method for video-specific flow
- [ ] Show "Extracting audio..." status during extraction
- [ ] Disable controls during extraction
- [ ] Feed extracted audio into existing transcription pipeline

## Phase 3: Error Handling
- [ ] Handle missing ffmpeg with helpful error message
- [ ] Handle extraction failures gracefully
- [ ] Handle videos with no audio track

## Phase 4: Cleanup
- [ ] Track temp files created during extraction
- [ ] Clean up temp files after transcription completes
- [ ] Clean up on application exit (safety net)
- [ ] Clean up on extraction cancellation/failure

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
