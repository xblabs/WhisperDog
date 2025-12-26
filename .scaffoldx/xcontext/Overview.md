# CLAUDE.md - Project Context for Claude Code

## Project Overview

**WhisperCat** is a Java Swing desktop application for audio recording and transcription using OpenAI Whisper API or local Faster-Whisper servers. It includes post-processing pipelines powered by LLMs.

## Quick Commands

```bash
# Build JAR
mvn clean package

# Run application
java -jar target/Audiorecorder-1.0-SNAPSHOT-jar-with-dependencies.jar

# Build without tests
mvn clean package -DskipTests
```

## Commit Message Convention

This project uses **conventional commits** for automatic changelog updates:

```
feat: description     → Added to CHANGELOG.md
fix: description      → Fixed section
docs: description     → Documentation section
refactor: description → Changed section
perf: description     → Changed section
```

The git post-commit hook automatically updates `CHANGELOG.md` when you use these prefixes.

## Project Structure

```
src/main/java/org/whispercat/
├── AudioRecorderUI.java      # Main entry point
├── MainForm.java             # Main window/navigation
├── ConfigManager.java        # Settings persistence
├── ConsoleLogger.java        # Execution log singleton
├── recording/
│   ├── RecorderForm.java     # Main recording UI
│   ├── SilenceRemover.java   # Audio silence detection/removal
│   ├── AudioFileAnalyzer.java # Large file analysis
│   ├── WavChunker.java       # Native WAV splitting
│   ├── FfmpegChunker.java    # FFmpeg-based splitting
│   ├── ChunkedTranscriptionWorker.java  # Async chunked transcription
│   └── clients/
│       ├── OpenAITranscribeClient.java
│       ├── FasterWhisperTranscribeClient.java
│       └── OpenWebUITranscribeClient.java
├── postprocessing/
│   ├── Pipeline.java         # Pipeline data model
│   ├── ProcessingUnit.java   # Unit data model
│   ├── PostProcessingService.java  # Pipeline execution
│   ├── PipelineListForm.java
│   ├── PipelineEditorForm.java
│   ├── UnitLibraryListForm.java
│   └── UnitEditorForm.java
└── settings/
    └── OptionsForm.java      # Settings UI
```

## Key Classes

| Class | Purpose |
|-------|---------|
| `RecorderForm` | Main recording UI, transcription, manual pipeline runner |
| `ConfigManager` | All settings persistence (properties file) |
| `PostProcessingService` | Executes pipelines, handles optimization |
| `SilenceRemover` | RMS-based silence detection and removal |
| `AudioFileAnalyzer` | Pre-flight analysis for large files (>25MB) |
| `ChunkedTranscriptionWorker` | SwingWorker for async chunked transcription |

## Recent Features

- **Large Audio Chunking**: Files >25MB are split into chunks for transcription
- **Manual Pipeline Runner**: Run pipelines on existing transcription text
- **Pipeline History**: Track multiple pipeline executions per recording
- **Log Search**: Search and highlight in console execution log
- **Save & Create Pipeline**: Quick unit-to-pipeline creation

## Important Notes

1. **UI Threading**: All UI updates must be on EDT (Event Dispatch Thread)
2. **SwingWorker**: Use for async operations (transcription, post-processing)
3. **Form Reuse**: MainForm reuses form instances to preserve state
4. **25MB Limit**: OpenAI Whisper API has 25MB file limit - use chunking for larger files
5. **Silence Removal**: Processes even with >90% silence to avoid losing dictation

## Git Hooks

After cloning, run:
```bash
./setup-git-hooks.sh
```

This installs the post-commit hook for automatic changelog updates.

## Documentation Requirements

When implementing new features, update these files:

1. **README.md** - Add to "xblabs Fork Changelog" section:
   - Add new entry under "Latest Update" with date
   - Move previous "Latest Update" to dated "Update" section
   - Add to "Key Enhancements" section if significant feature

2. **CHANGELOG.md** - Auto-updated by commit hook when using conventional commits

3. **CLAUDE.md** - Update if:
   - New key classes are added
   - Project structure changes significantly
   - New development patterns/notes are needed

## Testing Notes

- Test silence removal with various threshold values
- Test large file handling with files >25MB
- Test pipeline execution with multiple consecutive same-model units (optimization)
- Verify UI state persistence when switching screens during recording
