# Implementation Plan: Recipe System Audit and UX Improvements

## Phase 1: Terminology Rename (Foundation)

Do this first as it affects all subsequent UI work.

### 1.1 UI Label Changes

**File: `UnitEditorForm.java`**

| Line | Current | New |
|------|---------|-----|
| 65 | `"Processing Unit Editor"` | `"Step Editor"` |
| 75 | `"Unit Name:"` | `"Step Name:"` |
| 194 | `"Save and return to Unit Library"` | `"Save and return to Step Library"` |
| 199 | `"Save this unit and create a new pipeline with it"` | `"Save this step and create a new recipe with it"` |
| 278 | `"Unit saved!"` | `"Step saved!"` |
| 311-312 | `"Unit saved and pipeline '..."` | `"Step saved and recipe '..."` |

**File: `PipelineEditorForm.java`**

| Line | Current | New |
|------|---------|-----|
| 49 | `"Pipeline Editor"` | `"Recipe Editor"` |
| 60 | `"Pipeline Title:"` | `"Recipe Title:"` |
| 87-88 | `"Pipeline Enabled"` | `"Recipe Enabled"` |
| 107 | `"Save and return to Pipelines"` | `"Save and return to Recipes"` |
| 113 | `"Create New Unit"` | `"Create New Step"` |
| 118 | `"Add Existing Unit"` | `"Add Existing Step"` |
| 216-219 | `"No processing units available..."` | `"No steps available..."` |
| 293 | `"Pipeline saved!"` | `"Recipe saved!"` |
| 305 | `"Pipeline title is required."` | `"Recipe title is required."` |
| 313-314 | `"Pipeline must contain at least one unit."` | `"Recipe must contain at least one step."` |
| 353 | `"Unit: " + unit.name` | `"Step: " + unit.name` |

**File: `UnitLibraryListForm.java`**

| Line | Current | New |
|------|---------|-----|
| 30 | `"Processing Unit Library"` | `"Step Library"` |
| 97 | `"Edit this Processing Unit"` | `"Edit this Step"` |
| 106 | `"Delete this Processing Unit"` | `"Delete this Step"` |
| 110 | `"...delete this unit? It may be used in pipelines."` | `"...delete this step? It may be used in recipes."` |

**File: `PipelineListForm.java`**

| Line | Current | New |
|------|---------|-----|
| 30 | `"All Pipelines"` | `"All Recipes"` |
| 93 | `"Units: " + unitCount` | `"Steps: " + stepCount` |
| 107 | `"Edit this Pipeline"` | `"Edit this Recipe"` |
| 116 | `"Delete this Pipeline"` | `"Delete this Recipe"` |
| 120 | `"...delete this pipeline?"` | `"...delete this recipe?"` |

**File: `MainForm.java`**

Update menu item labels (locate in menu initialization code).

### 1.2 Variable Rename (Optional, Low Priority)

Consider renaming UI-layer variables for consistency:
- `pipelineCombo` → `recipeCombo`
- `unitReferences` → `stepReferences`
- `availableUnits` → `availableSteps`

This is optional and can be deferred.

---

## Phase 2: Step Editor UX Improvements

### 2.1 Quick Create Mode (Simple/Advanced Toggle)

**File: `UnitEditorForm.java`**

Add toggle button to top panel:

```java
// After line 107 (after typePanel)
JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
modePanel.setAlignmentX(LEFT_ALIGNMENT);
JToggleButton advancedToggle = new JToggleButton("Advanced Mode");
advancedToggle.setToolTipText("Show all configuration options");
advancedToggle.addActionListener(e -> updateFieldsVisibility());
modePanel.add(advancedToggle);
topPanel.add(modePanel);
```

Modify `updateFieldsVisibility()` method:

