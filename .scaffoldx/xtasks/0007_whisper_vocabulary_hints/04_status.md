---
status: todo
progress: 0%
last_updated: 2026-01-31
completed_on: null
---

# Task Status: Whisper Vocabulary Hints

Current status: **Not Started** (0% complete)

## Progress Notes

- **2026-01-23**: Task created (initial scope: add text field to existing API Configuration panel)
- **2026-01-31**: Scrutinizer audit completed
  - Identified missing files (00_meta.md, 02_implementation_plan.md, 04_status.md)
  - Identified incorrect file naming (01_overview.md → 01_prd.md)
  - Flagged specification ambiguities (UI placement, empty string handling, token validation)
- **2026-01-31**: Auto-remediation completed
  - Created 00_meta.md with YAML frontmatter and structured metadata
  - Renamed and enhanced 01_prd.md with full PRD schema (user stories, acceptance criteria, API reference)
  - Created 02_implementation_plan.md with comprehensive technical approach
  - Created 04_status.md (this file)
  - Created insights/ and artifacts/ directory structures
  - **Scope Adjustment**: Complexity upgraded from LOW → MEDIUM due to new settings panel requirement
- **2026-01-31**: Architecture decision finalized
  - **UI Placement**: Create separate "Transcription Settings" panel (not adding to existing API Configuration)
  - **Rationale**: Existing panel crowded; new panel enables future expansion without conflation
  - **Future-proofing**: Panel may evolve into "AI Modifiers" section if scope expands
- **2026-01-31**: Specification clarifications captured
  - **Empty Handling**: Skip prompt if `null || trim().isEmpty()`
  - **Token Validation**: Approximate as `word_count * 1.3`, show warning if >224
  - **No Hard Truncation**: User decides what to remove, API handles enforcement

## Blockers

None currently.

## Next Actions

1. **Phase 1 (ConfigManager)**:
   - [ ] Add `vocabularyHints` field (String, default "")
   - [ ] Implement getter/setter with persistence
   - [ ] Write unit tests (default value, save/load cycle, null handling)
   - [ ] Manual test: Verify config.json includes field after save

2. **Phase 2 (UI Panel)**:
   - [ ] Create `TranscriptionSettingsPanel.java` class
   - [ ] Implement UI layout (text area, help text, token warning)
   - [ ] Add DocumentListener for real-time token validation
   - [ ] Implement `estimateTokenCount()` method (word_count * 1.3)
   - [ ] Wire Save/Cancel button actions
   - [ ] Integrate panel into main UI left navigation
   - [ ] Manual test: UI layout, token warning behavior, save/load

3. **Phase 3 (API Integration)**:
   - [ ] Modify `OpenAITranscribeClient.buildMultipartRequest()`
   - [ ] Add conditional `prompt` parameter (only if hints non-empty after trim)
   - [ ] Write unit tests (with hints, without hints, whitespace-only)
   - [ ] Integration test: Real API call with hints, verify accuracy improvement
   - [ ] Regression test: Real API call without hints, verify no degradation

4. **Phase 4 (Comprehensive Testing)**:
   - [ ] Backward compatibility test: Load old config file, verify no errors
   - [ ] Edge case test: Null, whitespace-only, >224 tokens
   - [ ] Manual test: Record audio with "Claude", transcribe with/without hints
   - [ ] Create test audio artifacts in `artifacts/` directory
   - [ ] Document test results in `insights/`

5. **Phase 5 (Documentation)**:
   - [ ] Update user guide with "Transcription Settings" section
   - [ ] Document vocabulary hint best practices and examples
   - [ ] Add troubleshooting section (token limit errors, hint effectiveness)

## Implementation Readiness

- ✅ PRD approved (full requirements, acceptance criteria, user stories)
- ✅ Implementation plan approved (technical approach, architecture, testing strategy)
- ✅ Specification ambiguities resolved (UI placement, empty handling, token validation)
- ✅ Task structure complete (all required files present)
- ⏳ Ready for development to begin

## Completion Summary

*(To be filled when task is completed)*

---

**Last Status Update**: 2026-01-31 (Scrutinizer remediation complete)
**Next Update**: When Phase 1 (ConfigManager) work begins
