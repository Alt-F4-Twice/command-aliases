package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** First screen: pick which profile is on. Each profile has its own commands, hotkeys and automations. */
public class ProfileScreen extends Screen {
    private static final int ROW_TOP = 56;
    private static final int ROW_H = 30;
    private static final int SLOT_W = 190;

    private record Row(int x, int y, Profile profile) {
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int page = 0;
    private int pages = 1;
    private Profile pendingRemove = null;
    private int perPage = 1;
    private int pageStart = 0;
    private Profile dragProfile = null;   // the profile you are holding
    private boolean dragMoved = false;    // true once it has moved to another place
    private long lastEdgeMove = 0;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public ProfileScreen(Screen parent) {
        super(Component.literal("Profiles"));
        this.parent = parent;
    }

    /** Open = look inside and edit it. This does NOT turn the profile on. */
    private void open(Profile p) {
        MenuState.viewingId = p.id;
        MenuState.profileChosen = true;
        MenuState.tab = 0;
        MenuState.page = 0;
        Ui.open(new AliasListScreen(parent));
    }

    @Override
    protected void init() {
        rows.clear();
        int left = this.width / 2 - 170;
        List<Profile> all = ProfileManager.all();

        perPage = Math.max(1, (this.height - ROW_TOP - 64) / ROW_H);
        pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.min(Math.max(page, 0), pages - 1);
        int start = page * perPage;
        pageStart = start;
        int end = Math.min(all.size(), start + perPage);

        for (int i = start; i < end; i++) {
            final Profile p = all.get(i);
            int y = ROW_TOP + (i - start) * ROW_H;
            rows.add(new Row(left, y, p));

            this.addRenderableWidget(Button.builder(Component.literal("Open"), b -> open(p))
                    .bounds(left + 194, y + 3, 46, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Edit"),
                    b -> Ui.open(new ProfileEditScreen(this, p))).bounds(left + 244, y + 3, 40, 20).build());

            boolean confirming = p == pendingRemove;
            Button remove = Button.builder(Component.literal(confirming ? "Sure?" : "Remove"), b -> {
                if (pendingRemove == p) {
                    ProfileManager.delete(p);
                    pendingRemove = null;
                } else {
                    pendingRemove = p;
                }
                this.rebuildWidgets();
            }).bounds(left + 288, y + 3, 52, 20).build();
            remove.active = all.size() > 1; // you always keep at least one profile
            this.addRenderableWidget(remove);
        }

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

        this.addRenderableWidget(Button.builder(Component.literal("New profile"),
                b -> Ui.open(new ProfileEditScreen(this, null))).bounds(left, this.height - 28, 168, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"),
                b -> this.onClose()).bounds(left + 172, this.height - 28, 168, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        updateDrag(mouseY);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - 170;

        g.centeredText(this.font, Component.literal("Choose a profile"), this.width / 2, 10, Look.GOLD);
        Look.line(g, left, 22, left + 340, Look.GOLD);
        g.centeredText(this.font, Component.literal("Click a profile to turn it on. Only one is on at a time."),
                this.width / 2, 28, Look.GRAY);
        g.centeredText(this.font, Component.literal("Drag to reorder. Open edits what is inside (it can stay off)."),
                this.width / 2, 39, Look.GRAY);

        Look.box(g, left - 8, 50, left + 348, this.height - 56, Look.BORDER);

        Profile active = ProfileManager.active();
        for (Row r : rows) {
            Profile p = r.profile();
            boolean on = p == active;

            boolean moving = p == dragProfile && dragMoved;
            g.fill(r.x(), r.y(), r.x() + SLOT_W, r.y() + 26, (on || moving) ? 0xAA5A3A00 : 0x66000000);
            if (moving) {
                Look.box(g, r.x(), r.y(), r.x() + SLOT_W, r.y() + 26, Look.GOLD);
            } else if (on) {
                Look.box(g, r.x(), r.y(), r.x() + SLOT_W, r.y() + 26, Look.GOLD);
            } else if (dragProfile == null && profileAt(mouseX, mouseY) == p) {
                g.fill(r.x(), r.y(), r.x() + SLOT_W, r.y() + 26, 0x33FFFFFF); // hover: "click to turn on"
                Look.box(g, r.x(), r.y(), r.x() + SLOT_W, r.y() + 26, Look.GRAY);
            }
            Icons.draw(g, p.icon, r.x() + 5, r.y() + 5, 16);

            int tx = r.x() + 28;
            g.text(this.font, Component.literal(Look.fit(this.font, p.name, on ? 120 : 154)),
                    tx, r.y() + 4, on ? Look.GOLD : Look.WHITE);
            if (on) {
                String tag = "ON";
                g.text(this.font, Component.literal(tag), r.x() + SLOT_W - 6 - this.font.width(tag),
                        r.y() + 4, Look.GREEN);
            }
            String info = p.aliases.size() + " cmds | " + p.hotkeys.size() + " keys | " + p.automations.size() + " auto";
            g.text(this.font, Component.literal(Look.fit(this.font, info, 156)), tx, r.y() + 15, Look.GRAY);
        }
        g.centeredText(this.font, Component.literal("Page " + (page + 1) + " / " + pages),
                this.width / 2, this.height - 46, Look.GRAY);
        String hint = "Drag to reorder";
        g.text(this.font, Component.literal(hint), left + 340 - this.font.width(hint),
                this.height - 46, Look.DIM);
    }

    /** The profile whose box is under this point, or null. */
    private Profile profileAt(int mx, int my) {
        for (Row r : rows) {
            if (mx >= r.x() && mx < r.x() + SLOT_W && my >= r.y() && my < r.y() + 26) {
                return r.profile();
            }
        }
        return null;
    }

    /**
     * Press on a profile's box. If you let go without moving, it turns on (and the others turn off).
     * If you drag it up or down first, it changes place in the list instead.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        dragProfile = null; // clear anything left over
        Profile hit = profileAt(lastMouseX, lastMouseY);
        if (hit != null) {
            dragProfile = hit;
            dragMoved = false;
            lastEdgeMove = 0;
            pendingRemove = null;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragProfile != null) {
            if (dragMoved) {
                ProfileManager.save();              // keep the new order
            } else {
                ProfileManager.setActive(dragProfile); // plain click: turn it on
            }
            dragProfile = null;
            dragMoved = false;
            this.rebuildWidgets();
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Moves the held profile to follow the mouse. Holding above/below the list steps it along and flips pages. */
    private void updateDrag(int my) {
        if (dragProfile == null) {
            return;
        }
        List<Profile> all = ProfileManager.all();
        int idx = all.indexOf(dragProfile);
        if (idx < 0) {
            dragProfile = null;
            return;
        }
        int target = idx;
        long now = System.currentTimeMillis();
        if (my < ROW_TOP) {
            if (now - lastEdgeMove > 250) {
                target = idx - 1;
                lastEdgeMove = now;
            }
        } else if (my >= ROW_TOP + perPage * ROW_H) {
            if (now - lastEdgeMove > 250) {
                target = idx + 1;
                lastEdgeMove = now;
            }
        } else {
            target = pageStart + (my - ROW_TOP) / ROW_H;
        }
        target = Math.max(0, Math.min(all.size() - 1, target));
        if (target != idx) {
            all.remove(idx);
            all.add(target, dragProfile);
            dragMoved = true;
            page = target / perPage;
            this.rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        dragProfile = null;
        Ui.open(parent);
    }
}