```java
private void updateFieldsVisibility() {
    String selection = (String) typeCombo.getSelectedItem();
    boolean isAdvanced = advancedToggle.isSelected();

    if ("Prompt".equals(selection)) {
        promptPanel.setVisible(true);
        replacementPanel.setVisible(false);

        // In Simple mode, hide system prompt
        systemPanel.setVisible(isAdvanced);
        providerPanel.setVisible(isAdvanced);
        // Always show user prompt
    } else if ("Text Replacement".equals(selection)) {
        promptPanel.setVisible(false);
        replacementPanel.setVisible(true);
    }
    revalidate();
    repaint();
}
```

### 2.2 Collapsible System Prompt

Create helper class:

**File: `src/main/java/org/whisperdog/ui/JCollapsiblePanel.java` (new)**

```java
package org.whisperdog.ui;

import javax.swing.*;
import java.awt.*;

public class JCollapsiblePanel extends JPanel {
    private final JPanel contentPanel;
    private final JButton toggleButton;
    private boolean collapsed = true;

    public JCollapsiblePanel(String title, JComponent content) {
        setLayout(new BorderLayout());

        toggleButton = new JButton("▶ " + title);
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.addActionListener(e -> toggle());
        add(toggleButton, BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(content, BorderLayout.CENTER);
        contentPanel.setVisible(!collapsed);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void toggle() {
        collapsed = !collapsed;
        contentPanel.setVisible(!collapsed);
        toggleButton.setText((collapsed ? "▶ " : "▼ ") +
            toggleButton.getText().substring(2));
        revalidate();
    }

    public void setCollapsed(boolean collapsed) {
        if (this.collapsed != collapsed) {
            toggle();
        }
    }
}
```

Usage in `UnitEditorForm.java`:

```java
// Replace systemPanel creation with:
JCollapsiblePanel collapsibleSystemPrompt = new JCollapsiblePanel(
    "System Prompt (Optional)",
    systemScrollPane
);
promptPanel.add(collapsibleSystemPrompt);
```

### 2.3 Preview/Test Button

Add to bottom panel in `UnitEditorForm.java`:

```java
JButton testButton = new JButton("Test Step");
testButton.setToolTipText("Test this step with sample text");
testButton.addActionListener(e -> showTestDialog());
bottomPanel.add(testButton);
```

Add test dialog method:

```java
private void showTestDialog() {
    JDialog dialog = new JDialog(
        (Frame) SwingUtilities.getWindowAncestor(this),
        "Test Step",
        true
    );
    dialog.setLayout(new BorderLayout(10, 10));
    dialog.setSize(600, 400);

    // Input area
    JTextArea inputArea = new JTextArea(5, 40);
    inputArea.setText("Enter sample text here to test the step...");
    inputArea.setLineWrap(true);
    inputArea.setWrapStyleWord(true);

    // Output area
    JTextArea outputArea = new JTextArea(5, 40);
    outputArea.setEditable(false);
    outputArea.setLineWrap(true);
    outputArea.setWrapStyleWord(true);

    // Run button
    JButton runButton = new JButton("Run Test");
    runButton.addActionListener(evt -> {
        outputArea.setText("Processing...");

        // Build temporary unit from current form values
        ProcessingUnit tempUnit = buildUnitFromForm();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                PostProcessingService service = new PostProcessingService(configManager);
                return service.executeUnit(tempUnit, inputArea.getText());
            }

            @Override
            protected void done() {
                try {
                    outputArea.setText(get());
                } catch (Exception ex) {
                    outputArea.setText("Error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    });

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(new JLabel("Input:"), BorderLayout.NORTH);
    topPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

    JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.add(new JLabel("Output:"), BorderLayout.NORTH);
    bottomPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
    splitPane.setResizeWeight(0.5);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(runButton);

    dialog.add(splitPane, BorderLayout.CENTER);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
}

private ProcessingUnit buildUnitFromForm() {
    ProcessingUnit unit = new ProcessingUnit();
    unit.uuid = "temp-test-" + System.currentTimeMillis();
    unit.name = nameField.getText().trim();
    unit.type = (String) typeCombo.getSelectedItem();

    if ("Prompt".equals(unit.type)) {
        unit.provider = (String) providerCombo.getSelectedItem();
        unit.model = (String) modelCombo.getSelectedItem();
        unit.systemPrompt = getSystemPromptText();
        unit.userPrompt = getUserPromptText();
    } else if ("Text Replacement".equals(unit.type)) {
        unit.textToReplace = textToReplaceField.getText();
        unit.replacementText = replacementTextField.getText();
    }

    return unit;
}
```

