---
title: PRD - Whisper Vocabulary Hints
description: Requirements and specifications for Whisper vocabulary hints feature
context_type: prd
priority: medium
last_updated: 2026-01-31
status: approved
related_tasks: ["0006"]
---

# PRD: Whisper Vocabulary Hints

## 1. Overview

Add a user-configurable "Vocabulary hints" setting that leverages OpenAI Whisper's `prompt` parameter to improve transcription accuracy for domain-specific terms. This feature allows users to prime the transcription model with proper nouns, acronyms, technical terms, and other vocabulary that is frequently mistranscribed due to phonetic ambiguity (e.g., "Claude" often transcribed as "Cloud").

## 2. Goals

### Primary Goal
Enable users to provide context-specific vocabulary hints that improve Whisper transcription accuracy for proper nouns and domain terminology.

### Secondary Goals
- Minimize implementation complexity (leverage existing API feature)
- Provide clear user guidance on feature usage
- Establish foundation for future "AI Modifiers" settings panel
- Maintain backward compatibility with existing configurations

## 3. Requirements

### 3.1 Functional Requirements

**FR-1: Configuration Storage**
- Add `vocabularyHints` field to ConfigManager (String type, default empty string)
- Persist value across application restarts
- Support empty string as valid state (no hints provided)

**FR-2: Settings UI**
- Create new "Transcription Settings" panel with dedicated left menu icon
  - Alternative name: "AI Modifiers" (future-proofing for other pre-mediation features)
  - Rationale: Existing API Configuration panel is crowded; this enables future expansion
- Add multi-line text field for vocabulary hints input
- Display tooltip explaining:
  - Purpose: "Improve transcription of proper nouns and domain-specific terms"
  - Format: "Comma-separated terms (e.g., Claude, Anthropic, LLM, WhisperDog)"
  - Token limit: "Limit: ~160 words (224 tokens)"
- Show warning if input exceeds ~160 words (approximate token limit)

**FR-3: API Integration**
- Wire `vocabularyHints` value into OpenAITranscribeClient
- Add `prompt` parameter to multipart request when vocabularyHints is non-empty
- Skip `prompt` parameter when `vocabularyHints == null || vocabularyHints.trim().isEmpty()`
- No client-side escaping needed (OpenAI API handles raw text)

**FR-4: Token Limit Validation**
- Approximate token count as `word_count * 1.3` (conservative estimate)
- Show warning indicator if estimated tokens > 224
- Do NOT hard-truncate (user should decide what to remove)
- Warning message: "Input may exceed OpenAI's 224-token limit. Consider shortening."

### 3.2 Non-Functional Requirements

**NFR-1: Performance**
- Vocabulary hints processing must not add perceptible latency to transcription requests
- Token count validation should be instantaneous (<50ms)

**NFR-2: Usability**
- Tooltip/help text must clearly explain feature purpose and usage
- Warning messages must be non-blocking (informational, not errors)
- Empty field should be visually distinct from field with whitespace

**NFR-3: Compatibility**
- Existing configurations without `vocabularyHints` field must default to empty string
- Feature must degrade gracefully if OpenAI API ignores/rejects prompt parameter

**NFR-4: Maintainability**
- Code changes should be isolated to 3 components: ConfigManager, new settings panel, OpenAITranscribeClient
- No changes to audio pipeline or transcription result handling

## 4. User Stories

**US-1: Domain-Specific Transcription**
> As a user transcribing AI/ML content, I want to provide vocabulary hints like "Claude, Anthropic, GPT, LLM" so that these terms are transcribed correctly instead of phonetic approximations like "Cloud, anthropic, GBT, LOM".

**US-2: Proper Noun Accuracy**
> As a user recording interviews with named individuals, I want to provide names as hints (e.g., "Dario Amodei, Anthropic") so that the transcription uses correct spellings rather than guessing.

**US-3: Acronym Handling**
> As a user in technical meetings, I want to provide acronyms (e.g., "API, SDK, LLM, RAG") so that Whisper recognizes them as distinct terms rather than trying to transcribe phonetically.

**US-4: Feature Discovery**
> As a new user, I want clear tooltip guidance explaining what vocabulary hints are and how to format them, so I can use the feature effectively without trial-and-error.

## 5. Acceptance Criteria

**AC-1: Configuration Persistence**
- [ ] `vocabularyHints` value persists across app restarts
- [ ] Empty string is handled as valid (no prompt sent to API)
- [ ] Whitespace-only input is treated as empty (trimmed before checking)

