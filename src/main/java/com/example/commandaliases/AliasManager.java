package com.example.commandaliases;

import java.util.List;

/** Quick commands of the profile that is on. Also works out what a typed shortcut should run. */
public final class AliasManager {
    /** The characters a quick command can start with. */
    public static final String PREFIXES = "!?/";

    private AliasManager() {
    }

    public static boolean isPrefix(char c) {
        return PREFIXES.indexOf(c) >= 0;
    }

    /** "s" -> "/s", "!s" -> "!s", "//s" -> "/s". Returns "" if nothing usable. */
    public static String normalizeTrigger(String raw) {
        String t = raw.strip();
        if (t.isEmpty()) {
            return "";
        }
        char prefix = '/';
        if (isPrefix(t.charAt(0))) {
            prefix = t.charAt(0);
            t = t.substring(1);
        }
        if (prefix == '/') {
            while (t.startsWith("/")) {
                t = t.substring(1);
            }
        }
        return t.isEmpty() ? "" : prefix + t;
    }

    public static List<AliasEntry> all() {
        return ProfileManager.active().aliases;
    }

    public static void save() {
        ProfileManager.save();
    }

    public static AliasEntry find(String trigger) {
        for (AliasEntry e : all()) {
            if (e.enabled && e.name.equalsIgnoreCase(trigger)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Turns what the player typed (including its prefix: /s, !s or ?s) into the steps to run,
     * or null if it isn't a quick command. Extra words are added to the end of the last
     * command, or put where {args} appears.
     */
    public static List<Action> expand(String typed) {
        String t = typed.strip();
        int space = t.indexOf(' ');
        String trigger = space < 0 ? t : t.substring(0, space);
        String args = space < 0 ? "" : t.substring(space + 1).strip();

        AliasEntry entry = find(trigger);
        if (entry == null) {
            return null;
        }
        List<Action> out = Script.actions(entry.command);
        if (out.isEmpty()) {
            return null;
        }

        boolean usedPlaceholder = false;
        for (int i = 0; i < out.size(); i++) {
            Action a = out.get(i);
            if (!a.isWait() && a.command().contains("{args}")) {
                usedPlaceholder = true;
                out.set(i, Action.cmd(a.command().replace("{args}", args).strip()));
            }
        }
        if (!usedPlaceholder && !args.isEmpty()) {
            for (int i = out.size() - 1; i >= 0; i--) {
                if (!out.get(i).isWait()) {
                    out.set(i, Action.cmd(out.get(i).command() + " " + args));
                    break;
                }
            }
        }
        return out;
    }
}
