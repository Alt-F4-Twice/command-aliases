package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Colors and tiny drawing helpers so every screen looks the same. */
final class Look {
    static final int WHITE = 0xFFFFFFFF;
    static final int GOLD = 0xFFFFAA00;
    static final int AQUA = 0xFF55FFFF;
    static final int GRAY = 0xFFAAAAAA;
    static final int DIM = 0xFF808080;
    static final int RED = 0xFFFF5555;
    static final int BORDER = 0xFF555555;

    private Look() {
    }

    /** 1px outline (drawn in gaps, never over buttons). */
    static void box(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    static void line(GuiGraphicsExtractor g, int x1, int y, int x2, int color) {
        g.fill(x1, y, x2, y + 1, color);
    }
}
