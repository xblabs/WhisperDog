# Implementation Plan: Icon Theming System

## Phase 1: Icon Directory Reorganization

### 1.1 Create Source Icon Directory

**Action**: Create `src/main/resources/icon/source/` for pure black SVGs.

```
icon/
  source/           # NEW: Pure black source icons
    mic.svg
    waveform-1.svg  # For Recordings menu (preferred over waveform-2)
    sliders.svg     # For Settings (renamed from settings)
    pipeline-2.svg  # For PostPro Pipelines (replaces current pipeline icon)
    box.svg         # For PostPro Unit Library
    play.svg
    trash.svg
    delete.svg      # NEW - for delete buttons
    refresh.svg     # NEW - for refresh buttons
    folder.svg      # NEW - for folder open actions
    edit.svg
    ...
  png/
    logo.png
```

**New Icons Added** (already in source/, pure black):
- `refresh.svg` — refresh button
- `delete.svg` — delete button
- `folder.svg` — folder open action
- `waveform-1.svg` — Recordings menu icon (use this one)
- `waveform-2.svg` — alternative, not used
- `pipeline-2.svg` — replaces current pipeline icon for PostPro Pipelines

### 1.2 Prepare Source Icons

**Action**: For each icon, ensure fill color is pure black.

**Verification command** (run from resources/icon/source/):
```bash
grep -l "fill=\"#000000\"\|fill=\"black\"\|fill=\"rgb(0,0,0)\"" *.svg
```

**If icons have other colors**, convert them:
```bash
# Example sed replacement (or manual edit)
sed -i 's/fill="#[0-9A-Fa-f]\{6\}"/fill="#000000"/g' icon.svg
sed -i 's/fill="rgb([^"]*)"/fill="#000000"/g' icon.svg
```

### 1.3 Clean Up Legacy Icons

**Action**: After migration complete, delete numbered icons.

**Files to delete**:
- `icon/0.svg`
- `icon/1.svg`
- `icon/2.svg`
- `icon/3.svg`

**Note**: Keep `icon/svg/` temporarily for non-menu usages until all are migrated.

---

## Phase 2: Color Configuration System

### 2.1 Create IconContext Enum

**New File**: `src/main/java/org/whisperdog/ui/IconContext.java`

```java
package org.whisperdog.ui;

/**
 * Defines contexts where icons appear, each with potentially different colors.
 * Resolution follows hierarchical fallback: specific → category → app default.
 */
public enum IconContext {
    // Application-wide default
    APP_DEFAULT,

    // Left sidebar menu icons
    MENU_DEFAULT,
    MENU_COLLAPSED,
    MENU_HOVER,
    MENU_SELECTED,

    // In-content panel icons (informational)
    PANEL_DEFAULT,
    PANEL_HEADER,

    // Button icons (actionable, prominent)
    BUTTON_DEFAULT,
    BUTTON_HOVER,
    BUTTON_PRESSED,
    BUTTON_DISABLED
}
```

### 2.2 Create IconColors Configuration

**New File**: `src/main/java/org/whisperdog/ui/IconColors.java`

```java
package org.whisperdog.ui;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

/**
 * Centralized icon color configuration.
 * All icon colors defined here for consistent theming.
 */
public final class IconColors {

    private static final Map<IconContext, Color> COLORS = new EnumMap<>(IconContext.class);

    static {
        // Tailwind Gray palette for consistency
        Color gray300 = new Color(0xD1, 0xD5, 0xDB);  // #D1D5DB
        Color gray400 = new Color(0x9C, 0xA3, 0xAF);  // #9CA3AF
        Color gray500 = new Color(0x6B, 0x72, 0x80);  // #6B7280
        Color gray600 = new Color(0x4B, 0x55, 0x63);  // #4B5563
        Color gray700 = new Color(0x37, 0x41, 0x51);  // #374151
        Color gray800 = new Color(0x1F, 0x29, 0x37);  // #1F2937

        // App default (fallback for everything)
        COLORS.put(IconContext.APP_DEFAULT, gray500);

        // Menu icons - subtle, not distracting
        COLORS.put(IconContext.MENU_DEFAULT, gray400);
        COLORS.put(IconContext.MENU_COLLAPSED, gray300);
        COLORS.put(IconContext.MENU_HOVER, gray600);
        COLORS.put(IconContext.MENU_SELECTED, gray700);

        // Panel icons - informational
        COLORS.put(IconContext.PANEL_DEFAULT, gray500);
        COLORS.put(IconContext.PANEL_HEADER, gray600);

        // Button icons - prominent, actionable
        COLORS.put(IconContext.BUTTON_DEFAULT, gray700);
        COLORS.put(IconContext.BUTTON_HOVER, gray800);
        COLORS.put(IconContext.BUTTON_PRESSED, gray800);
        COLORS.put(IconContext.BUTTON_DISABLED, gray300);
    }

    /**
     * Get color for a specific context with fallback chain.
     */
    public static Color getColor(IconContext context) {
        Color color = COLORS.get(context);
        if (color != null) {
            return color;
        }

        // Fallback chain
        return switch (context) {
            case MENU_COLLAPSED, MENU_HOVER, MENU_SELECTED -> getColor(IconContext.MENU_DEFAULT);
            case PANEL_HEADER -> getColor(IconContext.PANEL_DEFAULT);
            case BUTTON_HOVER, BUTTON_PRESSED, BUTTON_DISABLED -> getColor(IconContext.BUTTON_DEFAULT);
            default -> COLORS.get(IconContext.APP_DEFAULT);
        };
    }

    /**
     * Override a color at runtime (for future theme support).
     */
    public static void setColor(IconContext context, Color color) {
        COLORS.put(context, color);
    }

    private IconColors() {} // Utility class
}
```

