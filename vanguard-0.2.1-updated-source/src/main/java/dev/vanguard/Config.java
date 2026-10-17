package dev.vanguard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.vanguard.gui.Theme;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/** Persists Vanguard settings, friends, theme, and named profiles. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("vanguard.json");
    }

    private static Path profilesDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("vanguard").resolve("profiles");
    }

    private static JsonObject snapshot() {
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = new JsonObject();
            for (Map.Entry<String, Double> e : m.settings().entrySet()) {
                o.addProperty(e.getKey(), e.getValue());
            }
            mods.add(m.name, o);
        }
        root.add("settings", mods);
        root.addProperty("theme", Theme.index);
        JsonArray friends = new JsonArray();
        for (String f : ModuleManager.friends()) friends.add(f);
        root.add("friends", friends);
        return root;
    }

    public static void save() {
        write(file(), snapshot());
    }

    /** Save a named profile separately from the active default config. */
    public static boolean saveProfile(String name) {
        Path path = profilePath(name);
        if (path == null) return false;
        try {
            Files.createDirectories(profilesDir());
            Files.writeString(path, GSON.toJson(snapshot()));
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    /** Load settings and theme from a named profile. */
    public static boolean loadProfile(String name) {
        Path path = profilePath(name);
        if (path == null || !Files.isRegularFile(path)) return false;
        try {
            apply(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            save();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean deleteProfile(String name) {
        Path path = profilePath(name);
        if (path == null) return false;
        try {
            return Files.deleteIfExists(path);
        } catch (IOException ignored) {
            return false;
        }
    }

    public static java.util.List<String> profiles() {
        try {
            Files.createDirectories(profilesDir());
            try (var files = Files.list(profilesDir())) {
                return files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5))
                    .sorted().toList();
            }
        } catch (IOException ignored) {
            return java.util.List.of();
        }
    }

    private static Path profilePath(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,32}")) return null;
        return profilesDir().resolve(name + ".json").normalize();
    }

    private static void write(Path path, JsonObject data) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(path, GSON.toJson(data));
        } catch (IOException ignored) {
            // Configuration is best-effort; the client should remain usable if disk writes fail.
        }
    }

    public static void load() {
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            apply(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
        } catch (Exception ignored) {
            // Corrupt config: retain defaults rather than crashing client startup.
        }
    }

    private static void apply(JsonObject root) {
        if (root.has("theme")) {
            Theme.index = Math.floorMod(root.get("theme").getAsInt(), Theme.ACCENTS.length);
        }
        if (root.has("friends") && root.get("friends").isJsonArray()) {
            ModuleManager.clearFriends();
            for (JsonElement f : root.getAsJsonArray("friends")) {
                if (f.isJsonPrimitive() && f.getAsJsonPrimitive().isString()) {
                    ModuleManager.addFriend(f.getAsString());
                }
            }
        }
        if (root.has("settings") && root.get("settings").isJsonObject()) {
            JsonObject mods = root.getAsJsonObject("settings");
            for (Module m : ModuleManager.all()) {
                if (!mods.has(m.name) || !mods.get(m.name).isJsonObject()) continue;
                for (Map.Entry<String, JsonElement> e : mods.getAsJsonObject(m.name).entrySet()) {
                    if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) {
                        m.set(e.getKey(), e.getValue().getAsDouble());
                    }
                }
            }
        }
    }
}
