---
title: Project Domain Knowledge Index
description: Project-specific knowledge, requirements, architectural decisions, and history
context_type: reference
priority: medium
domain: project
last_updated: 2025-12-26
tags: [index, project, architecture, decisions, requirements]
status: active
---

# Project Domain Knowledge Index

## 🎯 Token-Insensitive Overview (50 tokens)
**Essential context for basic understanding**

WhisperDog is a desktop audio transcription app, forked from WhisperCat by ddxy. This new version comes with enhancements for large files, processing enhancements through use of intelligent silence removal, compression and chunking, LLM post-processing pipelines, and flexible API support.

**Key Areas:** Project History, Architecture Decisions, Requirements, Future Roadmap
**Entry Points:** `README.md`, architectural decisions below

## 🔍 Token-Sensitive Summary (200 tokens)
**Balanced context for informed decisions**

**Project Background:**
- **Original**: xblabs audio recorder
- **Fork Name**: WhisperCat → WhisperDog (renamed)
- **Primary Goal**: Flexible audio transcription with post-processing pipelines
- **Key Enhancement**: Support for large files (>25MB) via chunking

**Architectural Decisions:**
- Desktop app (not web) for local file access and privacy
- Java Swing for native desktop experience
- Multiple transcription backends (cloud + local) for flexibility
- LLM post-processing pipelines for transcript refinement
- Properties-based configuration (simple, no database)

**Target Users:**
- Content creators transcribing interviews, podcasts
- Researchers transcribing interviews
- Professionals needing accurate dictation with post-processing
- Privacy-conscious users preferring local processing

**Critical Requirements:**
- Handle large audio files (1+ hours)
- Support offline operation (local Faster-Whisper)
- Preserve transcript quality through LLM refinement

## 📚 Deep Dive References (500+ tokens)
**Complete context for implementation**

### Project History

**Original Project:**
- Created by xblabs
- Basic audio recording and OpenAI transcription
- Simple UI with settings

**Fork Enhancements:**
1. **Large File Support** (v1.1)
   - Pre-flight analysis for files >25MB
   - Native WAV chunking
   - FFmpeg fallback chunking
   - Async chunked transcription with progress

2. **LLM Post-Processing Pipelines** (v1.2)
   - Pipeline system with reusable units
   - OpenWebUI LLM integration
   - Pipeline history tracking
   - Manual pipeline runner (run on existing text)

3. **Multiple Transcription Backends** (v1.3)
   - Faster-Whisper local server support
   - OpenWebUI Whisper integration
   - Model selection per backend

4. **UX Improvements** (v1.4)
   - Log search functionality
   - Pipeline history panel
   - Save & Create Pipeline quick action
   - Improved error messaging

**Naming:**
- Original fork: WhisperCat
- Renamed to: WhisperDog (avoid confusion with other projects)

### Architectural Decisions

**Decision 1: Desktop Application (Not Web)**

**Context:** Need local file access, system tray, global hotkeys

**Options Considered:**
- Web app (Electron or browser-based)
- Native desktop (Java Swing, JavaFX, or native)

**Decision:** Java Swing desktop application

**Rationale:**
- Direct file system access (no upload required)
- System tray integration
- Global hotkeys support (JNativeHook)
- Cross-platform (Windows, Mac, Linux)
- Mature UI framework with good libraries (FlatLaf)

**Trade-offs:**
- More complex UI code than web
- Requires Java runtime installation
- Harder to distribute updates
- BUT: Better privacy, performance, OS integration

---

**Decision 2: Multiple Transcription Backend Support**

**Context:** Users have different needs (cloud vs local, cost vs quality)

**Options Considered:**
- OpenAI only (original implementation)
- Local Whisper only
- Multiple backends with abstraction

**Decision:** Support multiple backends with common interface

**Rationale:**
- Flexibility for different use cases
- Privacy-conscious users prefer local
- Cloud APIs offer convenience and quality
- No vendor lock-in

**Implementation:**
- Common transcription client interface
- Backend selection in settings
- Per-recording backend override possible

---

**Decision 3: Chunking Strategy for Large Files**

**Context:** OpenAI has 25MB file size limit, need to support larger files

**Options Considered:**
- Compression only (FFmpeg)
- Split and transcribe chunks
- Fail with error message

**Decision:** Implement chunking with multiple strategies

**Rationale:**
- Compression alone may not be enough for very large files
- Chunking enables transcription of unlimited file sizes
- Multiple chunking strategies handle edge cases

**Implementation:**
- Pre-flight analysis warns user
- Native WAV chunker (fast, reliable)
- FFmpeg chunker (fallback, handles more formats)
- Async transcription with progress tracking
- Results concatenated seamlessly

