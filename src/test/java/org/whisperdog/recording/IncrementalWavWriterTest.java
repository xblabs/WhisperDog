package org.whisperdog.recording;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalWavWriterTest {

    @Test
    void updatesHeaderOnEveryWriteAndOnClose() throws Exception {
        File wavFile = Files.createTempFile("incremental-wav-writer-", ".wav").toFile();
        wavFile.deleteOnExit();

        byte[] chunk1 = buildSinePcm(4096);
        byte[] chunk2 = buildSinePcm(2048);

        try (IncrementalWavWriter writer = new IncrementalWavWriter(wavFile, 16000, 16, 1)) {
            writer.write(chunk1, 0, chunk1.length);
            assertHeaderSizes(wavFile, chunk1.length);
            assertEquals(chunk1.length, writer.getBytesWritten());

            writer.write(chunk2, 0, chunk2.length);
            assertHeaderSizes(wavFile, chunk1.length + chunk2.length);
            assertEquals(chunk1.length + chunk2.length, writer.getBytesWritten());
        }

        long totalBytes = chunk1.length + chunk2.length;
        assertHeaderSizes(wavFile, totalBytes);
        assertTrue(wavFile.length() >= 44 + totalBytes);
    }

    private static byte[] buildSinePcm(int byteCount) {
        int sampleCount = byteCount / 2;
        ByteBuffer buffer = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < sampleCount; i++) {
            double angle = 2.0 * Math.PI * 440.0 * i / 16000.0;
            short sample = (short) (Math.sin(angle) * 32767.0);
            buffer.putShort(sample);
        }
        return buffer.array();
    }

    private static void assertHeaderSizes(File wavFile, long expectedDataSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "r")) {
            raf.seek(4);
            int chunkSize = Integer.reverseBytes(raf.readInt());
            raf.seek(40);
            int subchunk2Size = Integer.reverseBytes(raf.readInt());

            assertEquals(expectedDataSize, Integer.toUnsignedLong(subchunk2Size));
            assertEquals(36 + expectedDataSize, Integer.toUnsignedLong(chunkSize));
        }
    }
}
