package dev.vanguard.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Small 2D helpers: rounded rectangles, glow, colour mixing. */
public final class Draw {
    private Draw() {}

    public static int alpha(int rgb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * t + ((b >> 16) & 255) * (1 - t));
        int g = (int) (((a >> 8) & 255) * t + ((b >> 8) & 255) * (1 - t));
        int bl = (int) ((a & 255) * t + (b & 255) * (1 - t));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    public static void roundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }
        g.fill(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            g.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    /** Filled rounded rect with a 1px rounded border. */
    public static void roundBox(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int border, int fill) {
        roundRect(g, x1, y1, x2, y2, r, border);
        roundRect(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), fill);
    }

    /** Soft glow behind a rounded rect, drawn from the outside in. */
    public static void glow(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int rgb) {
        for (int k = 5; k >= 1; k--) {
            roundRect(g, x1 - k, y1 - k, x2 + k, y2 + k, r + k, alpha(rgb, 14 + (6 - k) * 3));
        }
    }

    public static boolean inside(int mx, int my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }
}
