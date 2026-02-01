---
title: Task Checklist - Whisper Vocabulary Hints
description: Tracking progress on individual components of Task 0007
context_type: checklist
priority: medium
last_updated: 2026-01-31
progress: 0%
---

# Whisper Vocabulary Hints - Execution Checklist

> **Context Recovery**: Find first unchecked `[ ]` item → resume from there.

---

## Phase 1: ConfigManager Extension

**Objective**: Add persistence layer for vocabulary hints setting

- [ ] Add `vocabularyHints` field to ConfigManager
  - Type: `String`
  - Default value: `""` (empty string)
  - Location: `src/main/java/org/whisperdog/config/ConfigManager.java`
- [ ] Implement getter: `getVocabularyHints()`
  - Return `""` if field is null (backward compatibility)
- [ ] Implement setter: `setVocabularyHints(String)`
  - Call `saveConfig()` to persist immediately
- [ ] Update Gson serialization
  - Ensure `vocabularyHints` field included in config.json
  - Handle missing field in old configs (default to empty string)
- [ ] Write unit tests
  - [ ] Test default value for new configs
  - [ ] Test save/load cycle persists value
  - [ ] Test null value converts to empty string
- [ ] Manual test: Verify config.json includes `vocabularyHints` after save

**Acceptance**: Config file persists vocabulary hints across restarts

---

## Phase 2: Transcription Settings Panel (UI)

**Objective**: Create new settings panel with vocabulary hints input and token validation

**Architecture Decision**: Create separate "Transcription Settings" panel (not adding to existing API Configuration panel) to avoid overcrowding and enable future expansion.

### Panel Structure
- [ ] Create `TranscriptionSettingsPanel.java`
  - Location: `src/main/java/org/whisperdog/ui/TranscriptionSettingsPanel.java`
  - Extends: `JPanel`
- [ ] Implement UI components
  - [ ] `JLabel` title: "Vocabulary Hints"
  - [ ] `JTextArea` input field (3 rows, word wrap enabled): `vocabularyHintsField`
  - [ ] `JScrollPane` wrapping text area
  - [ ] `JLabel` help text (gray, smaller font):
    > "Comma-separated terms to improve transcription accuracy (proper nouns, acronyms). Limit: ~160 words (224 tokens)."
  - [ ] `JLabel` token warning (red, initially hidden): `tokenWarningLabel`
  - [ ] Save and Cancel buttons

### Token Validation Logic
- [ ] Add `DocumentListener` to `vocabularyHintsField`
  - Trigger validation on every text change
- [ ] Implement `validateTokenLimit()` method
  - Calculate estimated tokens: `word_count * 1.3`
  - Show warning if estimated tokens > 224
  - Warning text: `"⚠️ Input may exceed token limit ({count} estimated). Consider shortening."`
  - Hide warning if within limit
- [ ] Implement `estimateTokenCount(String text)` helper
  - Split text by whitespace: `text.trim().split("\\s+")`
  - Multiply word count by 1.3 (conservative estimate)
  - Round up to integer

### Save/Load Logic
- [ ] Wire Save button action
  - Read text from `vocabularyHintsField`
  - Call `ConfigManager.getInstance().setVocabularyHints(text)`
  - Close panel or show success message
- [ ] Wire Cancel button action
  - Discard changes and close panel
- [ ] Implement panel load method
  - Read `ConfigManager.getInstance().getVocabularyHints()`
  - Populate `vocabularyHintsField` with loaded value

### Main UI Integration
- [ ] Add "Transcription Settings" menu item to left navigation
  - Icon: (TBD - suggest microphone with settings gear)
  - Position: After "API Configuration" or in new "AI Modifiers" section
- [ ] Wire menu item to open `TranscriptionSettingsPanel` in main content area

### Manual Testing
- [ ] Test UI layout and component positioning
- [ ] Test help text visibility and clarity
- [ ] Test token warning appears when >160 words entered
- [ ] Test token warning disappears when text shortened
- [ ] Test Save persists hints to ConfigManager
- [ ] Test Cancel discards changes
- [ ] Test panel reload displays saved hints

**Acceptance**: User can input vocabulary hints via dedicated settings panel with real-time token validation

---

## Phase 3: API Integration

**Objective**: Wire vocabulary hints into OpenAI Whisper API request

- [ ] Modify `OpenAITranscribeClient.buildMultipartRequest()`
  - Location: `src/main/java/org/whisperdog/api/OpenAITranscribeClient.java`
- [ ] Read vocabulary hints from config
  - `String hints = ConfigManager.getInstance().getVocabularyHints()`
