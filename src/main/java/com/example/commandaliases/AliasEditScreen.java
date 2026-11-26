package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Add or edit a quick command: what you type, and what it runs (one or more commands with waits). */
public class AliasEditScreen extends Screen {
    private final Screen parent;
    private final AliasEntry editing; // null = adding a new one
    private final StepEditor editor;
    private EditBox nameBox;
    private Component error = Component.empty();
    private int top;

    public AliasEditScreen(Screen parent, AliasEntry editing) {
        super(Component.literal(editing == null ? "Add new command" : "Edit command"));
        this.parent = parent;
        this.editing = editing;
        this.editor = new StepEditor(editing != null ? editing.command : "", this::rebuildWidgets);
    }

    @Override
    protected void init() {
        // Keep what was typed if the window is resized or a row is added.
        String nameText = nameBox != null ? nameBox.getValue() : (editing != null ? editing.name : "");

        int left = this.width / 2 - 150;
        top = Math.max(20, (this.height - (90 + editor.height() + 88)) / 2);

        nameBox = new EditBox(this.font, left, top + 28, 300, 20, Component.literal("Quick command"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("e.g. /s   or   !s   or   ?s"));
        nameBox.setValue(nameText);
        this.addRenderableWidget(nameBox);

        editor.build(this.font, left, top + 90, w -> this.addRenderableWidget(w));
        int by = editor.bottom();

        this.setInitialFocus(nameBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, by + 62, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 154, by + 62, 146, 20).build());
    }

    private void save() {
        String name = AliasManager.normalizeTrigger(nameBox.getValue());
        String script = editor.script();

        if (name.isEmpty()) {
            error = Component.literal("Type the quick command first (like /s).");
            return;
        }
        if (name.chars().anyMatch(Character::isWhitespace)) {
            error = Component.literal("The quick command must be one word (no spaces).");
            return;
        }
        if (name.equalsIgnoreCase("/aliases")) {
            error = Component.literal("/aliases is reserved for opening this menu.");
            return;
        }
        if (!Script.hasCommand(script)) {
            error = Component.literal("Add at least one command for it to run.");
            return;
        }
        for (AliasEntry other : AliasManager.all()) {
            if (other != editing && other.name.equalsIgnoreCase(name)) {
                error = Component.literal("You already have a " + name + " command.");
                return;
            }
        }

        if (editing == null) {
            AliasManager.all().add(new AliasEntry(name, script, true));
        } else {
            editing.name = name;
            editing.command = script;
        }
        AliasManager.save();
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

        g.text(this.font, Component.literal("Quick command (what you type)"), left, top + 16, Look.GRAY);
        g.text(this.font, Component.literal("Available prefixes:   !     ?     /"), left, top + 52, Look.GOLD);
        g.text(this.font, Component.literal("/ = typed as a command     ! and ? = typed in chat"), left, top + 63, Look.DIM);
        g.text(this.font, Component.literal("Output command (what it runs)"), left, top + 78, Look.GRAY);

        editor.render(g, this.font);
        g.text(this.font, Component.literal("Extra words you type go on the end, or use {args}"),
                left, by + 11, Look.DIM);

        String trig = AliasManager.normalizeTrigger(nameBox.getValue());
        g.text(this.font, Component.literal("This is what runs when you type "
                + (trig.isEmpty() ? "your shortcut" : trig) + ":"), left, by + 24, Look.GRAY);
        String sum = Script.summary(editor.script());
        g.text(this.font, Component.literal(sum.isEmpty() ? "(nothing yet - type a command above)"
                : Look.fit(this.font, sum, 300)), left, by + 35, sum.isEmpty() ? Look.DIM : Look.WHITE);
        g.text(this.font, error, left, by + 49, Look.RED);
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
