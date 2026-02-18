package org.whisperdog.recording;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

/**
 * Writes PCM audio to a WAV file incrementally and keeps the header sizes updated.
 */
public class IncrementalWavWriter implements AutoCloseable {

    private static final int WAV_HEADER_SIZE = 44;
    private static final long MAX_WAV_DATA_SIZE = 0xFFFFFFFFL;

    private final File outputFile;
    private final RandomAccessFile wavFile;
    private final int sampleRate;
    private final int bitsPerSample;
    private final int channels;
    private final int byteRate;
    private final short blockAlign;

    private long bytesWritten;
    private boolean closed;

    public IncrementalWavWriter(File outputFile, int sampleRate, int bitsPerSample, int channels)
            throws IOException {
        this.outputFile = Objects.requireNonNull(outputFile, "outputFile");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be > 0");
        }
        if (bitsPerSample <= 0 || (bitsPerSample % 8) != 0) {
            throw new IllegalArgumentException("bitsPerSample must be a positive multiple of 8");
        }
        if (channels <= 0) {
            throw new IllegalArgumentException("channels must be > 0");
        }

        this.sampleRate = sampleRate;
        this.bitsPerSample = bitsPerSample;
        this.channels = channels;
        this.byteRate = sampleRate * channels * (bitsPerSample / 8);
        this.blockAlign = (short) (channels * (bitsPerSample / 8));

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IOException("Failed to create parent directory: " + parent);
        }

        this.wavFile = new RandomAccessFile(outputFile, "rw");
        try {
            this.wavFile.setLength(0);
            writeHeader();
        } catch (IOException e) {
            this.wavFile.close();
            throw e;
        }
    }

    public synchronized void write(byte[] pcmData, int offset, int length) throws IOException {
        ensureOpen();
        if (pcmData == null) {
            throw new NullPointerException("pcmData");
        }
        if (offset < 0 || length < 0 || offset + length > pcmData.length) {
            throw new IndexOutOfBoundsException(
                "Invalid offset/length: offset=" + offset + ", length=" + length + ", size=" + pcmData.length
            );
        }
        if (length == 0) {
            return;
        }

        long newSize = bytesWritten + length;
        if (newSize > MAX_WAV_DATA_SIZE) {
            throw new IOException("WAV data exceeds 4GB limit");
        }

        wavFile.seek(WAV_HEADER_SIZE + bytesWritten);
        wavFile.write(pcmData, offset, length);
        bytesWritten = newSize;
        updateHeaderSizes();
        wavFile.seek(WAV_HEADER_SIZE + bytesWritten);
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        IOException closeError = null;
        try {
            updateHeaderSizes();
        } catch (IOException e) {
            closeError = e;
        }
        try {
            wavFile.close();
        } catch (IOException e) {
            if (closeError == null) {
                closeError = e;
            }
        }
        closed = true;
        if (closeError != null) {
            throw closeError;
        }
    }

    public File getFile() {
        return outputFile;
    }

    public synchronized long getBytesWritten() {
        return bytesWritten;
    }

    private void writeHeader() throws IOException {
        wavFile.seek(0);
        writeAscii("RIFF");
        writeIntLE(36); // placeholder, updated as data is appended
        writeAscii("WAVE");

        writeAscii("fmt ");
        writeIntLE(16);
        writeShortLE((short) 1); // PCM
        writeShortLE((short) channels);
        writeIntLE(sampleRate);
        writeIntLE(byteRate);
        writeShortLE(blockAlign);
        writeShortLE((short) bitsPerSample);

        writeAscii("data");
        writeIntLE(0); // placeholder, updated as data is appended
    }

    private void updateHeaderSizes() throws IOException {
        long dataSize = bytesWritten;
        long chunkSize = 36 + dataSize;

        wavFile.seek(4);
        writeIntLE((int) chunkSize);
        wavFile.seek(40);
        writeIntLE((int) dataSize);
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Writer is already closed");
        }
    }

    private void writeAscii(String value) throws IOException {
        wavFile.writeBytes(value);
    }

    private void writeIntLE(int value) throws IOException {
        wavFile.writeInt(Integer.reverseBytes(value));
    }

    private void writeShortLE(short value) throws IOException {
        wavFile.writeShort(Short.reverseBytes(value));
    }
}
