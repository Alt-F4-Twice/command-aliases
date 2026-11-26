package com.example.commandaliases;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Hotkeys of the profile that is on. Fires them when their key (and modifiers) go down. */
public final class HotkeyManager {
    private HotkeyManager() {
    }

    public static List<HotkeyEntry> all() {
        return ProfileManager.active().hotkeys;
    }

    public static void save() {
        ProfileManager.save();
    }

    private static boolean down(int key) {
        return InputConstants.isKeyDown(key);
    }

    /** Which of Shift / Ctrl / Alt are held right now (1 / 2 / 4). Uses SDL scancodes. */
    public static int currentMods() {
        int m = 0;
        if (down(225) || down(229)) {   // left / right Shift
            m |= 1;
        }
        if (down(224) || down(228)) {   // left / right Ctrl
            m |= 2;
        }
        if (down(226) || down(230)) {   // left / right Alt
            m |= 4;
        }
        return m;
    }

    /** Called every client tick: fires a hotkey the moment its key (and modifiers) go down. */
    public static void tick(Minecraft mc) {
        List<HotkeyEntry> list = all();
        if (mc.player == null) {
            for (HotkeyEntry h : list) {
                h.wasDown = false;
            }
            return;
        }
        boolean canFire = Ui.current() == null;
        int modsNow = currentMods();

        for (HotkeyEntry h : list) {
            if (!h.enabled || h.key < 0) {
                h.wasDown = false;
                continue;
            }
            boolean active = down(h.key) && modsNow == h.mods;
            if (active && !h.wasDown && canFire) {
                Runner.run(h.command);
            }
            h.wasDown = active;
        }
    }
}
