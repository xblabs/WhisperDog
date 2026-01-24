# Task 0007: Whisper Vocabulary Hints

## Summary
Add a "Vocabulary hints" setting that sends a `prompt` parameter to the OpenAI Whisper API, allowing users to prime transcription with proper nouns, acronyms, and domain-specific terms that are frequently mistranscribed (e.g., "Claude" → "Cloud").

## Motivation
Whisper's `prompt` parameter is designed to bias transcription toward specific spellings when phonetics are ambiguous. This is a lightweight, high-impact improvement that requires minimal code changes.

## Scope
- Add a text field in Settings for vocabulary hints (comma-separated terms)
- Persist the value via ConfigManager
- Wire the prompt into OpenAITranscribeClient's multipart request
- No changes to audio pipeline or UI beyond settings

## API Reference
OpenAI `/v1/audio/transcriptions` supports:
- `prompt` (string): Optional text to guide transcription style/vocabulary
- Used to prime proper noun spelling, maintain context across segments, influence punctuation

## Priority
Medium - simple implementation, high user value
