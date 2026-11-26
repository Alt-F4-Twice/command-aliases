package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Pick a key (click the button, then press it - hold Ctrl/Shift/Alt for combos) and what it runs. */
public class HotkeyEditScreen extends Screen {
    private final Screen parent;
    private final HotkeyEntry editing; // null = adding a new one
    private final StepEditor editor;
    private int key;
    private String keyName;
    private int mods;
    private boolean capturing = false;
    private Button keyButton;
    private Component error = Component.empty();
    private int top;

    public HotkeyEditScreen(Screen parent, HotkeyEntry editing) {
        super(Component.literal(editing == null ? "Add new hotkey" : "Edit hotkey"));
        this.parent = parent;
        this.editing = editing;
        this.key = editing != null ? editing.key : -1;
        this.keyName = editing != null ? editing.keyName : null;
        this.mods = editing != null ? editing.mods : 0;
        this.editor = new StepEditor(editing != null ? editing.command : "", this::rebuildWidgets);
    }

    private Component keyLabel() {
        return Component.literal(capturing
                ? "> Press a key (Esc to cancel) <"
                : "Key: " + KeyNames.combo(mods, key, keyName));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 150;
        top = Math.max(20, (this.height - (90 + editor.height() + 88)) / 2);

        keyButton = Button.builder(keyLabel(), b -> {
            capturing = true;
            this.setFocused(null); // so the pressed key isn't typed into a command box
            b.setMessage(keyLabel());
        }).bounds(left, top + 28, 300, 20).build();
        this.addRenderableWidget(keyButton);

        editor.build(this.font, left, top + 90, w -> this.addRenderableWidget(w));
        int by = editor.bottom();

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, by + 62, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 154, by + 62, 146, 20).build());
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
        String script = editor.script();

        if (key < 0) {
            error = Component.literal("Click the key button and press a key first.");
            return;
        }
        if (!Script.hasCommand(script)) {
            error = Component.literal("Add at least one command for it to run.");
            return;
        }
        for (HotkeyEntry other : MenuState.viewed().hotkeys) {
            if (other != editing && other.key == key && other.mods == mods) {
                error = Component.literal(KeyNames.combo(mods, key, keyName) + " is already used by another hotkey.");
                return;
            }
        }

        if (editing == null) {
            MenuState.viewed().hotkeys.add(new HotkeyEntry(key, keyName, mods, script, true));
        } else {
            editing.key = key;
            editing.keyName = keyName;
            editing.mods = mods;
            editing.command = script;
        }
        HotkeyManager.save();
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

        g.text(this.font, Component.literal("Key (click the button, then press it)"), left, top + 16, Look.GRAY);
        g.text(this.font, Component.literal("Hold Ctrl / Shift / Alt while pressing a key"), left, top + 52, Look.DIM);
        g.text(this.font, Component.literal("to make a combo like Ctrl+H (so it never clashes)."), left, top + 63, Look.DIM);
        g.text(this.font, Component.literal("Command to run when you press it"), left, top + 78, Look.GRAY);

        editor.render(g, this.font);
        g.text(this.font, Component.literal("Quick commands work here too:  /s   !s   ?s"), left, by + 11, Look.GOLD);

        String pressed = key >= 0 ? KeyNames.combo(mods, key, keyName) : "the key";
        g.text(this.font, Component.literal("This is what runs when you press " + pressed + ":"),
                left, by + 24, Look.GRAY);
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