**Note**: This requires adding `executeUnit()` method to `PostProcessingService.java` if not already public.

### 2.4 Step Templates

Add template dropdown to `UnitEditorForm.java`:

```java
// Add after type selector panel
JPanel templatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
templatePanel.setAlignmentX(LEFT_ALIGNMENT);
JLabel templateLabel = new JLabel("Start from template:");
templateLabel.setPreferredSize(new Dimension(150, templateLabel.getPreferredSize().height));
templatePanel.add(templateLabel);

String[] templates = {"(None)", "Fix Grammar", "Remove Filler Words", "Format as Bullets", "Summarize"};
JComboBox<String> templateCombo = new JComboBox<>(templates);
templateCombo.addActionListener(e -> applyTemplate((String) templateCombo.getSelectedItem()));
templatePanel.add(templateCombo);
topPanel.add(templatePanel);
```

Add template application method:

```java
private void applyTemplate(String template) {
    if ("(None)".equals(template)) return;

    typeCombo.setSelectedItem("Prompt");

    switch (template) {
        case "Fix Grammar":
            systemPromptArea.setText("You are a professional editor. Fix grammar and spelling errors while preserving the original meaning and tone.");
            userPromptArea.setText("Fix any grammar or spelling errors in this text:\n\n{{input}}");
            break;
        case "Remove Filler Words":
            systemPromptArea.setText("You are a concise editor. Remove filler words and unnecessary phrases while preserving meaning.");
            userPromptArea.setText("Remove filler words (um, uh, like, you know, basically, actually, literally) from this text:\n\n{{input}}");
            break;
        case "Format as Bullets":
            systemPromptArea.setText("You are a formatting assistant. Convert text into clean bullet points.");
            userPromptArea.setText("Convert this text into a bulleted list:\n\n{{input}}");
            break;
        case "Summarize":
            systemPromptArea.setText("You are a summarization assistant. Create concise summaries that capture key points.");
            userPromptArea.setText("Summarize this text in 2-3 sentences:\n\n{{input}}");
            break;
    }

    systemPromptArea.setFont(defaultFont);
    userPromptArea.setFont(defaultFont);
}
```

---

## Phase 3: Recipe Editor UX Improvements

### 3.1 Inline Step Adding

**File: `PipelineEditorForm.java`**

Replace the "Add Existing Unit" button dialog with inline controls.

Remove `showAddUnitDialog()` method and replace with inline UI:

```java
// In constructor, replace bottomPanel setup:
JPanel bottomPanel = new JPanel();
bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

// Step addition panel (inline)
JPanel addStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
addStepPanel.setBorder(BorderFactory.createTitledBorder("Add Step"));

JComboBox<ProcessingUnit> stepCombo = new JComboBox<>();
stepCombo.setRenderer(new DefaultListCellRenderer() {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof ProcessingUnit) {
            ProcessingUnit unit = (ProcessingUnit) value;
            setText(unit.name + " (" + unit.type + ")");
        }
        return this;
    }
});
refreshStepCombo(stepCombo);

JButton addButton = new JButton("Add");
addButton.addActionListener(e -> {
    ProcessingUnit selected = (ProcessingUnit) stepCombo.getSelectedItem();
    if (selected != null) {
        addUnitReferencePanel(selected, true);
    }
});

addStepPanel.add(new JLabel("Step:"));
addStepPanel.add(stepCombo);
addStepPanel.add(addButton);
addStepPanel.add(Box.createHorizontalStrut(20));

JButton createNewButton = new JButton("Create New Step");
createNewButton.addActionListener(e -> showCreateUnitDialog());
addStepPanel.add(createNewButton);

bottomPanel.add(addStepPanel);

// Save button panel
JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
JButton doneButton = new JButton("Save Recipe");
doneButton.addActionListener(e -> saveAndReturn());
savePanel.add(doneButton);
bottomPanel.add(savePanel);

add(bottomPanel, BorderLayout.SOUTH);
```

