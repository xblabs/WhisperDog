---
title: UI Domain Knowledge Index
description: Swing components, forms, theming, and user interface patterns
context_type: reference
priority: high
domain: ui
last_updated: 2025-12-26
tags: [index, ui, swing, flatlaf, forms, components]
status: active
---

# UI Domain Knowledge Index

## 🎯 Token-Insensitive Overview (50 tokens)
**Essential context for basic understanding**

WhisperDog uses Java Swing with FlatLaf theme, custom side menu navigation, multiple form screens, and EDT threading discipline.

**Key Areas:** Forms, Menu System, Theming, Notifications, Threading
**Entry Points:** `MainForm.java`, `RecorderForm.java`, `sidemenu/Menu.java`

## 🔍 Token-Sensitive Summary (200 tokens)
**Balanced context for informed decisions**

**Core UI Components:**
- **MainForm**: Container frame, manages navigation and form lifecycle (reuses form instances)
- **RecorderForm**: Main recording UI with transcription controls and pipeline history
- **SettingsForm**: Configuration UI for API keys, endpoints, preferences
- **PostProcessingListForm**: List view of processing pipelines
- **PipelineEditorForm**: Edit pipeline configurations
- **UnitEditorForm**: Edit individual processing units

**UI Framework:**
- **FlatLaf 3.5.4**: Modern look and feel with light/dark mode
- **MigLayout**: Layout manager for complex forms
- **Custom Side Menu**: Animated navigation menu (`sidemenu/` package)

**Critical Knowledge:**
- **EDT Threading**: All UI updates MUST run on Event Dispatch Thread
- **Form Reuse**: MainForm reuses form instances to preserve state during navigation
- **SwingWorker**: Used for long operations (transcription, post-processing)

## 📚 Deep Dive References (500+ tokens)
**Complete context for implementation**

### Main UI Architecture

**MainForm.java**

**Location:** `src/main/java/org/whisperdog/MainForm.java` (migrating from `org.whispercat`)

**Responsibilities:**
- Application window container
- Side menu integration
- Form navigation and lifecycle management
- State preservation between screen switches

**Form Reuse Pattern:**
```java
// Forms are created once and reused
private RecorderForm recorderForm;
private SettingsForm settingsForm;
// ...

public void showRecorder() {
    if (recorderForm == null) {
        recorderForm = new RecorderForm();
    }
    setContent(recorderForm);
}
```

**Why Form Reuse?**
- Preserves user input during navigation
- Maintains recording state when switching screens
- Better performance (avoid recreation overhead)

### Recording UI

**RecorderForm.java**

**Location:** `src/main/java/org/whisperdog/recording/RecorderForm.java` (migrating from `org.whispercat`)

**Features:**
- Start/Stop recording buttons
- Transcription status and progress
- Manual pipeline runner (run pipeline on existing text)
- Pipeline execution history panel
- Silence removal toggle
- Log search functionality

**Layout Sections:**
1. **Recording Controls** - Start/Stop, file selection
2. **Transcription Area** - Text display with edit capability
3. **Pipeline Controls** - Select and execute pipelines
4. **History Panel** - Track multiple pipeline executions
5. **Log Panel** - Execution logs with search

**Threading Pattern:**
```java
// Start long operation in SwingWorker
new SwingWorker<String, Void>() {
    @Override
    protected String doInBackground() throws Exception {
        return transcriptionClient.transcribe(audioFile);
    }

    @Override
    protected void done() {
        // Update UI on EDT
        try {
            String result = get();
            transcriptionArea.setText(result);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }
}.execute();
```

### Settings UI

**SettingsForm.java**

**Location:** `src/main/java/org/whisperdog/settings/SettingsForm.java` (migrating from `org.whispercat`)

**Configuration Sections:**
- OpenAI API settings (key, model)
- Faster-Whisper server settings (endpoint, model)
- OpenWebUI settings (endpoint, API key, model)
- Global hotkey configuration
- Silence removal threshold (with visual progress bar)
- UI preferences

**Custom Components:**
- **KeyCombinationTextField**: Captures keyboard shortcuts
- **ThresholdProgressBar**: Visual RMS threshold indicator

