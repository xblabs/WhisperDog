# Implementation Plan: Incremental Audio Writes for Crash Resilience

## New Classes

### 1. IncrementalWavWriter

**Package**: `org.whisperdog.recording`
**File**: `src/main/java/org/whisperdog/recording/IncrementalWavWriter.java`

Writes PCM audio data incrementally to a WAV file, updating the header on every write so the file is always playable.

```java
public class IncrementalWavWriter implements AutoCloseable {

    /**
     * @param outputFile  target WAV file (created or overwritten)
     * @param sampleRate  e.g. 16000 (post-conversion output format; derive from actual converted stream)
     * @param bitsPerSample  typically 16
     * @param channels  1 (mono) or 2 (stereo)
     * @throws IOException if file cannot be opened
     */
    public IncrementalWavWriter(File outputFile, int sampleRate, int bitsPerSample, int channels)
        throws IOException

    /**
     * Append PCM audio data and update WAV header sizes.
     * Thread-safe (synchronized). Called from WASAPI audio callback thread.
     *
     * @param pcmData  raw PCM bytes to append
     * @param offset   start offset in pcmData
     * @param length   number of bytes to write
     * @throws IOException if write fails
     */
    public synchronized void write(byte[] pcmData, int offset, int length) throws IOException

    /**
     * Finalize the WAV header with correct sizes and close the file.
     * Called on normal recording stop.
     * After this call, the file is a complete, standard WAV.
     */
    @Override
    public synchronized void close() throws IOException

    /** @return the output file reference */
    public File getFile()

    /** @return total PCM bytes written so far */
    public long getBytesWritten()
}
```

**WAV header layout** (44 bytes):

| Offset | Size | Field | Value |
| ------ | ---- | ----- | ----- |
| 0 | 4 | ChunkID | "RIFF" |
| 4 | 4 | ChunkSize | `36 + dataSize` (updated on every write) |
| 8 | 4 | Format | "WAVE" |
| 12 | 4 | Subchunk1ID | "fmt " |
| 16 | 4 | Subchunk1Size | 16 |
| 20 | 2 | AudioFormat | 1 (PCM) |
| 22 | 2 | NumChannels | channels |
| 24 | 4 | SampleRate | sampleRate |
| 28 | 4 | ByteRate | sampleRate * channels * bitsPerSample/8 |
| 32 | 2 | BlockAlign | channels * bitsPerSample/8 |
| 34 | 2 | BitsPerSample | bitsPerSample |
| 36 | 4 | Subchunk2ID | "data" |
| 40 | 4 | Subchunk2Size | dataSize (updated on every write) |
| 44 | ... | Data | PCM audio bytes |

**Write cycle**: append PCM data at current file position, seek to offset 4, write updated ChunkSize, seek to offset 40, write updated Subchunk2Size, seek back to end. Uses `RandomAccessFile` for seeking.

## Modified Classes

### 2. SystemAudioCapture

**File**: `src/main/java/org/whisperdog/audio/SystemAudioCapture.java`

**Changes**:

- **Line 322**: Replace `capturedAudio = new ByteArrayOutputStream()` with `IncrementalWavWriter` initialization
- **Lines 388-390**: Replace `capturedAudio.write(converted)` with `writer.write(converted, 0, converted.length)`
- **Lines 555-557**: Replace `return capturedAudio.toByteArray()` with `writer.close(); return writer.getFile()`
- **Return type of stop()**: Changes from `byte[]` to `File`

**New field**: `private IncrementalWavWriter writer;` (replaces `private ByteArrayOutputStream capturedAudio;`)

**Import**: `import org.whisperdog.recording.IncrementalWavWriter;` (cross-package import; writer lives in `recording` package, SystemAudioCapture lives in `audio` package)

**Writer initialization**: Must happen after WASAPI format negotiation (audio format is known only after loopback starts). Initialize writer in the first invocation of the audio callback (around lines 380-390), guarded by a null check on the writer field, using the post-conversion format parameters (16kHz, 16-bit, mono).

**Error handling**: If `writer.write()` throws IOException in the audio callback:
- Log the error
- Set an error flag
- Do NOT throw from the callback (WASAPI thread must not be interrupted)
- On `stop()`, check error flag and report to caller

### 3. AudioCaptureManager

**File**: `src/main/java/org/whisperdog/audio/AudioCaptureManager.java`

**Changes**:

- **Lines 157-162**: Replace `writeWavFile(systemCapture.stop())` with direct file reference from `systemCapture.stop()` (which now returns `File` instead of `byte[]`)
- **Remove `writeWavFile()` method** (lines 304-330): No longer needed. The `IncrementalWavWriter` handles WAV writing.
- **`stopCapture()` return**: Already returns `File` for mic track. System track now also returns `File` directly from `SystemAudioCapture.stop()`.

### 4. AudioRecorder (Part 2 - optional)

**File**: `src/main/java/org/whisperdog/recording/AudioRecorder.java`

**Approach decision**: Replace `AudioSystem.write()` with `IncrementalWavWriter` OR defer to post-crash header repair in task 0017's scanner.

If replacing:
- **Line 44**: Replace `AudioSystem.write(ais, Type.WAVE, wavFile)` with manual read loop from `TargetDataLine` into `IncrementalWavWriter`
- Read buffer: 4096 bytes per read (standard for `TargetDataLine`)
- Audio format: known from `TargetDataLine.getFormat()` at line 32

If deferring:
- No changes to `AudioRecorder.java`
- Task 0017's `PreservedRecordingScanner` adds header repair logic for WAVs where `ChunkSize == 0` or `ChunkSize == -1`

## Files Summary

| Action | File | Change |
| ------ | ---- | ------ |
| CREATE | `src/.../recording/IncrementalWavWriter.java` | New class in `org.whisperdog.recording` |
| MODIFY | `src/.../audio/SystemAudioCapture.java` | Replace ByteArrayOutputStream with IncrementalWavWriter |
| MODIFY | `src/.../audio/AudioCaptureManager.java` | Consume File instead of byte[], remove writeWavFile() |
| MODIFY (optional) | `src/.../recording/AudioRecorder.java` | Replace AudioSystem.write() with IncrementalWavWriter |