Add helper method:

```java
private void refreshStepCombo(JComboBox<ProcessingUnit> combo) {
    combo.removeAllItems();
    List<ProcessingUnit> units = configManager.getProcessingUnits();
    for (ProcessingUnit unit : units) {
        combo.addItem(unit);
    }
}
```

### 3.2 Drag-and-Drop Reordering

Replace `unitsContainer` with a `JList` that supports drag-and-drop.

**Alternative approach (simpler)**: Keep BoxLayout but add DnD support via `TransferHandler`.

```java
// In UnitReferencePanel constructor, add drag support:
setTransferHandler(new PanelTransferHandler());

// Enable as drag source
addMouseMotionListener(new MouseMotionAdapter() {
    @Override
    public void mouseDragged(MouseEvent e) {
        JComponent comp = (JComponent) e.getSource();
        TransferHandler handler = comp.getTransferHandler();
        handler.exportAsDrag(comp, e, TransferHandler.MOVE);
    }
});
```

Create transfer handler class:

```java
class PanelTransferHandler extends TransferHandler {
    private final DataFlavor panelFlavor = new DataFlavor(UnitReferencePanel.class, "Panel");

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { panelFlavor };
            }
            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.equals(panelFlavor);
            }
            @Override
            public Object getTransferData(DataFlavor flavor) {
                return c;
            }
        };
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(panelFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            UnitReferencePanel draggedPanel = (UnitReferencePanel)
                support.getTransferable().getTransferData(panelFlavor);

            Point dropPoint = support.getDropLocation().getDropPoint();
            int dropIndex = calculateDropIndex(dropPoint);

            Container parent = draggedPanel.getParent();
            int currentIndex = getComponentIndex(draggedPanel);

            parent.remove(draggedPanel);
            if (dropIndex > currentIndex) dropIndex--;
            ((JPanel) parent).add(draggedPanel, dropIndex);
            parent.revalidate();
            parent.repaint();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int calculateDropIndex(Point dropPoint) {
        // Calculate based on Y position relative to other panels
        // Implementation depends on container layout
    }
}
```

**Note**: DnD in Swing is complex. Consider using the Up/Down buttons as fallback and implementing DnD as a stretch goal.

### 3.3 Show Step Descriptions

**File: `PipelineEditorForm.java`** - Modify `UnitReferencePanel` inner class:

```java
// In UnitReferencePanel constructor, modify infoPanel:
JPanel infoPanel = new JPanel();
infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

JLabel typeLabel = new JLabel("Type: " + unit.type);
typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
infoPanel.add(typeLabel);

// ADD: Description label
if (unit.description != null && !unit.description.trim().isEmpty()) {
    JLabel descLabel = new JLabel("<html><i>" + unit.description + "</i></html>");
    descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    descLabel.setForeground(Color.GRAY);
    infoPanel.add(descLabel);
}

// ADD: Show provider/model for Prompt type
if ("Prompt".equals(unit.type)) {
    JLabel modelLabel = new JLabel(unit.provider + " / " + unit.model);
    modelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    modelLabel.setFont(modelLabel.getFont().deriveFont(Font.ITALIC, 11f));
    infoPanel.add(modelLabel);
}
```

---

## Phase 4: Discovery & Organization

### 4.1 Search/Filter

