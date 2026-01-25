package org.whisperdog.audio;

import com.sun.jna.Pointer;
import xt.audio.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.EnumSet;

/**
 * Cross-platform utility for audio device detection and information.
 * Provides methods to detect default devices, check availability, and format names for display.
 */
public class AudioDeviceInfo {

    private static final org.apache.logging.log4j.Logger logger =
        org.apache.logging.log4j.LogManager.getLogger(AudioDeviceInfo.class);

    // Cache for default output device name (short TTL via lazy refresh)
    private static String cachedDefaultOutput = null;
    private static long cachedDefaultOutputTime = 0;
    private static final long CACHE_TTL_MS = 10000; // 10 seconds

    /**
     * Clear the cached default output device name.
     * Call this when you expect the device may have changed (e.g., window focus gained).
     */
    public static void clearCache() {
        cachedDefaultOutput = null;
        cachedDefaultOutputTime = 0;
    }

    /**
     * Get the name of the default/active audio output device.
     * Platform-specific implementation:
     * - Windows: WASAPI via XT-Audio
     * - Linux: pactl (PulseAudio)
     * - macOS: system_profiler
     *
     * @return Device name, or null if detection fails
     */
    public static String getDefaultOutputDeviceName() {
        // Check cache
        long now = System.currentTimeMillis();
        if (cachedDefaultOutput != null && (now - cachedDefaultOutputTime) < CACHE_TTL_MS) {
            return cachedDefaultOutput;
        }

        String result = null;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("windows")) {
            result = getDefaultOutputWindows();
        } else if (os.contains("linux")) {
            result = getDefaultOutputLinux();
        } else if (os.contains("mac")) {
            result = getDefaultOutputMacOS();
        }

        // Update cache
        cachedDefaultOutput = result;
        cachedDefaultOutputTime = now;

