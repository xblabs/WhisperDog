# ISS-0006-001: In-Memory Buffering Tradeoff Analysis

**Status**: Deferred (Do on separate branch)
**Branch**: `feature/file-streaming-capture` (create when implementing)
**Priority**: Medium
**Created**: 2026-01-25
**Source**: Codex audit (continuous-use sluggishness)

---

## Problem Statement

`SystemAudioCapture` uses `ByteArrayOutputStream` to buffer the entire loopback stream in memory before writing to disk. Long captures can spike heap usage and GC pressure, making the app feel sluggish.

**References**:
- `SystemAudioCapture.java:37` - ByteArrayOutputStream field
- `SystemAudioCapture.java:295` - Created in start()
- `SystemAudioCapture.java:382` - Written to in onBuffer()
- `SystemAudioCapture.java:519` - Converted to byte[] in stop()

---

## Memory Impact Estimates

| Duration | Sample Rate | Bits | Channels | Memory |
|----------|-------------|------|----------|--------|
| 10 min   | 16 kHz      | 16   | 1        | ~19 MB |
| 30 min   | 16 kHz      | 16   | 1        | ~58 MB |
| 1 hour   | 16 kHz      | 16   | 1        | ~115 MB |
| 2 hours  | 16 kHz      | 16   | 1        | ~230 MB |

---

## Proposed Solution: Direct File Streaming

Refactor `SystemAudioCapture` to write directly to a temp file instead of buffering in memory.

### Changes Required

1. Accept output `File` parameter in `start(File outputFile)`
2. Replace `ByteArrayOutputStream capturedAudio` with `BufferedOutputStream` to file
3. Write WAV header at start (with placeholder size)
4. Update WAV header with actual size in `stop()`
5. Return `File` from `stop()` instead of `byte[]`
6. Update `AudioCaptureManager.stopCapture()` to handle the new API

### API Change

```java
// Current
public void start() throws Exception;
public byte[] stop();

// Proposed
public void start(File outputFile) throws Exception;
public File stop();
```

---

## Pros and Cons

### Pros of Current (In-Memory)
- Simpler code, no file I/O complexity in callback
- No risk of file write failures during capture
- Data immediately available as byte[] for processing

### Cons of Current (In-Memory)
- Memory grows unbounded with capture duration
- GC pressure from large byte arrays
- Could cause OOM on very long captures

### Pros of File Streaming
- Constant memory usage regardless of duration
- Supports arbitrarily long captures
- Data persisted even if app crashes mid-capture

### Cons of File Streaming
- More complex code (WAV header management)
- I/O errors possible during capture
- Slightly slower due to disk I/O (though buffered)
- Changes API signature (breaking change for any callers)

---

## Recommendation

**Defer until user reports actual sluggishness**. Current ~58MB/30min is manageable for typical use cases. Most whisper transcriptions are short recordings (1-10 minutes). Monitor for user reports of long-capture issues before investing in refactor.

If implementing:
- Use `BufferedOutputStream` with 64KB buffer to minimize syscalls
- Pre-allocate file to expected size if duration known
- Consider hybrid: buffer first N minutes, then switch to file

---

## Related

- Audit finding: "Medium" priority
- Affects: Long recording sessions (30+ minutes)
- Does NOT affect: Typical short recordings
