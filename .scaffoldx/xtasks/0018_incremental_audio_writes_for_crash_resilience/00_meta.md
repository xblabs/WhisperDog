---
task:
  id: '0018'
  status: active
  priority: medium
  progress: 45
  title: Incremental audio writes for crash resilience
  last_updated: '2026-02-18 12:17:39'
  description: >-
    Replace in-memory ByteArrayOutputStream buffering in SystemAudioCapture.java
    with incremental disk writes so system audio survives JVM crashes. Add WAV
    header resilience for mic path. Split from task 0017.
  checklist:
    total_items: 31
    completed_items: 14
    completion_percentage: 45
  latest_work: 'Updated via x-task-update: progress: 45%'

---



# Task 0018: Incremental audio writes for crash resilience

Split from task 0017 (which now covers retry UI only). Two crash-loss vectors exist in the current recording architecture:

1. **System audio path** (`SystemAudioCapture.java`): All captured audio is buffered in a `ByteArrayOutputStream` during recording and written to disk only on `stop()`. JVM crash mid-recording = total loss of system audio track.
2. **Mic audio path** (`AudioRecorder.java`): Streams to disk via `AudioSystem.write()` during recording. The WAV exists on disk, but the RIFF/data chunk sizes in the header are only finalized when the stream completes. JVM crash mid-recording = file on disk with invalid header (unplayable without repair).

The mic path has LOW crash-loss risk (data is on disk, header is fixable). The system audio path has HIGH crash-loss risk (data is entirely in memory).
