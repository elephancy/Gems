package com.feel.gems.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Pixel-native surfaces shared by the guide, settings, HUD, and casino. */
public final class GemsUiTheme {
    public static final int GOLD = 0xFFE8C889;
    public static final int PALE = 0xFFF0E9D8;
    public static final int MUTED = 0xFFB7C6CC;
    public static final int RULE = 0xFF53636B;
    public static final int PANEL = 0xF0101E29;
    public static final int RAIL = 0xF0172934;
    public static final int WELL = 0xD5213944;
    public static final int FELT = 0xFF1D3940;
    public static final int READY = 0xFF80DAB3;
    public static final int WARNING = 0xFFFFD080;

    private GemsUiTheme() {
    }

    public static void backdrop(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fill(0, 0, width, height, 0xDF07121B);
        graphics.fill(0, 0, width, 2, GOLD);
        graphics.fill(0, height - 2, width, height, RULE);
        for (int x = 10; x < width - 10; x += 24) {
            graphics.fill(x, 8, x + 1, 11, 0x8053636B);
            graphics.fill(x, height - 11, x + 1, height - 8, 0x8053636B);
        }
    }

    public static void panel(GuiGraphicsExtractor graphics, int left, int top,
                             int right, int bottom, int color, boolean lead) {
        if (right <= left || bottom <= top) {
            return;
        }
        graphics.fill(left, top, right, bottom, color);
        graphics.fill(left, top, right, top + 1, RULE);
        graphics.fill(left, bottom - 1, right, bottom, RULE);
        graphics.fill(left, top, left + 1, bottom, RULE);
        graphics.fill(right - 1, top, right, bottom, RULE);
        if (lead) {
            graphics.fill(left + 1, top + 1, right - 1, top + 3, GOLD);
            graphics.fill(left + 7, top + 7, left + 10, top + 10, GOLD);
        }
    }

    public static void rule(GuiGraphicsExtractor graphics, int left, int y, int right, int progress) {
        graphics.fill(left, y, right, y + 1, RULE);
        graphics.fill(left, y, Math.min(right, left + Math.max(0, progress)), y + 1, GOLD);
    }

    /** A reusable inset for a task, setting, or table state. */
    public static void card(GuiGraphicsExtractor graphics, int left, int top,
                            int right, int bottom, int accent) {
        panel(graphics, left, top, right, bottom, RAIL, false);
        graphics.fill(left + 1, top + 1, left + 4, bottom - 1, accent);
    }

    /** Small segmented indicator that remains legible without color. */
    public static void segments(GuiGraphicsExtractor graphics, int left, int top,
                                int count, int filled, int color) {
        int clamped = Math.max(0, Math.min(count, filled));
        for (int i = 0; i < count; i++) {
            int x = left + i * 6;
            graphics.fill(x, top, x + 4, top + 3, i < clamped ? color : RULE);
        }
    }
}