---

## Phase 3: IconLoader Utility

### 3.1 Create IconLoader Class

**New File**: `src/main/java/org/whisperdog/ui/IconLoader.java`

```java
package org.whisperdog.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;

import javax.swing.Icon;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized icon loading with context-aware color theming.
 *
 * Usage:
 *   Icon menuIcon = IconLoader.getIcon("mic", IconContext.MENU_DEFAULT);
 *   Icon buttonIcon = IconLoader.getIcon("play", IconContext.BUTTON_DEFAULT);
 */
public final class IconLoader {

    private static final String SOURCE_PATH = "icon/source/";
    private static final int DEFAULT_SIZE = 24;

    // Cache for loaded icons (key: name_context_size)
    private static final Map<String, FlatSVGIcon> ICON_CACHE = new HashMap<>();

    /**
     * Load icon with default size (24x24).
     */
    public static Icon getIcon(String name, IconContext context) {
        return getIcon(name, context, DEFAULT_SIZE);
    }

    /**
     * Load icon with specified size.
     */
    public static Icon getIcon(String name, IconContext context, int size) {
        String cacheKey = name + "_" + context.name() + "_" + size;

        FlatSVGIcon icon = ICON_CACHE.get(cacheKey);
        if (icon != null) {
            return icon;
        }

        // Load fresh icon
        String path = SOURCE_PATH + name + ".svg";
        icon = new FlatSVGIcon(path, size, size);

        // Apply color filter for this context
        Color targetColor = IconColors.getColor(context);
        icon.setColorFilter(createColorFilter(targetColor));

        ICON_CACHE.put(cacheKey, icon);
        return icon;
    }

    /**
     * Load icon for button with hover support.
     * Returns array: [default, hover, pressed, disabled]
     */
    public static Icon[] getButtonIcons(String name, int size) {
        return new Icon[] {
            getIcon(name, IconContext.BUTTON_DEFAULT, size),
            getIcon(name, IconContext.BUTTON_HOVER, size),
            getIcon(name, IconContext.BUTTON_PRESSED, size),
            getIcon(name, IconContext.BUTTON_DISABLED, size)
        };
    }

    /**
     * Create ColorFilter that replaces black with target color.
     */
    private static ColorFilter createColorFilter(Color targetColor) {
        return new ColorFilter(color -> {
            // Replace black (the source icon color) with target
            if (isBlack(color)) {
                return targetColor;
            }
            return color;
        });
    }

    /**
     * Check if color is black (handles various representations).
     */
    private static boolean isBlack(Color color) {
        // Pure black
        if (color.equals(Color.BLACK)) {
            return true;
        }
        // Near-black (RGB all < 10)
        return color.getRed() < 10 && color.getGreen() < 10 && color.getBlue() < 10;
    }

    /**
     * Clear icon cache (call when theme changes).
     */
    public static void clearCache() {
        ICON_CACHE.clear();
    }

    private IconLoader() {} // Utility class
}
```

---

## Phase 4: Migrate Existing Icon Usages

### 4.1 Update MenuItem.java

**File**: `src/main/java/org/whisperdog/sidemenu/MenuItem.java`

