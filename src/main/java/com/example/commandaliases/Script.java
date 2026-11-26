package com.example.commandaliases;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A "script" is what you type in an output-command box: commands separated by ";",
 * and "wait 2" (seconds) to pause. Example:  /gamemode creative ; wait 2 ; /tp @s ~ 100 ~
 * The chain builder in the menus just writes this text for you.
 */
final class Script {
    private static final Pattern WAIT =
            Pattern.compile("^wait\\s+(\\d+(?:\\.\\d+)?)\\s*s?$", Pattern.CASE_INSENSITIVE);

    private Script() {
    }

    /** Ticks for "wait 2" or "wait 1.5s", or null if this part is not a wait. */
    static Integer waitTicks(String part) {
        Matcher m = WAIT.matcher(part.strip());
        if (!m.matches()) {
            return null;
        }
        double seconds = Double.parseDouble(m.group(1));
        long ticks = Math.round(seconds * 20);
        return (int) Math.min(1200L, ticks);
    }

    /** Splits a script into commands and waits. Leading slashes are removed from commands. */
    static List<Action> actions(String script) {
        List<Action> out = new ArrayList<>();
        if (script == null) {
            return out;
        }
        for (String raw : script.split(";")) {
            String part = raw.strip();
            if (part.isEmpty()) {
                continue;
            }
            Integer ticks = waitTicks(part);
            if (ticks != null) {
                out.add(Action.wait(ticks));
                continue;
            }
            while (part.startsWith("/")) {
                part = part.substring(1).strip();
            }
            if (!part.isEmpty()) {
                out.add(Action.cmd(part));
            }
        }
        return out;
    }

    static boolean hasCommand(String script) {
        for (Action a : actions(script)) {
            if (!a.isWait()) {
                return true;
            }
        }
        return false;
    }

    /** Readable one-liner: "/gamemode creative  |  wait 2s  |  /tp @s ~ 100 ~" */
    static String summary(String script) {
        List<String> bits = new ArrayList<>();
        for (Action a : actions(script)) {
            if (a.isWait()) {
                bits.add("wait " + seconds(a.waitTicks()));
            } else {
                String c = a.command();
                bits.add(c.startsWith("!") || c.startsWith("?") ? c : "/" + c);
            }
        }
        return String.join("  |  ", bits);
    }

    private static String seconds(int ticks) {
        if (ticks % 20 == 0) {
            return (ticks / 20) + "s";
        }
        return (Math.round(ticks / 2.0) / 10.0) + "s";
    }
}
