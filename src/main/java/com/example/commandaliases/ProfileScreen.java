package com.example.commandaliases;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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

    public ProfileScreen(Screen parent) {
        super(Component.literal("Profiles"));
        this.parent = parent;
    }

    private void open(Profile p) {
        ProfileManager.setActive(p);
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

        int perPage = Math.max(1, (this.height - ROW_TOP - 64) / ROW_H);
        pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.min(Math.max(page, 0), pages - 1);
        int start = page * perPage;
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
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - 170;

        g.centeredText(this.font, Component.literal("Choose a profile"), this.width / 2, 10, Look.GOLD);
        Look.line(g, left, 22, left + 340, Look.GOLD);
        g.centeredText(this.font, Component.literal("Only one profile is on at a time. Each one has its own"),
                this.width / 2, 28, Look.GRAY);
        g.centeredText(this.font, Component.literal("commands, hotkeys and automations. Click Open to turn one on."),
                this.width / 2, 39, Look.GRAY);

        Look.box(g, left - 8, 50, left + 348, this.height - 56, Look.BORDER);

        Profile active = ProfileManager.active();
        for (Row r : rows) {
            Profile p = r.profile();
            boolean on = p == active;

            g.fill(r.x(), r.y(), r.x() + SLOT_W, r.y() + 26, on ? 0xAA5A3A00 : 0x66000000);
            if (on) {
                Look.box(g, r.x(), r.y(), r.x() + SLOT_W, r.y() + 26, Look.GOLD);
            }
            Icons.draw(g, p.icon, r.x() + 5, r.y() + 5, 2);

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
    }

    @Override
    public void onClose() {
        Ui.open(parent);
    }
}
