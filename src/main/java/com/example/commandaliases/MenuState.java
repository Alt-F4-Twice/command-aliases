package com.example.commandaliases;

import net.minecraft.client.gui.screens.Screen;

/**
 * Remembers where you were in the menu (which tab and page) while you are in a world.
 * It is forgotten when you leave the world or server and join again.
 */
final class MenuState {
    static boolean profileChosen = false;
    static int tab = 0;
    static int page = 0;

    private MenuState() {
    }

    static void reset() {
        profileChosen = false;
        tab = 0;
        page = 0;
    }

    /** The screen to show when the menu is opened. */
    static Screen create() {
        return profileChosen ? new AliasListScreen(null) : new ProfileScreen(null);
    }
}
