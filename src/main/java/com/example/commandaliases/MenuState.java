package com.example.commandaliases;

import net.minecraft.client.gui.screens.Screen;

/**
 * Remembers where you were in the menu (which tab and page) while you are in a world.
 * It is forgotten when you leave the world or server and join again.
 */
final class MenuState {
    static boolean profileChosen = false;
    /** Id of the profile whose contents you are editing. It does not have to be the one that is on. */
    static String viewingId = null;
    static int tab = 0;
    static int page = 0;

    private MenuState() {
    }

    static void reset() {
        profileChosen = false;
        viewingId = null;
        tab = 0;
        page = 0;
    }

    /** The profile you are editing (falls back to the one that is on). */
    static Profile viewed() {
        Profile p = viewingId == null ? null : ProfileManager.byId(viewingId);
        return p != null ? p : ProfileManager.active();
    }

    /** The screen to show when the menu is opened. */
    static Screen create() {
        return profileChosen ? new AliasListScreen(null) : new ProfileScreen(null);
    }
}
