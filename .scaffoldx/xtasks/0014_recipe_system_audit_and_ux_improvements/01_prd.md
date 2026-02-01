# PRD: Recipe System Audit and UX Improvements

## Problem Statement

The WhisperDog post-processing pipeline system (to be renamed "Recipe System") has three documented UX pain points affecting usability for ADHD developers and AI solo dev entrepreneurs:

1. **Step creation is clunky** - `UnitEditorForm.java` presents all 8 fields at once (name, description, type, provider, model, system prompt, user prompt, replacement fields) with no progressive disclosure. Users must mentally filter which fields apply to their use case.

2. **Recipe composition is awkward** - `PipelineEditorForm.java` uses `JOptionPane.showInputDialog()` (line 228) for adding units, requiring a modal popup. Reordering uses Up/Down buttons (lines 386-413) instead of drag-and-drop.

3. **Discovery/organization is poor** - `UnitLibraryListForm.java` displays a flat alphabetical list with no search, filtering, or categorization. Users with 10+ units cannot quickly find what they need.

Additionally, two features are missing:
- **Conditional logic**: No way to branch pipeline execution based on content
- **Import/export**: No way to share recipes with others

**Impact**: These issues create friction for the target audience (ADHD developers) who need clear, simple, memorable workflows.

## Solution Overview

### Terminology Rename

Rename user-facing terminology to be more approachable:

| Current (Technical) | New (Friendly) | Rationale |
|---------------------|----------------|-----------|
| Pipeline | Recipe | Cooking metaphor, implies step-by-step process |
| ProcessingUnit | Step | Simple, clear, obvious what it means |
| Unit Library | Step Library | Consistency |

**Scope**: UI labels only. Internal class names (`Pipeline.java`, `ProcessingUnit.java`) remain unchanged to minimize refactoring risk.

### Phase 1: UX Improvements (Priority)

#### 1.1 Step Editor Improvements

**File**: `src/main/java/org/whisperdog/postprocessing/UnitEditorForm.java`

| Change | Implementation |
|--------|----------------|
| Quick Create mode | Add "Simple" vs "Advanced" toggle. Simple shows: name + type + user prompt only. Advanced shows all fields. |
| Collapsible system prompt | Wrap system prompt in `JCollapsiblePanel` (custom component) defaulting to collapsed state. |
| Preview/test button | Add "Test" button that opens modal with sample text input, runs the step, shows output. |
| Step templates | Add dropdown with pre-built templates: "Fix Grammar", "Remove Filler Words", "Format as Bullets", "Summarize". |

#### 1.2 Recipe Editor Improvements

**File**: `src/main/java/org/whisperdog/postprocessing/PipelineEditorForm.java`

| Change | Implementation |
|--------|----------------|
| Inline step adding | Replace `JOptionPane.showInputDialog()` with inline `JComboBox` + "Add" button at bottom of step list. |
| Drag-and-drop reordering | Implement `TransferHandler` on `unitsContainer` for DnD, or use `JList` with `setDragEnabled(true)`. |
| Show step descriptions | Modify `UnitReferencePanel` to show description below name (currently shows only type). |
| Preview/test button | Add "Test Recipe" button that opens modal with sample text, runs full pipeline, shows output. |

#### 1.3 Discovery & Organization

**File**: `src/main/java/org/whisperdog/postprocessing/UnitLibraryListForm.java`

| Change | Implementation |
|--------|----------------|
| Search/filter | Add `JTextField` with `DocumentListener` that filters displayed items by name/description. |
| Categories | Add `category` field to `ProcessingUnit.java`. UI shows collapsible sections per category. |
| Usage count | Add helper method in `ConfigManager` to count recipes using each step. Display "Used in X recipes". |
| Recently Used | Track last-used timestamp, show "Recent" section at top. |

### Phase 2: Conditional Logic

Add new step type "Condition" alongside "Prompt" and "Text Replacement":

**File**: `src/main/java/org/whisperdog/postprocessing/ProcessingUnit.java`