**File: `UnitLibraryListForm.java`**

Add search field to header:

```java
// In constructor, after headerLabel:
JTextField searchField = new JTextField(20);
searchField.setToolTipText("Search steps by name or description");
searchField.getDocument().addDocumentListener(new DocumentListener() {
    @Override
    public void insertUpdate(DocumentEvent e) { filterList(searchField.getText()); }
    @Override
    public void removeUpdate(DocumentEvent e) { filterList(searchField.getText()); }
    @Override
    public void changedUpdate(DocumentEvent e) { filterList(searchField.getText()); }
});

JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
searchPanel.add(new JLabel("Search:"));
searchPanel.add(searchField);
headerPanel.add(searchPanel, BorderLayout.EAST);
```

Add filter method:

```java
private String currentFilter = "";
private List<ProcessingUnit> allUnits = new ArrayList<>();

private void filterList(String filter) {
    currentFilter = filter.toLowerCase().trim();
    refreshList();
}

// Modify refreshList() to filter:
public void refreshList() {
    listContainer.removeAll();

    allUnits = configManager.getProcessingUnits();

    List<ProcessingUnit> filtered = allUnits.stream()
        .filter(unit -> {
            if (currentFilter.isEmpty()) return true;
            String name = unit.name != null ? unit.name.toLowerCase() : "";
            String desc = unit.description != null ? unit.description.toLowerCase() : "";
            return name.contains(currentFilter) || desc.contains(currentFilter);
        })
        .sorted((a, b) -> {
            String nameA = (a.name != null) ? a.name : "";
            String nameB = (b.name != null) ? b.name : "";
            return nameA.compareToIgnoreCase(nameB);
        })
        .collect(Collectors.toList());

    for (ProcessingUnit unit : filtered) {
        // ... existing panel creation code ...
    }

    listContainer.revalidate();
    listContainer.repaint();
}
```

### 4.2 Usage Count

**File: `ConfigManager.java`**

Add method:

```java
public int getStepUsageCount(String stepUuid) {
    List<Pipeline> pipelines = getPipelines();
    int count = 0;
    for (Pipeline pipeline : pipelines) {
        if (pipeline.unitReferences != null) {
            for (PipelineUnitReference ref : pipeline.unitReferences) {
                if (stepUuid.equals(ref.unitUuid)) {
                    count++;
                    break; // Count each pipeline only once
                }
            }
        }
    }
    return count;
}
```

**File: `UnitLibraryListForm.java`**

Add usage count to item panel:

```java
// In refreshList(), after typeLabel:
int usageCount = configManager.getStepUsageCount(unit.uuid);
JLabel usageLabel = new JLabel("Used in " + usageCount + " recipe" + (usageCount != 1 ? "s" : ""));
usageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
usageLabel.setForeground(usageCount > 0 ? new Color(0, 100, 0) : Color.GRAY);
infoPanel.add(usageLabel);
```

---

## Phase 5: Conditional Logic

### 5.1 Data Model Changes

**File: `ProcessingUnit.java`**

Add new fields:

```java
// Existing fields...

// New fields for Condition type
public String conditionPrompt;     // Question for LLM to evaluate (returns true/false)
public String ifTrueStepUuid;      // Step UUID to execute if condition is true
public String ifFalseStepUuid;     // Step UUID to execute if condition is false (nullable)
```

### 5.2 UI for Condition Type

**File: `UnitEditorForm.java`**

Add condition panel:

