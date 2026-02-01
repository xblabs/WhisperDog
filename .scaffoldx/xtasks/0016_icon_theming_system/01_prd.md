# PRD: Icon Theming System

## Problem Statement

The current icon system in WhisperDog has two fundamental architectural flaws causing maintenance burden and theming impossibility:

**Flaw 1: Index-Based Icon Naming**
Icons in `src/main/resources/icon/` are named `0.svg`, `1.svg`, `2.svg`, `3.svg` and loaded by menu position index in `MenuItem.java:65`. When the "Recordings" menu item was inserted at position 1, all icon associations shifted — Recordings displays the gear icon, Settings displays the pipeline icon, etc. This requires renumbering files whenever menu order changes.

**Flaw 2: Pre-Baked Colors**
Icons have gray fill colors baked directly into the SVG files. To change icon colors for theming or different contexts (e.g., more prominent button icons vs subtle menu icons), each SVG must be manually edited. This prevents:
- Consistent color updates across all icons
- Context-specific coloring (menu vs button vs panel)
- Dark/light theme support
- Hover/active state color changes

**Current State**:
- `icon/0.svg` through `icon/3.svg` — numbered, pre-colored gray
- `icon/svg/*.svg` — semantically named but still pre-colored
- `icon/png/logo.png` — raster logo

**Impact**:
- Every menu reorder requires icon renumbering (happened with Task 0010)
- No way to make button icons more prominent than menu icons
- Theme changes require editing 15+ SVG files
- Inconsistent icon colors across the application

## Solution Overview

Implement a runtime icon theming system following the standard pattern used by design systems:

### Architecture

```
Source Icons (Pure Black)     Color Configuration     Runtime Application
        │                            │                        │
  icon/source/*.svg    →    IconColors.java    →    IconLoader.getIcon()
  (fill="#000000")          (context→color map)     (ColorFilter replacement)
```

### Key Components

1. **Source Icon Directory**: `icon/source/` containing pure black (#000000) SVGs from standard icon libraries (Lucide, Feather, etc.)

2. **Color Configuration**: Centralized `IconColors.java` defining colors for each context:
   ```
   app.default     → #6B7280 (Gray-500, fallback)
   menu.default    → #9CA3AF (Gray-400, sidebar icons)
   menu.collapsed  → #D1D5DB (Gray-300, collapsed sidebar)
   panel.default   → #6B7280 (Gray-500, in-content)
   button.default  → #374151 (Gray-700, prominent)
   button.hover    → #1F2937 (Gray-800, hover state)
   ```

3. **IconLoader Utility**: Single point of icon loading that applies ColorFilter based on context:
   ```java
   IconLoader.getIcon("mic", IconContext.MENU)      // Gray-400
   IconLoader.getIcon("play", IconContext.BUTTON)   // Gray-700
   ```

### Hierarchical Resolution

Colors resolve with fallback chain:
```
button.hover → button.default → app.default
menu.collapsed → menu.default → app.default
panel.default → app.default
```

## Success Metrics

- Zero icon renumbering when menu order changes
- Single location to update all icon colors
- Distinct visual prominence: button > panel > menu
- New icon addition requires only: drop SVG in source/, optionally add to config

## Technical Constraints

- Must work with FlatLaf's FlatSVGIcon and ColorFilter API
- SVG color replacement targets `#000000` and `rgb(0,0,0)` and `black`
- Must not break existing icon usages during migration
- Performance: ColorFilter application must be negligible (<1ms per icon)

## Out of Scope

- Multi-color icon support (icons remain single-color)
- Icon caching/preloading optimization
- Dark/light theme switching (future task)
- Animated icons
