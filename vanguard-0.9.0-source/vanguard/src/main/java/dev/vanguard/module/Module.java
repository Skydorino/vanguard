package dev.vanguard.module;

import dev.vanguard.Hud;
import dev.vanguard.render.WorldDraw;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();

    public final String name;
    public final Category category;
    public final String description;
    public KeyMapping key;

    private boolean enabled;
    private final Map<String, Double> settings = new LinkedHashMap<>();
    private final Map<String, Double> defaults = new LinkedHashMap<>();

    protected Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    /** Declare a numeric setting. Names are lowercase so commands are easy to type. */
    protected void setting(String settingName, double defaultValue) {
        settings.put(settingName.toLowerCase(Locale.ROOT), defaultValue);
        defaults.put(settingName.toLowerCase(Locale.ROOT), defaultValue);
    }

    public double defaultOf(String settingName) {
        return defaults.getOrDefault(settingName.toLowerCase(Locale.ROOT), 0.0);
    }

    public double get(String settingName) {
        return settings.getOrDefault(settingName.toLowerCase(Locale.ROOT), 0.0);
    }

    public boolean set(String settingName, double value) {
        String k = settingName.toLowerCase(Locale.ROOT);
        if (!settings.containsKey(k)) return false;
        settings.put(k, value);
        return true;
    }

    public Map<String, Double> settings() {
        return settings;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
        if (mc.player != null && Hud.visible) {
            mc.player.displayClientMessage(
                Component.literal("[Vanguard] " + name + (value ? " enabled" : " disabled")), true);
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public String hudLabel() {
        return name;
    }

    protected void onEnable() {}

    protected void onDisable() {}

    /** Called at the very start of each client tick, before the game reads clicks and keys. */
    public void onPreTick(LocalPlayer player, ClientLevel level) {}

    /** Called every client tick while enabled and in a world. */
    public void onTick(LocalPlayer player, ClientLevel level) {}

    /** Called every frame while enabled; draw boxes, lines and rings in the 3D world. */
    public void onWorldRender(WorldDraw draw) {}
}