```java
// After replacementPanel creation:
conditionPanel = new JPanel();
conditionPanel.setLayout(new BoxLayout(conditionPanel, BoxLayout.Y_AXIS));
conditionPanel.add(Box.createVerticalStrut(20));

// Condition prompt
JPanel conditionPromptPanel = new JPanel(new BorderLayout());
conditionPromptPanel.add(new JLabel("Condition (LLM will evaluate to true/false):"), BorderLayout.NORTH);
conditionPromptArea = new JTextArea(3, 40);
conditionPromptArea.setLineWrap(true);
conditionPromptArea.setWrapStyleWord(true);
conditionPromptPanel.add(new JScrollPane(conditionPromptArea), BorderLayout.CENTER);
conditionPanel.add(conditionPromptPanel);
conditionPanel.add(Box.createVerticalStrut(10));

// If-true step selector
JPanel ifTruePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
ifTruePanel.add(new JLabel("If TRUE, run step:"));
ifTruePanel.add(Box.createHorizontalStrut(5));
ifTrueCombo = new JComboBox<>();
populateStepCombo(ifTrueCombo);
ifTruePanel.add(ifTrueCombo);
conditionPanel.add(ifTruePanel);
conditionPanel.add(Box.createVerticalStrut(5));

// If-false step selector
JPanel ifFalsePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
ifFalsePanel.add(new JLabel("If FALSE, run step:"));
ifFalsePanel.add(Box.createHorizontalStrut(5));
ifFalseCombo = new JComboBox<>();
ifFalseCombo.addItem(null); // Allow "none" option
populateStepCombo(ifFalseCombo);
ifFalsePanel.add(ifFalseCombo);
ifFalsePanel.add(new JLabel(" (optional)"));
conditionPanel.add(ifFalsePanel);

centerPanel.add(conditionPanel);
```

Update `typeCombo` initialization:

```java
typeCombo = new JComboBox<>(new String[]{"Prompt", "Text Replacement", "Condition"});
```

Update `updateFieldsVisibility()`:

```java
private void updateFieldsVisibility() {
    String selection = (String) typeCombo.getSelectedItem();
    promptPanel.setVisible("Prompt".equals(selection));
    replacementPanel.setVisible("Text Replacement".equals(selection));
    conditionPanel.setVisible("Condition".equals(selection));
    revalidate();
    repaint();
}
```

### 5.3 Execution Logic

**File: `PostProcessingService.java`**

Add condition evaluation in `applyPipeline()` method (around line 180):

```java
// Inside the loop processing units:
if ("Condition".equals(unit.type)) {
    boolean result = evaluateCondition(unit.conditionPrompt, currentText);

    String nextStepUuid = result ? unit.ifTrueStepUuid : unit.ifFalseStepUuid;

    if (nextStepUuid != null && !nextStepUuid.isEmpty()) {
        ProcessingUnit branchStep = configManager.getProcessingUnitByUuid(nextStepUuid);
        if (branchStep != null) {
            currentText = executePromptUnit(branchStep, currentText);
        }
    }
    // Continue to next step in pipeline
    continue;
}
```

Add evaluation method:

```java
private boolean evaluateCondition(String conditionPrompt, String inputText) {
    // Build prompt that asks LLM to evaluate condition
    String systemPrompt = "You are a condition evaluator. Answer only 'true' or 'false' based on the condition.";
    String userPrompt = "Condition to evaluate: " + conditionPrompt +
                        "\n\nText to evaluate:\n" + inputText +
                        "\n\nDoes this text satisfy the condition? Answer only 'true' or 'false'.";

    try {
        // Use OpenAI client (or configured provider)
        String response = openAIClient.processText(systemPrompt, userPrompt,
            configManager.getOpenAIModel());

        return response.trim().toLowerCase().startsWith("true");
    } catch (Exception e) {
        logger.error("Condition evaluation failed: {}", e.getMessage());
        return false; // Default to false on error
    }
}
```

---

## Phase 6: Import/Export

### 6.1 Export Model

**File: `src/main/java/org/whisperdog/postprocessing/RecipeBundle.java` (new)**

```java
package org.whisperdog.postprocessing;

import java.util.List;

public class RecipeBundle {
    public String version = "1.0";
    public String type = "recipe-bundle";
    public String exportedAt;
    public Pipeline recipe;
    public List<ProcessingUnit> steps;
}
```

