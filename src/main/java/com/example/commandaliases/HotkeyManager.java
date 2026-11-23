package com.example.commandaliases;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Loads/saves hotkeys (config/commandaliases/hotkeys.json) and fires them when pressed. */
public final class HotkeyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("commandaliases");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<HotkeyEntry> HOTKEYS = new ArrayList<>();
    private static Path file;

    private HotkeyManager() {
    }

    public static void init() {
        file = FabricLoader.getInstance().getConfigDir().resolve("commandaliases").resolve("hotkeys.json");
        load();
    }

    public static List<HotkeyEntry> all() {
        return HOTKEYS;
    }

    public static void load() {
        HOTKEYS.clear();
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<List<HotkeyEntry>>() {
            }.getType();
            List<HotkeyEntry> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                for (HotkeyEntry h : loaded) {
                    if (h != null && h.command != null) {
                        HOTKEYS.add(h);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not read {} - keeping a copy as hotkeys.json.broken", file, ex);
            try {
                Files.move(file, file.resolveSibling("hotkeys.json.broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("hotkeys.json.tmp");
            Files.writeString(tmp, GSON.toJson(HOTKEYS));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            LOGGER.error("Could not save {}", file, ex);
        }
    }

    private static boolean down(int key) {
        return InputConstants.isKeyDown(key);
    }

    /** Which of Shift / Ctrl / Alt are held right now (1 / 2 / 4). Uses SDL scancodes. */
    public static int currentMods() {
        int m = 0;
        if (down(225) || down(229)) {   // left / right Shift
            m |= 1;
        }
        if (down(224) || down(228)) {   // left / right Ctrl
            m |= 2;
        }
        if (down(226) || down(230)) {   // left / right Alt
            m |= 4;
        }
        return m;
    }

    /** Called every client tick: fires a hotkey the moment its key (and modifiers) go down. */
    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            for (HotkeyEntry h : HOTKEYS) {
                h.wasDown = false;
            }
            return;
        }
        boolean canFire = Ui.current() == null;

        int modsNow = currentMods();

        for (HotkeyEntry h : HOTKEYS) {
            if (!h.enabled || h.key < 0) {
                h.wasDown = false;
                continue;
            }
            boolean active = down(h.key) && modsNow == h.mods;
            if (active && !h.wasDown && canFire) {
                run(h.command);
            }
            h.wasDown = active;
        }
    }

    /** Runs a hotkey's command(s). Quick commands (/s, !s, ?s) are expanded; ";" chains several. */
    private static void run(String command) {
        List<String> out = new ArrayList<>();
        for (String part : command.split(";")) {
            String c = part.strip();
            if (c.isEmpty()) {
                continue;
            }
            char first = c.charAt(0);
            boolean chatPrefix = first == '!' || first == '?';
            String typed = (chatPrefix || first == '/') ? c : "/" + c;

            List<String> expanded = AliasManager.expand(typed);
            if (expanded != null) {
                out.addAll(expanded);
            } else if (!chatPrefix) {
                while (c.startsWith("/")) {
                    c = c.substring(1).strip();
                }
                if (!c.isEmpty()) {
                    out.add(c);
                }
            }
        }
        AliasManager.queue(out);
    }
}
