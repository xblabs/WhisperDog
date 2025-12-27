# UI/UX Improvements - Execution Checklist

**Note**: WD-0002 (Icons & Padding) moved to Task 0001 (Rebranding)

## Phase 1: Post-Processing UI Reorganization (WD-0003)
- [ ] Create PostProcessingPanel with separate show/auto checkboxes
- [ ] Implement "hidden but active" status indicator
- [ ] Replace existing post-processing UI in RecorderForm
- [ ] Add new configuration keys to ConfigManager

## Phase 2: Searchable Log Screen (WD-0009)
- [ ] Create SearchBar component with navigation buttons
- [ ] Create SearchableLogPanel with highlighting support
- [ ] Implement Ctrl+F keyboard shortcut
- [ ] Add incremental search with match counting
- [ ] Implement previous/next match navigation

## Phase 3: Long-Running Process UX (WD-0011)
- [ ] Create ProcessProgressPanel with file path display
- [ ] Implement Copy Path button functionality
- [ ] Implement Open Folder button functionality
- [ ] Add Cancel button with operation abort
- [ ] Add Retry button for failed operations
- [ ] Integrate progress panel into RecorderForm

## Phase 4: Testing & Verification
- [ ] Test post-processing visibility/auto-execute independence
- [ ] Test search highlighting with various query strings
- [ ] Test cancel/retry during actual transcription operations
- [ ] Verify all features work in both light and dark themes

---

**Note**: Each phase contains detailed code examples in `02_implementation_plan.md`
