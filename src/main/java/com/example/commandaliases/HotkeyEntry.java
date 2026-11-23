package com.example.commandaliases;

/** One saved hotkey: press {@code key} in game to run {@code command}. */
public final class HotkeyEntry {
    /** GLFW key code, or -1 if not set. */
    public int key = -1;
    /** What it runs (e.g. "/s" or "heal ; feed"). Aliases work here too. */
    public String command;
    public boolean enabled = true;

    public HotkeyEntry() {
    }

    public HotkeyEntry(int key, String command, boolean enabled) {
        this.key = key;
        this.command = command;
        this.enabled = enabled;
    }
}
