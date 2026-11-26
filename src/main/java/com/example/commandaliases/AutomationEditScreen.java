package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Add or edit an automation: pick when it runs (join / death / respawn) and what it runs. */
public class AutomationEditScreen extends Screen {
    private final Screen parent;
    private final AutomationEntry editing; // null = adding a new one
    private final StepEditor editor;
    private int triggerIndex = 0;
    private Component error = Component.empty();
    private int top;

    public AutomationEditScreen(Screen parent, AutomationEntry editing) {
        super(Component.literal(editing == null ? "Add new automation" : "Edit automation"));
        this.parent = parent;
        this.editing = editing;
        this.editor = new StepEditor(editing != null ? editing.command : "", this::rebuildWidgets);
        if (editing != null) {
            for (int i = 0; i < AutomationEntry.TRIGGERS.length; i++) {
                if (AutomationEntry.TRIGGERS[i].equals(editing.trigger)) {
                    triggerIndex = i;
                }
            }
        }
    }

    private String trigger() {
        return AutomationEntry.TRIGGERS[triggerIndex];
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 150;
        top = Math.max(20, (this.height - (80 + editor.height() + 88)) / 2);

        // Click to cycle: join -> death -> respawn -> join ...
        this.addRenderableWidget(Button.builder(Component.literal(AutomationEntry.buttonText(trigger())), b -> {
            triggerIndex = (triggerIndex + 1) % AutomationEntry.TRIGGERS.length;
            b.setMessage(Component.literal(AutomationEntry.buttonText(trigger())));
        }).bounds(left, top + 28, 300, 20).build());

        editor.build(this.font, left, top + 80, w -> this.addRenderableWidget(w));
        int by = editor.bottom();

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, by + 62, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 154, by + 62, 146, 20).build());
    }

    private void save() {
        String script = editor.script();
        if (!Script.hasCommand(script)) {
            error = Component.literal("Add at least one command for it to run.");
            return;
        }
        if (editing == null) {
            ProfileManager.active().automations.add(new AutomationEntry(trigger(), script, true));
        } else {
            editing.trigger = trigger();
            editing.command = script;
        }
        ProfileManager.save();
        Ui.open(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - 150;
        int by = editor.bottom();

        Look.box(g, left - 10, top - 14, left + 310, by + 88, Look.BORDER);
        g.centeredText(this.font, this.title, this.width / 2, top - 4, Look.GOLD);
        Look.line(g, left, top + 8, left + 300, Look.GOLD);

        g.text(this.font, Component.literal("When should it run? (click to change)"), left, top + 16, Look.GRAY);
        g.text(this.font, Component.literal(AutomationEntry.describe(trigger())), left, top + 52, Look.GOLD);
        g.text(this.font, Component.literal("What should it run?"), left, top + 68, Look.GRAY);

        editor.render(g, this.font);
        g.text(this.font, Component.literal("Automations only run for the profile that is on."),
                left, by + 11, Look.DIM);

        g.text(this.font, Component.literal("This is what runs " + whenText() + ":"), left, by + 24, Look.GRAY);
        String sum = Script.summary(editor.script());
        g.text(this.font, Component.literal(sum.isEmpty() ? "(nothing yet - type a command above)"
                : Look.fit(this.font, sum, 300)), left, by + 35, sum.isEmpty() ? Look.DIM : Look.WHITE);
        g.text(this.font, error, left, by + 49, Look.RED);
    }

    private String whenText() {
        switch (trigger()) {
            case "death": return "when you die";
            case "respawn": return "when you respawn";
            default: return "when you join";
        }
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
