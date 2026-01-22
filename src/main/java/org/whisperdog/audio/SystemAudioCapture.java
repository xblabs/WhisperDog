package org.whisperdog.audio;

import com.sun.jna.Pointer;
import xt.audio.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Captures system audio (loopback) using WASAPI via XT-Audio library.
 * This allows recording "what you hear" from the system speakers.
 */
public class SystemAudioCapture {

    private static final org.apache.logging.log4j.Logger logger =
        org.apache.logging.log4j.LogManager.getLogger(SystemAudioCapture.class);

    private static final int TARGET_SAMPLE_RATE = 16000;
    private static final int TARGET_CHANNELS = 1;

    private XtPlatform platform;
    private XtDevice device;
    private XtStream stream;
    private XtSafeBuffer safeBuffer;
    private String loopbackDeviceId;
    private ByteArrayOutputStream capturedAudio;
    private final AtomicBoolean capturing = new AtomicBoolean(false);

    // Format info for conversion
    private int deviceSampleRate;
    private int deviceChannels;

    /**
     * Check if WASAPI loopback is available on this system.
     */
    public static boolean isAvailable() {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return false;
        }
        try (XtPlatform platform = XtAudio.init("WhisperDog", Pointer.NULL)) {
            XtService service = platform.getService(Enums.XtSystem.WASAPI);
            if (service == null) return false;

            try (XtDeviceList devices = service.openDeviceList(
                    EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                for (int i = 0; i < devices.getCount(); i++) {
                    String id = devices.getId(i);
                    EnumSet<Enums.XtDeviceCaps> caps = devices.getCapabilities(id);
                    if (caps.contains(Enums.XtDeviceCaps.LOOPBACK)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("WASAPI loopback check failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * List available loopback devices.
     * @return Array of device names, or empty if none available
     */
    public static String[] listLoopbackDevices() {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return new String[0];
        }
        try (XtPlatform platform = XtAudio.init("WhisperDog", Pointer.NULL)) {
            XtService service = platform.getService(Enums.XtSystem.WASAPI);
            if (service == null) return new String[0];

            java.util.List<String> names = new java.util.ArrayList<>();
            try (XtDeviceList devices = service.openDeviceList(
                    EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                for (int i = 0; i < devices.getCount(); i++) {
                    String id = devices.getId(i);
                    EnumSet<Enums.XtDeviceCaps> caps = devices.getCapabilities(id);
                    if (caps.contains(Enums.XtDeviceCaps.LOOPBACK)) {
                        names.add(devices.getName(id));
                    }
                }
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            logger.debug("Failed to list loopback devices: {}", e.getMessage());
            return new String[0];
        }
    }

    /**
     * Initialize the capture system and find a loopback device.
     * @return true if initialization successful
     */
    public boolean initialize() {
        try {
            platform = XtAudio.init("WhisperDog", Pointer.NULL);
            XtService service = platform.getService(Enums.XtSystem.WASAPI);

            if (service == null) {
                logger.error("WASAPI service not available");
                return false;
            }

            // Find first loopback device
            try (XtDeviceList devices = service.openDeviceList(
                    EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                for (int i = 0; i < devices.getCount(); i++) {
                    String id = devices.getId(i);
                    EnumSet<Enums.XtDeviceCaps> caps = devices.getCapabilities(id);
                    if (caps.contains(Enums.XtDeviceCaps.LOOPBACK)) {
                        loopbackDeviceId = id;
                        logger.info("Found loopback device: {}", devices.getName(id));
                        break;
                    }
                }
            }

            if (loopbackDeviceId == null) {
                logger.error("No loopback device found");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to initialize system audio capture: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Start capturing system audio.
     */
    public void start() throws Exception {
        if (capturing.get()) {
            logger.warn("Already capturing");
            return;
        }

        XtService service = platform.getService(Enums.XtSystem.WASAPI);
        device = service.openDevice(loopbackDeviceId);

        // Try various format combinations - WASAPI loopback may have specific requirements
        int[] sampleRates = {48000, 44100, 32000, 16000};
        int[] channelCounts = {2, 1};
        Enums.XtSample[] sampleTypes = {
            Enums.XtSample.FLOAT32,
            Enums.XtSample.INT16,
            Enums.XtSample.INT32,
            Enums.XtSample.INT24
        };

        Structs.XtFormat format = null;
        Structs.XtMix mix = null;
        Structs.XtChannels channels = null;

        outer:
        for (int rate : sampleRates) {
            for (int chCount : channelCounts) {
                for (Enums.XtSample sampleType : sampleTypes) {
                    mix = new Structs.XtMix(rate, sampleType);
                    channels = new Structs.XtChannels(chCount, 0, 0, 0);
                    Structs.XtFormat testFormat = new Structs.XtFormat(mix, channels);

                    if (device.supportsFormat(testFormat)) {
                        format = testFormat;
                        deviceSampleRate = rate;
                        deviceChannels = chCount;
                        logger.info("Found supported format: {}Hz, {} ch, {}",
                            rate, chCount, sampleType);
                        break outer;
                    }
                }
            }
        }

        if (format == null) {
            // Log what formats we tried
            logger.error("No supported format found. Tried rates: 48k/44.1k/32k/16k, channels: 2/1, types: FLOAT32/INT16/INT32/INT24");
            throw new Exception("No supported audio format found for loopback device");
        }

        logger.info("Using format: {}Hz, {} channels, {}",
            deviceSampleRate, deviceChannels, mix.sample);

        capturedAudio = new ByteArrayOutputStream();
        capturing.set(true);

        Structs.XtBufferSize bufferSize = device.getBufferSize(format);
        double latency = bufferSize.current > 0 ? bufferSize.current : 20.0;

        Structs.XtStreamParams streamParams = new Structs.XtStreamParams(
            true, this::onBuffer, null, null);
        Structs.XtDeviceStreamParams deviceParams = new Structs.XtDeviceStreamParams(
            streamParams, format, latency);

        stream = device.openStream(deviceParams, null);
        safeBuffer = XtSafeBuffer.register(stream);
        stream.start();

        logger.info("Started system audio capture");
    }

    /**
     * Callback for audio buffer processing.
     */
    private int onBuffer(XtStream stream, Structs.XtBuffer buffer, Object user) {
        if (!capturing.get() || buffer.frames == 0) {
            return 0;
        }

        try {
            XtSafeBuffer safe = XtSafeBuffer.get(stream);
            safe.lock(buffer);

            Object input = safe.getInput();
            byte[] converted;

            if (input instanceof float[]) {
                float[] samples = (float[]) input;
                converted = convertFloatToInt16(samples, buffer.frames, deviceChannels);
            } else if (input instanceof short[]) {
                short[] samples = (short[]) input;
                converted = convertInt16(samples, buffer.frames, deviceChannels);
            } else {
                safe.unlock(buffer);
                return 0;
            }

            safe.unlock(buffer);

            synchronized (capturedAudio) {
                capturedAudio.write(converted);
            }
        } catch (Exception e) {
            logger.error("Error processing audio buffer: {}", e.getMessage());
        }

        return 0;
    }

    /**
     * Convert float samples to 16-bit PCM mono at target sample rate.
     */
    private byte[] convertFloatToInt16(float[] input, int frames, int inputChannels) {
        // Calculate output size with resampling
        int outputSamples = (int) ((long) frames * TARGET_SAMPLE_RATE / deviceSampleRate);
        ByteBuffer outBuf = ByteBuffer.allocate(outputSamples * 2).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < outputSamples; i++) {
            // Simple nearest-neighbor resampling
            int srcFrame = (int) ((long) i * deviceSampleRate / TARGET_SAMPLE_RATE);
            if (srcFrame >= frames) srcFrame = frames - 1;

            // Mix down to mono
            float sample = 0;
            for (int ch = 0; ch < inputChannels; ch++) {
                int idx = srcFrame * inputChannels + ch;
                if (idx < input.length) {
                    sample += input[idx];
                }
            }
            sample /= inputChannels;

            // Clamp and convert to 16-bit
            sample = Math.max(-1.0f, Math.min(1.0f, sample));
            outBuf.putShort((short) (sample * 32767));
        }

        return outBuf.array();
    }

    /**
     * Convert int16 samples to mono at target sample rate.
     */
    private byte[] convertInt16(short[] input, int frames, int inputChannels) {
        // Calculate output size with resampling
        int outputSamples = (int) ((long) frames * TARGET_SAMPLE_RATE / deviceSampleRate);
        ByteBuffer outBuf = ByteBuffer.allocate(outputSamples * 2).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < outputSamples; i++) {
            // Simple nearest-neighbor resampling
            int srcFrame = (int) ((long) i * deviceSampleRate / TARGET_SAMPLE_RATE);
            if (srcFrame >= frames) srcFrame = frames - 1;

            // Mix down to mono
            int sample = 0;
            for (int ch = 0; ch < inputChannels; ch++) {
                int idx = srcFrame * inputChannels + ch;
                if (idx < input.length) {
                    sample += input[idx];
                }
            }
            sample /= inputChannels;

            // Clamp
            sample = Math.max(-32768, Math.min(32767, sample));
            outBuf.putShort((short) sample);
        }

        return outBuf.array();
    }

    /**
     * Stop capturing and return the captured audio.
     * @return Captured audio as 16-bit PCM, 16kHz, mono
     */
    public byte[] stop() {
        if (!capturing.get()) {
            return new byte[0];
        }

        capturing.set(false);

        if (stream != null) {
            try {
                stream.stop();
            } catch (Exception e) {
                logger.debug("Error stopping stream: {}", e.getMessage());
            }
        }

        if (safeBuffer != null) {
            safeBuffer.close();
            safeBuffer = null;
        }

        if (stream != null) {
            stream.close();
            stream = null;
        }

        if (device != null) {
            device.close();
            device = null;
        }

        synchronized (capturedAudio) {
            return capturedAudio.toByteArray();
        }
    }

    /**
     * Release all resources.
     */
    public void dispose() {
        stop();
        if (platform != null) {
            platform.close();
            platform = null;
        }
    }

    /**
     * Check if currently capturing.
     */
    public boolean isCapturing() {
        return capturing.get();
    }

    // =========================================================================
    // PoC: Main method for testing
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.out.println("=== System Audio Capture PoC ===\n");

        // Check availability
        System.out.println("Checking WASAPI loopback availability...");
        if (!isAvailable()) {
            System.out.println("ERROR: WASAPI loopback not available on this system.");
            return;
        }
        System.out.println("OK: WASAPI loopback is available.\n");

        // List devices
        System.out.println("Available loopback devices:");
        String[] devices = listLoopbackDevices();
        for (int i = 0; i < devices.length; i++) {
            System.out.println("  [" + i + "] " + devices[i]);
        }
        System.out.println();

        // Capture 10 seconds
        System.out.println("Starting 10-second capture...");
        System.out.println("(Play some audio to capture it)\n");

        SystemAudioCapture capture = new SystemAudioCapture();
        if (!capture.initialize()) {
            System.out.println("ERROR: Failed to initialize capture.");
            return;
        }

        capture.start();

        // Wait 10 seconds with countdown
        for (int i = 10; i > 0; i--) {
            System.out.println("  Capturing... " + i + "s remaining");
            Thread.sleep(1000);
        }

        byte[] audioData = capture.stop();
        capture.dispose();

        System.out.println("\nCapture complete!");
        System.out.println("  Captured: " + audioData.length + " bytes");
        System.out.println("  Duration: " + String.format("%.2f", audioData.length / 2.0 / TARGET_SAMPLE_RATE) + " seconds");

        // Save to WAV file
        Path tempFile = Files.createTempFile("whisperdog_poc_", ".wav");
        writeWav(tempFile.toFile(), audioData, TARGET_SAMPLE_RATE, TARGET_CHANNELS);
        System.out.println("  Saved to: " + tempFile.toAbsolutePath());
    }

    /**
     * Write PCM data to WAV file.
     */
    private static void writeWav(File file, byte[] pcmData, int sampleRate, int channels)
            throws IOException {
        int byteRate = sampleRate * channels * 2;
        int blockAlign = channels * 2;

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            // RIFF header
            out.writeBytes("RIFF");
            out.writeInt(Integer.reverseBytes(36 + pcmData.length));
            out.writeBytes("WAVE");

            // fmt chunk
            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16)); // chunk size
            out.writeShort(Short.reverseBytes((short) 1)); // PCM
            out.writeShort(Short.reverseBytes((short) channels));
            out.writeInt(Integer.reverseBytes(sampleRate));
            out.writeInt(Integer.reverseBytes(byteRate));
            out.writeShort(Short.reverseBytes((short) blockAlign));
            out.writeShort(Short.reverseBytes((short) 16)); // bits per sample

            // data chunk
            out.writeBytes("data");
            out.writeInt(Integer.reverseBytes(pcmData.length));
            out.write(pcmData);
        }
    }
}
