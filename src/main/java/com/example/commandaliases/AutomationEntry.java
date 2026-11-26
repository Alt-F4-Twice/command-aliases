package com.example.commandaliases;

/** Runs a script by itself when something happens: you join, you die, or you respawn. */
public final class AutomationEntry {
    public static final String[] TRIGGERS = {"join", "death", "respawn"};

    /** "join", "death" or "respawn". */
    public String trigger = "join";
    /** The script to run (commands, ";" and "wait N"). */
    public String command = "";
    public boolean enabled = true;

    public AutomationEntry() {
    }

    public AutomationEntry(String trigger, String command, boolean enabled) {
        this.trigger = trigger;
        this.command = command;
        this.enabled = enabled;
    }

    /** Short label for the list. */
    public static String label(String trigger) {
        switch (trigger == null ? "" : trigger) {
            case "death": return "On death";
            case "respawn": return "On respawn";
            default: return "On join";
        }
    }

    /** Text for the trigger button. */
    public static String buttonText(String trigger) {
        switch (trigger == null ? "" : trigger) {
            case "death": return "When: I die";
            case "respawn": return "When: I respawn";
            default: return "When: I join a world or server";
        }
    }

    /** Plain-English explanation shown under the trigger button. */
    public static String describe(String trigger) {
        switch (trigger == null ? "" : trigger) {
            case "death": return "Runs the moment you die.";
            case "respawn": return "Runs a moment after you respawn (after dying).";
            default: return "Runs about 2 seconds after you join a world or server.";
        }
    }
}
