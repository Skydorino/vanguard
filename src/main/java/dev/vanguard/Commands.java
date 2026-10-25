package dev.vanguard;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import dev.vanguard.module.impl.AutoSell;
import dev.vanguard.module.impl.BaseFinder;
import dev.vanguard.module.impl.SchematicBuilder;
import dev.vanguard.module.impl.SusChunks;
import dev.vanguard.util.LitematicaBridge;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * /vg list | toggle | set | save | friend add|remove
 * /vg schem list|load|origin|clear|info     /vg bases [clear]
 */
public final class Commands {
    private static final SuggestionProvider<FabricClientCommandSource> MODULE_NAMES =
        (ctx, builder) -> SharedSuggestionProvider.suggest(ModuleManager.names(), builder);

    private Commands() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("vg")
                .then(literal("list").executes(Commands::list))
                .then(literal("save").executes(ctx -> {
                    Config.save();
                    reply(ctx, "Config saved");
                    return 1;
                }))
                .then(literal("off").executes(ctx -> {
                    ModuleManager.disableAll();
                    reply(ctx, "All modules off");
                    return 1;
                }))
                .then(literal("hud").executes(ctx -> {
                    Hud.visible = !Hud.visible;
                    Config.save();
                    reply(ctx, Hud.visible ? "HUD shown" : "HUD hidden");
                    return 1;
                }))
                .then(literal("autosell")
                    .executes(ctx -> {
                        reply(ctx, "AutoSell runs /" + AutoSell.command);
                        return 1;
                    })
                    .then(argument("command", StringArgumentType.greedyString()).executes(ctx -> {
                        String c = StringArgumentType.getString(ctx, "command").trim();
                        if (c.startsWith("/")) c = c.substring(1);
                        AutoSell.command = c;
                        Config.save();
                        reply(ctx, "AutoSell will run /" + c);
                        return 1;
                    })))
                .then(literal("toggle")
                    .then(argument("module", StringArgumentType.word()).suggests(MODULE_NAMES)
                        .executes(Commands::toggle)))
                .then(literal("set")
                    .then(argument("module", StringArgumentType.word()).suggests(MODULE_NAMES)
                        .then(argument("setting", StringArgumentType.word())
                            .then(argument("value", DoubleArgumentType.doubleArg())
                                .executes(Commands::set)))))
                .then(literal("friend")
                    .then(literal("add").then(argument("name", StringArgumentType.word()).executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        reply(ctx, ModuleManager.addFriend(name) ? "Added friend " + name : name + " is already a friend");
                        return 1;
                    })))
                    .then(literal("remove").then(argument("name", StringArgumentType.word()).executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        reply(ctx, ModuleManager.removeFriend(name) ? "Removed friend " + name : name + " is not a friend");
                        return 1;
                    }))))
                .then(literal("schem")
                    .then(literal("list").executes(Commands::schemList))
                    .then(literal("load").then(argument("file", StringArgumentType.string()).executes(Commands::schemLoad)))
                    .then(literal("origin").executes(Commands::schemOrigin))
                    .then(literal("litematica").executes(Commands::schemLitematica))
                    .then(literal("use").then(argument("index", IntegerArgumentType.integer(1)).executes(Commands::schemUse)))
                    .then(literal("clear").executes(ctx -> {
                        builder().clear();
                        reply(ctx, "Schematic cleared");
                        return 1;
                    }))
                    .then(literal("info").executes(ctx -> {
                        reply(ctx, builder().status());
                        return 1;
                    })))
                .then(literal("sus")
                    .executes(Commands::susList)
                    .then(literal("clear").executes(ctx -> {
                        sus().clear();
                        reply(ctx, "Cleared flagged chunks");
                        return 1;
                    })))
                .then(literal("bases")
                    .executes(Commands::bases)
                    .then(literal("clear").executes(ctx -> {
                        finder().clear();
                        reply(ctx, "Cleared flagged bases");
                        return 1;
                    })))));
    }

    private static SchematicBuilder builder() {
        return (SchematicBuilder) ModuleManager.find("SchematicBuilder");
    }

    private static BaseFinder finder() {
        return (BaseFinder) ModuleManager.find("BaseFinder");
    }

    private static SusChunks sus() {
        return (SusChunks) ModuleManager.find("SusChunks");
    }

    private static int susList(CommandContext<FabricClientCommandSource> ctx) {
        List<SusChunks.Sus> hits = sus().hits();
        if (hits.isEmpty()) reply(ctx, "No sus chunks yet (enable SusChunks and explore)");
        for (SusChunks.Sus h : hits) {
            reply(ctx, h.x() + ", " + h.y() + ", " + h.z() + "  (score " + h.score() + ")");
        }
        return 1;
    }

    /** Litematica's own schematics folder first, then Vanguard's. */
    private static List<Path> schemDirs() {
        Path game = FabricLoader.getInstance().getGameDir();
        List<Path> dirs = new ArrayList<>();
        dirs.add(game.resolve("schematics"));
        dirs.add(game.resolve("vanguard").resolve("schematics"));
        for (Path d : dirs) {
            try {
                Files.createDirectories(d);
            } catch (IOException ignored) {
                // listing/loading will report the problem
            }
        }
        return dirs;
    }

    private static int list(CommandContext<FabricClientCommandSource> ctx) {
        for (Module m : ModuleManager.all()) {
            StringBuilder line = new StringBuilder(m.name).append(m.isEnabled() ? " [on]" : " [off]");
            for (Map.Entry<String, Double> e : m.settings().entrySet()) {
                line.append("  ").append(e.getKey()).append('=').append(e.getValue());
            }
            reply(ctx, line.toString());
        }
        return 1;
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        Module m = ModuleManager.find(StringArgumentType.getString(ctx, "module"));
        if (m == null) {
            reply(ctx, "Unknown module");
            return 0;
        }
        m.toggle();
        return 1;
    }

    private static int set(CommandContext<FabricClientCommandSource> ctx) {
        Module m = ModuleManager.find(StringArgumentType.getString(ctx, "module"));
        if (m == null) {
            reply(ctx, "Unknown module");
            return 0;
        }
        String setting = StringArgumentType.getString(ctx, "setting");
        double value = DoubleArgumentType.getDouble(ctx, "value");
        if (!m.set(setting, value)) {
            reply(ctx, m.name + " has no setting '" + setting + "'");
            return 0;
        }
        reply(ctx, m.name + "." + setting + " = " + value);
        return 1;
    }

    private static int schemList(CommandContext<FabricClientCommandSource> ctx) {
        if (LitematicaBridge.installed()) reply(ctx, "Litematica detected, reading its schematics folder too.");
        int count = 0;
        for (Path dir : schemDirs()) {
            try (Stream<Path> files = Files.list(dir)) {
                List<String> names = files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".litematic") || n.endsWith(".nbt"))
                    .sorted().toList();
                for (String n : names) {
                    reply(ctx, n);
                    count++;
                }
            } catch (IOException e) {
                reply(ctx, "Could not read " + dir);
            }
        }
        if (count == 0) reply(ctx, "No schematics found in " + schemDirs().get(0) + " or " + schemDirs().get(1));
        return 1;
    }

    private static int schemLoad(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "file");
        for (Path dir : schemDirs()) {
            Path base = dir.toAbsolutePath().normalize();
            Path file = base.resolve(name).normalize();
            if (!file.startsWith(base) || !Files.isRegularFile(file)) continue;
            try {
                reply(ctx, "Loaded " + builder().load(file) + ". Stand at the lowest corner and run /vg schem origin");
                return 1;
            } catch (Exception e) {
                reply(ctx, "Failed to load: " + e.getMessage());
                return 0;
            }
        }
        reply(ctx, "Could not find " + name + ". Use /vg schem list to see what is available.");
        return 0;
    }

    private static int schemOrigin(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        if (!builder().hasSchematic()) {
            reply(ctx, "Load a schematic first");
            return 0;
        }
        builder().setOrigin(mc.player.blockPosition(), false);
        reply(ctx, "Origin set. Enable SchematicBuilder to start.");
        return 1;
    }

    private static int schemLitematica(CommandContext<FabricClientCommandSource> ctx) {
        if (!LitematicaBridge.installed()) reply(ctx, "Litematica does not look installed; checking for saved placements anyway");
        List<LitematicaBridge.Placement> list = LitematicaBridge.placements();
        if (list.isEmpty()) {
            reply(ctx, "No saved Litematica placements found");
            return 0;
        }
        for (int i = 0; i < list.size(); i++) {
            LitematicaBridge.Placement p = list.get(i);
            reply(ctx, (i + 1) + ": " + p.name() + " at " + p.origin().getX() + ", " + p.origin().getY() + ", " + p.origin().getZ());
        }
        reply(ctx, "Use /vg schem use <number> to build one");
        return 1;
    }

    private static int schemUse(CommandContext<FabricClientCommandSource> ctx) {
        int index = IntegerArgumentType.getInteger(ctx, "index");
        List<LitematicaBridge.Placement> list = LitematicaBridge.placements();
        if (index < 1 || index > list.size()) {
            reply(ctx, "No placement " + index + " (run /vg schem litematica first)");
            return 0;
        }
        LitematicaBridge.Placement p = list.get(index - 1);
        try {
            reply(ctx, "Loaded " + builder().load(p.file()));
            builder().setOrigin(p.origin(), true);
            reply(ctx, "Origin set to the Litematica placement. Enable SchematicBuilder to start.");
            return 1;
        } catch (Exception e) {
            reply(ctx, "Failed to load: " + e.getMessage());
            return 0;
        }
    }

    private static int bases(CommandContext<FabricClientCommandSource> ctx) {
        List<BaseFinder.Hit> hits = finder().hits();
        if (hits.isEmpty()) reply(ctx, "No bases flagged yet (enable BaseFinder and explore)");
        for (BaseFinder.Hit h : hits) {
            reply(ctx, h.x() + ", " + h.y() + ", " + h.z() + "  (" + h.storage() + " storage)");
        }
        return 1;
    }

    private static void reply(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Component.literal("[Vanguard] " + message));
    }
}
