---
title: WhisperDog Knowledge Index
description: Master index for WhisperDog audio transcription application with progressive domain loading
context_type: reference
priority: critical
domain: master
last_updated: 2026-01-25
tags: [index, master, knowledge-base, token-aware, whisperdog]
status: active
---

# WhisperDog Knowledge Index

**WhisperDog** is a Java Swing desktop application for audio recording and transcription using OpenAI Whisper API or local Faster-Whisper servers, with LLM-powered post-processing pipelines.

_Your attentive audio companion for transcription and text processing._

## Current Project State

The project currently has **6 tasks** in the backlog, with **3 tasks in review** and **3 tasks pending**. There's no active task at the moment, which suggests the project is in a planning or review phase.

**Recent Work Focus:**
- **Task 0007**: Whisper Vocabulary Hints - When working on improving transcription accuracy through vocabulary customization, load the Core Domain index to understand transcription workflows and the API Domain for Whisper client integration details.
- **Task 0006**: System Audio Capture - If implementing system audio recording (not just microphone), the Core Domain covers recording architecture, and the UI Domain explains form patterns for audio input selection.
- **Task 0005**: MP4 Drag-Drop Support - When adding file format support beyond WAV, check the Core Domain for chunking strategies (especially FfmpegChunker) and the Data Domain for file handling patterns.

**When to Load Domain Context:**

Load the **Core Domain** (`core/index.md`) when you're working on audio processing, transcription workflows, or need to understand how recording and chunking work. This domain explains the 25MB limit, silence removal algorithms, and the async transcription patterns.

Load the **API Domain** (`api/index.md`) when integrating with external services, configuring API keys, or troubleshooting transcription client issues. It covers OpenAI, Faster-Whisper, and OpenWebUI integration patterns.

Load the **UI Domain** (`ui/index.md`) when modifying forms, adding new screens, or working on Swing components. This explains EDT threading discipline, form reuse patterns, and the FlatLaf theming system.

Load the **Data Domain** (`data/index.md`) when working with configuration, persistence, or data models. ConfigManager patterns, pipeline storage, and JSON serialization are documented here.

Load the **Development Domain** (`development/index.md`) when setting up the build environment, working with git hooks, or understanding testing patterns. Maven configuration and Swing threading best practices are covered.

Load the **Project Domain** (`project/index.md`) when you need architectural context, understanding why decisions were made, or planning new features. This explains the desktop-first approach, multiple backend support, and the chunking strategy rationale.

## Domain Map

### [Core Domain](core/index.md)
Audio recording, transcription workflows, silence removal, chunking strategies. **Load this first** when working on recording features, transcription logic, or audio processing. Contains the main workflow patterns and critical knowledge about the 25MB limit and async operations.

### [API Domain](api/index.md)
External service integrations: OpenAI, Faster-Whisper, OpenWebUI clients. **Load when** configuring API endpoints, troubleshooting API calls, or adding new transcription backends. Explains client abstraction patterns and error handling.

### [UI Domain](ui/index.md)
Swing components, FlatLaf theming, forms, menus, notifications. **Load when** modifying user interface, adding screens, or working on form components. Critical for understanding EDT threading and form lifecycle management.

### [Data Domain](data/index.md)
Models, configuration management, persistence. **Load when** working with ConfigManager, storing user data, or understanding how pipelines and units are persisted. Explains properties file patterns and JSON serialization.

### [Development Domain](development/index.md)
Build process, git hooks, testing, conventions. **Load when** setting up the development environment, understanding build configuration, or following testing patterns. Includes Maven setup and Swing threading best practices.

### [Project Domain](project/index.md)
Project-specific knowledge, requirements, architectural decisions. **Load when** you need to understand why certain decisions were made, planning new features, or reviewing the project roadmap. Contains ADRs and architectural rationale.

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

## Recent Session Context

A session summary from January 2026 is available in `.scaffoldx/xmemory/session_summaries/2026-01.md`. **Load this when** you need to understand recent work patterns or context from previous development sessions.

---

*Navigate to domain indexes above for progressive context loading. Each domain provides token-aware summaries (50t overview, 200t summary, 500t+ deep dive) to match your context needs.*
