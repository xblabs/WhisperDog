# PRD: UI/UX Improvements

## Problem Statement

WhisperDog's user interface has accumulated several usability issues that affect the overall experience:

1. **Inconsistent Icons**: Mix of Unicode characters (some broken), FlatLaf icons, and custom SVGs create visual inconsistency. Unicode icons display incorrectly on some systems.

2. **Spacing Inconsistencies**: Different screens have different padding values, list items have varying margins, creating an unprofessional appearance.

3. **Confusing Post-Processing UI**: The current "enable post-processing" checkbox controls both visibility AND automatic execution, which confuses users who want to see the section but not auto-run pipelines.

4. **No Log Search**: With hundreds of transcription entries, users cannot find specific text without scrolling. This wastes time and makes log review tedious.

5. **Poor Long-Process Feedback**: When transcription takes a long time (compression, API call), users see a spinner but cannot access the recording file, cannot cancel, and cannot retry on failure without restarting.

**Impact:**
- Visual inconsistency affects perceived quality
- Users accidentally trigger pipelines or hide important UI sections
- Log review takes 10x longer without search
- Hung processes require app restart to recover

## Solution Overview

Implement four UI/UX improvement areas:

### 1. Icons & Padding Overhaul (WD-0002)

Replace all icons with a consistent SVG icon set:
- Use Lucide or Feather icon style (line icons, consistent stroke)
- Standard sizes: 16px for inline, 20px for buttons, 24px for prominent actions
- Consistent padding: 8px base unit, 12px for sections, 16px for panels

### 2. Post-Processing UI Reorganization (WD-0003)

Split into two separate controls:
- "Show pipeline section" - Controls visibility
- "Enable automatic post-processing" - Controls auto-execution
- Clear labeling with subtle indicators when hidden but active

### 3. Searchable Log Screen (WD-0009)

Add search functionality:
- Ctrl+F opens search overlay
- Incremental search with highlighting
- Previous/Next navigation
- Match count display

### 4. Long-Running Process UX (WD-0011)

Improve feedback during long operations:
- Show current file path with copy button
- Cancel button to abort operations
- Retry button on failure
- Keep UI responsive (non-blocking)

## Functional Requirements

### FR-1: Icon System

**Icon Sources**: Use FlatSVGIcon with custom SVG files from resources/icon/svg/

**Required Icons**:
| Purpose | Icon | Size | File |
|---------|------|------|------|
| Record | Filled circle | 20px | mic.svg |
| Stop | Square | 20px | stop.svg |
| Play | Triangle | 16px | play.svg |
| Settings/Options | Sliders | 20px | sliders.svg |
| Logs | Terminal | 20px | terminal.svg |
| Pipelines | Git-pull-request | 20px | git-pull-request.svg |
| Units | Box | 20px | box.svg |
| Expand | Chevron-down | 16px | chevron-down.svg |
| Collapse | Chevron-up | 16px | chevron-up.svg |
| Edit | Pencil | 16px | edit.svg |
| Delete | Trash | 16px | trash.svg |
| Dark mode | Moon | 16px | dark.svg |

**Loading**:
```java
FlatSVGIcon icon = new FlatSVGIcon("icon/svg/play.svg", 16, 16);
```

### FR-2: Consistent Padding

**Base Unit**: 8px

**Padding Scale**:
- xs: 4px (dense inline elements)
- sm: 8px (standard inline spacing)
- md: 12px (section padding)
- lg: 16px (panel padding)
- xl: 24px (major section breaks)

**Application**:
| Element | Padding |
|---------|---------|
| List items | 8px vertical, 12px horizontal |
| Buttons | 8px vertical, 16px horizontal |
| Panel borders | 16px all sides |
| Section headers | 24px top, 12px bottom |
| Form fields | 8px between labels and inputs |

**Implementation**:
```java
// Create utility class
public class Spacing {
    public static final int XS = 4;
    public static final int SM = 8;
    public static final int MD = 12;
    public static final int LG = 16;
    public static final int XL = 24;

    public static Border listItemBorder() {
        return BorderFactory.createEmptyBorder(SM, MD, SM, MD);
    }

    public static Border panelBorder() {
        return BorderFactory.createEmptyBorder(LG, LG, LG, LG);
    }
}
```

