package dev.vanguard;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import dev.vanguard.module.impl.BaseFinder;
import dev.vanguard.module.impl.SchematicBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
                .then(literal("profile")
                    .then(literal("list").executes(Commands::profileList))
                    .then(literal("save").then(argument("name", StringArgumentType.word()).executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        if (!Config.saveProfile(name)) {
                            reply(ctx, "Could not save profile. Use 1-32 letters, numbers, _ or -.");
                            return 0;
                        }
                        reply(ctx, "Saved profile " + name);
                        return 1;
                    })))
                    .then(literal("load").then(argument("name", StringArgumentType.word()).executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        if (!Config.loadProfile(name)) {
                            reply(ctx, "Could not load profile " + name + ". Use /vg profile list to see saved profiles.");
                            return 0;
                        }
                        reply(ctx, "Loaded profile " + name);
                        return 1;
                    })))
                    .then(literal("delete").then(argument("name", StringArgumentType.word()).executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        if (!Config.deleteProfile(name)) {
                            reply(ctx, "Could not delete profile " + name);
                            return 0;
                        }
                        reply(ctx, "Deleted profile " + name);
                        return 1;
                    }))))
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
                    .then(literal("clear").executes(ctx -> {
                        builder().clear();
                        reply(ctx, "Schematic cleared");
                        return 1;
                    }))
                    .then(literal("info").executes(ctx -> {
                        reply(ctx, builder().status());
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

    private static Path schemDir() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("vanguard").resolve("schematics");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // listing/loading will report the problem
        }
        return dir;
    }

    private static int profileList(CommandContext<FabricClientCommandSource> ctx) {
        List<String> profiles = Config.profiles();
        if (profiles.isEmpty()) reply(ctx, "No profiles saved. Use /vg profile save <name>.");
        else for (String name : profiles) reply(ctx, name);
        return 1;
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
        Path dir = schemDir();
        try (Stream<Path> files = Files.list(dir)) {
            List<String> names = files.map(p -> p.getFileName().toString())
                .filter(n -> n.endsWith(".litematic") || n.endsWith(".nbt"))
                .sorted().toList();
            if (names.isEmpty()) reply(ctx, "No schematics in " + dir);
            for (String n : names) reply(ctx, n);
        } catch (IOException e) {
            reply(ctx, "Could not read " + dir);
        }
        return 1;
    }

    private static int schemLoad(CommandContext<FabricClientCommandSource> ctx) {
        Path dir = schemDir().toAbsolutePath().normalize();
        Path file = dir.resolve(StringArgumentType.getString(ctx, "file")).normalize();
        if (!file.startsWith(dir)) {
            reply(ctx, "File must be inside " + dir);
            return 0;
        }
        try {
            reply(ctx, "Loaded " + builder().load(file) + ". Stand at the lowest corner and run /vg schem origin");
            return 1;
        } catch (Exception e) {
            reply(ctx, "Failed to load: " + e.getMessage());
            return 0;
        }
    }

    private static int schemOrigin(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        if (!builder().hasSchematic()) {
            reply(ctx, "Load a schematic first");
            return 0;
        }
        builder().setOrigin(mc.player.blockPosition());
        reply(ctx, "Origin set. Enable SchematicBuilder to start.");
        return 1;
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
