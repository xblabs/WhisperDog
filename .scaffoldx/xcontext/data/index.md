---
title: Data Domain Knowledge Index
description: Data models, configuration management, and persistence patterns
context_type: reference
priority: high
domain: data
last_updated: 2025-12-26
tags: [index, data, models, configuration, persistence]
status: active
---

# Data Domain Knowledge Index

## 🎯 Token-Insensitive Overview (50 tokens)
**Essential context for basic understanding**

WhisperDog uses properties files for configuration, JSON for pipeline/unit storage, and in-memory models for runtime data.

**Key Areas:** Configuration, Pipelines, Processing Units, History
**Entry Points:** `ConfigManager.java`, `Pipeline.java`, `ProcessingUnit.java`
**Package:** `org.whisperdog` (migrating from `org.whispercat`)

## 🔍 Token-Sensitive Summary (200 tokens)
**Balanced context for informed decisions**

**Core Data Components:**
- **ConfigManager**: Singleton for all app settings (properties file: `~/.whisperdog/config.properties`, migrating from `~/.whispercat/`)
- **Pipeline**: Post-processing pipeline model (stored as JSON files)
- **ProcessingUnit**: Individual LLM processing step model (JSON files)
- **PipelineExecutionHistory**: Tracks multiple pipeline runs per recording

**Persistence Patterns:**
- **Configuration**: Properties file (key-value pairs)
- **Pipelines/Units**: JSON files in `~/.whisperdog/pipelines/` and `~/.whisperdog/units/` (migrating from `~/.whispercat/`)
- **History**: In-memory per session (not persisted)
- **Recordings**: WAV files on disk, paths stored in config

**Critical Knowledge:**
- ConfigManager is thread-safe singleton
- All file I/O operations handle exceptions gracefully
- JSON serialization via Jackson/Gson
- Default values provided for missing config keys

## 📚 Deep Dive References (500+ tokens)
**Complete context for implementation**

### Configuration Management

**ConfigManager.java**

**Location:** `src/main/java/org/whisperdog/ConfigManager.java` (migrating from `org.whispercat`)

**Purpose:** Centralized configuration storage and retrieval

**Storage Location:**
- Windows: `C:\Users\{user}\.whisperdog\config.properties` (migrating from `.whispercat`)
- Linux/Mac: `~/.whisperdog/config.properties` (migrating from `~/.whispercat`)

**Key Configuration Keys:**
```
# OpenAI Settings
openai.api.key=sk-...
openai.model=whisper-1

# Faster-Whisper Settings
fasterwhisper.endpoint=http://localhost:8000
fasterwhisper.model=base

# OpenWebUI Settings
openwebui.endpoint=http://localhost:3000
openwebui.api.key=...
openwebui.whisper.model=...
openwebui.process.model=...

# Recording Settings
recording.output.dir=...
recording.silence.threshold=0.02
recording.silence.enabled=true

# Global Hotkey
hotkey.record.combination=...

# UI Settings
ui.theme=dark
ui.accent.color=...
```

**API:**
```java
// Get configuration value
String apiKey = ConfigManager.getInstance().getProperty("openai.api.key");

// Set and persist configuration
ConfigManager.getInstance().setProperty("openai.api.key", "sk-...");
ConfigManager.getInstance().save();

// Get with default value
String threshold = ConfigManager.getInstance().getProperty(
    "recording.silence.threshold",
    "0.02"
);
```

**Thread Safety:**
- Singleton instance with synchronized access
- Properties object is thread-safe
- File writes are synchronized

### Pipeline Data Model

**Pipeline.java**

**Location:** `src/main/java/org/whisperdog/postprocessing/Pipeline.java` (migrating from `org.whispercat`)

**Structure:**
```java
public class Pipeline {
    private String id;              // Unique identifier (UUID)
    private String name;            // User-friendly name
    private String description;     // Optional description
    private List<PipelineUnitReference> units;  // Ordered processing steps
    private Date created;
    private Date modified;
}
```

**PipelineUnitReference.java**

**Location:** `src/main/java/org/whispercat/postprocessing/PipelineUnitReference.java`

**Structure:**
```java
public class PipelineUnitReference {
    private String unitId;          // Reference to ProcessingUnit
    private int order;              // Execution order
    private Map<String, String> overrides;  // Override unit defaults
}
```

**Persistence:**
- Stored as JSON in `~/.whispercat/pipelines/{pipeline-id}.json`
- Jackson serialization/deserialization
- Lazy loading (only load when needed)

