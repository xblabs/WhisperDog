package org.whisperdog.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized icon management using FlatSVGIcon.
 * All icons loaded from resources/icon/svg/ directory.
 */
public final class Icons {

    private Icons() {} // Utility class

    // Standard sizes
    public static final int SIZE_SM = 16;
    public static final int SIZE_MD = 20;
    public static final int SIZE_LG = 24;

    // Icon cache
    private static final Map<String, FlatSVGIcon> cache = new HashMap<>();

    // Pre-defined icons (lazy-loaded)
    public static FlatSVGIcon mic() { return get("mic", SIZE_MD); }
    public static FlatSVGIcon stop() { return get("stop", SIZE_MD); }
    public static FlatSVGIcon play() { return get("play", SIZE_SM); }
    public static FlatSVGIcon sliders() { return get("sliders", SIZE_MD); }
    public static FlatSVGIcon terminal() { return get("terminal", SIZE_MD); }
    public static FlatSVGIcon gitPullRequest() { return get("git-pull-request", SIZE_MD); }
    public static FlatSVGIcon box() { return get("box", SIZE_MD); }
    public static FlatSVGIcon chevronDown() { return get("chevron-down", SIZE_SM); }
    public static FlatSVGIcon chevronUp() { return get("chevron-up", SIZE_SM); }
    public static FlatSVGIcon edit() { return get("edit", SIZE_SM); }
    public static FlatSVGIcon trash() { return get("trash", SIZE_SM); }
    public static FlatSVGIcon dark() { return get("dark", SIZE_SM); }
    public static FlatSVGIcon search() { return get("search", SIZE_SM); }
    public static FlatSVGIcon copy() { return get("copy", SIZE_SM); }
    public static FlatSVGIcon folder() { return get("folder", SIZE_SM); }
    public static FlatSVGIcon x() { return get("x", SIZE_SM); }
    public static FlatSVGIcon clock() { return get("clock", SIZE_SM); }
    public static FlatSVGIcon refreshCw() { return get("refresh-cw", SIZE_SM); }
    public static FlatSVGIcon alertCircle() { return get("alert-circle", SIZE_SM); }
    public static FlatSVGIcon checkCircle() { return get("check-circle", SIZE_SM); }

    /**
     * Get icon by name and size, with caching.
     */
    public static FlatSVGIcon get(String name, int size) {
        String key = name + "_" + size;
        return cache.computeIfAbsent(key, k -> {
            try {
                return new FlatSVGIcon("icon/svg/" + name + ".svg", size, size);
            } catch (Exception e) {
                System.err.println("Failed to load icon: " + name);
                return null;
            }
        });
    }

    /**
     * Create a scaled version of an icon.
     */
    public static FlatSVGIcon scaled(String name, float scale) {
        return get(name, (int)(SIZE_MD * scale));
    }
}