### 6.2 Exporter/Importer

**File: `src/main/java/org/whisperdog/postprocessing/RecipeExporter.java` (new)**

```java
package org.whisperdog.postprocessing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.time.Instant;
import java.util.*;

public class RecipeExporter {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void exportRecipe(Pipeline recipe, ConfigManager configManager, File outputFile)
            throws IOException {
        RecipeBundle bundle = new RecipeBundle();
        bundle.exportedAt = Instant.now().toString();
        bundle.recipe = recipe;

        // Collect all steps used by this recipe
        bundle.steps = new ArrayList<>();
        if (recipe.unitReferences != null) {
            for (PipelineUnitReference ref : recipe.unitReferences) {
                ProcessingUnit unit = configManager.getProcessingUnitByUuid(ref.unitUuid);
                if (unit != null) {
                    bundle.steps.add(unit);
                }
            }
        }

        try (Writer writer = new FileWriter(outputFile)) {
            gson.toJson(bundle, writer);
        }
    }

    public static ImportResult importRecipe(File inputFile, ConfigManager configManager)
            throws IOException {
        ImportResult result = new ImportResult();

        try (Reader reader = new FileReader(inputFile)) {
            RecipeBundle bundle = gson.fromJson(reader, RecipeBundle.class);

            if (bundle == null || bundle.recipe == null) {
                result.success = false;
                result.error = "Invalid recipe bundle format";
                return result;
            }

            // Map old UUIDs to new UUIDs
            Map<String, String> uuidMap = new HashMap<>();

            // Import steps first
            for (ProcessingUnit step : bundle.steps) {
                String oldUuid = step.uuid;

                // Check for UUID collision
                if (configManager.getProcessingUnitByUuid(oldUuid) != null) {
                    step.uuid = UUID.randomUUID().toString();
                    result.warnings.add("Step UUID collision, generated new: " + step.name);
                }

                // Check for name collision
                List<ProcessingUnit> existing = configManager.getProcessingUnits();
                boolean nameExists = existing.stream()
                    .anyMatch(u -> u.name != null && u.name.equals(step.name));
                if (nameExists) {
                    step.name = step.name + " (imported)";
                    result.warnings.add("Step name collision, renamed: " + step.name);
                }

                uuidMap.put(oldUuid, step.uuid);
                configManager.saveProcessingUnit(step);
            }

            // Update recipe references to use new UUIDs
            Pipeline recipe = bundle.recipe;
            String oldRecipeUuid = recipe.uuid;

            // Check recipe UUID collision
            if (configManager.getPipelineByUuid(oldRecipeUuid) != null) {
                recipe.uuid = UUID.randomUUID().toString();
                result.warnings.add("Recipe UUID collision, generated new");
            }

            // Check recipe name collision
            List<Pipeline> existingPipelines = configManager.getPipelines();
            boolean recipeNameExists = existingPipelines.stream()
                .anyMatch(p -> p.title != null && p.title.equals(recipe.title));
            if (recipeNameExists) {
                recipe.title = recipe.title + " (imported)";
                result.warnings.add("Recipe name collision, renamed: " + recipe.title);
            }

            // Update step references
            if (recipe.unitReferences != null) {
                for (PipelineUnitReference ref : recipe.unitReferences) {
                    if (uuidMap.containsKey(ref.unitUuid)) {
                        ref.unitUuid = uuidMap.get(ref.unitUuid);
                    }
                }
            }

            configManager.savePipeline(recipe);

            result.success = true;
            result.importedRecipeTitle = recipe.title;
            result.importedStepCount = bundle.steps.size();
        }

        return result;
    }

    public static class ImportResult {
        public boolean success;
        public String error;
        public List<String> warnings = new ArrayList<>();
        public String importedRecipeTitle;
        public int importedStepCount;
    }
}
```

### 6.3 UI Integration

**File: `PipelineListForm.java`**

Add to header panel:

