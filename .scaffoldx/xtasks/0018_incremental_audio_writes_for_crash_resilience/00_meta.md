---
id: "0018"
name: Incremental audio writes for crash resilience
status: todo
priority: medium
tags: [recording, crash-resilience, system-audio, data-loss]
created: 2026-02-18
related_issues: [ISS_00012]
related_tasks: ["0017"]
---

# Task 0018: Incremental audio writes for crash resilience

Split from task 0017 (which now covers retry UI only). Two crash-loss vectors exist in the current recording architecture:

1. **System audio path** (`SystemAudioCapture.java`): All captured audio is buffered in a `ByteArrayOutputStream` during recording and written to disk only on `stop()`. JVM crash mid-recording = total loss of system audio track.
2. **Mic audio path** (`AudioRecorder.java`): Streams to disk via `AudioSystem.write()` during recording. The WAV exists on disk, but the RIFF/data chunk sizes in the header are only finalized when the stream completes. JVM crash mid-recording = file on disk with invalid header (unplayable without repair).

The mic path has LOW crash-loss risk (data is on disk, header is fixable). The system audio path has HIGH crash-loss risk (data is entirely in memory).
