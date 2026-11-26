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
 * The menu for the profile that is on, with three tabs: Commands, Hotkeys and Automations.
 * Press and hold on a row, then drag it up or down to change the order.
 * It remembers the tab and page until you leave the world.
 */
public class AliasListScreen extends Screen {
    private static final int ROW_TOP = 62;
    private static final int ROW_H = 24;
    private static final int PANEL_W = 340;
    private static final int SLOT_W = 190;

    /** One list row's text: number, trigger (gold/aqua/green) -> what it runs (white). */
    private record Row(int index, int x, int y, String trigger, String command, boolean enabled, int triggerColor) {
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int tab = MenuState.tab; // 0 = Commands, 1 = Hotkeys, 2 = Automations
    private int page = MenuState.page;
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
        Profile p = ProfileManager.active();
        return tab == 0 ? p.aliases.size() : tab == 1 ? p.hotkeys.size() : p.automations.size();
    }

    private static <T> void move(List<T> list, int from, int to) {
        T item = list.remove(from);
        list.add(to, item);
    }

    private void moveCurrent(int from, int to) {
        Profile p = ProfileManager.active();
        if (tab == 0) {
            move(p.aliases, from, to);
        } else if (tab == 1) {
            move(p.hotkeys, from, to);
        } else {
            move(p.automations, from, to);
        }
    }

    @Override
    protected void init() {
        rows.clear();
        Profile profile = ProfileManager.active();
        int left = this.width / 2 - PANEL_W / 2;

        // Tabs (the current one is greyed out)
        Button t0 = Button.builder(Component.literal("Commands (" + profile.aliases.size() + ")"),
                b -> switchTab(0)).bounds(left, 8, 110, 20).build();
        t0.active = tab != 0;
        this.addRenderableWidget(t0);
        Button t1 = Button.builder(Component.literal("Hotkeys (" + profile.hotkeys.size() + ")"),
                b -> switchTab(1)).bounds(left + 115, 8, 110, 20).build();
        t1.active = tab != 1;
        this.addRenderableWidget(t1);
        Button t2 = Button.builder(Component.literal("Automations (" + profile.automations.size() + ")"),
                b -> switchTab(2)).bounds(left + 230, 8, 110, 20).build();
        t2.active = tab != 2;
        this.addRenderableWidget(t2);

        int count = currentSize();
        perPage = Math.max(1, (this.height - ROW_TOP - 64) / ROW_H);
        pages = Math.max(1, (count + perPage - 1) / perPage);
        page = Math.min(Math.max(page, 0), pages - 1);
        MenuState.tab = tab;   // remembered until you leave the world
        MenuState.page = page;

        pageStart = page * perPage;
        int end = Math.min(count, pageStart + perPage);
        rowsOnPage = end - pageStart;

        for (int i = pageStart; i < end; i++) {
            int y = ROW_TOP + (i - pageStart) * ROW_H;
            if (tab == 0) {
                final AliasEntry e = profile.aliases.get(i);
                addRow(left, y, i, e.enabled, e.name, summaryOf(e.command), Look.GOLD,
                        () -> {
                            e.enabled = !e.enabled;
                            ProfileManager.save();
                        },
                        () -> Ui.open(new AliasEditScreen(this, e)),
                        e,
                        () -> {
                            profile.aliases.remove(e);
                            ProfileManager.save();
                        });
            } else if (tab == 1) {
                final HotkeyEntry h = profile.hotkeys.get(i);
                addRow(left, y, i, h.enabled, KeyNames.combo(h.mods, h.key, h.keyName), summaryOf(h.command),
                        Look.AQUA,
                        () -> {
                            h.enabled = !h.enabled;
                            ProfileManager.save();
                        },
                        () -> Ui.open(new HotkeyEditScreen(this, h)),
                        h,
                        () -> {
                            profile.hotkeys.remove(h);
                            ProfileManager.save();
                        });
            } else {
                final AutomationEntry a = profile.automations.get(i);
                addRow(left, y, i, a.enabled, AutomationEntry.label(a.trigger), summaryOf(a.command),
                        Look.GREEN,
                        () -> {
                            a.enabled = !a.enabled;
                            ProfileManager.save();
                        },
                        () -> Ui.open(new AutomationEditScreen(this, a)),
                        a,
                        () -> {
                            profile.automations.remove(a);
                            ProfileManager.save();
                        });
            }
        }

        // Re-read the file (useful if you edited the .json by hand)
        this.addRenderableWidget(Button.builder(Component.literal("Reload"), b -> {
            ProfileManager.load();
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
        String addLabel = tab == 0 ? "Add new command" : tab == 1 ? "Add new hotkey" : "Add new automation";
        this.addRenderableWidget(Button.builder(Component.literal(addLabel), b -> {
            if (tab == 0) {
                Ui.open(new AliasEditScreen(this, null));
            } else if (tab == 1) {
                Ui.open(new HotkeyEditScreen(this, null));
            } else {
                Ui.open(new AutomationEditScreen(this, null));
            }
        }).bounds(left, this.height - 28, 140, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            // back to the profile list (the menu forgets which profile you opened)
            MenuState.reset();
            Ui.open(new ProfileScreen(parent));
        }).bounds(left + 144, this.height - 28, 96, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"),
                b -> this.onClose()).bounds(left + 244, this.height - 28, 96, 20).build());
    }

    private static String summaryOf(String script) {
        String s = Script.summary(script);
        return s.isEmpty() ? script : s;
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
            ProfileManager.save();
            this.rebuildWidgets();
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Moves the dragged row to follow the mouse. Holding above/below the list steps it along and flips pages. */
    private void updateDrag(int my) {
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
        updateDrag(mouseY);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - PANEL_W / 2;
        Profile profile = ProfileManager.active();

        // Accent line under the tabs, which profile is on, and what this tab is for
        Look.line(g, left, 31, left + PANEL_W, Look.GOLD);
        Icons.draw(g, profile.icon, left, 36, 1);
        g.text(this.font, Component.literal("Profile: " + Look.fit(this.font, profile.name, 200)),
                left + 12, 36, Look.GOLD);
        String about = tab == 0
                ? "Type a short command (like /s) and it runs a longer one for you."
                : tab == 1
                ? "Press a key in game to run a command."
                : "Runs commands for you automatically when something happens.";
        g.text(this.font, Component.literal(about), left, 48, Look.GRAY);

        // Frame around the list
        Look.box(g, left - 8, 58, left + PANEL_W + 8, this.height - 56, Look.BORDER);

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

            String trig = Look.fit(this.font, r.trigger(), 80);
            g.text(this.font, Component.literal(trig), tx, ty, r.enabled() ? r.triggerColor() : Look.DIM);

            int ax = tx + this.font.width(trig) + 4;
            g.text(this.font, Component.literal("->"), ax, ty, Look.DIM);

            int cx = ax + this.font.width("->") + 4;
            int room = r.x() + SLOT_W - 4 - cx;
            if (room > 8) {
                g.text(this.font, Component.literal(Look.fit(this.font, r.command(), room)), cx, ty,
                        r.enabled() ? Look.WHITE : Look.DIM);
            }
        }

        if (currentSize() == 0) {
            String msg = tab == 0
                    ? "No commands yet - click \"Add new command\""
                    : tab == 1
                    ? "No hotkeys yet - click \"Add new hotkey\""
                    : "No automations yet - click \"Add new automation\"";
            g.centeredText(this.font, Component.literal(msg), this.width / 2, 86, Look.GRAY);
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
