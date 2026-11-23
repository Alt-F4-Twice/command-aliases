package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Two boxes: the quick command you type, and the command it runs. Used for Add and Edit. */
public class AliasEditScreen extends Screen {
    private final Screen parent;
    private final AliasEntry editing; // null = adding a new one
    private EditBox nameBox;
    private EditBox commandBox;
    private Component error = Component.empty();
    private int top;

    public AliasEditScreen(Screen parent, AliasEntry editing) {
        super(Component.literal(editing == null ? "Add new command" : "Edit command"));
        this.parent = parent;
        this.editing = editing;
    }

    @Override
    protected void init() {
        // Keep what was typed if the window is resized.
        String nameText = nameBox != null ? nameBox.getValue() : (editing != null ? editing.name : "");
        String cmdText = commandBox != null ? commandBox.getValue() : (editing != null ? editing.command : "");

        int left = this.width / 2 - 150;
        top = Math.max(24, this.height / 2 - 90);

        nameBox = new EditBox(this.font, left, top + 28, 300, 20, Component.literal("Quick command"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("e.g. /s   or   !s   or   ?s"));
        nameBox.setValue(nameText);
        this.addRenderableWidget(nameBox);

        commandBox = new EditBox(this.font, left, top + 72, 300, 20, Component.literal("Output command"));
        commandBox.setMaxLength(256);
        commandBox.setHint(Component.literal("e.g. /gamemode survival @s"));
        commandBox.setValue(cmdText);
        this.addRenderableWidget(commandBox);

        this.setInitialFocus(nameBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, top + 160, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 154, top + 160, 146, 20).build());
    }

    private void save() {
        String name = AliasManager.normalizeTrigger(nameBox.getValue());
        String cmd = commandBox.getValue().strip();

        if (name.isEmpty()) {
            error = Component.literal("The quick command can't be empty.");
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
        if (cmd.replace("/", "").replace(";", "").isBlank()) {
            error = Component.literal("The output command can't be empty.");
            return;
        }
        for (AliasEntry other : AliasManager.all()) {
            if (other != editing && other.name.equalsIgnoreCase(name)) {
                error = Component.literal("You already have a " + name + " command.");
                return;
            }
        }

        if (editing == null) {
            AliasManager.all().add(new AliasEntry(name, cmd, true));
        } else {
            editing.name = name;
            editing.command = cmd;
        }
        AliasManager.sort();
        AliasManager.save();
        Ui.open(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - 150;

        Look.box(g, left - 10, top - 14, left + 310, top + 190, Look.BORDER);
        g.centeredText(this.font, this.title, this.width / 2, top - 4, Look.GOLD);
        Look.line(g, left, top + 8, left + 300, Look.GOLD);

        g.text(this.font, Component.literal("Quick command (what you type)"), left, top + 16, Look.GRAY);
        g.text(this.font, Component.literal("Output command (what it runs)"), left, top + 60, Look.GRAY);

        g.text(this.font, Component.literal("Available prefixes:   !     ?     /"), left, top + 98, Look.GOLD);
        g.text(this.font, Component.literal("/ = typed as a command    ! and ? = typed in chat"), left, top + 109, Look.DIM);
        g.text(this.font, Component.literal("Extra words you type go on the end, or use {args}"), left, top + 120, Look.DIM);
        g.text(this.font, Component.literal("Chain several commands with ;"), left, top + 131, Look.DIM);
        g.text(this.font, error, left, top + 146, Look.RED);
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
