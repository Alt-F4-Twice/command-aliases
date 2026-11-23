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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads/saves hotkeys (config/commandaliases/hotkeys.json) and fires them when pressed. */
public final class HotkeyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("commandaliases");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<HotkeyEntry> HOTKEYS = new ArrayList<>();
    private static final Set<Integer> DOWN = new HashSet<>();
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

    /** Called every client tick: fires a hotkey the moment its key goes down (no screen open). */
    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            DOWN.clear();
            return;
        }
        boolean canFire = Ui.current() == null;
        Set<Integer> nowDown = new HashSet<>();
        for (HotkeyEntry h : HOTKEYS) {
            if (!h.enabled || h.key < 0) {
                continue;
            }
            if (InputConstants.isKeyDown(mc.getWindow(), h.key)) {
                nowDown.add(h.key);
                if (canFire && !DOWN.contains(h.key)) {
                    run(h.command);
                }
            }
        }
        DOWN.clear();
        DOWN.addAll(nowDown);
    }

    /** Runs a hotkey's command(s). Quick commands (aliases) are expanded; ";" chains several. */
    private static void run(String command) {
        List<String> out = new ArrayList<>();
        for (String part : command.split(";")) {
            String c = part.strip();
            while (c.startsWith("/")) {
                c = c.substring(1).strip();
            }
            if (c.isEmpty()) {
                continue;
            }
            List<String> expanded = AliasManager.expand(c);
            if (expanded != null) {
                out.addAll(expanded);
            } else {
                out.add(c);
            }
        }
        AliasManager.queue(out);
    }
}
