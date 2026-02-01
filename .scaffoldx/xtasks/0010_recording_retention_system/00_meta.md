---
title: Recording Retention System
task_id: "0010"
status: review
priority: high
created: 2026-01-25
updated: 2026-01-25
task_type: executable
tags:
  - audio
  - recordings
  - retention
  - storage
  - ui
  - settings
dependencies: []
related_tasks:
  - task_id: "0008"
    relationship: "Channel file retention enables attribution accuracy testing"
---

# Task 0010: Recording Retention System

## Summary

Add a recording retention system that keeps the last N recordings (configurable, default 20) in a persistent location (`%APPDATA%\WhisperDog\recordings`), linked to execution logs for later inspection.

## Problem Statement

Currently, WhisperDog temp files are either:
1. Deleted immediately after transcription completes (`cleanupTempAudioFile`)
2. Deleted via `deleteOnExit()` when the JVM shuts down

This prevents users from:
- Inspecting past recordings for debugging
- Re-playing audio that was transcribed
- Correlating recordings with execution logs

## Solution

Implement a `RecordingRetentionManager` that:
1. Copies the transcribed audio file to a persistent location before cleanup
2. Maintains a JSON manifest with metadata (timestamp, transcription preview, etc.)
3. Prunes old recordings when the count exceeds the configured limit
4. Provides a UI panel to browse and manage retained recordings

## Context Files

- **Plan**: `C:\Users\henry\.claude\plans\floating-swimming-flute.md`
- **Chat context**: Session from 2026-01-25 discussing temp file issues

## Success Criteria

- [ ] Recordings persist in `%APPDATA%\WhisperDog\recordings/`
- [ ] Manifest file tracks metadata for each recording
- [ ] Configurable retention count (1-100, default 20)
- [ ] Configurable storage path
- [ ] Settings UI for retention options
- [ ] Recordings browser panel in side menu
- [ ] Old recordings pruned automatically
- [ ] Optional channel file retention for dual-source recordings (enables Task 0008 testing)
  - [ ] Separate mic and system channel files saved when enabled
  - [ ] Channel files pruned with parent recording
  - [ ] Null-safe handling for single-source recordings
