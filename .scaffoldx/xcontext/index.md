---
title: WhisperDog Knowledge Index
description: Master index for WhisperDog audio transcription application with progressive domain loading
context_type: reference
priority: critical
domain: master
last_updated: 2025-12-26
tags: [index, master, knowledge-base, token-aware, whisperdog]
status: active
---

# WhisperDog Knowledge Index

**WhisperDog** is a Java Swing desktop application for audio recording and transcription using OpenAI Whisper API or local Faster-Whisper servers, with LLM-powered post-processing pipelines.

_Your attentive audio companion for transcription and text processing._

## Domain Map

### [Core Domain](core/index.md)
Audio recording, transcription workflows, silence removal, chunking strategies

### [API Domain](api/index.md)
External service integrations: OpenAI, Faster-Whisper, OpenWebUI clients

### [UI Domain](ui/index.md)
Swing components, FlatLaf theming, forms, menus, notifications

### [Data Domain](data/index.md)
Models, configuration management, persistence

### [Development Domain](development/index.md)
Build process, git hooks, testing, conventions

### [Project Domain](project/index.md)
Project-specific knowledge, requirements, architectural decisions

## Quick Navigation

**Entry Point**: `WhisperDogApp.java` (currently `AudioRecorderUI.java`) → Main application launcher
**Main UI**: `MainForm.java` → Navigation and form management
**Core Recording**: `RecorderForm.java` → Recording UI and transcription
**Post-Processing**: `PostProcessingService.java` → Pipeline execution

## Technology Stack

- **Language**: Java 11
- **UI Framework**: Swing with FlatLaf 3.5.4
- **Build Tool**: Maven
- **Key Libraries**: Jackson, Gson, Apache HttpClient, JNativeHook
- **External APIs**: OpenAI Whisper, Faster-Whisper, OpenWebUI

---

*Navigate to domain indexes above for progressive context loading*
