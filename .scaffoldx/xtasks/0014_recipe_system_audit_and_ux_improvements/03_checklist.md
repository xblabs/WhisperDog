# Checklist: Recipe System Audit and UX Improvements

## Phase 1: Terminology Rename (Foundation)

- [ ] Update UI labels in UnitEditorForm.java (Unit → Step)
- [ ] Update UI labels in PipelineEditorForm.java (Pipeline → Recipe)
- [ ] Update UI labels in UnitLibraryListForm.java
- [ ] Update UI labels in PipelineListForm.java
- [ ] Update menu items in MainForm.java
- [ ] Verify all toast notifications use new terminology
- [ ] Build and test - no "Pipeline" or "Unit" visible in UI

## Phase 2: Step Editor UX Improvements

- [ ] Add Simple/Advanced mode toggle
- [ ] Implement JCollapsiblePanel for system prompt
- [ ] Add "Test Step" button with preview dialog
- [ ] Add step templates dropdown (4 templates minimum)
- [ ] Manual test: create step in Simple mode
- [ ] Manual test: test step with sample input

## Phase 3: Recipe Editor UX Improvements

- [ ] Replace dialog-based step adding with inline dropdown
- [ ] Add step descriptions to UnitReferencePanel display
- [ ] Implement drag-and-drop reordering (or keep Up/Down as fallback)
- [ ] Add "Test Recipe" button with preview dialog
- [ ] Manual test: add steps without popup dialogs
- [ ] Manual test: reorder steps

## Phase 4: Discovery & Organization

- [ ] Add search/filter TextField to Step Library header
- [ ] Implement real-time filtering by name/description
- [ ] Add getStepUsageCount() to ConfigManager
- [ ] Display "Used in X recipes" on each step
- [ ] Performance test: search 100 steps in <100ms

## Phase 5: Conditional Logic

- [ ] Add condition fields to ProcessingUnit.java
- [ ] Add "Condition" to type dropdown in UnitEditorForm
- [ ] Build condition UI (prompt, if-true selector, if-false selector)
- [ ] Add evaluateCondition() to PostProcessingService
- [ ] Update applyPipeline() to handle Condition type
- [ ] Unit test: condition evaluates to true
- [ ] Unit test: condition evaluates to false
- [ ] Integration test: pipeline branches correctly

## Phase 6: Import/Export

- [ ] Create RecipeBundle.java data class
- [ ] Create RecipeExporter.java with export/import methods
- [ ] Add "Import Recipe" button to PipelineListForm header
- [ ] Add "Export" button to each recipe item
- [ ] Handle UUID conflicts (generate new)
- [ ] Handle name conflicts (append suffix)
- [ ] Round-trip test: export → import → verify identical behavior

## Final Verification

- [ ] Full integration test: record → transcribe → run recipe with conditions
- [ ] All acceptance criteria from PRD met
- [ ] No regressions in existing functionality
