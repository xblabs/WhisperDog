---
title: Implementation Plan - Whisper Vocabulary Hints
description: Technical approach and implementation steps for vocabulary hints feature
context_type: implementation
priority: medium
last_updated: 2026-01-31
related_prd: 01_prd.md
status: approved
---

# Implementation Plan: Whisper Vocabulary Hints

## 1. Technical Approach

This feature adds vocabulary hint support to WhisperDog's transcription pipeline by:
1. Extending ConfigManager to store user-provided vocabulary hints
2. Creating a new "Transcription Settings" panel in the UI (separate from crowded API Configuration)
3. Wiring the hints into OpenAITranscribeClient's multipart request as the `prompt` parameter
4. Implementing client-side token limit validation with user warnings

**Key Principle**: Minimal invasiveness - changes isolated to 3 components, no modifications to audio pipeline or transcription processing.

## 2. Architecture

### 2.1 Component Overview

```
┌─────────────────────────────────────────────────────────┐
│                     User Interface                       │
│  ┌────────────────────────────────────────────────┐    │
│  │  New: TranscriptionSettingsPanel               │    │
│  │  - vocabularyHintsField (JTextArea)            │    │
│  │  - tokenWarningLabel (JLabel)                  │    │
│  │  - helpTooltip (JToolTip)                      │    │
│  └──────────────────┬─────────────────────────────┘    │
└─────────────────────┼──────────────────────────────────┘
                      │ save/load
                      ↓
┌─────────────────────────────────────────────────────────┐
│                   ConfigManager                          │
│  + String vocabularyHints (default: "")                 │
│  + getVocabularyHints(): String                         │
│  + setVocabularyHints(String): void                     │
└──────────────────┬──────────────────────────────────────┘
                   │ read config
                   ↓
┌─────────────────────────────────────────────────────────┐
│              OpenAITranscribeClient                      │
│  buildMultipartRequest():                               │
│    - Read vocabularyHints from ConfigManager            │
│    - If non-empty (after trim):                         │
│        Add "prompt" field to multipart request          │
│    - Else: omit "prompt" parameter                      │
└─────────────────────────────────────────────────────────┘
                   │
                   ↓ HTTP POST
         ┌─────────────────────┐
         │  OpenAI Whisper API │
         │  /v1/audio/transcriptions  │
         └─────────────────────┘
```

### 2.2 Data Flow

1. **Configuration Phase**:
   - User opens Transcription Settings panel (new left menu item)
   - Enters comma-separated vocabulary hints (e.g., "Claude, Anthropic, LLM")
   - Token validation runs on input change → shows warning if >224 estimated tokens
   - User saves settings → ConfigManager persists to config file

2. **Transcription Phase**:
   - User initiates recording/transcription
   - OpenAITranscribeClient reads `vocabularyHints` from ConfigManager
   - If `vocabularyHints != null && !vocabularyHints.trim().isEmpty()`:
     - Add `prompt` parameter to multipart request
   - Send request to OpenAI API
   - API uses prompt to bias transcription toward hinted vocabulary

3. **Edge Cases**:
   - Null value: Treated as empty (no prompt sent)
   - Whitespace-only: Trimmed and treated as empty
   - Exceeds 224 tokens: Warning shown in UI, but request still sent (API returns error if too long)
   - Missing config field: Defaults to empty string on load

## 3. Implementation Steps

### Phase 1: ConfigManager Extension

**File**: `src/main/java/org/whisperdog/config/ConfigManager.java`

**Changes**:
1. Add field:
   ```java
   private String vocabularyHints = ""; // Default: empty
   ```

2. Add getter:
   ```java
   public String getVocabularyHints() {
       return vocabularyHints != null ? vocabularyHints : "";
   }
   ```

3. Add setter:
   ```java
   public void setVocabularyHints(String vocabularyHints) {
       this.vocabularyHints = vocabularyHints;
       saveConfig(); // Persist immediately
   }
   ```

