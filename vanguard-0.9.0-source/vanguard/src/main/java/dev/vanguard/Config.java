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

/** Saves module settings, friends and theme to config/vanguard.json. Enabled state is not saved. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("vanguard.json");
    }

    public static void save() {
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = new JsonObject();
            for (Map.Entry<String, Double> e : m.settings().entrySet()) o.addProperty(e.getKey(), e.getValue());
            mods.add(m.name, o);
        }
        root.add("settings", mods);
        root.addProperty("theme", Theme.index);
        root.addProperty("hud", Hud.visible);
        JsonArray friends = new JsonArray();
        for (String f : ModuleManager.friends()) friends.add(f);
        root.add("friends", friends);
        try {
            Files.writeString(file(), GSON.toJson(root));
        } catch (IOException ignored) {
            // Config is best-effort
        }
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (root.has("hud")) Hud.visible = root.get("hud").getAsBoolean();
            if (root.has("theme")) Theme.index = Math.floorMod(root.get("theme").getAsInt(), Theme.ACCENTS.length);
            if (root.has("friends")) {
                for (JsonElement f : root.getAsJsonArray("friends")) ModuleManager.addFriend(f.getAsString());
            }
            if (root.has("settings")) {
                JsonObject mods = root.getAsJsonObject("settings");
                for (Module m : ModuleManager.all()) {
                    if (!mods.has(m.name)) continue;
                    for (Map.Entry<String, JsonElement> e : mods.getAsJsonObject(m.name).entrySet()) {
                        m.set(e.getKey(), e.getValue().getAsDouble());
                    }
                }
            }
        } catch (Exception ignored) {
            // Corrupt config: fall back to defaults
        }
    }
}