### FR-3: Post-Processing UI

**Current UI** (confusing):
```
☑ Enable post-processing
  └─ Pipeline dropdown (hidden when unchecked)
  └─ History (hidden when unchecked)
```

**New UI** (clear separation):
```
☑ Show pipeline section          ← Controls visibility only
  └─ Pipeline dropdown
  └─ History
  └─ ☑ Enable automatic post-processing    ← Controls auto-execution
     └─ Activate on startup checkbox
```

**Behavior Matrix**:
| Show Section | Auto Execute | Result |
|--------------|--------------|--------|
| ✓ | ✓ | Section visible, pipelines run automatically |
| ✓ | ✗ | Section visible, manual trigger only |
| ✗ | ✓ | Section hidden, BUT pipelines still run (subtle indicator) |
| ✗ | ✗ | Section hidden, no pipelines |

**Subtle Indicator**: When section hidden but auto-execute enabled, show small icon in status bar: "⚡ Auto-pipeline active"

### FR-4: Searchable Log

**Search UI**:
```
┌─────────────────────────────────────────────────────────┐
│ Search: [_________________] [↑] [↓] [✕]  3 of 42 matches│
├─────────────────────────────────────────────────────────┤
│ [11:42:15] Transcription complete                       │
│ [11:42:10] API response received                        │
│ [11:41:58] >>> HIGHLIGHT: Sending to OpenAI API <<<     │
│ [11:41:45] Compression complete: 2.3MB                  │
│ ...                                                     │
└─────────────────────────────────────────────────────────┘
```

**Keyboard Shortcuts**:
- Ctrl+F: Open search bar
- Enter: Next match
- Shift+Enter: Previous match
- Escape: Close search

**Highlighting**:
- Background: Yellow (#FFF59D)
- Current match: Orange (#FFB74D)

**Implementation Approach**:
- Use JTextPane or JEditorPane for highlighting capability
- Custom Highlighter.HighlightPainter for match highlighting
- DocumentListener for incremental search

### FR-5: Long-Running Process UX

**Progress Panel** (shown during long operations):
```
┌─────────────────────────────────────────────────────────┐
│ Processing: record_20251230_154500.wav                  │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━░░░░░░░░░░ 73%           │
│                                                         │
│ Stage: Compressing audio to MP3...                      │
│ File: C:\Users\...\record_20251230_154500.wav           │
│        [📋 Copy Path]  [📁 Open Folder]                 │
│                                                         │
│                              [Cancel]                   │
└─────────────────────────────────────────────────────────┘
```

**On Failure**:
```
┌─────────────────────────────────────────────────────────┐
│ ⚠ Processing Failed                                     │
│                                                         │
│ Error: HTTP 507 - Server temporarily unavailable        │
│                                                         │
│ File: C:\Users\...\record_20251230_154500.mp3           │
│        [📋 Copy Path]  [📁 Open Folder]                 │
│                                                         │
│                    [Retry]  [Dismiss]                   │
└─────────────────────────────────────────────────────────┘
```

**Requirements**:
- Copy Path: Copies full file path to clipboard
- Open Folder: Opens containing folder in system file manager
- Cancel: Aborts current operation (with confirmation if processing)
- Retry: Re-attempts the failed operation
- UI thread must remain responsive (progress updates on EDT)

## Non-Functional Requirements

### NFR-1: Performance
- Icon loading: <50ms per icon (pre-cached)
- Search highlighting: <100ms for incremental update
- UI must remain responsive during all operations

### NFR-2: Accessibility
- Icons must have tooltips for screen readers
- Search results must be keyboard navigable
- Color not sole indicator (icons + color)

### NFR-3: Consistency
- All changes must work in both light and dark themes
- Spacing values from Spacing utility class only

## Out of Scope

- Complete redesign of application layout
- New screens or major navigation changes
- Animation or transitions
- Mobile/responsive adaptations
