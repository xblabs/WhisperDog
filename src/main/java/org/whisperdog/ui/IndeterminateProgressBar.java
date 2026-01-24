package org.whisperdog.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A subtle 3-pixel indeterminate progress bar with animated gradient sweep.
 * Changes color based on processing stage (transcription vs post-processing).
 */
public class IndeterminateProgressBar extends JPanel {

    public enum Stage {
        TRANSCRIPTION(new Color(100, 149, 237)),    // Cornflower blue
        POST_PROCESSING(new Color(138, 103, 201));  // Bluish purple

        private final Color color;

        Stage(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    private static final int BAR_HEIGHT = 3;
    private static final int ANIMATION_SPEED_MS = 30;
    private static final float GRADIENT_WIDTH_RATIO = 0.3f; // Width of gradient highlight as ratio of bar width

    private Stage currentStage = Stage.TRANSCRIPTION;
    private float animationPosition = 0f; // 0.0 to 1.0
    private Timer animationTimer;
    private boolean isAnimating = false;

    public IndeterminateProgressBar() {
        setPreferredSize(new Dimension(0, BAR_HEIGHT));
        setMinimumSize(new Dimension(0, BAR_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, BAR_HEIGHT));
        setOpaque(false);
        setVisible(false);

        // Animation timer
        animationTimer = new Timer(ANIMATION_SPEED_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animationPosition += 0.02f;
                if (animationPosition > 1.0f + GRADIENT_WIDTH_RATIO) {
                    animationPosition = -GRADIENT_WIDTH_RATIO;
                }
                repaint();
            }
        });
    }

    /**
     * Start the animation with the specified stage color.
     */
    public void start(Stage stage) {
        this.currentStage = stage;
        this.animationPosition = -GRADIENT_WIDTH_RATIO;
        this.isAnimating = true;
        setVisible(true);
        animationTimer.start();
    }

    /**
     * Stop the animation and hide the bar.
     */
    public void stop() {
        this.isAnimating = false;
        animationTimer.stop();
        setVisible(false);
    }

    /**
     * Change the stage (and color) while animation is running.
     */
    public void setStage(Stage stage) {
        this.currentStage = stage;
        repaint();
    }

    /**
     * Check if the bar is currently animating.
     */
    public boolean isAnimating() {
        return isAnimating;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!isAnimating) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            g2.dispose();
            return;
        }

        Color baseColor = currentStage.getColor();
        Color darkColor = darker(baseColor, 0.6f);
        Color lightColor = brighter(baseColor, 1.4f);

        // Draw base bar (darker shade)
        g2.setColor(darkColor);
        g2.fillRect(0, 0, width, height);

        // Calculate gradient sweep position
        int gradientWidth = (int) (width * GRADIENT_WIDTH_RATIO);
        int gradientStart = (int) ((width + gradientWidth) * animationPosition) - gradientWidth;

        // Create gradient paint for the sweep effect
        if (gradientStart < width && gradientStart + gradientWidth > 0) {
            // Clamp to visible area
            int visibleStart = Math.max(0, gradientStart);
            int visibleEnd = Math.min(width, gradientStart + gradientWidth);

            // Create smooth gradient: dark -> light -> dark
            float[] fractions = {0.0f, 0.5f, 1.0f};
            Color[] colors = {darkColor, lightColor, darkColor};

            GradientPaint gradient = new GradientPaint(
                gradientStart, 0, darkColor,
                gradientStart + gradientWidth / 2, 0, lightColor
            );

            // Draw first half (dark to light)
            g2.setPaint(new GradientPaint(
                gradientStart, 0, darkColor,
                gradientStart + gradientWidth / 2, 0, lightColor
            ));
            g2.fillRect(visibleStart, 0, gradientWidth / 2, height);

            // Draw second half (light to dark)
            g2.setPaint(new GradientPaint(
                gradientStart + gradientWidth / 2, 0, lightColor,
                gradientStart + gradientWidth, 0, darkColor
            ));
            g2.fillRect(gradientStart + gradientWidth / 2, 0, gradientWidth / 2, height);
        }

        g2.dispose();
    }

    /**
     * Make a color darker by a factor.
     */
    private Color darker(Color color, float factor) {
        return new Color(
            Math.max(0, (int) (color.getRed() * factor)),
            Math.max(0, (int) (color.getGreen() * factor)),
            Math.max(0, (int) (color.getBlue() * factor)),
            color.getAlpha()
        );
    }

    /**
     * Make a color brighter by a factor.
     */
    private Color brighter(Color color, float factor) {
        return new Color(
            Math.min(255, (int) (color.getRed() * factor)),
            Math.min(255, (int) (color.getGreen() * factor)),
            Math.min(255, (int) (color.getBlue() * factor)),
            color.getAlpha()
        );
    }
}