- [ ] Add conditional `prompt` parameter
  - **Condition**: `hints != null && !hints.trim().isEmpty()`
    - Clarification: Skip if null OR trimmed empty (handles whitespace-only)
  - **Action if true**: `requestBody.addFormDataPart("prompt", hints.trim())`
  - **Action if false**: Omit `prompt` parameter (no change to request)
- [ ] Write unit tests
  - [ ] Test with hints present → verify `prompt` field added to request
  - [ ] Test with empty string → verify `prompt` field omitted
  - [ ] Test with whitespace-only → verify `prompt` field omitted (trim behavior)
  - [ ] Test with null value → verify `prompt` field omitted
- [ ] Integration test: Real API call
  - [ ] Test with hints="Claude, Anthropic" → verify transcription accuracy
  - [ ] Test without hints → verify no regression in quality

**Acceptance**: Vocabulary hints sent to OpenAI API as `prompt` parameter when non-empty

---

## Phase 4: Comprehensive Testing

**Objective**: Validate edge cases and backward compatibility

### Edge Case Tests
- [ ] Test null value handling
  - ConfigManager returns null → getter converts to `""`
  - OpenAITranscribeClient omits `prompt` parameter
- [ ] Test whitespace-only input
  - Input: `"   \n   "` (spaces, newlines, tabs)
  - Expected: Trimmed to empty, `prompt` parameter omitted
- [ ] Test exceeds 224 tokens
  - Input: 300-word vocabulary list
  - Expected: Warning shown in UI, API request sent, API returns error gracefully
- [ ] Test special characters
  - Input: `"Claude", O'Reilly, 50% accuracy`
  - Expected: No escaping issues, API accepts raw text

### Backward Compatibility
- [ ] Test loading old config file (without `vocabularyHints` field)
  - Expected: App loads without errors
  - Expected: `vocabularyHints` defaults to empty string
  - Expected: Transcription Settings panel shows empty field

### Regression Testing
- [ ] Test transcription without hints
  - Expected: No change in behavior from pre-feature version
  - Expected: No `prompt` parameter in API request logs
  - Expected: Transcription quality unchanged

### Transcription Accuracy Testing
- [ ] Create test audio: `artifacts/audio_claude.wav` (contains "Claude")
- [ ] Baseline test (no hints)
  - Transcribe audio without hints
  - Expected: Likely transcribes as "Cloud"
  - Document result in `artifacts/transcription_comparison.md`
- [ ] Hints test
  - Set hints: "Claude, Anthropic"
  - Transcribe same audio
  - Expected: Transcribes as "Claude"
  - Document improvement in `artifacts/transcription_comparison.md`
- [ ] Create additional test audio
  - [ ] `artifacts/audio_anthropic.wav` (contains "Anthropic")
  - [ ] `artifacts/audio_acronyms.wav` (contains "LLM, GPT, API, SDK")

**Acceptance**: All edge cases handled, backward compatibility verified, transcription accuracy improved with hints

---

## Phase 5: Documentation

**Objective**: Document feature usage and best practices

- [ ] Update user guide
  - Add "Transcription Settings" section
  - Explain vocabulary hints feature
  - Provide usage examples (comma-separated list)
- [ ] Document best practices
  - When to use vocabulary hints (proper nouns, acronyms, domain terms)
  - Example hint lists for common domains (AI/ML, medical, legal)
  - Token limit guidance (~160 words max)
- [ ] Add troubleshooting section
  - Token limit exceeded error (how to shorten hints)
  - Hints not improving accuracy (explain biasing behavior, not enforcement)
  - Special character handling (clarify no escaping needed)
- [ ] Capture implementation insights
  - Document learnings in `insights/` directory
  - Examples: Token estimation accuracy, UI/UX decisions, API behavior observations

**Acceptance**: Feature is documented with clear usage guidance and troubleshooting

---

## Completion Criteria

**Task is complete when**:
- ✅ All checklist items marked complete
- ✅ All unit tests pass
- ✅ Integration tests demonstrate accuracy improvement
- ✅ Manual testing confirms UI/UX meets requirements
- ✅ Backward compatibility verified (old configs load without errors)
- ✅ Documentation updated with usage examples

**Scrutinizer Re-Audit**: Run `x-role-activate --as scrutinizer 0007` after completion to verify repository-completeness

---

**Source**: Discovered during Task 0006 system audio capture work (prompt parameter investigation)
**Last Updated**: 2026-01-31 (Scrutinizer remediation - clarified specifications)
**Progress**: 0% (Structure complete, implementation pending)
