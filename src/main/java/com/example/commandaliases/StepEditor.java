package com.example.commandaliases;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The "chain builder": a first command box, then up to 4 more rows. Each extra row has a
 * Wait button (click to cycle 0-5 seconds) and its own command box. It reads and writes
 * the plain script text, e.g.  /gamemode creative ; wait 2 ; /tp @s ~ 100 ~
 */
final class StepEditor {
    interface Adder {
        void add(AbstractWidget widget);
    }

    static final int MAX_EXTRA = 4;

    private static final class Step {
        int wait;
        String text;
        EditBox box;

        Step(int wait, String text) {
            this.wait = wait;
            this.text = text;
        }
    }

    private final List<Step> steps = new ArrayList<>();
    private final Runnable rebuild;
    private int leftX = 0;
    private int bottomY = 0;

    StepEditor(String script, Runnable rebuild) {
        this.rebuild = rebuild;
        load(script);
    }

    private void load(String script) {
        int pendingWait = 0;
        for (String raw : script.split(";")) {
            String part = raw.strip();
            if (part.isEmpty()) {
                continue;
            }
            Integer ticks = Script.waitTicks(part);
            if (ticks != null) {
                pendingWait += Math.round(ticks / 20f);
                continue;
            }
            if (steps.isEmpty()) {
                steps.add(new Step(0, part));
            } else if (steps.size() < MAX_EXTRA + 1) {
                steps.add(new Step(pendingWait, part));
            } else {
                // more than the builder has rows for: keep the rest as text in the last row
                Step last = steps.get(steps.size() - 1);
                last.text = last.text + (pendingWait > 0 ? " ; wait " + pendingWait : "") + " ; " + part;
            }
            pendingWait = 0;
        }
        if (steps.isEmpty()) {
            steps.add(new Step(0, ""));
        }
    }

    private void syncText() {
        for (Step s : steps) {
            if (s.box != null) {
                s.text = s.box.getValue();
            }
        }
    }

    /** Height this editor needs: one row per command plus the "add" row. */
    int height() {
        return 24 * (steps.size() + 1);
    }

    /** Y just below the editor (where the screen can continue). */
    int bottom() {
        return bottomY;
    }

    void build(Font font, int left, int y, Adder adder) {
        syncText(); // keep what was typed when the screen is rebuilt
        leftX = left;
        int cy = y;

        for (int i = 0; i < steps.size(); i++) {
            final Step s = steps.get(i);
            if (i == 0) {
                s.box = new EditBox(font, left, cy, 300, 20, Component.literal("First command"));
            } else {
                adder.add(Button.builder(Component.literal("Wait: " + s.wait + "s"), b -> {
                    s.wait = s.wait >= 5 ? 0 : s.wait + 1;
                    b.setMessage(Component.literal("Wait: " + s.wait + "s"));
                }).bounds(left, cy, 74, 20).build());

                s.box = new EditBox(font, left + 78, cy, 198, 20, Component.literal("Next command"));

                adder.add(Button.builder(Component.literal("X"), b -> {
                    syncText();
                    steps.remove(s);
                    rebuild.run();
                }).bounds(left + 280, cy, 20, 20).build());
            }
            s.box.setMaxLength(256);
            s.box.setHint(Component.literal(i == 0 ? "e.g. /gamemode survival @s" : "e.g. /tp @s ~ 100 ~"));
            s.box.setValue(s.text);
            adder.add(s.box);
            cy += 24;
        }

        if (steps.size() - 1 < MAX_EXTRA) {
            adder.add(Button.builder(Component.literal("+ Add another command (with a wait)"), b -> {
                syncText();
                steps.add(new Step(1, ""));
                rebuild.run();
            }).bounds(left, cy, 300, 20).build());
        }
        cy += 24;
        bottomY = cy;
    }

    /** The script text for what is in the boxes right now. */
    String script() {
        syncText();
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            String t = s.text.strip();
            if (t.isEmpty()) {
                continue;
            }
            if (i > 0 && s.wait > 0) {
                parts.add("wait " + s.wait);
            }
            parts.add(t);
        }
        return String.join(" ; ", parts);
    }

    void render(GuiGraphicsExtractor g, Font font) {
        String tip = steps.size() > 1
                ? "Wait = a pause before the next command (0-5 seconds)."
                : "Tip: add another command to run things one after another.";
        g.text(font, Component.literal(tip), leftX, bottomY, Look.DIM);
    }
}
