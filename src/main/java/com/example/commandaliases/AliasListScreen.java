package com.example.commandaliases;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Main menu: lists every alias with ON/OFF, Edit and Remove, plus "Add new command". */
public class AliasListScreen extends Screen {
    private record Row(int x, int y, Component text, int color) {
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int page = 0;
    private int pages = 1;
    private AliasEntry pendingRemove = null;

    public AliasListScreen(Screen parent) {
        super(Component.literal("Command Aliases"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();
        List<AliasEntry> list = AliasManager.all();

        int perPage = Math.max(1, (this.height - 36 - 64) / 24);
        pages = Math.max(1, (list.size() + perPage - 1) / perPage);
        page = Math.min(Math.max(page, 0), pages - 1);

        int left = this.width / 2 - 150;
        int start = page * perPage;
        int end = Math.min(list.size(), start + perPage);

        for (int i = start; i < end; i++) {
            final AliasEntry e = list.get(i);
            int y = 36 + (i - start) * 24;

            Component toggleLabel = e.enabled
                    ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                    : Component.literal("OFF").withStyle(ChatFormatting.RED);
            this.addRenderableWidget(Button.builder(toggleLabel, b -> {
                e.enabled = !e.enabled;
                AliasManager.save();
                this.rebuildWidgets();
            }).bounds(left, y, 40, 20).build());

            String out = e.command.startsWith("/") ? e.command : "/" + e.command;
            String label = fit("/" + e.name + "  ->  " + out, 148);
            rows.add(new Row(left + 44, y + 6, Component.literal(label), e.enabled ? 0xFFFFFFFF : 0xFF808080));

            this.addRenderableWidget(Button.builder(Component.literal("Edit"),
                    b -> Ui.open(new AliasEditScreen(this, e))).bounds(left + 198, y, 44, 20).build());

            boolean confirming = e == pendingRemove;
            this.addRenderableWidget(Button.builder(
                    Component.literal(confirming ? "Sure?" : "Remove"), b -> {
                        if (pendingRemove == e) {
                            AliasManager.all().remove(e);
                            AliasManager.save();
                            pendingRemove = null;
                        } else {
                            pendingRemove = e;
                        }
                        this.rebuildWidgets();
                    }).bounds(left + 246, y, 54, 20).build());
        }

        // Paging
        Button prev = Button.builder(Component.literal("<"), b -> {
            page--;
            pendingRemove = null;
            this.rebuildWidgets();
        }).bounds(this.width / 2 - 60, this.height - 52, 20, 20).build();
        prev.active = page > 0;
        this.addRenderableWidget(prev);

        Button next = Button.builder(Component.literal(">"), b -> {
            page++;
            pendingRemove = null;
            this.rebuildWidgets();
        }).bounds(this.width / 2 + 40, this.height - 52, 20, 20).build();
        next.active = page < pages - 1;
        this.addRenderableWidget(next);

        // Bottom buttons
        this.addRenderableWidget(Button.builder(Component.literal("Add new command"),
                b -> Ui.open(new AliasEditScreen(this, null))).bounds(left, this.height - 28, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"),
                b -> this.onClose()).bounds(left + 154, this.height - 28, 146, 20).build());
    }

    private String fit(String s, int maxWidth) {
        if (this.font.width(s) <= maxWidth) {
            return s;
        }
        String ellipsis = "...";
        while (!s.isEmpty() && this.font.width(s + ellipsis) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + ellipsis;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);

        for (Row r : rows) {
            g.text(this.font, r.text(), r.x(), r.y(), r.color());
        }
        if (AliasManager.all().isEmpty()) {
            g.centeredText(this.font, Component.literal("No commands yet - click \"Add new command\""),
                    this.width / 2, 60, 0xFFAAAAAA);
        }
        g.centeredText(this.font, Component.literal("Page " + (page + 1) + " / " + pages),
                this.width / 2, this.height - 46, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
