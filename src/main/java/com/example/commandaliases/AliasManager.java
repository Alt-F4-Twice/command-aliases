package com.example.commandaliases;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/** Loads/saves aliases (config/commandaliases/aliases.json) and expands typed commands. */
public final class AliasManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("commandaliases");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<AliasEntry> ALIASES = new ArrayList<>();
    private static final Deque<String> PENDING = new ArrayDeque<>();
    private static Path file;

    /** True while we are sending an expanded command, so it is not expanded again (no loops). */
    static boolean bypass = false;

    private AliasManager() {
    }

    /** The characters a quick command can start with. */
    public static final String PREFIXES = "!?/";

    public static boolean isPrefix(char c) {
        return PREFIXES.indexOf(c) >= 0;
    }

    /** "s" -> "/s", "!s" -> "!s", "//s" -> "/s". Returns "" if nothing usable. */
    public static String normalizeTrigger(String raw) {
        String t = raw.strip();
        if (t.isEmpty()) {
            return "";
        }
        char prefix = '/';
        if (isPrefix(t.charAt(0))) {
            prefix = t.charAt(0);
            t = t.substring(1);
        }
        if (prefix == '/') {
            while (t.startsWith("/")) {
                t = t.substring(1);
            }
        }
        return t.isEmpty() ? "" : prefix + t;
    }

    public static void init() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("commandaliases");
        file = dir.resolve("aliases.json");
        load();
    }

    public static List<AliasEntry> all() {
        return ALIASES;
    }

    public static void load() {
        ALIASES.clear();
        if (!Files.exists(file)) {
            // First run: one example so the menu isn't empty.
            ALIASES.add(new AliasEntry("/s", "gamemode survival", true));
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<List<AliasEntry>>() {
            }.getType();
            List<AliasEntry> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                for (AliasEntry e : loaded) {
                    if (e != null && e.name != null && e.command != null) {
                        e.name = normalizeTrigger(e.name);
                        if (!e.name.isEmpty()) {
                            ALIASES.add(e);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not read {} - keeping a copy as aliases.json.broken", file, ex);
            try {
                Files.move(file, file.resolveSibling("aliases.json.broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
        }
    }

    public static void sort() {
        ALIASES.sort(Comparator.comparing(a -> a.name.toLowerCase()));
    }

    public static void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("aliases.json.tmp");
            Files.writeString(tmp, GSON.toJson(ALIASES));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            LOGGER.error("Could not save {}", file, ex);
        }
    }

    public static AliasEntry find(String name) {
        for (AliasEntry e : ALIASES) {
            if (e.enabled && e.name.equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Turns what the player typed (including its prefix: /s, !s or ?s) into the commands to run,
     * or null if it isn't an alias. Extra words are added to the end of the last
     * command, or put where {args} appears.
     */
    public static List<String> expand(String typed) {
        String t = typed.strip();
        int space = t.indexOf(' ');
        String trigger = space < 0 ? t : t.substring(0, space);
        String args = space < 0 ? "" : t.substring(space + 1).strip();

        AliasEntry entry = find(trigger);
        if (entry == null) {
            return null;
        }

        List<String> out = new ArrayList<>();
        for (String part : entry.command.split(";")) {
            String c = part.strip();
            while (c.startsWith("/")) {
                c = c.substring(1).strip();
            }
            if (!c.isEmpty()) {
                out.add(c);
            }
        }
        if (out.isEmpty()) {
            return null;
        }
        if (out.size() > 10) {
            out = new ArrayList<>(out.subList(0, 10)); // keep chains sane
        }

        boolean usedPlaceholder = false;
        for (int i = 0; i < out.size(); i++) {
            String c = out.get(i);
            if (c.contains("{args}")) {
                usedPlaceholder = true;
                out.set(i, c.replace("{args}", args).strip());
            }
        }
        if (!usedPlaceholder && !args.isEmpty()) {
            int last = out.size() - 1;
            out.set(last, out.get(last) + " " + args);
        }
        return out;
    }

    /** Queue extra chained commands; they are sent one per tick, after the first. */
    public static void queue(List<String> commands) {
        for (String c : commands) {
            if (PENDING.size() < 20) { // never flood the server
                PENDING.add(c);
            }
        }
    }

    public static void tick(Minecraft mc) {
        String next = PENDING.poll();
        if (next == null) {
            return;
        }
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            PENDING.clear();
            return;
        }
        bypass = true;
        try {
            connection.sendCommand(next);
        } finally {
            bypass = false;
        }
    }
}
