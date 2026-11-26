package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Name a profile and pick its picture. Used for New profile and Edit. */
public class ProfileEditScreen extends Screen {
    private final Screen parent;
    private final Profile editing; // null = new profile
    private EditBox nameBox;
    private String icon;
    private Component error = Component.empty();
    private int top;

    public ProfileEditScreen(Screen parent, Profile editing) {
        super(Component.literal(editing == null ? "New profile" : "Edit profile"));
        this.parent = parent;
        this.editing = editing;
        this.icon = editing != null ? editing.icon : Icons.IDS[0];
    }

    @Override
    protected void init() {
        String nameText = nameBox != null ? nameBox.getValue() : (editing != null ? editing.name : "");
        int left = this.width / 2 - 150;
        top = Math.max(20, (this.height - 236) / 2);

        nameBox = new EditBox(this.font, left, top + 28, 300, 20, Component.literal("Profile name"));
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.literal("e.g. Building, PvP, Redstone"));
        nameBox.setValue(nameText);
        this.addRenderableWidget(nameBox);
        this.setInitialFocus(nameBox);

        // Picture picker: 2 rows of 5 buttons (the pictures are drawn on top of them)
        for (int i = 0; i < Icons.IDS.length; i++) {
            final String id = Icons.IDS[i];
            int bx = left + (i % 5) * 61;
            int by = top + 72 + (i / 5) * 42;
            this.addRenderableWidget(Button.builder(Component.literal(""), b -> icon = id)
                    .bounds(bx, by, 56, 38).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left, top + 198, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left + 154, top + 198, 146, 20).build());
    }

    private void save() {
        String name = nameBox.getValue().strip();
        if (name.isEmpty()) {
            error = Component.literal("Give your profile a name.");
            return;
        }
        if (editing == null) {
            ProfileManager.create(name, icon);
        } else {
            editing.name = name;
            editing.icon = icon;
            ProfileManager.save();
        }
        Ui.open(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - 150;

        Look.box(g, left - 10, top - 14, left + 310, top + 226, Look.BORDER);
        g.centeredText(this.font, this.title, this.width / 2, top - 4, Look.GOLD);
        Look.line(g, left, top + 8, left + 300, Look.GOLD);

        g.text(this.font, Component.literal("Profile name"), left, top + 16, Look.GRAY);
        g.text(this.font, Component.literal("Pick a picture"), left, top + 60, Look.GRAY);

        for (int i = 0; i < Icons.IDS.length; i++) {
            String id = Icons.IDS[i];
            int bx = left + (i % 5) * 61;
            int by = top + 72 + (i / 5) * 42;
            Icons.draw(g, id, bx + 12, by + 3, 32); // 32x32 picture centred in the 56x38 button
            if (id.equals(icon)) {
                Look.box(g, bx - 1, by - 1, bx + 57, by + 39, Look.GOLD);
            }
        }

        g.text(this.font, Component.literal("Tip: name it after what you do, like Building or PvP."),
                left, top + 160, Look.DIM);
        g.text(this.font, Component.literal("Each profile keeps its own commands, hotkeys and automations."),
                left, top + 171, Look.DIM);
        g.text(this.font, error, left, top + 186, Look.RED);
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
