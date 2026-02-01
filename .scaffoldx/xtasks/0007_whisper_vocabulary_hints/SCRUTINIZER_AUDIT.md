# Repository Thinking Audit: Task 0007 (Whisper Vocabulary Hints)

**Auditor**: Scrutinizer Role
**Target**: `.scaffoldx/xtasks/0007_whisper_vocabulary_hints/`
**Audit Date**: 2026-01-31
**Audit Context**: Structure completeness and specification quality

---

## Completeness Score: 1/5

| Question | Status | Fatal Gap |
|----------|--------|-----------|
| 1. WHAT files? | ❌ | Missing required files: 00_meta.md, 02_implementation_plan.md, 04_status.md; incorrect file name: 01_overview.md → 01_prd.md; missing directories: insights/, artifacts/ |
| 2. WHAT content? | ⚠️ | 01_overview.md exists but lacks PRD schema; no implementation plan content; no status tracking |
| 3. WHAT triggers? | ✅ | Checklist provides clear trigger sequence |
| 4. HOW done? | ⚠️ | Checklist defines completion but no overall status tracking |
| 5. Edge cases? | ❌ | No edge case handling specified; empty string behavior mentioned but not tested |

---

## Structural Violations

### Missing Required Files (CRITICAL)

According to ScaffoldX standardized task structure, each task MUST contain:

| Required File | Status | Impact |
|---------------|--------|--------|
| `00_meta.md` | ❌ MISSING | No structured metadata (ID, status, priority, tags, dependencies) |
| `01_prd.md` | ❌ WRONG NAME | File exists as `01_overview.md` - violates naming convention |
| `02_implementation_plan.md` | ❌ MISSING | No technical approach, architecture, or testing strategy |
| `03_checklist.md` | ✅ EXISTS | Properly structured |
| `04_status.md` | ❌ MISSING | No progress tracking or blocker visibility |

### Missing Optional Directories (RECOMMENDED)

| Directory | Status | Impact |
|-----------|--------|--------|
| `insights/` | ❌ MISSING | No mechanism to capture learnings during implementation |
| `artifacts/` | ❌ MISSING | No storage for test examples, mock data, or validation outputs |

---

## Ambiguity Flags

### File: 01_overview.md

| Line | Text | Problem | Fix |
|------|------|---------|-----|
| 1 | "# Task 0007: Whisper Vocabulary Hints" | File should be named 01_prd.md | Rename file and add PRD frontmatter |
| 11 | "Add a text field in Settings" | WHERE in Settings? Which panel? | Specify: "Add to API Configuration panel in SettingsForm, below transcriptionLanguage field" |
| 12 | "comma-separated terms" | How are commas in terms handled? Escaping? | Specify: "Comma-separated (no escaping needed - OpenAI handles raw text)" |
| 13 | "Wire the prompt into OpenAITranscribeClient" | WHICH method? WHERE in the request? | Specify: "In buildMultipartRequest(), add 'prompt' field if vocabularyHints is non-empty" |
| 21 | "Optional text to guide transcription style/vocabulary" | What is the character limit? | Specify: "Max 224 tokens per OpenAI API spec" |

### File: 03_checklist.md

| Line | Text | Problem | Fix |
|------|------|---------|-----|
| 9 | "Add `vocabularyHints` field to ConfigManager" | What data type? Default value? | Specify: "String field, default empty string ("")" |
| 20 | "Add `prompt` parameter to multipart request (skip if empty)" | What counts as empty? null? "" ? whitespace-only? | Specify: "Skip if vocabularyHints == null OR vocabularyHints.trim().isEmpty()" |
| 22 | "Test transcription with vocabulary hints set" | WHAT specific test case? | Provide concrete example: "Test with 'Claude,Anthropic,LLM' and verify transcription accuracy" |

---

## Required Additions

### 1. Create 00_meta.md (CRITICAL)

**Purpose**: Provide structured task metadata for tracking and discovery
**Estimated Lines**: 20-25