### Side Menu System

**Package:** `src/main/java/org/whisperdog/sidemenu/` (migrating from `org.whispercat`)

**Components:**
- **Menu.java**: Main menu container
- **MenuItem.java**: Individual menu item
- **MenuAnimation.java**: Expand/collapse animations
- **MenuEvent.java**: Navigation event handling
- **PopupSubmenu.java**: Submenu popups
- **LightDarkMode.java**: Theme toggle integration

**Navigation Flow:**
1. User clicks menu item
2. MenuEvent fired with action ID
3. MainForm receives event
4. MainForm switches to appropriate form

### Notification System

**ToastNotification.java**

**Location:** `src/main/java/org/whisperdog/ToastNotification.java` (migrating from `org.whispercat`)

**Purpose:** Non-blocking popup notifications

**Usage:**
```java
ToastNotification.show("Transcription complete", ToastNotification.Type.SUCCESS);
```

**TrayIconManager.java**

**Location:** `src/main/java/org/whisperdog/TrayIconManager.java` (migrating from `org.whispercat`)

**Purpose:** System tray integration with notifications

### Post-Processing UI

**PipelineListForm.java**

**Location:** `src/main/java/org/whisperdog/postprocessing/PipelineListForm.java` (migrating from `org.whispercat`)

**Features:**
- List all saved pipelines
- Create, edit, delete pipelines
- Quick actions (duplicate, export)

**PipelineEditorForm.java**

**Location:** `src/main/java/org/whisperdog/postprocessing/PipelineEditorForm.java` (migrating from `org.whispercat`)

**Features:**
- Add/remove processing units
- Reorder units (drag and drop)
- Configure unit parameters
- Save pipeline configuration

**UnitEditorForm.java**

**Location:** `src/main/java/org/whisperdog/postprocessing/UnitEditorForm.java` (migrating from `org.whispercat`)

**Features:**
- Edit unit name and description
- Configure system prompt
- Configure user prompt template
- Select LLM model
- Test unit execution

### Logging UI

**LogsForm.java**

**Location:** `src/main/java/org/whisperdog/LogsForm.java` (migrating from `org.whispercat`)

**Features:**
- Display execution logs
- Search and highlight
- Clear logs
- Auto-scroll toggle

**ConsoleLogger.java**

**Location:** `src/main/java/org/whisperdog/ConsoleLogger.java` (migrating from `org.whispercat`)

**Purpose:** Singleton logger for UI log display

### Theming System

**FlatLaf Integration:**
- Light mode: FlatLaf Light
- Dark mode: FlatLaf Dark
- Accent color customization via `ToolBarAccentColor`

**Theme Toggle:**
```java
LightDarkMode.setMode(isDark);
SwingUtilities.updateComponentTreeUI(MainForm.instance);
```

### EDT Threading Discipline

**Rules:**
1. **All UI updates on EDT** - Use `SwingUtilities.invokeLater()`
2. **No blocking operations on EDT** - Use SwingWorker
3. **SwingWorker for async** - Background operations with progress

**Example Pattern:**
```java
// Background work
SwingWorker<Result, Progress> worker = new SwingWorker<>() {
    @Override
    protected Result doInBackground() throws Exception {
        // Heavy computation here
        publish(progressValue); // Publish progress
        return result;
    }

    @Override
    protected void process(List<Progress> chunks) {
        // Update UI with progress (on EDT)
        progressBar.setValue(chunks.get(chunks.size() - 1));
    }

    @Override
    protected void done() {
        // Update UI with result (on EDT)
        updateUI(get());
    }
};
worker.execute();
```

### Global Hotkey System

**GlobalHotkeyListener.java**

**Location:** `src/main/java/org/whisperdog/GlobalHotkeyListener.java` (migrating from `org.whispercat`)

**Purpose:** System-wide hotkey registration using JNativeHook

**Features:**
- Start/stop recording from anywhere
- Configurable key combinations
- Platform-specific handling

### Related Domains
- [Core Domain](../core/index.md) - RecorderForm integration with recording workflow
- [Data Domain](../data/index.md) - Form data binding with ConfigManager
- [Development Domain](../development/index.md) - UI testing patterns
