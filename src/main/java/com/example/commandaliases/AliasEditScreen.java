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
        top = Math.max(24, this.height / 2 - 80);

        nameBox = new EditBox(this.font, left, top + 24, 300, 20, Component.literal("Quick command"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("e.g. /s"));
        nameBox.setValue(nameText);
        this.addRenderableWidget(nameBox);

        commandBox = new EditBox(this.font, left, top + 70, 300, 20, Component.literal("Output command"));
        commandBox.setMaxLength(256);
        commandBox.setHint(Component.literal("e.g. /gamemode survival"));
        commandBox.setValue(cmdText);
        this.addRenderableWidget(commandBox);

        this.setInitialFocus(nameBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, top + 132, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 154, top + 132, 146, 20).build());
    }

    private void save() {
        String name = nameBox.getValue().strip();
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        String cmd = commandBox.getValue().strip();

        if (name.isEmpty()) {
            error = Component.literal("The quick command can't be empty.");
            return;
        }
        if (name.chars().anyMatch(Character::isWhitespace)) {
            error = Component.literal("The quick command must be one word (no spaces).");
            return;
        }
        if (name.equalsIgnoreCase("aliases")) {
            error = Component.literal("/aliases is reserved for opening this menu.");
            return;
        }
        if (cmd.replace("/", "").replace(";", "").isBlank()) {
            error = Component.literal("The output command can't be empty.");
            return;
        }
        for (AliasEntry other : AliasManager.all()) {
            if (other != editing && other.name.equals(name)) {
                error = Component.literal("You already have a /" + name + " command.");
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

        g.centeredText(this.font, this.title, this.width / 2, top - 4, 0xFFFFFFFF);
        g.text(this.font, Component.literal("Quick command (what you type)"), left, top + 12, 0xFFAAAAAA);
        g.text(this.font, Component.literal("Output command (what it runs)"), left, top + 58, 0xFFAAAAAA);
        g.text(this.font, Component.literal("Chain several with ;   e.g. /heal ; /feed"), left, top + 98, 0xFF808080);
        g.text(this.font, Component.literal("Extra words you type go on the end, or use {args}"), left, top + 110, 0xFF808080);
        g.text(this.font, error, left, top + 122, 0xFFFF5555);
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
