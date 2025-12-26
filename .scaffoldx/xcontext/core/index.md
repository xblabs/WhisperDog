---
title: Core Domain Knowledge Index
description: Audio recording, transcription, and processing workflows for WhisperDog
context_type: reference
priority: critical
domain: core
last_updated: 2025-12-26
tags: [index, core, audio, transcription, recording, chunking]
status: active
---

# Core Domain Knowledge Index

## 🎯 Token-Insensitive Overview (50 tokens)
**Essential context for basic understanding**

WhisperDog's core handles audio recording, transcription via Whisper APIs, and audio processing (silence removal, chunking for large files).

**Key Areas:** Recording, Transcription, Audio Processing, Chunking
**Entry Points:** `RecorderForm.java`, `SilenceRemover.java`, `ChunkedTranscriptionWorker.java`

**Package:** `org.whisperdog.recording` (migrating from `org.whispercat`)

## 🔍 Token-Sensitive Summary (200 tokens)
**Balanced context for informed decisions**

**Core Components:**
- **RecorderForm**: Main recording UI, handles recording start/stop, transcription initiation, manual pipeline runner
- **AudioRecorder**: Low-level audio capture using Java Sound API
- **SilenceRemover**: RMS-based silence detection and removal (processes even with >90% silence)
- **AudioFileAnalyzer**: Pre-flight analysis for large files (>25MB threshold)
- **Chunking System**: Splits large files via `WavChunker` (native) or `FfmpegChunker` (ffmpeg-based)
- **ChunkedTranscriptionWorker**: SwingWorker for async chunked transcription with progress tracking

**Critical Knowledge:**
- **25MB Limit**: OpenAI Whisper API enforces 25MB file size limit
- **Async Pattern**: All long operations use SwingWorker to keep UI responsive on EDT
- **Client Abstraction**: Transcription clients implement common interface for OpenAI/Faster-Whisper/OpenWebUI

## 📚 Deep Dive References (500+ tokens)
**Complete context for implementation**

### Recording Workflow
1. **User starts recording** → `RecorderForm.startRecording()`
2. **AudioRecorder captures** → Raw audio data to WAV file
3. **User stops recording** → Triggers transcription or saves file
4. **Pre-flight check** → `AudioFileAnalyzer` checks if >25MB
5. **Chunking decision** → If large, offer chunking options to user
6. **Transcription** → Either direct API call or chunked processing

### Audio Processing Classes

**SilenceRemover.java**
- RMS-based silence detection with configurable threshold
- Removes silent sections from WAV files
- Always processes (even if >90% silence) to preserve dictation
- Location: `src/main/java/org/whispercat/recording/SilenceRemover.java`

**AudioFileAnalyzer.java**
- Analyzes WAV files for size, duration, format
- Determines if chunking is needed (>25MB threshold)
- Location: `src/main/java/org/whispercat/recording/AudioFileAnalyzer.java`

**WavChunker.java**
- Native Java WAV file splitter (no external dependencies)
- Fast, reliable for WAV format
- Location: `src/main/java/org/whispercat/recording/WavChunker.java`

**FfmpegChunker.java**
- FFmpeg-based chunking (supports multiple formats)
- Fallback when native chunker can't handle format
- Location: `src/main/java/org/whispercat/recording/FfmpegChunker.java`

**ChunkedTranscriptionWorker.java**
- SwingWorker that orchestrates chunked transcription
- Progress tracking, error handling, result aggregation
- Location: `src/main/java/org/whispercat/recording/ChunkedTranscriptionWorker.java`

### Transcription Clients

All clients implement a common pattern:
- Accept audio file and configuration
- Return transcription text
- Handle errors with exceptions

See **API Domain** for client implementation details.

### Threading Model

**EDT (Event Dispatch Thread):**
- All UI updates MUST run on EDT
- Use `SwingUtilities.invokeLater()` or `SwingWorker.publish()`

**Background Threads:**
- Recording: `AudioRecorder` runs in separate thread
- Transcription: `ChunkedTranscriptionWorker` or direct API calls
- Post-processing: `PostProcessingService` uses SwingWorker

### Architecture Decisions

**Why RMS-based silence removal?**
- Simple, fast, no ML dependencies
- Configurable threshold for different use cases
- Preserves dictation context (processes even high-silence recordings)

**Why 25MB chunking threshold?**
- OpenAI API hard limit
- Local servers may have different limits (configurable)

**Why both WavChunker and FfmpegChunker?**
- WavChunker: Fast, no dependencies, reliable for WAV
- FfmpegChunker: Handles edge cases, supports other formats

### Related Domains
- [API Domain](../api/index.md) - Transcription client implementations
- [UI Domain](../ui/index.md) - RecorderForm UI components
- [Data Domain](../data/index.md) - ConfigManager for settings