**Trade-offs:**
- More complex implementation
- Potential accuracy loss at chunk boundaries
- BUT: Enables use cases that were impossible before

---

**Decision 4: Properties File for Configuration (Not Database)**

**Context:** Need to persist user settings, API keys, preferences

**Options Considered:**
- Properties file
- JSON file
- SQLite database

**Decision:** Properties file (`~/.whispercat/config.properties`)

**Rationale:**
- Simple, human-readable
- Built-in Java support
- Easy to debug and manually edit
- Sufficient for key-value config
- No database overhead

**Trade-offs:**
- Not suitable for complex data structures
- Pipelines/units use JSON instead (appropriate)

---

**Decision 5: LLM Pipeline System Design**

**Context:** Users want to refine transcripts (grammar, formatting, summarization)

**Options Considered:**
- Hardcoded post-processing options
- User-defined pipelines with reusable units
- Plugin system

**Decision:** User-defined pipelines with reusable units

**Rationale:**
- Maximum flexibility without code changes
- Reusable units reduce duplication
- Pipeline composition enables complex workflows
- JSON storage makes sharing pipelines easy

**Implementation:**
- ProcessingUnit: Reusable LLM prompt templates
- Pipeline: Ordered sequence of units
- PostProcessingService: Executes pipeline with optimization

**Optimization:**
- Consecutive same-model units combined (single API call)
- Reduces API costs and latency

### Requirements

**Functional Requirements:**

1. **Audio Recording**
   - Record from system microphone
   - Save to WAV format
   - Optional silence removal
   - Global hotkey for start/stop

2. **Transcription**
   - Support OpenAI Whisper API
   - Support local Faster-Whisper server
   - Support OpenWebUI Whisper integration
   - Handle files up to unlimited size (via chunking)
   - Progress indication for long operations

3. **Post-Processing**
   - Create and manage pipelines
   - Create and manage processing units
   - Execute pipelines on transcripts
   - Track pipeline execution history
   - Manual pipeline runner (run on existing text)

4. **Configuration**
   - API key management
   - Endpoint configuration
   - Model selection
   - Silence removal threshold
   - Global hotkey customization

5. **User Interface**
   - Intuitive navigation
   - Light/dark theme support
   - Non-blocking notifications
   - System tray integration
   - Execution logs with search

**Non-Functional Requirements:**

1. **Performance**
   - UI responsiveness (no freezing)
   - Efficient memory usage for large files
   - Fast chunking and transcription

2. **Usability**
   - Simple setup (minimal config required)
   - Clear error messages
   - Preserve state during navigation
   - Keyboard shortcuts

3. **Privacy**
   - Local processing option (Faster-Whisper)
   - API keys stored locally (not shared)
   - No telemetry or tracking

4. **Reliability**
   - Graceful error handling
   - No data loss (auto-save configs)
   - Resilient to API failures

### Future Roadmap

**Planned Features:**

1. **Speaker Diarization**
   - Identify different speakers
   - Label speakers in transcript
   - Integration with Pyannote or similar

2. **Batch Processing**
   - Process multiple files at once
   - Queue management
   - Scheduled processing

3. **Export Formats**
   - SRT subtitles
   - VTT captions
   - Formatted documents (DOCX, PDF)

4. **Advanced Pipeline Features**
   - Conditional units (execute based on criteria)
   - Variables and templating
   - Pipeline branching

5. **Cloud Sync**
   - Sync pipelines/units across devices
   - Optional cloud storage for recordings
   - Collaboration features

**Potential Improvements:**

- Auto-update mechanism
- Installer packages (Windows MSI, Mac DMG)
- Mobile companion app
- Voice activity detection for better silence removal
- Real-time transcription (streaming)

### Technical Debt

**Known Issues:**

1. **Form State Management**
   - Form reuse pattern works but could be cleaner
   - Consider proper state management library

2. **Error Handling**
   - Some error messages too technical
   - Need better user guidance for common issues

3. **Testing**
   - Mostly manual testing
   - Need automated UI tests
   - Need unit tests for core logic

4. **Code Organization**
   - Some classes are large (RecorderForm, MainForm)
   - Could benefit from further decomposition

**Refactoring Candidates:**

- Extract transcription client factory
- Separate UI and business logic more cleanly
- Consolidate HTTP client usage
- Improve pipeline optimization algorithm

### Related Domains
- [Core Domain](../core/index.md) - Implementation of key architectural decisions
- [API Domain](../api/index.md) - Multiple backend support implementation
- [Development Domain](../development/index.md) - Build and release process