**AC-2: UI Implementation**
- [ ] New "Transcription Settings" panel accessible from left menu
- [ ] Multi-line text field with clear label: "Vocabulary Hints"
- [ ] Tooltip displays usage guidance (format, examples, token limit)
- [ ] Warning appears when estimated tokens > 224

**AC-3: Transcription Accuracy**
- [ ] Test case: Transcribe audio containing "Claude" without hints → likely produces "Cloud"
- [ ] Test case: Transcribe same audio with hints="Claude, Anthropic" → produces "Claude"
- [ ] Test case: Empty hints → no regression in transcription quality

**AC-4: API Integration**
- [ ] `prompt` parameter appears in multipart request when hints provided
- [ ] `prompt` parameter absent from request when hints empty/null
- [ ] No transcription errors when hints exceed 224 tokens (API handles gracefully)

**AC-5: Backward Compatibility**
- [ ] Existing config files without `vocabularyHints` field load successfully
- [ ] Default value (empty string) is applied for missing field

## 6. Dependencies

### External Dependencies
- **OpenAI Whisper API**: `/v1/audio/transcriptions` endpoint with `prompt` parameter support
  - API Reference: [OpenAI Audio API Documentation](https://platform.openai.com/docs/api-reference/audio)
  - `prompt` parameter: Optional string, max 224 tokens
  - Purpose: Guide transcription style, vocabulary, and context

### Internal Dependencies
- **ConfigManager**: Requires new field with getter/setter
- **SettingsForm** (or new TranscriptionSettingsPanel): UI component for user input
- **OpenAITranscribeClient**: Multipart request builder needs prompt parameter

### Task Dependencies
- No blocking dependencies
- Related to Task 0006 (System Audio Capture) - same OpenAI API investigation origin

## 7. Out of Scope

**Explicitly NOT Included:**
- ❌ Automatic vocabulary hint generation (AI-suggested terms based on content)
- ❌ Vocabulary hint templates/presets (e.g., "AI/ML", "Medical", "Legal")
- ❌ Per-recording hint overrides (global setting only)
- ❌ Hint effectiveness analytics (measuring impact on accuracy)
- ❌ Integration with post-processing LLM (this is pre-transcription only)
- ❌ Token limit enforcement (hard truncation) - user decides what to keep
- ❌ Validation of hint quality (duplicate detection, spell-check)

**Future Enhancements (Not in Scope):**
- Hint management UI (save/load hint sets)
- Context-aware hint suggestions
- Per-language hint sets
- Hint effectiveness reporting

## 8. API Reference

### OpenAI Whisper `/v1/audio/transcriptions`

**Supported Parameters:**
- `file` (required): Audio file to transcribe
- `model` (required): Model ID (e.g., "whisper-1")
- `language` (optional): ISO-639-1 language code
- **`prompt` (optional)**: Text to guide transcription
  - Type: String
  - Max length: 224 tokens
  - Purpose: Prime proper noun spelling, maintain context across segments, influence punctuation
  - Behavior: Model uses prompt as context but does not guarantee exact matches
  - Example: "Claude, Anthropic, LLM, GPT-4, Dario Amodei"

**Usage Notes:**
- Prompt is NOT guaranteed to force specific transcriptions (biases model, doesn't override)
- Works best with proper nouns and acronyms (clear phonetic alternatives)
- Comma-separated list is convention, but any text format works
- Exceeding 224 tokens results in API error (400 Bad Request)

## 9. Success Metrics

**Qualitative:**
- Users report improved transcription accuracy for domain-specific terms
- Feature is discoverable (users find it without extensive documentation)
- No user confusion about feature purpose or usage

**Quantitative (Manual Testing):**
- Baseline: Audio with "Claude" → transcribes as "Cloud" (0% accuracy)
- With hints: Audio with "Claude" + hints="Claude, Anthropic" → transcribes as "Claude" (100% accuracy)
- Target: >80% accuracy improvement for hinted terms in test corpus

## 10. Open Questions

**Q1: Should we create "Transcription Settings" or "AI Modifiers" panel name?**
- Decision pending: "Transcription Settings" more descriptive, "AI Modifiers" more future-proof
- Recommendation: Start with "Transcription Settings", rename later if scope expands

**Q2: Should warning be tooltip, inline text, or dialog?**
- Recommendation: Inline warning label (non-intrusive, always visible when over limit)

**Q3: Do we need a "Test Hints" feature (quick transcription test)?**
- Out of scope for initial implementation
- Future enhancement candidate

---

**PRD Status**: Approved for implementation
**Last Updated**: 2026-01-31
**Approved By**: Scrutinizer audit + user clarification session