4. Update JSON serialization:
   - Ensure Gson includes `vocabularyHints` field in config file
   - Add default value handling for missing field (backward compatibility)

**Testing**:
- Unit test: Getter returns default empty string for new configs
- Unit test: Setter persists value across save/load cycle
- Manual test: Verify config.json includes `vocabularyHints` field after save

### Phase 2: Transcription Settings Panel (UI)

**New File**: `src/main/java/org/whisperdog/ui/TranscriptionSettingsPanel.java`

**Panel Structure**:
```
┌─ Transcription Settings ───────────────────────────┐
│                                                     │
│  Vocabulary Hints                                   │
│  ┌───────────────────────────────────────────────┐ │
│  │ Claude, Anthropic, LLM, WhisperDog            │ │
│  │                                               │ │
│  │                                               │ │
│  └───────────────────────────────────────────────┘ │
│  ⓘ Comma-separated terms to improve transcription  │
│     accuracy (proper nouns, acronyms). Limit: ~160 │
│     words (224 tokens).                            │
│                                                     │
│  ⚠️ Input may exceed token limit. Consider         │
│     shortening. (shown only if >224 tokens)        │
│                                                     │
│  [ Save ]  [ Cancel ]                              │
└─────────────────────────────────────────────────────┘
```

**Implementation**:
1. Create panel extending `JPanel`
2. Add components:
   - `JLabel` title: "Vocabulary Hints"
   - `JTextArea` (3 rows, wrap enabled): `vocabularyHintsField`
   - `JScrollPane` wrapping text area
   - `JLabel` help text (smaller font, gray): Tooltip content
   - `JLabel` warning (red, initially hidden): `tokenWarningLabel`
   - Save/Cancel buttons

3. Add DocumentListener to `vocabularyHintsField`:
   ```java
   vocabularyHintsField.getDocument().addDocumentListener(new DocumentListener() {
       public void changedUpdate(DocumentEvent e) { validateTokenLimit(); }
       public void insertUpdate(DocumentEvent e) { validateTokenLimit(); }
       public void removeUpdate(DocumentEvent e) { validateTokenLimit(); }
   });
   ```

4. Implement token validation:
   ```java
   private void validateTokenLimit() {
       String text = vocabularyHintsField.getText();
       int estimatedTokens = estimateTokenCount(text);

       if (estimatedTokens > 224) {
           tokenWarningLabel.setText("⚠️ Input may exceed token limit (" + estimatedTokens + " estimated). Consider shortening.");
           tokenWarningLabel.setVisible(true);
       } else {
           tokenWarningLabel.setVisible(false);
       }
   }

   private int estimateTokenCount(String text) {
       // Conservative estimate: word_count * 1.3
       String[] words = text.trim().split("\\s+");
       return (int) Math.ceil(words.length * 1.3);
   }
   ```

5. Wire Save button:
   ```java
   saveButton.addActionListener(e -> {
       String hints = vocabularyHintsField.getText();
       ConfigManager.getInstance().setVocabularyHints(hints);
       dispose(); // Close panel
   });
   ```

**Integration with Main UI**:
- Add menu item to left navigation: "Transcription Settings" (or icon)
- Position after "API Configuration" or in separate "AI Modifiers" section
- Clicking menu item opens TranscriptionSettingsPanel in main content area

**Testing**:
- Manual test: Open panel, verify UI layout and help text
- Manual test: Enter >160 words, verify warning appears
- Manual test: Save hints, verify ConfigManager updated
- Manual test: Reopen panel, verify hints loaded from config

### Phase 3: API Integration

**File**: `src/main/java/org/whisperdog/api/OpenAITranscribeClient.java`

**Method**: `buildMultipartRequest()` (or equivalent multipart builder)

**Changes**:
1. Read vocabulary hints from config:
   ```java
   String vocabularyHints = ConfigManager.getInstance().getVocabularyHints();
   ```

2. Add prompt parameter if non-empty:
   ```java
   if (vocabularyHints != null && !vocabularyHints.trim().isEmpty()) {
       requestBody.addFormDataPart("prompt", vocabularyHints.trim());
   }
   ```

