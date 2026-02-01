package org.whisperdog.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized icon loader with context-aware color theming.
 * Loads pure black source SVGs and applies runtime coloring based on context.
 */
public final class IconLoader {

    private IconLoader() {} // Utility class

    // Source color in SVG files (pure black)
    private static final Color SOURCE_COLOR = Color.BLACK;

    // Icon cache: key = "name_size_context"
    private static final Map<String, FlatSVGIcon> cache = new HashMap<>();

    // Menu icon name mapping (semantic name to icon file name)
    private static final Map<String, String> MENU_ICONS = new HashMap<>();

    static {
        // Menu icons by menu name (first menu item text)
        MENU_ICONS.put("Record", "mic");
        MENU_ICONS.put("Recordings", "waveform-1");
        MENU_ICONS.put("Settings", "sliders");
        MENU_ICONS.put("PostPro Pipelines", "pipeline-1");
        MENU_ICONS.put("PostPro Unit Library", "box");
    }

    // Standard sizes
    public static final int SIZE_SM = 16;
    public static final int SIZE_MD = 20;
    public static final int SIZE_LG = 24;

    /**
     * Load an icon with default context (MENU).
     */
    public static FlatSVGIcon load(String name, int size) {
        return load(name, size, IconContext.DEFAULT);
    }

    /**
     * Load an icon with specific context for coloring.
     */
    public static FlatSVGIcon load(String name, int size, IconContext context) {
        String cacheKey = name + "_" + size + "_" + context.name();

        return cache.computeIfAbsent(cacheKey, k -> {
            try {
                FlatSVGIcon icon = new FlatSVGIcon("icon/source/" + name + ".svg", size, size);
                applyColorFilter(icon, context);
                return icon;
            } catch (Exception e) {
                System.err.println("Failed to load icon from source: " + name + ", falling back to svg/");
                // Fallback to svg/ directory
                try {
                    return new FlatSVGIcon("icon/svg/" + name + ".svg", size, size);
                } catch (Exception e2) {
                    System.err.println("Failed to load icon: " + name);
                    return null;
                }
            }
        });
    }

    /**
     * Load a menu icon by menu name (e.g., "Record", "Settings").
     */
    public static FlatSVGIcon loadMenuIcon(String menuName, int size, boolean collapsed) {
        String iconName = MENU_ICONS.get(menuName);
        if (iconName == null) {
            System.err.println("No icon mapping for menu: " + menuName);
            return null;
        }
        IconContext context = collapsed ? IconContext.MENU_COLLAPSED : IconContext.MENU;
        return load(iconName, size, context);
    }

    /**
     * Load a menu icon by menu index (for backward compatibility).
     * Maps index to semantic name internally.
     */
    public static FlatSVGIcon loadMenuIconByIndex(int index, int size, boolean collapsed) {
        String menuName = getMenuNameByIndex(index);
        if (menuName == null) {
            System.err.println("No menu at index: " + index);
            return null;
        }
        return loadMenuIcon(menuName, size, collapsed);
    }

    /**
     * Load a button icon (more prominent coloring).
     */
    public static FlatSVGIcon loadButton(String name, int size) {
        return load(name, size, IconContext.BUTTON);
    }

    /**
     * Load a panel icon.
     */
    public static FlatSVGIcon loadPanel(String name, int size) {
        return load(name, size, IconContext.PANEL);
    }

    /**
     * Load a disabled icon.
     */
    public static FlatSVGIcon loadDisabled(String name, int size) {
        return load(name, size, IconContext.DISABLED);
    }

    /**
     * Create an icon with custom color override.
     */
    public static FlatSVGIcon loadWithColor(String name, int size, Color lightColor, Color darkColor) {
        String cacheKey = name + "_" + size + "_custom_" + lightColor.getRGB() + "_" + darkColor.getRGB();

        return cache.computeIfAbsent(cacheKey, k -> {
            try {
                FlatSVGIcon icon = new FlatSVGIcon("icon/source/" + name + ".svg", size, size);
                FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter();
                filter.add(SOURCE_COLOR, lightColor, darkColor);
                icon.setColorFilter(filter);
                return icon;
            } catch (Exception e) {
                System.err.println("Failed to load icon with custom color: " + name);
                return null;
            }
        });
    }

    /**
     * Apply color filter to icon based on context.
     */
    private static void applyColorFilter(FlatSVGIcon icon, IconContext context) {
        Color[] colors = IconColors.getColors(context);
        FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter();
        filter.add(SOURCE_COLOR, colors[0], colors[1]);
        icon.setColorFilter(filter);
    }

    /**
     * Get menu name by index (for backward compatibility).
     */
    private static String getMenuNameByIndex(int index) {
        // These must match the order in Menu.java menuItems array
        // (excluding title entries marked with ~)
        switch (index) {
            case 0: return "Record";
            case 1: return "Recordings";
            case 2: return "Settings";
            case 3: return "PostPro Pipelines";
            case 4: return "PostPro Unit Library";
            default: return null;
        }
    }

    /**
     * Clear the icon cache (useful when theme changes).
     */
    public static void clearCache() {
        cache.clear();
    }

    /**
     * Get all registered menu icon names.
     */
    public static Map<String, String> getMenuIconMappings() {
        return new HashMap<>(MENU_ICONS);
    }
}
