# PRD: System Audio Capture

## Problem Statement

Users need to transcribe content from multiple audio sources simultaneously:
- YouTube videos while providing spoken feedback
- Voice messages (Telegram/WhatsApp) with response capture
- Meeting participants (Slack Huddle) with no native recording

Currently, WhisperDog captures microphone only. Users cannot:
1. Record what they're listening to (system audio)
2. Combine their commentary with the source material
3. Distinguish who said what in the transcript

**Impact:**
- Can't do consumption + feedback sessions (video analysis, voice message responses)
- No way to record meetings without platform support
- Lost context when transcribing multi-party conversations

## Solution Overview

**Dual-source recording with source-level attribution:**

| Source | Attribution | Detection |
|--------|-------------|-----------|
| Microphone | **[User]** | Always the user |
| System Audio | **[System]** | External/third-party |

This approach avoids complex speaker detection AI - the source IS the attribution.

## Functional Requirements

### FR-1: System Audio Capture Toggle

**UI Element**: Toggle in recording controls
**Behavior**:
- Off by default (opt-in to avoid background noise/music)
- Can toggle on-the-fly during recording
- Can pause/resume system audio capture independently
- Visual indicator when system audio is active

**States**:
- `Mic Only` (default)
- `Mic + System`
- `System paused` (mic still active)

### FR-2: Dual-Track Recording

**Architecture**:
```
Microphone ──────► Track A ──► [User] segments
System Audio ────► Track B ──► [System] segments
```

**Technical Approach**:
- Record mic and system as separate streams/tracks
- Track which source is active via RMS threshold detection
- Merge with source labels during transcription output

### FR-3: Source-Level Attribution

**Output Format**:
```
[User]: So this video is explaining the API integration...
[System]: The REST endpoint accepts POST requests with JSON body...
[User]: Interesting, so we need to handle authentication first.
```

**Attribution Logic**:
- Sample RMS at intervals (e.g., every 100ms)
- If mic RMS > threshold → `[User]`
- If system RMS > threshold → `[System]`
- If both active → show both or prioritize mic (user anchor)

### FR-4: Platform Audio APIs

**Windows**: WASAPI (Windows Audio Session API)
- Loopback capture for system audio
- Well-documented, standard approach

**macOS**: Core Audio
- Requires virtual audio device or screen capture permissions
- More complex than Windows

**Scope**: Windows first (primary platform), macOS as stretch goal

### FR-5: Settings Integration

**Options Panel**:
- Enable/disable system audio feature
- Default toggle state preference
- Attribution label customization (optional)

## Technical Considerations

### Speaker Attribution Approaches (Reference)

| Approach | Feasibility | Notes |
|----------|-------------|-------|
| Source-level tracking | **High** | Track mic vs system - simple, reliable |
| RMS signal sampling | Medium | Detect which source is active |
| Gemini API | Optional | For finer attribution within system audio |
| Dual-track recording | **High** | Separate tracks, merge with metadata |

### Known Complication

**Silence removal disrupts timestamp mapping**: The existing silence removal tool compresses audio, breaking correlation between audio timestamps and transcript segments. Source attribution must account for this.

### Optional: Gemini API Enhancement

For users wanting finer speaker detection within system audio:
- Add Gemini API key field in settings
- Send audio for speaker attribution
- Or: Export audio for manual processing in Gemini Flash web UI

## UI Mockup

```
┌─────────────────────────────────────────────────────────────┐
│  WhisperDog                                             [X] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [●] Record   [⏹] Stop   [🎤 Mic ✓] [🔊 System ○]          │
│                                                             │
│  Recording: 00:01:23                                        │
│  Sources: Mic (active) | System (waiting)                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Non-Functional Requirements

- System audio capture should not introduce latency
- Minimal CPU overhead for dual-track recording
- Clear visual feedback for active sources
- Graceful degradation if system audio unavailable

## Out of Scope

- Complex AI speaker detection (use source-level instead)
- Video capture
- Multi-microphone support
- Real-time speaker labeling in UI (post-processing only)

## Success Criteria

1. Can record mic + system audio simultaneously
2. Transcript shows `[User]`/`[System]` labels
3. Toggle works on-the-fly during recording
4. Works on Windows with WASAPI
