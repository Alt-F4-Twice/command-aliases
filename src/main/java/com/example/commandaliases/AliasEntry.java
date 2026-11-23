package com.example.commandaliases;

/** One saved alias. Public fields + no-arg constructor so Gson can read/write it. */
public final class AliasEntry {
    /** What you type, without the leading slash (e.g. "s"). */
    public String name;
    /** What it runs (e.g. "gamemode survival"). May chain several with ";". */
    public String command;
    public boolean enabled = true;

    public AliasEntry() {
    }

    public AliasEntry(String name, String command, boolean enabled) {
        this.name = name;
        this.command = command;
        this.enabled = enabled;
    }
}
