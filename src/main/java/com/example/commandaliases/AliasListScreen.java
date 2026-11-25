package com.example.commandaliases;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Main menu with two tabs: Quick Commands and Hotkeys.
 * Press and hold on a row, then drag it up or down to change the order.
 */
public class AliasListScreen extends Screen {
    private static final int ROW_TOP = 40;
    private static final int ROW_H = 24;
    private static final int PANEL_W = 340;
    private static final int SLOT_W = 190;

    /** One list row's text: number, trigger (gold/aqua) -> command (white). */
    private record Row(int index, int x, int y, String trigger, String command, boolean enabled, int triggerColor) {
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int tab = 0; // 0 = Quick Commands, 1 = Hotkeys
    private int page = 0;
    private int pages = 1;
    private int perPage = 1;
    private int pageStart = 0;
    private int rowsOnPage = 0;
    private Object pendingRemove = null;

    // dragging
    private int dragIndex = -1;
    private long lastEdgeMove = 0;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public AliasListScreen(Screen parent) {
        super(Component.literal("Command Aliases"));
        this.parent = parent;
    }

    private void switchTab(int newTab) {
        tab = newTab;
        page = 0;
        pendingRemove = null;
        dragIndex = -1;
        this.rebuildWidgets();
    }

    private int currentSize() {
        return tab == 0 ? AliasManager.all().size() : HotkeyManager.all().size();
    }

    private static <T> void move(List<T> list, int from, int to) {
        T item = list.remove(from);
        list.add(to, item);
    }

    private void moveCurrent(int from, int to) {
        if (tab == 0) {
            move(AliasManager.all(), from, to);
        } else {
            move(HotkeyManager.all(), from, to);
        }
    }

    private void saveCurrent() {
        if (tab == 0) {
            AliasManager.save();
        } else {
            HotkeyManager.save();
        }
    }

