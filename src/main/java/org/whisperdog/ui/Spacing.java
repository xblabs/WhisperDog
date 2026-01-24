package org.whisperdog.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Centralized spacing constants based on 8px base unit.
 * Use these exclusively for consistent padding/margins.
 */
public final class Spacing {

    private Spacing() {} // Utility class

    // Base spacing scale
    public static final int XS = 4;   // Dense inline
    public static final int SM = 8;   // Standard inline
    public static final int MD = 12;  // Section padding
    public static final int LG = 16;  // Panel padding
    public static final int XL = 24;  // Major sections

    // Pre-built borders for common use cases
    public static Border listItem() {
        return BorderFactory.createEmptyBorder(SM, MD, SM, MD);
    }

    public static Border panel() {
        return BorderFactory.createEmptyBorder(LG, LG, LG, LG);
    }

    public static Border section() {
        return BorderFactory.createEmptyBorder(XL, MD, MD, MD);
    }

    public static Border button() {
        return BorderFactory.createEmptyBorder(SM, LG, SM, LG);
    }

    public static Border formField() {
        return BorderFactory.createEmptyBorder(XS, 0, SM, 0);
    }

    // Insets for layout managers
    public static Insets listItemInsets() {
        return new Insets(SM, MD, SM, MD);
    }

    public static Insets panelInsets() {
        return new Insets(LG, LG, LG, LG);
    }

    // Gap utilities for layouts
    public static int gap() { return SM; }
    public static int sectionGap() { return MD; }
    public static int panelGap() { return LG; }
}
