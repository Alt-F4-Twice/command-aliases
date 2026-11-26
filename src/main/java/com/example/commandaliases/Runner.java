package com.example.commandaliases;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Sends commands one at a time (one per tick) and pauses for "wait" steps. */
public final class Runner {
    private static final int MAX_PENDING = 30;
    private static final Deque<Action> PENDING = new ArrayDeque<>();
    private static int waitLeft = 0;

    /** True while we are sending an expanded command, so it is not expanded again (no loops). */
    static boolean bypass = false;

    private Runner() {
    }

    public static void queue(List<Action> actions) {
        for (Action a : actions) {
            if (PENDING.size() < MAX_PENDING) { // never flood the server
                PENDING.add(a);
            }
        }
    }

    public static void clear() {
        PENDING.clear();
        waitLeft = 0;
    }

    /**
     * Turns a script (from a hotkey or an automation) into things to send.
     * Quick commands inside it (/s, !s, ?s) are expanded too.
     */
    public static List<Action> compile(String script) {
        List<Action> out = new ArrayList<>();
        for (Action a : Script.actions(script)) {
            if (a.isWait()) {
                out.add(a);
                continue;
            }
            String c = a.command();
            boolean chatPrefix = c.startsWith("!") || c.startsWith("?");
            String typed = chatPrefix ? c : "/" + c;

            List<Action> expanded = AliasManager.expand(typed);
            if (expanded != null) {
                out.addAll(expanded);
            } else if (!chatPrefix) {
                out.add(a);
            }
        }
        return out;
    }

    public static void run(String script) {
        queue(compile(script));
    }

    public static void tick(Minecraft mc) {
        if (waitLeft > 0) {
            waitLeft--;
            return;
        }
        Action a = PENDING.poll();
        if (a == null) {
            return;
        }
        if (a.isWait()) {
            waitLeft = a.waitTicks();
            return;
        }
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            clear();
            return;
        }
        bypass = true;
        try {
            connection.sendCommand(a.command());
        } finally {
            bypass = false;
        }
    }
}
