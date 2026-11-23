package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Pick a key (click the button, then press it - hold Ctrl/Shift/Alt for combos) and a command. */
public class HotkeyEditScreen extends Screen {
    private final Screen parent;
    private final HotkeyEntry editing; // null = adding a new one
    private int key;
    private String keyName;
    private int mods;
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
        this.keyName = editing != null ? editing.keyName : null;
        this.mods = editing != null ? editing.mods : 0;
    }

    private Component keyLabel() {
        return Component.literal(capturing
                ? "> Press a key (Esc to cancel) <"
                : "Key: " + KeyNames.combo(mods, key, keyName));
    }

    @Override
    protected void init() {
        String cmdText = commandBox != null ? commandBox.getValue() : (editing != null ? editing.command : "");

        int left = this.width / 2 - 150;
        top = Math.max(24, this.height / 2 - 85);

        keyButton = Button.builder(keyLabel(), b -> {
            capturing = true;
            this.setFocused(null); // so the pressed key isn't typed into the command box
            b.setMessage(keyLabel());
        }).bounds(left, top + 28, 300, 20).build();
        this.addRenderableWidget(keyButton);

        commandBox = new EditBox(this.font, left, top + 72, 300, 20, Component.literal("Command to run"));
        commandBox.setMaxLength(256);
        commandBox.setHint(Component.literal("e.g. /s   or   /gamemode creative"));
        commandBox.setValue(cmdText);
        this.addRenderableWidget(commandBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, top + 158, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 154, top + 158, 146, 20).build());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturing) {
            int k = event.key(); // the physical key
            if (k == KeyNames.ESCAPE) {
                capturing = false;
            } else if (KeyNames.isModifier(k)) {
                return true; // hold Ctrl/Shift/Alt first, then press the real key
            } else {
                capturing = false;
                key = k;
                keyName = KeyNames.fromEvent(k, event.keycode()); // what's printed on the key
                mods = HotkeyManager.currentMods();               // Shift=1, Ctrl=2, Alt=4
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
            if (other != editing && other.key == key && other.mods == mods) {
                error = Component.literal(KeyNames.combo(mods, key, keyName) + " is already used by another hotkey.");
                return;
            }
        }

        if (editing == null) {
            HotkeyManager.all().add(new HotkeyEntry(key, keyName, mods, cmd, true));
        } else {
            editing.key = key;
            editing.keyName = keyName;
            editing.mods = mods;
            editing.command = cmd;
        }
        HotkeyManager.save();
        Ui.open(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - 150;

        Look.box(g, left - 10, top - 14, left + 310, top + 188, Look.BORDER);
        g.centeredText(this.font, this.title, this.width / 2, top - 4, Look.GOLD);
        Look.line(g, left, top + 8, left + 300, Look.GOLD);

        g.text(this.font, Component.literal("Key (click the button, then press it)"), left, top + 16, Look.GRAY);
        g.text(this.font, Component.literal("Command to run when you press it"), left, top + 60, Look.GRAY);

        g.text(this.font, Component.literal("Quick commands work here too:  /s   !s   ?s"), left, top + 98, Look.GOLD);
        g.text(this.font, Component.literal("Chain several commands with ;"), left, top + 109, Look.DIM);
        g.text(this.font, Component.literal("Tip: hold Ctrl / Shift / Alt while pressing a key"), left, top + 120, Look.DIM);
        g.text(this.font, Component.literal("for combos like Ctrl+H, so it never clashes."), left, top + 131, Look.DIM);
        g.text(this.font, error, left, top + 144, Look.RED);
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
