# Whisper Vocabulary Hints - Execution Checklist

> **Context Recovery**: Find first unchecked `[ ]` item → resume from there.

---

## Cluster 1: Settings & Persistence

- [ ] Add `vocabularyHints` field to ConfigManager
- [ ] Add getter/setter with default empty string
- [ ] Add "Vocabulary Hints" text field in SettingsForm
- [ ] Add tooltip explaining usage (comma-separated proper nouns, acronyms)
- [ ] Test save/load cycle

---

## Cluster 2: API Integration

- [ ] Wire `vocabularyHints` into OpenAITranscribeClient
- [ ] Add `prompt` parameter to multipart request (skip if empty)
- [ ] Test transcription with vocabulary hints set
- [ ] Verify no regression when hints are empty

---

**Source**: Discovered during Task 0006 system audio capture work (prompt parameter investigation)
