package com.example.commandaliases;

import net.minecraft.client.Minecraft;

/** Watches for joining a world, dying and respawning, and runs the matching automations. */
final class Automations {
    private static boolean inGame = false;
    private static int joinTimer = -1;
    private static boolean wasDead = false;
    private static int respawnTimer = -1;

    private Automations() {
    }

    static void tick(Minecraft mc) {
        boolean now = mc.level != null && mc.player != null;

        if (now && !inGame) {          // just joined a world or server
            joinTimer = 40;            // wait about 2 seconds so the game is ready
            wasDead = false;
            respawnTimer = -1;
        }
        if (!now && inGame) {          // just left the world or server
            MenuState.reset();
            Runner.clear();
            joinTimer = -1;
            wasDead = false;
            respawnTimer = -1;
        }
        inGame = now;
        if (!now) {
            return;
        }

        if (joinTimer > 0) {
            joinTimer--;
            if (joinTimer == 0) {
                fire("join");
            }
        }

        boolean dead = mc.player.getHealth() <= 0.0F;
        if (dead && !wasDead) {
            wasDead = true;
            respawnTimer = -1;
            fire("death");
        } else if (!dead && wasDead) {
            wasDead = false;
            respawnTimer = 10;         // short pause after respawning
        }
        if (respawnTimer > 0) {
            respawnTimer--;
            if (respawnTimer == 0) {
                fire("respawn");
            }
        }
    }

    private static void fire(String trigger) {
        for (AutomationEntry a : ProfileManager.active().automations) {
            if (a.enabled && trigger.equals(a.trigger)) {
                Runner.run(a.command);
            }
        }
    }
}
