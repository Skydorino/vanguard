package dev.vanguard.module;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanguard.Config;
import dev.vanguard.VanguardClient;
import dev.vanguard.gui.ClickGui;
import dev.vanguard.module.impl.AirWalk;
import dev.vanguard.module.impl.AmethystNotifier;
import dev.vanguard.module.impl.AnchorMacro;
import dev.vanguard.module.impl.AutoCart;
import dev.vanguard.module.impl.AutoDrain;
import dev.vanguard.module.impl.AutoHitCrystal;
import dev.vanguard.module.impl.AutoMLG;
import dev.vanguard.module.impl.AutoMace;
import dev.vanguard.module.impl.AutoMine;
import dev.vanguard.module.impl.AutoPearlCatch;
import dev.vanguard.module.impl.AutoRefill;
import dev.vanguard.module.impl.AutoShieldBreak;
import dev.vanguard.module.impl.AutoStunSlam;
import dev.vanguard.module.impl.AutoTap;
import dev.vanguard.module.impl.AutoTotem;
import dev.vanguard.module.impl.AutoWalk;
import dev.vanguard.module.impl.AutoXP;
import dev.vanguard.module.impl.BaseFinder;
import dev.vanguard.module.impl.Boatfly;
import dev.vanguard.module.impl.CartRefill;
import dev.vanguard.module.impl.CrystalOptimizer;
import dev.vanguard.module.impl.FastUse;
import dev.vanguard.module.impl.Flight;
import dev.vanguard.module.impl.FpsDisplay;
import dev.vanguard.module.impl.Freecam;
import dev.vanguard.module.impl.Fullbright;
import dev.vanguard.module.impl.Galaxy;
import dev.vanguard.module.impl.InstantLunge;
import dev.vanguard.module.impl.JumpCircles;
import dev.vanguard.module.impl.JumpReset;
import dev.vanguard.module.impl.PerformanceMode;
import dev.vanguard.module.impl.PingDisplay;
import dev.vanguard.module.impl.PlayerESP;
import dev.vanguard.module.impl.RangeRing;
import dev.vanguard.module.impl.Reach;
import dev.vanguard.module.impl.SafeAnchor;
import dev.vanguard.module.impl.SchematicBuilder;
import dev.vanguard.module.impl.Sprint;
import dev.vanguard.module.impl.SwordInspect;
import dev.vanguard.module.impl.Tracers;
import dev.vanguard.module.impl.TriggerBot;
import dev.vanguard.module.impl.TunnelESP;
import dev.vanguard.module.impl.Xray;
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

        add(new AutoHitCrystal());
        add(new CrystalOptimizer());
        add(new AnchorMacro());
        add(new SafeAnchor());
        add(new AutoCart());
        add(new CartRefill());
        add(new AutoTotem());
        add(new AutoRefill());
        add(new AutoXP());
        add(new AutoDrain());
        add(new TriggerBot());
        add(new Reach());
        add(new AutoShieldBreak());
        add(new AutoStunSlam());
        add(new AutoMace());
        add(new InstantLunge());
        add(new AutoTap());
        add(new JumpReset());
        add(new Sprint());
        add(new AutoWalk());
        add(new Flight());
        add(new Boatfly());
        add(new AirWalk());
        add(new AutoMLG());
        add(new AutoPearlCatch());
        add(new Freecam());
        add(new BaseFinder());
        add(new SchematicBuilder());
        add(new Xray());
        add(new TunnelESP());
        add(new AmethystNotifier());
        add(new Fullbright());
        add(new Galaxy());
        add(new SwordInspect());
        add(new PlayerESP());
        add(new Tracers());
        add(new JumpCircles());
        add(new RangeRing());
        add(new FpsDisplay());
        add(new PingDisplay());
        add(new AutoMine());
        add(new PerformanceMode());
        add(new FastUse());

        Config.load();
        ClientTickEvents.START_CLIENT_TICK.register(ModuleManager::preTick);
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

    private static void preTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;
        for (Module m : MODULES) {
            if (m.isEnabled()) m.onPreTick(player, level);
        }
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

    public static boolean isFriend(Player player) {
        return FRIENDS.contains(player.getName().getString().toLowerCase(Locale.ROOT));
    }
}