**Required Content**:
```markdown
---
task_id: "0007"
title: "Whisper Vocabulary Hints"
status: todo
priority: medium
created: 2026-01-23
tags: [whisper, transcription, settings, api-integration]
complexity: low
estimated_phases: 1
parent_task: null
related_tasks: ["0006"]
dependencies: []
---

# Task 0007: Whisper Vocabulary Hints

## Summary

Add a "Vocabulary hints" setting that sends a `prompt` parameter to the OpenAI Whisper API, allowing users to prime transcription with proper nouns, acronyms, and domain-specific terms.

## Source

Discovered during Task 0006 system audio capture work (prompt parameter investigation)
```

### 2. Create 02_implementation_plan.md (CRITICAL)

**Purpose**: Define technical approach, architecture, and testing strategy
**Estimated Lines**: 60-80

**Required Sections**:
1. **Technical Approach**: Describe ConfigManager → SettingsForm → OpenAITranscribeClient data flow
2. **Architecture**: Diagram showing component interactions
3. **Implementation Steps**: Break down into:
   - Phase 1: ConfigManager changes
   - Phase 2: SettingsForm UI changes
   - Phase 3: OpenAITranscribeClient API wiring
   - Phase 4: Testing
4. **Technical Considerations**:
   - Empty string handling
   - OpenAI token limit (224 tokens)
   - Backward compatibility (existing configs without this field)
5. **Testing Strategy**:
   - Unit test: ConfigManager getter/setter
   - Integration test: Settings save/load cycle
   - Manual test: Transcription with/without hints
6. **Edge Cases**:
   - Null value handling
   - Whitespace-only strings
   - Exceeding token limit (should we validate?)

### 3. Create 04_status.md (CRITICAL)

**Purpose**: Track overall progress and blockers
**Estimated Lines**: 15-20

**Required Content**:
```markdown
---
status: todo
progress: 0%
last_updated: 2026-01-31
completed_on: null
---

# Task Status: Whisper Vocabulary Hints

Current status: **Not Started** (0% complete)

## Progress Notes
- 2026-01-23: Task created
- 2026-01-31: Scrutinizer audit completed - identified structural gaps

## Blockers
None

## Next Actions
1. Create missing metadata file (00_meta.md)
2. Rename 01_overview.md → 01_prd.md and enhance with full PRD schema
3. Create implementation plan (02_implementation_plan.md)
4. Create insights/ and artifacts/ directories
```

### 4. Rename and Enhance 01_overview.md → 01_prd.md (CRITICAL)

**Purpose**: Align with naming convention and add full PRD structure
**Estimated Lines**: Current 22 → expand to 60-80

**Required Additions**:
- YAML frontmatter with metadata
- User Stories section: "As a user transcribing domain-specific content, I want to provide vocabulary hints so that proper nouns are transcribed correctly"
- Acceptance Criteria: Define measurable success (e.g., "Claude" transcribed as "Claude" not "Cloud" when hint provided)
- Dependencies section: List OpenAI API dependency
- Out of Scope: Clarify what is NOT included (e.g., automatic hint generation, hint validation)

### 5. Create insights/ Directory Structure (RECOMMENDED)

**Purpose**: Enable knowledge capture during implementation
**Estimated Lines**: N/A (directory structure)

**Required Structure**:
```
insights/
├── index.md
├── examples/
├── reflections/
└── insights/
```

### 6. Create artifacts/ Directory (RECOMMENDED)

**Purpose**: Store test examples and validation data
**Estimated Lines**: N/A (directory structure)

**Suggested Artifacts**:
- `test_vocabulary_hints.txt` - Example hint strings for testing
- `transcription_comparison.md` - Before/after comparison with hints enabled

---

## Content Specification Gaps

### Current State Analysis

**01_overview.md** (to become 01_prd.md):
- ✅ Clear motivation
- ✅ API reference included
- ❌ Missing acceptance criteria
- ❌ Missing user stories
- ❌ Missing dependencies
- ❌ No frontmatter metadata

