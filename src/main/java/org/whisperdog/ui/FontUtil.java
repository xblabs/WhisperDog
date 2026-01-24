package org.whisperdog.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for font management with fallback support.
 * Currently uses Java's default logical fonts (MONOSPACED, SANS_SERIF) for consistency.
 * Can be extended in the future to use specific fonts with better Unicode/emoji support.
 */
public class FontUtil {

    private static final Set<String> availableFonts;

    static {
        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        availableFonts = new HashSet<>(Arrays.asList(fontNames));
    }

    /**
     * Get the standard monospaced font.
     * Uses Java's logical MONOSPACED font for consistency with the original design.
     */
    public static Font getMonospacedFont(int style, int size) {
        return new Font(Font.MONOSPACED, style, size);
    }

    /**
     * Get the standard UI font.
     * Uses Java's logical SANS_SERIF font for consistency.
     */
    public static Font getUiFont(int style, int size) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    /**
     * Check if a specific font is available on the system.
     * Useful for future font selection logic.
     */
    public static boolean isFontAvailable(String fontName) {
        return availableFonts.contains(fontName);
    }

    /**
     * Get a specific font by name with fallback.
     * Returns the requested font if available, otherwise returns the fallback.
     */
    public static Font getFontWithFallback(String preferredFont, String fallbackFont, int style, int size) {
        if (availableFonts.contains(preferredFont)) {
            return new Font(preferredFont, style, size);
        }
        return new Font(fallbackFont, style, size);
    }
}
