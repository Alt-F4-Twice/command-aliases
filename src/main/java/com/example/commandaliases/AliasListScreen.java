package com.example.commandaliases;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Main menu with two tabs: Quick Commands and Hotkeys. */
public class AliasListScreen extends Screen {
    private static final int ROW_TOP = 40;

    /** One list row's text: trigger (gold/aqua) -> command (white). */
    private record Row(int x, int y, String trigger, String command, boolean enabled, int triggerColor) {
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int tab = 0; // 0 = Quick Commands, 1 = Hotkeys
    private int page = 0;
    private int pages = 1;
    private Object pendingRemove = null;

    public AliasListScreen(Screen parent) {
        super(Component.literal("Command Aliases"));
        this.parent = parent;
    }

    private void switchTab(int newTab) {
        tab = newTab;
        page = 0;
        pendingRemove = null;
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        rows.clear();
        int left = this.width / 2 - 150;

        // Tabs (the current one is greyed out)
        Button quick = Button.builder(Component.literal("Quick Commands (" + AliasManager.all().size() + ")"),
                b -> switchTab(0)).bounds(left, 8, 148, 20).build();
        quick.active = tab != 0;
        this.addRenderableWidget(quick);
        Button hot = Button.builder(Component.literal("Hotkeys (" + HotkeyManager.all().size() + ")"),
                b -> switchTab(1)).bounds(left + 152, 8, 148, 20).build();
        hot.active = tab != 1;
        this.addRenderableWidget(hot);

        int count = tab == 0 ? AliasManager.all().size() : HotkeyManager.all().size();
        int perPage = Math.max(1, (this.height - ROW_TOP - 64) / 24);
        pages = Math.max(1, (count + perPage - 1) / perPage);
        page = Math.min(Math.max(page, 0), pages - 1);

        int start = page * perPage;
        int end = Math.min(count, start + perPage);

        for (int i = start; i < end; i++) {
            int y = ROW_TOP + (i - start) * 24;
            if (tab == 0) {
                final AliasEntry e = AliasManager.all().get(i);
                String out = e.command.startsWith("/") ? e.command : "/" + e.command;
                addRow(left, y, e.enabled, e.name, out, Look.GOLD,
                        () -> {
                            e.enabled = !e.enabled;
                            AliasManager.save();
                        },
                        () -> Ui.open(new AliasEditScreen(this, e)),
                        e,
                        () -> {
                            AliasManager.all().remove(e);
                            AliasManager.save();
                        });
            } else {
                final HotkeyEntry h = HotkeyManager.all().get(i);
                String out = h.command.startsWith("/") || h.command.startsWith("!") || h.command.startsWith("?")
                        ? h.command : "/" + h.command;
                addRow(left, y, h.enabled, KeyNames.combo(h.mods, h.key, h.keyName), out, Look.AQUA,
                        () -> {
                            h.enabled = !h.enabled;
                            HotkeyManager.save();
                        },
                        () -> Ui.open(new HotkeyEditScreen(this, h)),
                        h,
                        () -> {
                            HotkeyManager.all().remove(h);
                            HotkeyManager.save();
                        });
            }
        }

        // Re-read the files (useful if you edited the .json by hand)
        this.addRenderableWidget(Button.builder(Component.literal("Reload"), b -> {
            AliasManager.load();
            HotkeyManager.load();
            pendingRemove = null;
            this.rebuildWidgets();
        }).bounds(left, this.height - 52, 60, 20).build());

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
        this.addRenderableWidget(Button.builder(
                Component.literal(tab == 0 ? "Add new command" : "Add new hotkey"),
                b -> Ui.open(tab == 0 ? new AliasEditScreen(this, null) : new HotkeyEditScreen(this, null)))
                .bounds(left, this.height - 28, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"),
                b -> this.onClose()).bounds(left + 154, this.height - 28, 146, 20).build());
    }

    private void addRow(int left, int y, boolean enabled, String trigger, String command, int triggerColor,
                        Runnable toggle, Runnable edit, Object token, Runnable delete) {
        Component toggleLabel = enabled
                ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                : Component.literal("OFF").withStyle(ChatFormatting.RED);
        this.addRenderableWidget(Button.builder(toggleLabel, b -> {
            toggle.run();
            this.rebuildWidgets();
        }).bounds(left, y, 40, 20).build());

        rows.add(new Row(left + 44, y, trigger, command, enabled, triggerColor));

        this.addRenderableWidget(Button.builder(Component.literal("Edit"), b -> edit.run())
                .bounds(left + 198, y, 44, 20).build());

        boolean confirming = token == pendingRemove;
        this.addRenderableWidget(Button.builder(
                Component.literal(confirming ? "Sure?" : "Remove"), b -> {
                    if (pendingRemove == token) {
                        delete.run();
                        pendingRemove = null;
                    } else {
                        pendingRemove = token;
                    }
                    this.rebuildWidgets();
                }).bounds(left + 246, y, 54, 20).build());
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
        int left = this.width / 2 - 150;

        // Frame around the list + accent line under the tabs
        Look.box(g, left - 8, 33, left + 308, this.height - 56, Look.BORDER);
        Look.line(g, left, 31, left + 300, Look.GOLD);

        for (Row r : rows) {
            // dark inset behind the text, like a slot
            g.fill(r.x(), r.y(), r.x() + 150, r.y() + 20, 0x66000000);

            int tx = r.x() + 5;
            int ty = r.y() + 6;
            String trig = fit(r.trigger(), 84);
            g.text(this.font, Component.literal(trig), tx, ty, r.enabled() ? r.triggerColor() : Look.DIM);

            int ax = tx + this.font.width(trig) + 4;
            g.text(this.font, Component.literal("->"), ax, ty, Look.DIM);

            int cx = ax + this.font.width("->") + 4;
            int room = r.x() + 150 - 4 - cx;
            if (room > 8) {
                g.text(this.font, Component.literal(fit(r.command(), room)), cx, ty,
                        r.enabled() ? Look.WHITE : Look.DIM);
            }
        }

        boolean empty = tab == 0 ? AliasManager.all().isEmpty() : HotkeyManager.all().isEmpty();
        if (empty) {
            String msg = tab == 0
                    ? "No quick commands yet - click \"Add new command\""
                    : "No hotkeys yet - click \"Add new hotkey\"";
            g.centeredText(this.font, Component.literal(msg), this.width / 2, 64, Look.GRAY);
        }
        g.centeredText(this.font, Component.literal("Page " + (page + 1) + " / " + pages),
                this.width / 2, this.height - 46, Look.GRAY);
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
