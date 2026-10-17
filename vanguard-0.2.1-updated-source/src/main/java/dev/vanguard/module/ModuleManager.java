package dev.vanguard.module;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanguard.Config;
import dev.vanguard.VanguardClient;
import dev.vanguard.gui.ClickGui;
import dev.vanguard.module.impl.AnchorMacro;
import dev.vanguard.module.impl.AutoCrystal;
import dev.vanguard.module.impl.AutoRefill;
import dev.vanguard.module.impl.AutoTotem;
import dev.vanguard.module.impl.AutoWalk;
import dev.vanguard.module.impl.AutoXP;
import dev.vanguard.module.impl.BaseFinder;
import dev.vanguard.module.impl.CrystalOptimizer;
import dev.vanguard.module.impl.Fullbright;
import dev.vanguard.module.impl.KillAura;\nimport dev.vanguard.module.impl.HudOptions;
import dev.vanguard.module.impl.SchematicBuilder;
import dev.vanguard.module.impl.Sprint;
import dev.vanguard.module.impl.SwordInspect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class ModuleManager {
    public static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(VanguardClient.MOD_ID, "modules"));

    private static final List<Module> MODULES = new ArrayList<>();
    private static final Set<String> FRIENDS = new HashSet<>();
    private static KeyMapping guiKey;

    private ModuleManager() {}

    public static void init() {
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.vanguard.gui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, CATEGORY));
        SwordInspect.registerKey();

        add(new AutoCrystal());
        add(new CrystalOptimizer());
        add(new AnchorMacro());
        add(new AutoTotem());
        add(new AutoRefill());
        add(new AutoXP());
        add(new KillAura());
        add(new Sprint());
        add(new AutoWalk());
        add(new BaseFinder());
        add(new SchematicBuilder());
        add(new Fullbright());
        add(new SwordInspect());

        Config.load();
        ClientTickEvents.END_CLIENT_TICK.register(ModuleManager::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());
    }

    private static void add(Module module) {
        // No default key: bind them in Options > Controls > Key Binds > Vanguard
        module.key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.vanguard." + module.name.toLowerCase(Locale.ROOT),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY));
        MODULES.add(module);
    }

    private static void tick(Minecraft mc) {
        for (Module m : MODULES) {
            while (m.key.consumeClick()) m.toggle();
        }
        while (guiKey.consumeClick()) {
            if (mc.screen == null && mc.player != null) mc.setScreen(new ClickGui());
        }
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;
        for (Module m : MODULES) {
            if (m.isEnabled()) m.onTick(player, level);
        }
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (Module m : MODULES) out.add(m.name.toLowerCase(Locale.ROOT));
        return out;
    }

    public static Module find(String name) {
        for (Module m : MODULES) {
            if (m.name.equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public static Set<String> friends() {
        return Collections.unmodifiableSet(FRIENDS);
    }

    public static boolean addFriend(String name) {
        return FRIENDS.add(name.toLowerCase(Locale.ROOT));
    }

    public static boolean removeFriend(String name) {
        return FRIENDS.remove(name.toLowerCase(Locale.ROOT));
    }

    public static void clearFriends() {
        FRIENDS.clear();
    }

    public static boolean isFriend(Player player) {
        return FRIENDS.contains(player.getName().getString().toLowerCase(Locale.ROOT));
    }
}