**Current code (lines 62-70)**:
```java
private Icon getIcon() {
    Color lightColor = FlatUIUtils.getUIColor("Menu.icon.lightColor", Color.red);
    Color darkColor = FlatUIUtils.getUIColor("Menu.icon.darkColor", Color.red);
    FlatSVGIcon icon = new FlatSVGIcon("icon/" + menuIndex + ".svg", 24, 24);
    // ...
}
```

**Replace with**:
```java
private static final Map<String, String> MENU_ICONS = Map.of(
    "Record", "mic",
    "Recordings", "waveform-1",
    "Settings", "sliders",
    "PostPro Pipelines", "pipeline-2",
    "PostPro Unit Library", "box"
);

private Icon getIcon() {
    String menuName = menus[0];  // First element is the menu name
    String iconName = MENU_ICONS.getOrDefault(menuName, "box");  // Fallback to box

    IconContext context = isCollapsed() ? IconContext.MENU_COLLAPSED : IconContext.MENU_DEFAULT;
    return IconLoader.getIcon(iconName, context, 24);
}
```

**Also update hover/selected states** if MenuItem handles those:
```java
private Icon getHoverIcon() {
    String iconName = MENU_ICONS.getOrDefault(menus[0], "box");
    return IconLoader.getIcon(iconName, IconContext.MENU_HOVER, 24);
}

private Icon getSelectedIcon() {
    String iconName = MENU_ICONS.getOrDefault(menus[0], "box");
    return IconLoader.getIcon(iconName, IconContext.MENU_SELECTED, 24);
}
```

### 4.2 Update RecordingsPanel.java Button Icons

**File**: `src/main/java/org/whisperdog/recording/RecordingsPanel.java`

**Current code**:
```java
JButton playButton = new JButton("Play");
playButton.setIcon(new FlatSVGIcon("icon/svg/play.svg", 14, 14));
```

**Replace with**:
```java
JButton playButton = new JButton("Play");
playButton.setIcon(IconLoader.getIcon("play", IconContext.BUTTON_DEFAULT, 14));
```

**For delete button**:
```java
JButton deleteButton = new JButton("Delete");
deleteButton.setIcon(IconLoader.getIcon("trash", IconContext.BUTTON_DEFAULT, 14));
```

### 4.3 Search and Replace All Icon Usages

**Find all FlatSVGIcon instantiations**:
```bash
grep -rn "new FlatSVGIcon" src/main/java/
```

**Expected locations to update**:
- `MenuItem.java` — menu icons
- `RecordingsPanel.java` — play, delete buttons
- `RecordingPanel.java` — record controls (if separate)
- `SettingsPanel.java` — any setting icons
- `MainFrame.java` — window icons (if any)

**Conversion pattern**:
```java
// OLD
new FlatSVGIcon("icon/svg/play.svg", 14, 14)

// NEW
IconLoader.getIcon("play", IconContext.BUTTON_DEFAULT, 14)
```

---

## Phase 5: Testing & Verification

### 5.1 Visual Verification Checklist

- [ ] Menu icons display correct colors when expanded
- [ ] Menu icons display different color when collapsed (if implemented)
- [ ] Button icons are visually more prominent than menu icons
- [ ] Hover states change icon color (if implemented)
- [ ] No icons appear as red/missing (FlatSVGIcon fallback color)

### 5.2 Add New Icon Test

1. Download any black SVG from Lucide/Feather
2. Save to `icon/source/test-icon.svg`
3. Use: `IconLoader.getIcon("test-icon", IconContext.BUTTON_DEFAULT)`
4. Verify it appears with correct button color

### 5.3 Reorder Menu Test

1. Change menu order in `MainFrame.java` or wherever menus are defined
2. Rebuild and run
3. Verify all icons still match their menu items

---

## File Summary

| File | Action |
|------|--------|
| `icon/source/*.svg` | NEW - Pure black source icons |
| `icon/0.svg` through `icon/3.svg` | DELETE after migration |
| `IconContext.java` | NEW - Context enum |
| `IconColors.java` | NEW - Color configuration |
| `IconLoader.java` | NEW - Loading utility |
| `MenuItem.java` | MODIFY - Use IconLoader |
| `RecordingsPanel.java` | MODIFY - Use IconLoader |
| Other panels with icons | MODIFY - Use IconLoader |

---

## Migration Sequence

1. Create `icon/source/` with black SVGs (can be done incrementally)
2. Create `IconContext.java`, `IconColors.java`, `IconLoader.java`
3. Update `MenuItem.java` to use IconLoader
4. Test menu icons work correctly
5. Update other icon usages one file at a time
6. Delete legacy numbered icons
7. Optionally delete `icon/svg/` if fully migrated
