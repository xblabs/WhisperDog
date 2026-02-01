package org.whisperdog.ui;

/**
 * Defines icon contexts for color theming.
 * Each context can have different colors for light/dark themes.
 */
public enum IconContext {
    /**
     * Default context - used when no specific context is provided.
     */
    DEFAULT,

    /**
     * Side menu icons in expanded state.
     */
    MENU,

    /**
     * Side menu icons in collapsed state (may be more prominent).
     */
    MENU_COLLAPSED,

    /**
     * Button icons - typically more prominent than menu icons.
     */
    BUTTON,

    /**
     * Toolbar icons.
     */
    TOOLBAR,

    /**
     * Panel icons (e.g., in RecordingsPanel).
     */
    PANEL,

    /**
     * Disabled state icons.
     */
    DISABLED
}
