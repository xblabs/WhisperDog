package org.whisperdog.ui;

import com.formdev.flatlaf.ui.FlatUIUtils;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

/**
 * Centralized icon color configuration.
 * Defines colors for each IconContext with light/dark theme support.
 */
public final class IconColors {

    private IconColors() {} // Utility class

    // Color definitions
    private static final Color MENU_LIGHT = Color.decode("#6B7280");      // Gray-500
    private static final Color MENU_DARK = Color.decode("#9CA3AF");       // Gray-400

    private static final Color MENU_COLLAPSED_LIGHT = Color.decode("#4B5563"); // Gray-600
    private static final Color MENU_COLLAPSED_DARK = Color.decode("#D1D5DB");  // Gray-300

    private static final Color BUTTON_LIGHT = Color.decode("#374151");    // Gray-700
    private static final Color BUTTON_DARK = Color.decode("#E5E7EB");     // Gray-200

    private static final Color TOOLBAR_LIGHT = Color.decode("#4B5563");   // Gray-600
    private static final Color TOOLBAR_DARK = Color.decode("#D1D5DB");    // Gray-300

    private static final Color PANEL_LIGHT = Color.decode("#6B7280");     // Gray-500
    private static final Color PANEL_DARK = Color.decode("#9CA3AF");      // Gray-400

    private static final Color DISABLED_LIGHT = Color.decode("#9CA3AF");  // Gray-400
    private static final Color DISABLED_DARK = Color.decode("#4B5563");   // Gray-600

    // Color maps
    private static final Map<IconContext, Color> LIGHT_COLORS = new EnumMap<>(IconContext.class);
    private static final Map<IconContext, Color> DARK_COLORS = new EnumMap<>(IconContext.class);

    static {
        LIGHT_COLORS.put(IconContext.DEFAULT, MENU_LIGHT);
        LIGHT_COLORS.put(IconContext.MENU, MENU_LIGHT);
        LIGHT_COLORS.put(IconContext.MENU_COLLAPSED, MENU_COLLAPSED_LIGHT);
        LIGHT_COLORS.put(IconContext.BUTTON, BUTTON_LIGHT);
        LIGHT_COLORS.put(IconContext.TOOLBAR, TOOLBAR_LIGHT);
        LIGHT_COLORS.put(IconContext.PANEL, PANEL_LIGHT);
        LIGHT_COLORS.put(IconContext.DISABLED, DISABLED_LIGHT);

        DARK_COLORS.put(IconContext.DEFAULT, MENU_DARK);
        DARK_COLORS.put(IconContext.MENU, MENU_DARK);
        DARK_COLORS.put(IconContext.MENU_COLLAPSED, MENU_COLLAPSED_DARK);
        DARK_COLORS.put(IconContext.BUTTON, BUTTON_DARK);
        DARK_COLORS.put(IconContext.TOOLBAR, TOOLBAR_DARK);
        DARK_COLORS.put(IconContext.PANEL, PANEL_DARK);
        DARK_COLORS.put(IconContext.DISABLED, DISABLED_DARK);
    }

    /**
     * Get the color for a given context, automatically selecting light/dark based on current theme.
     */
    public static Color getColor(IconContext context) {
        return getColor(context, isDarkTheme());
    }

    /**
     * Get the color for a given context and theme.
     */
    public static Color getColor(IconContext context, boolean darkTheme) {
        Map<IconContext, Color> colors = darkTheme ? DARK_COLORS : LIGHT_COLORS;
        return colors.getOrDefault(context, darkTheme ? MENU_DARK : MENU_LIGHT);
    }

    /**
     * Get light and dark colors for a given context (for ColorFilter).
     */
    public static Color[] getColors(IconContext context) {
        return new Color[] {
            LIGHT_COLORS.getOrDefault(context, MENU_LIGHT),
            DARK_COLORS.getOrDefault(context, MENU_DARK)
        };
    }

    /**
     * Check if current theme is dark.
     */
    public static boolean isDarkTheme() {
        // FlatLaf provides this through UIManager
        Color bg = FlatUIUtils.getUIColor("Panel.background", Color.WHITE);
        // Simple heuristic: if background is dark, it's dark theme
        return bg.getRed() + bg.getGreen() + bg.getBlue() < 384;
    }

    /**
     * Get menu icon colors from UIManager (for backward compatibility with existing theme settings).
     * Falls back to our defaults if not set.
     */
    public static Color[] getMenuColors() {
        Color lightColor = FlatUIUtils.getUIColor("Menu.icon.lightColor", MENU_LIGHT);
        Color darkColor = FlatUIUtils.getUIColor("Menu.icon.darkColor", MENU_DARK);
        return new Color[] { lightColor, darkColor };
    }
}
