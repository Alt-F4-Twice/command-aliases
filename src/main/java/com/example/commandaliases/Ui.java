package com.example.commandaliases;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * The only place that touches "which screen is open / open a screen".
 * If a Minecraft update renames these, this is the one file to fix.
 */
final class Ui {
    private Ui() {
    }

    static void open(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    static Screen current() {
        return Minecraft.getInstance().gui.screen();
    }
}
