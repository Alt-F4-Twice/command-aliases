package com.example.commandaliases;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Loads/saves every profile in config/commandaliases/profiles.json and tracks which one is on. */
public final class ProfileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("commandaliases");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final class Store {
        String active;
        List<Profile> profiles = new ArrayList<>();
    }

    private static Store store = new Store();
    private static Path dir;
    private static Path file;

    private ProfileManager() {
    }

    public static void init() {
        dir = FabricLoader.getInstance().getConfigDir().resolve("commandaliases");
        file = dir.resolve("profiles.json");
        load();
    }

    public static List<Profile> all() {
        return store.profiles;
    }

    /** The profile that is on right now. */
    public static Profile active() {
        for (Profile p : store.profiles) {
            if (p.id.equals(store.active)) {
                return p;
            }
        }
        return store.profiles.get(0);
    }

    public static Profile byId(String id) {
        for (Profile p : store.profiles) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    public static void setActive(Profile p) {
        store.active = p.id;
        save();
    }

    public static Profile create(String name, String icon) {
        Profile p = new Profile();
        p.name = name;
        p.icon = icon;
        store.profiles.add(p);
        save();
        return p;
    }

    /** Removes a profile. The last one can't be removed. */
    public static void delete(Profile p) {
        if (store.profiles.size() <= 1) {
            return;
        }
        store.profiles.remove(p);
        if (p.id.equals(store.active)) {
            store.active = store.profiles.get(0).id;
        }
        save();
    }

    public static void load() {
        store = new Store();
        boolean loaded = false;

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Store s = GSON.fromJson(reader, Store.class);
                if (s != null && s.profiles != null) {
                    store = s;
                    loaded = true;
                }
            } catch (Exception ex) {
                LOGGER.error("Could not read {} - keeping a copy as profiles.json.broken", file, ex);
                try {
                    Files.move(file, file.resolveSibling("profiles.json.broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                }
            }
        }

        if (!loaded) {
            migrateOrSeed();
        }
        sanitize();
        if (!loaded) {
            save();
        }
    }

    /** First run of this version: bring over aliases.json / hotkeys.json from older versions. */
    private static void migrateOrSeed() {
        store = new Store();
        Profile p = new Profile();
        p.name = "Default";
        p.icon = "grass_block";
        boolean any = false;

        Path oldAliases = dir.resolve("aliases.json");
        if (Files.exists(oldAliases)) {
            try (Reader r = Files.newBufferedReader(oldAliases)) {
                Type t = new TypeToken<List<AliasEntry>>() {
                }.getType();
                List<AliasEntry> list = GSON.fromJson(r, t);
                if (list != null) {
                    p.aliases.addAll(list);
                }
                any = true;
            } catch (Exception ex) {
                LOGGER.error("Could not read old {}", oldAliases, ex);
            }
        }
        Path oldHotkeys = dir.resolve("hotkeys.json");
        if (Files.exists(oldHotkeys)) {
            try (Reader r = Files.newBufferedReader(oldHotkeys)) {
                Type t = new TypeToken<List<HotkeyEntry>>() {
                }.getType();
                List<HotkeyEntry> list = GSON.fromJson(r, t);
                if (list != null) {
                    p.hotkeys.addAll(list);
                }
                any = true;
            } catch (Exception ex) {
                LOGGER.error("Could not read old {}", oldHotkeys, ex);
            }
        }
        if (!any) {
            p.aliases.add(new AliasEntry("/s", "gamemode survival", true));
        }
        store.profiles.add(p);
        store.active = p.id;
    }

    /** Fix up anything missing or hand-edited wrongly so the menus never crash. */
    private static void sanitize() {
        if (store.profiles == null) {
            store.profiles = new ArrayList<>();
        }
        store.profiles.removeIf(p -> p == null);
        for (Profile p : store.profiles) {
            if (p.id == null || p.id.isEmpty()) {
                p.id = Long.toHexString(System.nanoTime());
            }
            if (p.name == null || p.name.isBlank()) {
                p.name = "Profile";
            }
            if (p.icon == null) {
                p.icon = "grass_block";
            }
            if (p.aliases == null) {
                p.aliases = new ArrayList<>();
            }
            if (p.hotkeys == null) {
                p.hotkeys = new ArrayList<>();
            }
            if (p.automations == null) {
                p.automations = new ArrayList<>();
            }

            Iterator<AliasEntry> ai = p.aliases.iterator();
            while (ai.hasNext()) {
                AliasEntry e = ai.next();
                if (e == null || e.name == null || e.command == null) {
                    ai.remove();
                    continue;
                }
                e.name = AliasManager.normalizeTrigger(e.name);
                if (e.name.isEmpty()) {
                    ai.remove();
                }
            }
            p.hotkeys.removeIf(h -> h == null || h.command == null);
            p.automations.removeIf(a -> a == null || a.command == null || a.trigger == null);
        }
        if (store.profiles.isEmpty()) {
            Profile p = new Profile();
            p.name = "Default";
            store.profiles.add(p);
        }
        boolean found = false;
        for (Profile p : store.profiles) {
            if (p.id.equals(store.active)) {
                found = true;
                break;
            }
        }
        if (!found) {
            store.active = store.profiles.get(0).id;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("profiles.json.tmp");
            Files.writeString(tmp, GSON.toJson(store));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            LOGGER.error("Could not save {}", file, ex);
        }
    }
}
