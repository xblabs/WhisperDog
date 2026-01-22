# System Audio Capture - Research Findings

**Date**: 2026-01-22
**Status**: Complete

---

## Executive Summary

**Recommended Approach**: Use **XT-Audio 2.x** library with WASAPI backend on Windows.

**Key Decision**: Upgrade project from Java 11 to Java 17 to use latest XT-Audio.

---

## WASAPI Loopback Capture Overview

### What It Is
WASAPI (Windows Audio Session API) loopback mode captures the audio stream being played by a rendering endpoint device - essentially "what you hear" recording without requiring special hardware or Stereo Mix.

### Technical Requirements (from Microsoft docs)
1. **Endpoint**: Use `eRender` (rendering endpoint, not capture)
2. **Stream Flag**: `AUDCLNT_STREAMFLAGS_LOOPBACK`
3. **Mode**: Shared mode only (exclusive mode not supported)
4. **Windows 10 1703+**: Event-driven loopback fully supported

### Important Gotcha
If no audio is playing, the DataAvailable event won't fire. To record "silence", you may need to play silence through the device.

---

## Java Library Analysis

### XT-Audio (Recommended)

| Property | Value |
|----------|-------|
| Maven | `com.github.sjoerdvankreel:xt.audio:2.0+` |
| Java Requirement | JRE 17+ |
| License | Open source |
| WASAPI Loopback | Confirmed via `XtDeviceCaps.LOOPBACK` |
| Active | Yes (June 2024: v2.2) |

**Pros**:
- Pure Java/JNA, no external dependencies
- Cross-platform (Windows WASAPI, Linux PulseAudio)
- Maven Central (bundleable)
- Proven in production (Firefly Luciferin project)

**Cons**:
- Requires Java 17+ (need project upgrade)

### Other Options Considered

| Library | Status | Why Not |
|---------|--------|---------|
| FFmpeg + virtual device | External | Requires driver install |
| Jitsi libjitsi | Complex | Large library for one feature |
| Custom JNA | High effort | XT-Audio already exists |
| XT-Audio 1.9 | Java 11 | Unclear loopback support |

---

## Reference Implementation

From **Firefly Luciferin** project (production usage):

```java
// Initialize XtAudio
try (XtPlatform platform = XtAudio.init("AppName", Pointer.NULL)) {
    // Get WASAPI service on Windows
    XtService service = platform.getService(Enums.XtSystem.WASAPI);

    // Enumerate loopback devices
    try (XtDeviceList inputs = service.openDeviceList(
            EnumSet.of(Enums.XtEnumFlags.INPUT))) {
        // Filter for LOOPBACK capability
        for (int i = 0; i < inputs.getCount(); i++) {
            String id = inputs.getId(i);
            EnumSet<Enums.XtDeviceCaps> caps = inputs.getCapabilities(id);
            if (caps.contains(Enums.XtDeviceCaps.LOOPBACK)) {
                // Found loopback device
            }
        }
    }

    // Open stream with callback
    Structs.XtStreamParams streamParams = new Structs.XtStreamParams(
        true, this::onBuffer, null, null);
    try (XtStream stream = device.openStream(deviceParams, null)) {
        stream.start();
        // Capture audio...
        stream.stop();
    }
}
```

---

## Integration Plan

### Required Changes

1. **pom.xml**:
   - Update `maven.compiler.source` from 11 to 17
   - Update `maven.compiler.target` from 11 to 17
   - Add XT-Audio dependency

```xml
<dependency>
    <groupId>com.github.sjoerdvankreel</groupId>
    <artifactId>xt.audio</artifactId>
    <version>2.2</version>
</dependency>
```

2. **New Classes**:
   - `SystemAudioCapture` - WASAPI loopback wrapper
   - `AudioCaptureManager` - Coordinates mic + system audio

3. **Audio Format**:
   - XT-Audio captures in configurable format
   - Convert to 16-bit PCM, 16kHz mono for consistency with existing pipeline

---

## Sources

- [Microsoft WASAPI Loopback Recording](https://learn.microsoft.com/en-us/windows/win32/coreaudio/loopback-recording)
- [XT-Audio GitHub](https://github.com/sjoerdvankreel/xt-audio)
- [XT-Audio Maven](https://mvnrepository.com/artifact/com.github.sjoerdvankreel/xt.audio)
- [Firefly Luciferin AudioLoopbackSoftware.java](https://github.com/sblantipodi/firefly_luciferin/blob/master/src/main/java/org/dpsoftware/audio/AudioLoopbackSoftware.java)

---

## Next Steps

1. [ ] Create minimal PoC with XT-Audio
2. [ ] Verify audio format compatibility
3. [ ] Test mic + system capture simultaneously