        return result;
    }

    /**
     * Windows: Get default output via Core Audio API (PowerShell).
     * Uses the Windows Core Audio API through COM interop to get the actual default device.
     */
    private static String getDefaultOutputWindows() {
        // Use PowerShell with Core Audio API for accurate default detection
        String psResult = getDefaultOutputViaPowerShell();
        if (psResult != null && !psResult.isEmpty()) {
            return psResult;
        }

        // Fallback to WASAPI enumeration (less accurate - just takes first device)
        logger.debug("PowerShell detection failed, falling back to WASAPI enumeration");
        return getDefaultOutputWasapiFallback();
    }

    /**
     * Use PowerShell with Windows Core Audio API to get the actual default output device.
     * Creates a temp script file for reliable execution.
     */
    private static String getDefaultOutputViaPowerShell() {
        java.io.File tempScript = null;
        try {
            // Create temp PowerShell script file (avoids command-line escaping issues)
            tempScript = java.io.File.createTempFile("whisperdog_audio_", ".ps1");

            // PowerShell script using Core Audio API - written to temp file to avoid escaping issues
            String psScript = "Add-Type -TypeDefinition @\"\r\n" +
                "using System;\r\n" +
                "using System.Runtime.InteropServices;\r\n" +
                "\r\n" +
                "[ComImport, Guid(\"BCDE0395-E52F-467C-8E3D-C4579291692E\")]\r\n" +
                "internal class MMDeviceEnumerator {}\r\n" +
                "\r\n" +
                "[Guid(\"A95664D2-9614-4F35-A746-DE8DB63617E6\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\r\n" +
                "internal interface IMMDeviceEnumerator {\r\n" +
                "    int NotImpl1();\r\n" +
                "    [PreserveSig] int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice ppDevice);\r\n" +
                "}\r\n" +
                "\r\n" +
                "[Guid(\"D666063F-1587-4E43-81F1-B948E807363F\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\r\n" +
                "internal interface IMMDevice {\r\n" +
                "    int NotImpl1();\r\n" +
                "    [PreserveSig] int OpenPropertyStore(int stgmAccess, out IPropertyStore ppProperties);\r\n" +
                "}\r\n" +
                "\r\n" +
                "[Guid(\"886d8eeb-8cf2-4446-8d02-cdba1dbdcf99\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\r\n" +
                "internal interface IPropertyStore {\r\n" +
                "    int GetCount(out int count);\r\n" +
                "    int GetAt(int iProp, out PropertyKey pkey);\r\n" +
                "    [PreserveSig] int GetValue(ref PropertyKey key, out PropVariant pv);\r\n" +
                "}\r\n" +
                "\r\n" +
                "[StructLayout(LayoutKind.Sequential)]\r\n" +
                "public struct PropertyKey {\r\n" +
                "    public Guid fmtid;\r\n" +
                "    public int pid;\r\n" +
                "}\r\n" +
                "\r\n" +
                "[StructLayout(LayoutKind.Sequential)]\r\n" +
                "public struct PropVariant {\r\n" +
                "    public ushort vt;\r\n" +
                "    public ushort r1, r2, r3;\r\n" +
                "    public IntPtr p;\r\n" +
                "    public int p2;\r\n" +
                "}\r\n" +
                "\r\n" +
                "public class AudioHelper {\r\n" +
                "    public static string GetDefaultOutputName() {\r\n" +
                "        try {\r\n" +
                "            var enumerator = (IMMDeviceEnumerator)new MMDeviceEnumerator();\r\n" +
                "            IMMDevice device;\r\n" +
                "            int hr = enumerator.GetDefaultAudioEndpoint(0, 1, out device);\r\n" +
                "            if (hr != 0 || device == null) return null;\r\n" +
                "            IPropertyStore store;\r\n" +
                "            device.OpenPropertyStore(0, out store);\r\n" +
                "            var key = new PropertyKey {\r\n" +
                "                fmtid = new Guid(\"a45c254e-df1c-4efd-8020-67d146a850e0\"),\r\n" +
                "                pid = 2\r\n" +
                "            };\r\n" +
                "            PropVariant pv;\r\n" +
                "            store.GetValue(ref key, out pv);\r\n" +
                "            return Marshal.PtrToStringUni(pv.p);\r\n" +
                "        } catch { return null; }\r\n" +
                "    }\r\n" +
                "}\r\n" +
                "\"@\r\n" +
                "[AudioHelper]::GetDefaultOutputName()";

            // Write script to temp file
            try (java.io.FileWriter writer = new java.io.FileWriter(tempScript)) {
                writer.write(psScript);
            }

            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", tempScript.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String result = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Skip empty lines and PowerShell noise
                    if (!line.isEmpty() && !line.startsWith("At line:") && !line.contains("Exception")) {
                        result = line;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && result != null && !result.isEmpty()) {
                logger.debug("Windows default output (PowerShell): {}", result);
                return result;
            }
        } catch (Exception e) {
            logger.debug("PowerShell detection failed: {}", e.getMessage());
        } finally {
            // Clean up temp file
            if (tempScript != null && tempScript.exists()) {
                tempScript.delete();
            }
        }
        return null;
    }

    /**
     * Fallback: Get first output device via WASAPI (not necessarily the default).
     */
    private static String getDefaultOutputWasapiFallback() {
        try (XtPlatform platform = XtAudio.init("WhisperDog", Pointer.NULL)) {
            XtService service = platform.getService(Enums.XtSystem.WASAPI);
            if (service == null) return null;

            try (XtDeviceList outputs = service.openDeviceList(
                    EnumSet.of(Enums.XtEnumFlags.OUTPUT))) {
                if (outputs.getCount() > 0) {
                    String id = outputs.getId(0);
                    String name = outputs.getName(id);
                    logger.debug("WASAPI fallback output device: {}", name);
                    return name;
                }
            }
        } catch (Exception e) {
            logger.debug("WASAPI fallback failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Linux: Get default sink via pactl (PulseAudio/PipeWire).
     */
    private static String getDefaultOutputLinux() {
        try {
            ProcessBuilder pb = new ProcessBuilder("pactl", "info");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Default Sink:")) {
                        String sinkName = line.substring("Default Sink:".length()).trim();
                        logger.debug("Linux default output (sink): {}", sinkName);
                        // Try to get friendly name
                        return getLinuxSinkDescription(sinkName);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            logger.debug("Could not detect Linux default output: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get friendly description for a PulseAudio sink.
     */
    private static String getLinuxSinkDescription(String sinkName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("pactl", "list", "sinks");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean foundSink = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Name: " + sinkName)) {
                        foundSink = true;
                    } else if (foundSink && line.trim().startsWith("Description:")) {
                        String desc = line.substring(line.indexOf(":") + 1).trim();
                        return desc.isEmpty() ? sinkName : desc;
                    } else if (foundSink && line.startsWith("Sink #")) {
                        // Moved to next sink without finding description
                        break;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            logger.debug("Could not get Linux sink description: {}", e.getMessage());
        }
        return sinkName;
    }

    /**
     * macOS: Get default output via system_profiler.
     */
    private static String getDefaultOutputMacOS() {
        try {
            ProcessBuilder pb = new ProcessBuilder("system_profiler", "SPAudioDataType");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                String currentDevice = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Device names appear as headers ending with ":"
                    if (line.endsWith(":") && !line.startsWith("Audio") && !line.isEmpty()) {
                        currentDevice = line.substring(0, line.length() - 1);
                    }
                    // Look for "Default Output Device: Yes"
                    if (line.contains("Default Output Device: Yes") && currentDevice != null) {
                        logger.debug("macOS default output device: {}", currentDevice);
                        return currentDevice;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            logger.debug("Could not detect macOS default output: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Check if a microphone with the given name is currently available.
     * Uses Java AudioSystem for cross-platform support.
     *
     * @param microphoneName The microphone name (as stored in config)
     * @return true if the microphone is available
     */
    public static boolean isMicrophoneAvailable(String microphoneName) {
        if (microphoneName == null || microphoneName.isEmpty()) {
            return false;
        }

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixers) {
            // Match by name prefix (config stores "Name Description: ...")
            if (microphoneName.startsWith(info.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format a device name for UI display, truncating if necessary.
     *
     * @param fullName The full device name
     * @param maxLength Maximum display length
     * @return Formatted name with ellipsis if truncated
     */
    public static String formatDeviceNameForDisplay(String fullName, int maxLength) {
        if (fullName == null) {
            return "Unknown";
        }

        // Clean up common verbose suffixes
        String cleaned = fullName;

        // Remove common Windows audio device suffixes for cleaner display
        if (cleaned.contains(" (") && cleaned.endsWith(")")) {
            // Keep brand/type info in parentheses if short, otherwise trim
            int parenStart = cleaned.lastIndexOf(" (");
            String suffix = cleaned.substring(parenStart);
            if (suffix.length() > 20) {
                cleaned = cleaned.substring(0, parenStart);
            }
        }

        // Truncate if still too long
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength - 3) + "...";
        }
        return cleaned;
    }

    /**
     * Extract just the device name from a full mixer info string.
     * ConfigManager stores format: "Device Name Description: Some description"
     *
     * @param fullMixerString The full mixer string from config
     * @return Just the device name portion
     */
    public static String extractDeviceName(String fullMixerString) {
        if (fullMixerString == null) {
            return null;
        }
        int descIndex = fullMixerString.indexOf(" Description:");
        if (descIndex > 0) {
            return fullMixerString.substring(0, descIndex);
        }
        return fullMixerString;
    }

    // Platform detection helpers
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static boolean isMacOS() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }
}
