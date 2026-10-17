package dev.vanguard.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanguard.gui.Draw;
import dev.vanguard.gui.Theme;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * CS:GO-style inspect, drawn as a HUD overlay: press the inspect key and the item in your hand
 * slides in, tilts and flips, then slides out. It is purely visual and client-side.
 */
public class SwordInspect extends Module {
    private static final long DURATION_MS = 2800;
    private static KeyMapping inspectKey;
    private static long startMs = -1;

    public SwordInspect() {
        super("SwordInspect", Category.VISUALS, "Press the Inspect key to show off your held item");
    }

    public static void registerKey() {
        inspectKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.vanguard.inspect", InputConstants.Type.KEYSYM, InputConstants.KEY_I, ModuleManager.CATEGORY));
    }

    @Override
    protected void onEnable() {
        if (inspectKey != null) while (inspectKey.consumeClick()) { }
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (inspectKey == null) return;
        while (inspectKey.consumeClick()) {
            if (!player.getMainHandItem().isEmpty()) startMs = Util.getMillis();
        }
    }

    private static float easeOut(float t) {
        float u = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - u * u * u;
    }

    private static float easeInOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    /** Called from the HUD every frame. */
    public static void render(GuiGraphics g, Minecraft mc) {
        Module self = ModuleManager.find("SwordInspect");
        if (startMs < 0 || self == null || !self.isEnabled() || mc.player == null) return;

        long now = Util.getMillis();
        float t = (now - startMs) / (float) DURATION_MS;
        if (t >= 1f) {
            startMs = -1;
            return;
        }
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) return;

        // 0..1 presence: slides in over the first 18%, out over the last 14%
        float presence = 1f;
        if (t < 0.18f) presence = easeOut(t / 0.18f);
        else if (t > 0.86f) presence = 1f - easeOut((t - 0.86f) / 0.14f) ;

        float u = Math.max(0f, Math.min(1f, (t - 0.18f) / 0.68f));
        float spin = easeInOut(u) * (float) (Math.PI * 3.0);        // one and a half turns
        float flip = Math.max(0.10f, Math.abs((float) Math.cos(spin)));
        float tilt = (float) Math.toRadians(-28.0 + 9.0 * Math.sin(u * Math.PI * 2.0));
        float bob = (float) Math.sin(now / 260.0) * 2.5f;

        float cx = g.guiWidth() - 74 + (1f - presence) * 150f;
        float cy = g.guiHeight() - 82 + bob;
        float scale = 5.5f;

        int accent = Theme.accent();
        int a = (int) (presence * 255);

        // soft shadow under the item, shrinking with the flip
        int sw = (int) (34 * flip);
        Draw.roundRect(g, (int) cx - sw, (int) cy + 50, (int) cx + sw, (int) cy + 55, 2, Draw.alpha(0x000000, (int) (presence * 110)));

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate(tilt);
        g.pose().scale(scale * flip, scale);
        g.renderItem(stack, -8, -8);
        g.pose().popMatrix();

        // name plate
        String name = stack.getHoverName().getString();
        int w = mc.font.width(name);
        int px = (int) cx - w / 2;
        int py = (int) cy + 62;
        Draw.roundRect(g, px - 6, py - 3, px + w + 6, py + 12, 4, Draw.alpha(0x000000, (int) (presence * 210)));
        g.fill(px - 4, py + 11, px + w + 4, py + 12, Draw.alpha(accent, a));
        g.drawString(mc.font, name, px, py, Draw.alpha(0xFFFFFF, Math.max(8, a)), false);
    }
}