3. No changes to response handling (prompt doesn't affect response structure)

**Testing**:
- Unit test: Mock ConfigManager, verify prompt field added when hints present
- Unit test: Mock ConfigManager with empty string, verify prompt field omitted
- Unit test: Mock ConfigManager with whitespace-only, verify prompt field omitted
- Integration test: Real API call with hints="Claude, Anthropic", verify transcription accuracy
- Regression test: Real API call with no hints, verify no transcription degradation

### Phase 4: Edge Case Handling

**Scenario 1: Missing Config Field (Backward Compatibility)**
- **Behavior**: Existing config files don't have `vocabularyHints` field
- **Handling**: ConfigManager's Gson deserialization sets field to null → getter returns "" (default)
- **Test**: Load old config file, verify no errors and vocabularyHints returns ""

**Scenario 2: Null Value**
- **Behavior**: ConfigManager returns null for vocabularyHints
- **Handling**: OpenAITranscribeClient checks `vocabularyHints != null` before adding prompt
- **Test**: Unit test with null value, verify prompt omitted

**Scenario 3: Whitespace-Only Input**
- **Behavior**: User enters "   \n   " (spaces, newlines, tabs)
- **Handling**: OpenAITranscribeClient uses `.trim().isEmpty()` check
- **Test**: Integration test with whitespace-only, verify prompt omitted

**Scenario 4: Exceeds 224 Tokens**
- **Behavior**: User enters >224 tokens (warning shown but not blocked)
- **Handling**: API request sent with full prompt → OpenAI returns 400 error
- **Test**: Manual test with 300-word hints, verify API error logged gracefully

**Scenario 5: Special Characters**
- **Behavior**: User enters `"Claude", O'Reilly, 50% accuracy`
- **Handling**: OpenAI API handles raw text, no escaping needed
- **Test**: Integration test with special chars, verify no request errors

## 4. Technical Considerations

### 4.1 Token Limit Validation Accuracy

**Challenge**: Exact token count requires OpenAI's tokenizer (tiktoken)
**Decision**: Use approximation (word_count * 1.3) for client-side validation
**Trade-off**: May underestimate tokens for long words, overestimate for short words
**Mitigation**: Use conservative multiplier (1.3) to bias toward showing warnings

### 4.2 UI Placement Architecture Decision

**Challenge**: Existing API Configuration panel is crowded
**Decision**: Create separate "Transcription Settings" panel with dedicated menu item
**Rationale**:
- Future-proofs for additional transcription features (hint templates, per-language settings)
- Avoids conflating API credentials with transcription options
- Enables future rename to "AI Modifiers" if scope expands (pre-mediation vs post-processing)

**Alternative Considered**: Add to API Configuration panel
- **Rejected**: Panel already has 5+ fields, adding more degrades UX

### 4.3 Prompt Parameter Behavior

**OpenAI API Notes**:
- Prompt biases model but doesn't guarantee exact matches
- Works best for proper nouns with clear phonetic alternatives
- Can influence punctuation and capitalization across entire transcription
- Exceeding 224 tokens causes API error (not silently truncated)

**Implication**: Users should treat this as "guidance" not "enforcement"

### 4.4 Performance Impact

**Token Validation**: Runs on every keystroke in text area
- Complexity: O(n) where n = character count (split by whitespace)
- Expected input size: <200 words (avg 1000 chars)
- Performance: <1ms per keystroke on modern hardware

**API Request**: Adding `prompt` parameter increases request size by ~100-1000 bytes
- Impact: Negligible (<10ms additional latency)

### 4.5 Backward Compatibility Strategy

**Config File Evolution**:
```json
// Old config (pre-Task-0007)
{
  "apiKey": "sk-...",
  "transcriptionLanguage": "en"
}

// New config (post-Task-0007)
{
  "apiKey": "sk-...",
  "transcriptionLanguage": "en",
  "vocabularyHints": ""  // New field, defaults to empty
}
```

**Migration**: No explicit migration needed
- Gson sets missing fields to null
- ConfigManager getter converts null → "" (default)
- No user action required

## 5. Testing Strategy

### 5.1 Unit Tests

**ConfigManager Tests** (`ConfigManagerTest.java`):
```java
@Test
public void testVocabularyHints_DefaultValue() {
    ConfigManager config = new ConfigManager();
    assertEquals("", config.getVocabularyHints());
}

@Test
public void testVocabularyHints_SaveLoad() {
    ConfigManager config = new ConfigManager();
    config.setVocabularyHints("Claude, Anthropic");

    // Simulate reload
    ConfigManager reloaded = ConfigManager.loadFromFile();
    assertEquals("Claude, Anthropic", reloaded.getVocabularyHints());
}

@Test
public void testVocabularyHints_NullHandling() {
    ConfigManager config = new ConfigManager();
    config.setVocabularyHints(null);
    assertEquals("", config.getVocabularyHints()); // Converts null to empty
}
```

**OpenAITranscribeClient Tests** (`OpenAITranscribeClientTest.java`):
```java
@Test
public void testBuildRequest_WithVocabularyHints() {
    // Mock ConfigManager to return hints
    when(mockConfig.getVocabularyHints()).thenReturn("Claude, Anthropic");

    MultipartBody request = client.buildMultipartRequest(audioFile);

    assertTrue(request.hasFormDataPart("prompt"));
    assertEquals("Claude, Anthropic", request.getFormDataPart("prompt"));
}

@Test
public void testBuildRequest_WithoutVocabularyHints() {
    when(mockConfig.getVocabularyHints()).thenReturn("");

    MultipartBody request = client.buildMultipartRequest(audioFile);

    assertFalse(request.hasFormDataPart("prompt"));
}

@Test
public void testBuildRequest_WhitespaceOnlyHints() {
    when(mockConfig.getVocabularyHints()).thenReturn("   \n   ");

    MultipartBody request = client.buildMultipartRequest(audioFile);

    assertFalse(request.hasFormDataPart("prompt")); // Trimmed to empty
}
```

### 5.2 Integration Tests

**End-to-End Transcription Test**:
```java
@Test
public void testTranscription_WithVocabularyHints() {
    // Use test audio file containing "Claude"
    File audioFile = new File("test/resources/audio_with_claude.wav");

    // Test WITHOUT hints (baseline)
    ConfigManager.getInstance().setVocabularyHints("");
    String transcriptWithoutHints = transcribe(audioFile);
    assertTrue(transcriptWithoutHints.contains("Cloud")); // Likely mistranscription

    // Test WITH hints
    ConfigManager.getInstance().setVocabularyHints("Claude, Anthropic");
    String transcriptWithHints = transcribe(audioFile);
    assertTrue(transcriptWithHints.contains("Claude")); // Correct transcription
}
```

### 5.3 Manual Testing

**Test Case 1: Settings UI**
1. Launch WhisperDog
2. Open "Transcription Settings" panel
3. Verify help text is visible and clear
4. Enter "Claude, Anthropic, LLM, WhisperDog, Dario Amodei"
5. Click Save → verify no errors
6. Reopen panel → verify hints persisted

**Test Case 2: Token Warning**
1. Open Transcription Settings
2. Paste 200 words into vocabulary hints field
3. Verify warning label appears: "⚠️ Input may exceed token limit"
4. Delete half the text
5. Verify warning disappears

**Test Case 3: Transcription Accuracy**
1. Record audio saying "I'm using Claude from Anthropic"
2. WITHOUT hints: Transcribe → likely produces "I'm using Cloud from Anthropic"
3. Set hints: "Claude, Anthropic"
4. Transcribe same audio → should produce "I'm using Claude from Anthropic"
5. Verify improved accuracy

**Test Case 4: Backward Compatibility**
1. Locate existing config file (from version without vocabularyHints)
2. Launch updated WhisperDog
3. Verify app loads without errors
4. Open Transcription Settings → verify field is empty (default)

**Test Case 5: Empty Hints Regression**
1. Ensure vocabularyHints is empty
2. Transcribe audio → verify no errors
3. Check logs → verify no "prompt" parameter in API request
4. Verify transcription quality matches pre-feature behavior

### 5.4 Test Data

**Audio Files for Testing**:
- `test/resources/audio_claude.wav` - Audio containing "Claude" (should transcribe as "Cloud" without hints)
- `test/resources/audio_anthropic.wav` - Audio containing "Anthropic"
- `test/resources/audio_acronyms.wav` - Audio containing "LLM, GPT, API, SDK"

**Vocabulary Hint Examples**:
- Simple: "Claude"
- Multiple terms: "Claude, Anthropic, LLM"
- With company names: "Anthropic, OpenAI, Google, Microsoft"
- With technical terms: "API, SDK, RAG, embeddings, transformer"
- Long list (near limit): 160-word list of proper nouns

## 6. Dependencies & Prerequisites

### External Dependencies
- **OpenAI Whisper API**: Must support `prompt` parameter (already in use)
- No new libraries required

### Internal Dependencies
- **ConfigManager**: Existing persistence system
- **SettingsForm architecture**: Framework for adding new settings panels
- **OpenAITranscribeClient**: Existing API client

### Development Prerequisites
- Java 11+ (existing requirement)
- Swing UI toolkit (existing dependency)
- Gson for JSON serialization (existing dependency)

## 7. Estimated Effort

### Complexity Assessment
- **Overall**: MEDIUM (upgraded from LOW due to new settings panel requirement)
- **ConfigManager changes**: LOW (simple field addition)
- **UI implementation**: MEDIUM (new panel creation, token validation logic)
- **API integration**: LOW (single parameter addition)
- **Testing**: MEDIUM (comprehensive coverage needed for edge cases)

### Implementation Time Breakdown
*(Note: Time estimates removed per behavioral guidelines - focusing on scope only)*

**Phase 1: ConfigManager** - Simple field addition and persistence
**Phase 2: UI Panel** - New component with validation logic
**Phase 3: API Integration** - Single parameter wiring
**Phase 4: Testing** - Unit, integration, and manual testing

### Risk Factors
- **UI Complexity**: Creating new settings panel vs adding field to existing panel
- **Token Validation**: Approximation may not match OpenAI's exact tokenizer
- **API Behavior**: Prompt parameter behavior may vary across Whisper models

## 8. Rollout Plan

### Development Sequence
1. ConfigManager changes (enables testing without UI)
2. API integration (enables command-line testing)
3. UI panel (user-facing functionality)
4. Comprehensive testing
5. Documentation updates

### Testing Sequence
1. Unit tests (ConfigManager, OpenAITranscribeClient)
2. Integration tests (end-to-end transcription)
3. Manual UI testing
4. Backward compatibility verification
5. Real-world usage testing (dogfooding)

### Documentation Updates
- Add "Transcription Settings" section to user guide
- Document vocabulary hint best practices (examples, formatting)
- Add troubleshooting section (token limit errors, hint effectiveness)

## 9. Future Enhancements (Out of Current Scope)

**Post-MVP Opportunities**:
1. **Hint Templates**: Pre-defined hint sets (e.g., "AI/ML", "Medical", "Legal")
2. **Hint Management**: Save/load multiple hint configurations
3. **Context-Aware Suggestions**: Analyze recent transcriptions to suggest hints
4. **Per-Recording Overrides**: Allow hints to be set for individual recordings
5. **Effectiveness Analytics**: Measure and report hint impact on accuracy
6. **Integration with Post-Processing**: Pass hints to LLM for consistency
7. **Token Limit Enforcement**: Real tokenization using tiktoken library

---

**Implementation Plan Status**: Approved
**Ready for Development**: Yes (pending final UI placement decision)
**Last Updated**: 2026-01-31
