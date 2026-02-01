---
title: Recipe System Audit and UX Improvements
task_id: "0014"
status: todo
priority: medium
created: 2026-02-01
context_source: C:\Users\henry\.claude\plans\cheeky-splashing-rain.md
tags: [ux, refactor, post-processing, pipeline]
---

# Task 0014: Recipe System Audit and UX Improvements

## Summary

Audit and improve the post-processing pipeline system (renamed to "Recipe/Step" terminology) with focus on UX improvements, conditional logic, and import/export capabilities.

## Target Audience

- ADHD developers
- AI solo dev entrepreneurs

## Key Decisions

| Decision | Choice |
|----------|--------|
| Terminology | Pipeline → **Recipe**, ProcessingUnit → **Step** |
| Priority | UX improvements first, then features |
| Import/export | Start minimal (recipe bundles only) |

## Scope

### Phase 1: UX Audit & Quick Wins
- Step Editor improvements (quick create, preview/test, templates)
- Recipe Editor improvements (inline step adding, drag-and-drop, preview)
- Discovery & Organization (search, categories, usage tracking)

### Phase 2: Conditional Logic
- New "Condition" step type with if/then branching

### Phase 3: Import/Export
- Recipe bundle export/import with `.whisperdog-recipe` extension

### Phase 4: Terminology Rename
- UI labels: Pipeline → Recipe, Unit → Step
- Keep internal class names unchanged

## Related Files

- `src/main/java/org/whisperdog/postprocessing/UnitEditorForm.java`
- `src/main/java/org/whisperdog/postprocessing/PipelineEditorForm.java`
- `src/main/java/org/whisperdog/postprocessing/UnitLibraryListForm.java`
- `src/main/java/org/whisperdog/postprocessing/PipelineListForm.java`
- `src/main/java/org/whisperdog/postprocessing/PostProcessingService.java`
- `src/main/java/org/whisperdog/postprocessing/ProcessingUnit.java`
- `src/main/java/org/whisperdog/postprocessing/Pipeline.java`
- `src/main/java/org/whisperdog/ConfigManager.java`
