# UI Icons & Styling Specification

## Overview

This document specifies UI improvements for WhisperCat, focusing on icon replacements and padding fixes.

---

## 1. Broken Icons (Priority: High)

### Problem
Unicode characters used for buttons don't render on all systems, appearing as empty boxes.

### Affected Components

| Location | Current Code | Unicode | File |
|----------|--------------|---------|------|
| Run Pipeline button | `"\u25B6 Run Pipeline"` | ▶ | `RecorderForm.java:261` |
| Search prev button | `"\u25B2"` | ▲ | `RecorderForm.java:364` |
| Search next button | `"\u25BC"` | ▼ | `RecorderForm.java:369` |

### Solution
Create SVG icons in `src/main/resources/icon/svg/` and use `FlatSVGIcon`:

```java
// Example replacement
runPipelineButton = new JButton("Run Pipeline");
runPipelineButton.setIcon(new FlatSVGIcon("icon/svg/play.svg", 16, 16));
```

### Required SVG Files
- `play.svg` - Play/run icon for Run Pipeline button
- `arrow_up.svg` - Up arrow for search previous
- `arrow_down.svg` - Down arrow for search next

---

## 2. Left Menu Icons (Priority: Medium)

### Current State
Menu uses numbered SVG files (`icon/0.svg`, `icon/1.svg`, etc.) from FlatIcons pack.

### Icon Concepts by Menu Item

#### Pipelines
- Connected nodes / flow diagram
- Chain links
- Funnel
- Waterfall / cascade
- Arrow sequence
- Stacked filters

#### Units
- Single puzzle piece
- Cube / building block
- Atom / molecule
- Circuit chip / module
- Hexagon cell

#### Settings
- Sliders / equalizer bars
- Toggle switches
- Wrench + screwdriver
- Dial / knob
- Tune icon (horizontal lines with dots)

#### Log
- Scroll / parchment
- Terminal / command prompt (>_)
- List with lines
- Receipt / tape printout
- Notepad with lines

### Design Notes
- Avoid using gear for both Settings and Units - pick distinct metaphors
- Icons should work at 24x24 px (menu) and 16x16 px (buttons)
- Must support light/dark themes (use `currentColor` in SVG stroke/fill)

---

## 3. List Padding Fix (Priority: High)

### Problem
Items in Unit Library and Pipeline Library lists have insufficient padding, making text cramped against borders.

### Affected Files
- `UnitLibraryListForm.java` - line 75-84
- `PipelineListForm.java` - similar structure

### Solution
Add empty border to `infoPanel`:

```java
JPanel infoPanel = new JPanel();
infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 0));  // top, left, bottom, right
```

---

## 4. Implementation Checklist

- [ ] Create `play.svg` icon
- [ ] Create `arrow_up.svg` icon
- [ ] Create `arrow_down.svg` icon
- [ ] Update `RecorderForm.java` to use SVG icons
- [ ] Add padding to `UnitLibraryListForm.java`
- [ ] Add padding to `PipelineListForm.java`
- [ ] Replace left menu icons (pending new icon pack selection)

---

## 5. SVG Requirements

### Format
```xml
<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path stroke="currentColor" stroke-width="1.5" ... />
</svg>
```

### Theme Compatibility
Use `currentColor` for stroke/fill to automatically adapt to light/dark themes.

### Size
- Viewbox: 24x24 recommended
- FlatSVGIcon scales as needed
