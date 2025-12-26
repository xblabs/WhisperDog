---
title: API Domain Knowledge Index
description: External service integrations for transcription and post-processing
context_type: reference
priority: high
domain: api
last_updated: 2025-12-26
tags: [index, api, clients, openai, faster-whisper, openwebui]
status: active
---

# API Domain Knowledge Index

## 🎯 Token-Insensitive Overview (50 tokens)
**Essential context for basic understanding**

WhisperDog integrates with multiple external APIs for transcription (OpenAI, Faster-Whisper, OpenWebUI) and post-processing (OpenWebUI LLMs).

**Key Areas:** Transcription Clients, Post-Processing Clients, Model Management
**Entry Points:** `recording/clients/`, `postprocessing/clients/`

## 🔍 Token-Sensitive Summary (200 tokens)
**Balanced context for informed decisions**

**Transcription Clients:**
- **OpenAITranscribeClient**: Official OpenAI Whisper API (25MB limit, requires API key)
- **FasterWhisperTranscribeClient**: Local Faster-Whisper server (no file size limit, configurable endpoint)
- **OpenWebUITranscribeClient**: OpenWebUI's Whisper integration (alternative local option)

**Post-Processing Clients:**
- **OpenWebUIProcessClient**: Executes LLM pipelines for text post-processing

**Critical Knowledge:**
- All transcription clients follow common interface pattern
- API keys stored in `ConfigManager` (properties file)
- Clients use Apache HttpClient for HTTP requests
- JSON parsing via Jackson/Gson
- Error handling with exceptions, user-friendly messages

## 📚 Deep Dive References (500+ tokens)
**Complete context for implementation**

### Transcription Client Architecture

**Common Pattern:**
```java
public String transcribe(File audioFile, String model, String language) throws Exception
```

All clients:
1. Accept audio file, model name, language code
2. Build multipart HTTP request
3. Send to configured endpoint
4. Parse JSON response
5. Return transcription text or throw exception

### OpenAITranscribeClient.java

**Location:** `src/main/java/org/whisperdog/recording/clients/OpenAITranscribeClient.java` (migrating from `org.whispercat`)

**Configuration:**
- API Key: Required (from ConfigManager)
- Endpoint: `https://api.openai.com/v1/audio/transcriptions`
- Models: `whisper-1` (default)
- File Limit: 25MB maximum

**Implementation Details:**
- Uses Apache HttpClient with MultipartEntityBuilder
- Sends audio file as multipart/form-data
- Requires Authorization header: `Bearer {api_key}`
- Response format: JSON with `text` field

**Error Handling:**
- 401: Invalid API key
- 413: File too large (>25MB)
- Network errors: Connection timeout, DNS failures

### FasterWhisperTranscribeClient.java

**Location:** `src/main/java/org/whisperdog/recording/clients/FasterWhisperTranscribeClient.java` (migrating from `org.whispercat`)

**Configuration:**
- Endpoint: Configurable (default: `http://localhost:8000`)
- Models: Fetched from `/models` endpoint
- No API key required (local server)
- No file size limit

**Implementation Details:**
- Connects to local Faster-Whisper server
- Model list cached via `FasterWhisperModelsClient`
- Supports multiple model sizes: tiny, base, small, medium, large
- Response format: JSON with `text` field

**Error Handling:**
- Connection refused: Server not running
- Model not found: Invalid model name
- Transcription errors: Server-side processing failures

### OpenWebUITranscribeClient.java

**Location:** `src/main/java/org/whisperdog/recording/clients/OpenWebUITranscribeClient.java` (migrating from `org.whispercat`)

**Configuration:**
- Endpoint: Configurable OpenWebUI instance
- Models: Shared with OpenWebUI's Whisper integration
- API key: Optional (depends on OpenWebUI config)

**Implementation Details:**
- Alternative to Faster-Whisper for users already running OpenWebUI
- Uses OpenWebUI's audio transcription endpoint
- Model management integrated with OpenWebUI

### Post-Processing Client

**OpenWebUIProcessClient.java**

**Location:** `src/main/java/org/whisperdog/postprocessing/clients/OpenWebUIProcessClient.java` (migrating from `org.whispercat`)

**Purpose:** Execute LLM pipelines on transcription text

**Configuration:**
- Endpoint: OpenWebUI instance URL
- API Key: Required for OpenWebUI authentication
- Models: Fetched from `/models` endpoint

**Implementation Details:**
- Sends text to LLM with system prompt and user prompt
- Supports streaming responses (chat completion API)
- Model selection per processing unit
- Response parsing extracts assistant message content

**Pipeline Integration:**
- Called by `PostProcessingService`
- Each pipeline unit can use different model
- Consecutive same-model units optimized (single API call)

### Model Management

**FasterWhisperModelsClient.java**

**Location:** `src/main/java/org/whisperdog/settings/clients/FasterWhisperModelsClient.java` (migrating from `org.whispercat`)

**Purpose:** Fetch available models from Faster-Whisper server

**OpenWebUIModelsResponse.java**

**Location:** `src/main/java/org/whisperdog/postprocessing/clients/OpenWebUIModelsResponse.java` (migrating from `org.whispercat`)

**Purpose:** Parse OpenWebUI models list response

### HTTP Request Patterns

**Multipart File Upload:**
```java
MultipartEntityBuilder builder = MultipartEntityBuilder.create();
builder.addBinaryBody("file", audioFile, ContentType.MULTIPART_FORM_DATA, audioFile.getName());
builder.addTextBody("model", modelName);
builder.addTextBody("language", languageCode);
HttpEntity entity = builder.build();
```

**JSON Response Parsing:**
```java
String jsonResponse = EntityUtils.toString(response.getEntity());
JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
String text = json.get("text").getAsString();
```

### Configuration Storage

All API settings stored in `ConfigManager`:
- `openai.api.key` - OpenAI API key
- `fasterwhisper.endpoint` - Faster-Whisper server URL
- `openwebui.endpoint` - OpenWebUI instance URL
- `openwebui.api.key` - OpenWebUI API key

See **Data Domain** for ConfigManager details.

### Related Domains
- [Core Domain](../core/index.md) - Transcription workflow integration
- [Data Domain](../data/index.md) - ConfigManager for API settings
- [UI Domain](../ui/index.md) - Settings forms for API configuration