```java
// After headerLabel:
JButton importButton = new JButton("Import Recipe");
importButton.setToolTipText("Import a recipe from file");
importButton.addActionListener(e -> importRecipe());
headerPanel.add(importButton, BorderLayout.EAST);
```

Add import method:

```java
private void importRecipe() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter(
        "WhisperDog Recipe (*.whisperdog-recipe)", "whisperdog-recipe"));

    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
            RecipeExporter.ImportResult result = RecipeExporter.importRecipe(
                chooser.getSelectedFile(), configManager);

            if (result.success) {
                String message = "Imported: " + result.importedRecipeTitle +
                    "\nSteps imported: " + result.importedStepCount;
                if (!result.warnings.isEmpty()) {
                    message += "\n\nWarnings:\n" + String.join("\n", result.warnings);
                }
                Notificationmanager.getInstance().showNotification(
                    ToastNotification.Type.SUCCESS, "Recipe imported!");
                JOptionPane.showMessageDialog(this, message, "Import Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                refreshList();
            } else {
                Notificationmanager.getInstance().showNotification(
                    ToastNotification.Type.ERROR, "Import failed: " + result.error);
            }
        } catch (Exception ex) {
            Notificationmanager.getInstance().showNotification(
                ToastNotification.Type.ERROR, "Import error: " + ex.getMessage());
        }
    }
}
```

Add export button to each recipe item (in the `for` loop):

```java
// After deleteButton creation:
JButton exportButton = new JButton();
exportButton.setIcon(new FlatSVGIcon("icon/svg/download.svg", 16, 16));
exportButton.setToolTipText("Export this Recipe");
exportButton.addActionListener((ActionEvent e) -> exportRecipe(pipeline));

buttonPanel.add(exportButton);
buttonPanel.add(Box.createVerticalStrut(5));
```

Add export method:

```java
private void exportRecipe(Pipeline pipeline) {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter(
        "WhisperDog Recipe (*.whisperdog-recipe)", "whisperdog-recipe"));
    chooser.setSelectedFile(new File(pipeline.title.replaceAll("[^a-zA-Z0-9]", "_") + ".whisperdog-recipe"));

    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".whisperdog-recipe")) {
                file = new File(file.getAbsolutePath() + ".whisperdog-recipe");
            }
            RecipeExporter.exportRecipe(pipeline, configManager, file);
            Notificationmanager.getInstance().showNotification(
                ToastNotification.Type.SUCCESS, "Recipe exported!");
        } catch (Exception ex) {
            Notificationmanager.getInstance().showNotification(
                ToastNotification.Type.ERROR, "Export error: " + ex.getMessage());
        }
    }
}
```

---

## Verification Checklist

After each phase, verify:

### Phase 1 (Terminology)
- [ ] Build succeeds with no compilation errors
- [ ] All UI forms display new terminology
- [ ] No references to "Pipeline" or "Unit" in user-visible strings

### Phase 2 (Step Editor UX)
- [ ] Simple/Advanced toggle works
- [ ] System prompt collapses/expands
- [ ] Test button opens dialog and executes step
- [ ] Templates populate form correctly

### Phase 3 (Recipe Editor UX)
- [ ] Inline dropdown shows all available steps
- [ ] Steps can be reordered (Up/Down or DnD)
- [ ] Step descriptions visible in list

### Phase 4 (Discovery)
- [ ] Search filters step list in real-time
- [ ] Usage count shows correct numbers
- [ ] Performance: <100ms for 100 steps

### Phase 5 (Conditional Logic)
- [ ] Condition type appears in dropdown
- [ ] Condition UI shows step selectors
- [ ] Condition evaluation returns true/false correctly
- [ ] Pipeline branches based on condition

### Phase 6 (Import/Export)
- [ ] Export button creates valid JSON file
- [ ] Import button loads recipe and steps
- [ ] UUID conflicts handled correctly
- [ ] Name conflicts handled correctly
