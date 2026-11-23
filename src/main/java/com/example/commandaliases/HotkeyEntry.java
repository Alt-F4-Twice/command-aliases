package com.example.commandaliases;

/** One saved hotkey: press {@code key} (with {@code mods} held) in game to run {@code command}. */
public final class HotkeyEntry {
    /** Physical key (SDL scancode), or -1 if not set. */
    public int key = -1;
    /** What to show for the key (e.g. "H", "["). Optional. */
    public String keyName;
    /** Modifiers that must be held: 1 = Shift, 2 = Ctrl, 4 = Alt (added together). 0 = none. */
    public int mods = 0;
    /** What it runs (e.g. "/s" or "heal ; feed"). Quick commands work here too. */
    public String command;
    public boolean enabled = true;

    /** Not saved: was this hotkey held down last tick? */
    transient boolean wasDown = false;

    public HotkeyEntry() {
    }

    public HotkeyEntry(int key, String keyName, int mods, String command, boolean enabled) {
        this.key = key;
        this.keyName = keyName;
        this.mods = mods;
        this.command = command;
        this.enabled = enabled;
    }
}
