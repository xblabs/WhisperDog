---
task_id: "0007"
title: "Whisper Vocabulary Hints"
status: todo
priority: medium
created: 2026-01-23
updated: 2026-01-31
tags: [whisper, transcription, settings, api-integration, vocabulary]
complexity: medium
estimated_phases: 2
parent_task: null
related_tasks: ["0006"]
dependencies: []
architectural_decision: "Separate settings panel for AI modifiers (future-proofing)"
---

# Task 0007: Whisper Vocabulary Hints

## Summary

Add a "Vocabulary hints" setting that sends a `prompt` parameter to the OpenAI Whisper API, allowing users to prime transcription with proper nouns, acronyms, and domain-specific terms that are frequently mistranscribed (e.g., "Claude" → "Cloud").

## Motivation

Whisper's `prompt` parameter is designed to bias transcription toward specific spellings when phonetics are ambiguous. This is a lightweight, high-impact improvement that addresses common transcription errors for domain-specific terminology.

## Architectural Note

Initial design considered adding field to existing API Configuration panel, but that panel is already crowded. Decision made to create separate "Transcription Settings" or "AI Modifiers" section with dedicated left menu icon, enabling future expansion (vocabulary management, other pre-mediation features) without conflating with post-processing.

## Source

Discovered during Task 0006 system audio capture work (prompt parameter investigation)

## Complexity Adjustment

Complexity upgraded from LOW to MEDIUM due to new settings panel requirement (not just adding a text field to existing panel).