```java
// Existing fields...
public String type;  // "Prompt", "Text Replacement", or "Condition"

// New fields for Condition type:
public String conditionPrompt;     // LLM evaluates this to true/false
public String ifTrueStepUuid;      // Step to run if condition is true
public String ifFalseStepUuid;     // Step to run if condition is false (optional, skip if null)
```

**File**: `src/main/java/org/whisperdog/postprocessing/PostProcessingService.java`

Add handling in `applyPipeline()` method (around line 159):

```java
if ("Condition".equals(unit.type)) {
    boolean result = evaluateCondition(unit.conditionPrompt, inputText);
    String nextUuid = result ? unit.ifTrueStepUuid : unit.ifFalseStepUuid;
    if (nextUuid != null) {
        ProcessingUnit nextUnit = configManager.getProcessingUnitByUuid(nextUuid);
        inputText = executeUnit(nextUnit, inputText);
    }
    // Continue to next step in pipeline
}
```

### Phase 3: Import/Export

**Export Format** (JSON):

```json
{
  "version": "1.0",
  "type": "recipe-bundle",
  "exportedAt": "2026-02-01T00:00:00Z",
  "recipe": {
    "uuid": "...",
    "title": "My Recipe",
    "description": "...",
    "enabled": true,
    "unitReferences": [...]
  },
  "steps": [
    { "uuid": "...", "name": "Step 1", ... },
    { "uuid": "...", "name": "Step 2", ... }
  ]
}
```

**File**: `src/main/java/org/whisperdog/postprocessing/RecipeExporter.java` (new)

```java
public class RecipeExporter {
    public static void exportRecipe(Pipeline recipe, List<ProcessingUnit> steps, File outputFile) {
        // Generate JSON bundle
        // Write to file with .whisperdog-recipe extension
    }

    public static ImportResult importRecipe(File inputFile, ConfigManager configManager) {
        // Parse JSON
        // Handle UUID conflicts (generate new UUIDs)
        // Handle name collisions (append " (imported)" suffix)
        // Save recipe and steps
        // Return result with any warnings
    }
}
```

**UI Integration**:

- Add "Export" button to `PipelineListForm.java` item buttons
- Add "Import" button to `PipelineListForm.java` header panel
- Use `JFileChooser` with `.whisperdog-recipe` filter

## Acceptance Criteria

### Phase 1: UX

- [ ] Step Editor has Simple/Advanced toggle, defaulting to Simple
- [ ] System prompt is collapsed by default in Advanced mode
- [ ] "Test Step" button works with sample input
- [ ] At least 4 step templates are available
- [ ] Recipe Editor uses inline dropdown for step adding (no popup)
- [ ] Steps can be reordered via drag-and-drop
- [ ] Step Library has working search filter
- [ ] Steps show "Used in X recipes" count

### Phase 2: Conditional Logic

- [ ] "Condition" type appears in step type dropdown
- [ ] Condition steps can reference other steps for if-true/if-false branches
- [ ] Conditions are evaluated by LLM and return true/false
- [ ] Pipeline execution correctly branches based on condition result

### Phase 3: Import/Export

- [ ] "Export" button appears on recipe list items
- [ ] "Import" button appears in recipe list header
- [ ] Exported file has `.whisperdog-recipe` extension
- [ ] Import handles UUID conflicts by generating new UUIDs
- [ ] Import handles name collisions by appending suffix

### Phase 4: Terminology

- [ ] All UI labels show "Recipe" instead of "Pipeline"
- [ ] All UI labels show "Step" instead of "Unit" or "Processing Unit"
- [ ] Menu items updated
- [ ] Toast notifications updated
- [ ] Tooltips updated

## Out of Scope

- Renaming Java class names (`Pipeline.java` → `Recipe.java`) - too much refactoring risk
- Recipe versioning or revision history
- Cloud sync or team sharing
- Granular step-only export (only full recipe bundles)

## Technical Constraints

- Must remain pure Java Swing (no external UI frameworks)
- Must maintain backward compatibility with existing pipeline/unit data
- Performance: Step library search must be <100ms for 100 steps

## Dependencies

- FlatLaf library (already in use for icons and theming)
- Gson (already in use for JSON serialization)