**03_checklist.md**:
- ✅ Logical clustering (Settings/Persistence, API Integration)
- ✅ Sequential flow
- ⚠️ Missing test coverage details
- ⚠️ Ambiguous empty string handling

---

## Scrutinizer Kill Signals

### Linguistic Ambiguity Detected

| Pattern | Location | Fix |
|---------|----------|-----|
| "comma-separated terms" | 01_overview.md:12 | Define escaping rules or note that none are needed |
| "skip if empty" | 03_checklist.md:20 | Define empty: null? ""? whitespace? |
| "vocabulary hints set" | 03_checklist.md:22 | Provide concrete test example value |
| "Add a text field in Settings" | 01_overview.md:11 | Specify exact panel and position |

### Structural Ambiguity Detected

| Pattern | Location | Fix |
|---------|----------|-----|
| Missing metadata schema | Task root | Create 00_meta.md with YAML frontmatter |
| Missing implementation detail | Task root | Create 02_implementation_plan.md |
| Missing progress tracking | Task root | Create 04_status.md |
| No insight capture mechanism | Task root | Create insights/ directory |

---

## Verdict: FAIL

**Status**: ❌ NOT READY for autonomous execution

**Critical Gaps Summary**:
1. **Structural incompleteness**: Missing 4 of 5 required files, 1 file incorrectly named
2. **Specification ambiguity**: Multiple undefined behaviors (empty string, field placement, test cases)
3. **No progress tracking**: Cannot determine task state or blockers
4. **No knowledge capture**: Missing mechanisms to preserve learnings

**Autonomous Execution Risk**: HIGH
- LLM would need to make 6+ interpretation decisions
- Unclear success criteria would lead to inconsistent implementations
- No way to track progress or capture insights during work

---

## Remediation Checklist

Complete these items to achieve PASS status:

### Phase 1: Structural Compliance (CRITICAL)
- [ ] Create `00_meta.md` with YAML frontmatter and structured metadata
- [ ] Rename `01_overview.md` → `01_prd.md`
- [ ] Enhance `01_prd.md` with full PRD schema (user stories, acceptance criteria, dependencies)
- [ ] Create `02_implementation_plan.md` with technical approach and testing strategy
- [ ] Create `04_status.md` with progress tracking
- [ ] Create `insights/` directory structure (index.md, examples/, reflections/, insights/)
- [ ] Create `artifacts/` directory

### Phase 2: Specification Precision (CRITICAL)
- [ ] Define exact Settings panel placement for vocabulary hints field
- [ ] Specify empty string handling logic (null vs "" vs whitespace)
- [ ] Provide concrete test case examples
- [ ] Document OpenAI token limit handling
- [ ] Add acceptance criteria to PRD

### Phase 3: Edge Case Coverage (HIGH)
- [ ] Document null value handling
- [ ] Document whitespace-only string handling
- [ ] Document backward compatibility (configs without this field)
- [ ] Document token limit validation approach (or lack thereof)

### Phase 4: Validation (RECOMMENDED)
- [ ] Re-run scrutinizer audit after remediation
- [ ] Verify all 5 completeness questions pass
- [ ] Confirm zero ambiguity flags remain

---

## Meta-Principle Evaluation

> "If you have to explain it, it's not repository-complete."

**Current State**: Task 0007 requires extensive explanation:
- "Where should the field go?" → Not specified
- "How do I handle empty strings?" → Not defined
- "What does success look like?" → No acceptance criteria
- "What if the config doesn't have this field yet?" → Not addressed

**Target State**: Task should answer these questions inline, requiring zero follow-up.

---

## Next Actions

1. **Immediate**: Create 00_meta.md to establish task identity
2. **Immediate**: Rename and enhance 01_prd.md
3. **Immediate**: Create 02_implementation_plan.md with concrete technical details
4. **Short-term**: Create 04_status.md and directory structure
5. **Before work begins**: Re-audit with scrutinizer to verify PASS status

---

**Audit Complete**
**Recommendation**: BLOCK implementation until structural gaps remediated

*Scrutinizer: Hostile to ambiguity since 2026*
