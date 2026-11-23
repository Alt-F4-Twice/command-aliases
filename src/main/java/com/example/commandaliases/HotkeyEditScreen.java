package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Pick a key (click the button, then press it) and the command it should run. */
public class HotkeyEditScreen extends Screen {
    private final Screen parent;
    private final HotkeyEntry editing; // null = adding a new one
    private int key;
    private boolean capturing = false;
    private Button keyButton;
    private EditBox commandBox;
    private Component error = Component.empty();
    private int top;

    public HotkeyEditScreen(Screen parent, HotkeyEntry editing) {
        super(Component.literal(editing == null ? "Add new hotkey" : "Edit hotkey"));
        this.parent = parent;
        this.editing = editing;
        this.key = editing != null ? editing.key : -1;
    }

    private Component keyLabel() {
        return Component.literal(capturing ? "> Press a key (Esc to cancel) <" : "Key: " + KeyNames.name(key));
    }

    @Override
    protected void init() {
        String cmdText = commandBox != null ? commandBox.getValue() : (editing != null ? editing.command : "");

        int left = this.width / 2 - 150;
        top = Math.max(24, this.height / 2 - 80);

        keyButton = Button.builder(keyLabel(), b -> {
            capturing = true;
            this.setFocused(null); // so the pressed key isn't typed into the command box
            b.setMessage(keyLabel());
        }).bounds(left, top + 24, 300, 20).build();
        this.addRenderableWidget(keyButton);

        commandBox = new EditBox(this.font, left, top + 70, 300, 20, Component.literal("Command to run"));
        commandBox.setMaxLength(256);
        commandBox.setHint(Component.literal("e.g. /s   or   /gamemode creative"));
        commandBox.setValue(cmdText);
        this.addRenderableWidget(commandBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, top + 132, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 154, top + 132, 146, 20).build());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturing) {
            capturing = false;
            if (event.key() != KeyNames.ESCAPE) {
                key = event.key();
            }
            keyButton.setMessage(keyLabel());
            return true;
        }
        return super.keyPressed(event);
    }

    private void save() {
        String cmd = commandBox.getValue().strip();

        if (key < 0) {
            error = Component.literal("Click the key button and press a key first.");
            return;
        }
        if (cmd.replace("/", "").replace(";", "").isBlank()) {
            error = Component.literal("The command can't be empty.");
            return;
        }
        for (HotkeyEntry other : HotkeyManager.all()) {
            if (other != editing && other.key == key) {
                error = Component.literal(KeyNames.name(key) + " is already used by another hotkey.");
                return;
            }
        }

        if (editing == null) {
            HotkeyManager.all().add(new HotkeyEntry(key, cmd, true));
        } else {
            editing.key = key;
            editing.command = cmd;
        }
        HotkeyManager.save();
        Ui.open(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - 150;

        g.centeredText(this.font, this.title, this.width / 2, top - 4, 0xFFFFFFFF);
        g.text(this.font, Component.literal("Key (click the button, then press the key)"), left, top + 12, 0xFFAAAAAA);
        g.text(this.font, Component.literal("Command to run when you press it"), left, top + 58, 0xFFAAAAAA);
        g.text(this.font, Component.literal("Quick commands work here too. Chain with ;"), left, top + 98, 0xFF808080);
        g.text(this.font, Component.literal("Pick a key Minecraft isn't already using."), left, top + 110, 0xFF808080);
        g.text(this.font, error, left, top + 122, 0xFFFF5555);
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
