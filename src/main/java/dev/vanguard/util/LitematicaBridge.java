package dev.vanguard.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

/**
 * Best-effort detection of Litematica: whether the mod is installed, where its schematics folder
 * is, and which schematic placements it has saved (name, file, origin). It reads Litematica's
 * saved JSON leniently, so if a version stores placements differently it simply finds none.
 */
public final class LitematicaBridge {
    public record Placement(String name, Path file, BlockPos origin) {}

    private LitematicaBridge() {}

    public static boolean installed() {
        return FabricLoader.getInstance().isModLoaded("litematica");
    }

    public static Path schematicsFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("schematics");
    }

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();
        Path root = FabricLoader.getInstance().getConfigDir().resolve("litematica");
        if (!Files.isDirectory(root)) return out;

        List<Path> files;
        try (Stream<Path> walk = Files.walk(root, 4)) {
            files = walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.endsWith(".json") && n.contains("placement");
                })
                .toList();
        } catch (IOException e) {
            return out;
        }
        for (Path p : files) {
            try {
                walk(JsonParser.parseString(Files.readString(p)), out);
            } catch (Exception ignored) {
                // unreadable or unexpected file
            }
        }
        return out;
    }

    private static void walk(JsonElement e, List<Placement> out) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            BlockPos origin = readPos(o.get("origin"));
            String file = firstString(o, "schematic_file", "file", "schematic", "schematic_path");
            if (origin != null && file != null) {
                String name = firstString(o, "name");
                Path resolved = resolve(file);
                Placement p = new Placement(name == null ? resolved.getFileName().toString() : name, resolved, origin);
                if (out.stream().noneMatch(q -> q.file().equals(p.file()) && q.origin().equals(p.origin()))) out.add(p);
            }
            for (Map.Entry<String, JsonElement> en : o.entrySet()) walk(en.getValue(), out);
        } else if (e.isJsonArray()) {
            for (JsonElement c : e.getAsJsonArray()) walk(c, out);
        }
    }

    private static BlockPos readPos(JsonElement e) {
        if (e == null) return null;
        try {
            if (e.isJsonArray() && e.getAsJsonArray().size() >= 3) {
                return new BlockPos(e.getAsJsonArray().get(0).getAsInt(), e.getAsJsonArray().get(1).getAsInt(),
                    e.getAsJsonArray().get(2).getAsInt());
            }
            if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("x") && o.has("y") && o.has("z")) {
                    return new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
                }
            }
        } catch (RuntimeException ignored) {
            // not a position
        }
        return null;
    }

    private static String firstString(JsonObject o, String... keys) {
        for (String k : keys) {
            JsonElement v = o.get(k);
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) return v.getAsString();
        }
        return null;
    }

    private static Path resolve(String file) {
        Path p = Path.of(file);
        if (p.isAbsolute()) return p;
        Path inSchematics = schematicsFolder().resolve(file);
        if (Files.exists(inSchematics)) return inSchematics;
        return FabricLoader.getInstance().getGameDir().resolve(file);
    }
}