**Example JSON:**
```json
{
  "id": "uuid-...",
  "name": "Grammar Correction",
  "description": "Fix grammar and punctuation",
  "units": [
    {
      "unitId": "unit-uuid-...",
      "order": 0,
      "overrides": {}
    }
  ],
  "created": "2025-01-15T10:30:00Z",
  "modified": "2025-01-15T10:30:00Z"
}
```

### Processing Unit Data Model

**ProcessingUnit.java**

**Location:** `src/main/java/org/whispercat/postprocessing/ProcessingUnit.java`

**Structure:**
```java
public class ProcessingUnit {
    private String id;              // Unique identifier (UUID)
    private String name;            // User-friendly name
    private String description;     // What this unit does
    private String systemPrompt;    // LLM system prompt
    private String userPromptTemplate;  // Template with {{text}} placeholder
    private String modelId;         // OpenWebUI model ID
    private Date created;
    private Date modified;
}
```

**Persistence:**
- Stored as JSON in `~/.whispercat/units/{unit-id}.json`
- Jackson serialization/deserialization
- Loaded on demand for pipeline execution

**Example JSON:**
```json
{
  "id": "uuid-...",
  "name": "Grammar Fixer",
  "description": "Corrects grammar and punctuation errors",
  "systemPrompt": "You are a grammar correction assistant.",
  "userPromptTemplate": "Fix grammar in this text:\n\n{{text}}",
  "modelId": "gpt-4",
  "created": "2025-01-15T10:30:00Z",
  "modified": "2025-01-15T10:30:00Z"
}
```

**Template Variable:**
- `{{text}}` is replaced with input text during execution
- Future: Support for additional variables

### Post-Processing Data

**PostProcessingData.java**

**Location:** `src/main/java/org/whispercat/postprocessing/PostProcessingData.java`

**Purpose:** Runtime data for pipeline execution

**Structure:**
```java
public class PostProcessingData {
    private String originalText;
    private String currentText;     // Updated after each unit
    private List<ProcessingStepData> steps;
    private Date executionTime;
}
```

**ProcessingStepData.java**

**Location:** `src/main/java/org/whispercat/postprocessing/ProcessingStepData.java`

**Structure:**
```java
public class ProcessingStepData {
    private String unitId;
    private String unitName;
    private String inputText;
    private String outputText;
    private long durationMs;
    private String modelUsed;
}
```

**Usage:**
- Tracks transformation through pipeline
- Enables debugging and history viewing
- Shows which units changed what

### Pipeline Execution History

**PipelineExecutionHistory.java**

**Location:** `src/main/java/org/whispercat/recording/PipelineExecutionHistory.java`

**Purpose:** Track multiple pipeline runs on same recording

**Structure:**
```java
public class PipelineExecutionHistory {
    private List<HistoryEntry> entries;

    public void addEntry(String pipelineName, String result, Date timestamp);
    public List<HistoryEntry> getEntries();
}
```

**Features:**
- Stores in RecorderForm (per-recording session)
- Not persisted to disk (in-memory only)
- Displayed in history panel
- Allows comparison of different pipeline results

### History Panel Integration

**HistoryPanel.java**

**Location:** `src/main/java/org/whispercat/recording/HistoryPanel.java`

**Purpose:** UI component for displaying execution history

**Features:**
- List of pipeline executions
- Timestamps
- Click to view result
- Clear history

### File System Structure

```
~/.whispercat/
├── config.properties         # All app settings
├── pipelines/
│   ├── {uuid-1}.json        # Pipeline definition
│   ├── {uuid-2}.json
│   └── ...
├── units/
│   ├── {uuid-1}.json        # Processing unit definition
│   ├── {uuid-2}.json
│   └── ...
└── recordings/              # Default recording output (configurable)
    ├── recording-1.wav
    └── ...
```

### Data Loading Patterns

**Lazy Loading:**
- Pipelines and units loaded only when needed
- ConfigManager eagerly loads on startup
- History built in-memory during session

**Caching:**
- ConfigManager caches properties in memory
- Pipeline/Unit models cached after first load
- Cache invalidation on save operations

**Error Handling:**
- Missing config keys return defaults
- Missing pipeline/unit files throw FileNotFoundException
- Corrupt JSON handled with parse exceptions
- User-friendly error messages in UI

### JSON Serialization

**Jackson Configuration:**
- Date format: ISO 8601
- Pretty printing enabled for human readability
- Null values omitted
- Unknown properties ignored (forward compatibility)

**Gson Usage:**
- Alternative for simple JSON operations
- Used in some API response parsing

### Related Domains
- [Core Domain](../core/index.md) - Configuration affects recording behavior
- [API Domain](../api/index.md) - API keys and endpoints from ConfigManager
- [UI Domain](../ui/index.md) - Forms bind to and update ConfigManager