    @Override
    protected void init() {
        rows.clear();
        int left = this.width / 2 - PANEL_W / 2;

        // Tabs (the current one is greyed out)
        Button quick = Button.builder(Component.literal("Quick Commands (" + AliasManager.all().size() + ")"),
                b -> switchTab(0)).bounds(left, 8, 168, 20).build();
        quick.active = tab != 0;
        this.addRenderableWidget(quick);
        Button hot = Button.builder(Component.literal("Hotkeys (" + HotkeyManager.all().size() + ")"),
                b -> switchTab(1)).bounds(left + 172, 8, 168, 20).build();
        hot.active = tab != 1;
        this.addRenderableWidget(hot);

        int count = currentSize();
        perPage = Math.max(1, (this.height - ROW_TOP - 64) / ROW_H);
        pages = Math.max(1, (count + perPage - 1) / perPage);
        page = Math.min(Math.max(page, 0), pages - 1);

        pageStart = page * perPage;
        int end = Math.min(count, pageStart + perPage);
        rowsOnPage = end - pageStart;

        for (int i = pageStart; i < end; i++) {
            int y = ROW_TOP + (i - pageStart) * ROW_H;
            if (tab == 0) {
                final AliasEntry e = AliasManager.all().get(i);
                String out = e.command.startsWith("/") ? e.command : "/" + e.command;
                addRow(left, y, i, e.enabled, e.name, out, Look.GOLD,
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
                addRow(left, y, i, h.enabled, KeyNames.combo(h.mods, h.key, h.keyName), out, Look.AQUA,
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
            dragIndex = -1;
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
                .bounds(left, this.height - 28, 168, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"),
                b -> this.onClose()).bounds(left + 172, this.height - 28, 168, 20).build());
    }

    private void addRow(int left, int y, int index, boolean enabled, String trigger, String command,
                        int triggerColor, Runnable toggle, Runnable edit, Object token, Runnable delete) {
        Component toggleLabel = enabled
                ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                : Component.literal("OFF").withStyle(ChatFormatting.RED);
        this.addRenderableWidget(Button.builder(toggleLabel, b -> {
            toggle.run();
            this.rebuildWidgets();
        }).bounds(left, y, 40, 20).build());

        rows.add(new Row(index, left + 44, y, trigger, command, enabled, triggerColor));

        this.addRenderableWidget(Button.builder(Component.literal("Edit"), b -> edit.run())
                .bounds(left + 238, y, 44, 20).build());

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
                }).bounds(left + 286, y, 54, 20).build());
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

    // ---------- drag to reorder ----------

    /** Which row (index in the list) is under this point, or -1. Only the dark text slot counts. */
    private int rowAt(int mx, int my) {
        int left = this.width / 2 - PANEL_W / 2;
        int slotX = left + 44;
        if (mx < slotX || mx >= slotX + SLOT_W) {
            return -1;
        }
        if (my < ROW_TOP || my >= ROW_TOP + rowsOnPage * ROW_H) {
            return -1;
        }
        int rel = my - ROW_TOP;
        if (rel % ROW_H >= 20) {
            return -1;
        }
        return pageStart + rel / ROW_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        dragIndex = -1; // clear anything left over
        int idx = rowAt(lastMouseX, lastMouseY);
        if (idx >= 0) {
            dragIndex = idx;
            pendingRemove = null;
            lastEdgeMove = 0;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragIndex >= 0) {
            dragIndex = -1;
            saveCurrent();
            this.rebuildWidgets();
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Moves the dragged row to follow the mouse. Holding above/below the list steps it along and flips pages. */
    private void updateDrag(int mx, int my) {
        if (dragIndex < 0) {
            return;
        }
        int size = currentSize();
        if (dragIndex >= size) {
            dragIndex = -1;
            return;
        }
        int target = dragIndex;
        long now = System.currentTimeMillis();
        if (my < ROW_TOP) {
            if (now - lastEdgeMove > 250) {
                target = dragIndex - 1;
                lastEdgeMove = now;
            }
        } else if (my >= ROW_TOP + perPage * ROW_H) {
            if (now - lastEdgeMove > 250) {
                target = dragIndex + 1;
                lastEdgeMove = now;
            }
        } else {
            target = pageStart + (my - ROW_TOP) / ROW_H;
        }
        target = Math.max(0, Math.min(size - 1, target));
        if (target != dragIndex) {
            moveCurrent(dragIndex, target);
            dragIndex = target;
            page = dragIndex / perPage;
            this.rebuildWidgets();
        }
    }

    // ---------- drawing ----------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        updateDrag(mouseX, mouseY);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - PANEL_W / 2;

        // Frame around the list + accent line under the tabs
        Look.box(g, left - 8, 33, left + PANEL_W + 8, this.height - 56, Look.BORDER);
        Look.line(g, left, 31, left + PANEL_W, Look.GOLD);

        for (Row r : rows) {
            boolean dragged = r.index() == dragIndex;

            // dark inset behind the text, like a slot (gold when you are dragging it)
            g.fill(r.x(), r.y(), r.x() + SLOT_W, r.y() + 20, dragged ? 0xAA5A3A00 : 0x66000000);
            if (dragged) {
                Look.box(g, r.x(), r.y(), r.x() + SLOT_W, r.y() + 20, Look.GOLD);
            }

            // grip (three little lines) = "you can drag this"
            int gx = r.x() + 4;
            for (int k = 0; k < 3; k++) {
                g.fill(gx, r.y() + 6 + k * 3, gx + 6, r.y() + 7 + k * 3, dragged ? Look.GOLD : Look.DIM);
            }

            int tx = r.x() + 14;
            int ty = r.y() + 6;
            String num = "#" + (r.index() + 1);
            g.text(this.font, Component.literal(num), tx, ty, Look.DIM);
            tx += this.font.width(num) + 5;

            String trig = fit(r.trigger(), 80);
            g.text(this.font, Component.literal(trig), tx, ty, r.enabled() ? r.triggerColor() : Look.DIM);

            int ax = tx + this.font.width(trig) + 4;
            g.text(this.font, Component.literal("->"), ax, ty, Look.DIM);

            int cx = ax + this.font.width("->") + 4;
            int room = r.x() + SLOT_W - 4 - cx;
            if (room > 8) {
                g.text(this.font, Component.literal(fit(r.command(), room)), cx, ty,
                        r.enabled() ? Look.WHITE : Look.DIM);
            }
        }

        boolean empty = currentSize() == 0;
        if (empty) {
            String msg = tab == 0
                    ? "No quick commands yet - click \"Add new command\""
                    : "No hotkeys yet - click \"Add new hotkey\"";
            g.centeredText(this.font, Component.literal(msg), this.width / 2, 64, Look.GRAY);
        }
        g.centeredText(this.font, Component.literal("Page " + (page + 1) + " / " + pages),
                this.width / 2, this.height - 46, Look.GRAY);
        String hint = "Drag to reorder";
        g.text(this.font, Component.literal(hint), left + PANEL_W - this.font.width(hint),
                this.height - 46, Look.DIM);
    }

    @Override
    public void onClose() {
        dragIndex = -1;
        Ui.open(parent);
    }
}
