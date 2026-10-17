package dev.vanguard;

import dev.vanguard.gui.Draw;
import dev.vanguard.gui.Theme;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import dev.vanguard.module.impl.BaseFinder;
import dev.vanguard.module.impl.SwordInspect;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** In-game overlay: black rounded pills with an accent edge. */
public final class Hud {
    private static final int PILL = 0xD0000000;

    private Hud() {}

    public static void init() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(VanguardClient.MOD_ID, "hud"),
            Hud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        Font font = mc.font;
        int accent = Theme.accent();

        // Watermark
        String mark = "VANGUARD";
        int mw = font.width(mark);
        Draw.roundRect(g, 4, 4, mw + 18, 18, 5, PILL);
        g.fill(5, 7, 7, 15, accent);
        g.drawString(font, mark, 12, 7, 0xFFFFFFFF, false);

        // Enabled modules, right-aligned, longest first
        List<Module> active = ModuleManager.all().stream()
            .filter(Module::isEnabled)
            .sorted(Comparator.comparingInt((Module m) -> -font.width(m.hudLabel())))
            .toList();
        int y = 4;
        for (Module m : active) {
            String label = m.hudLabel();
            int w = font.width(label);
            int x2 = g.guiWidth() - 4;
            int x1 = x2 - w - 12;
            Draw.roundRect(g, x1, y, x2, y + font.lineHeight + 4, 4, PILL);
            g.fill(x2 - 3, y + 2, x2 - 1, y + font.lineHeight + 2, accent);
            g.drawString(font, label, x1 + 5, y + 3, 0xFFFFFFFF, false);
            y += font.lineHeight + 6;
        }

        // Totem and crystal counts, bottom-left
        int totems = 0;
        int crystals = 0;
        ItemStack off = player.getOffhandItem();
        if (off.is(Items.TOTEM_OF_UNDYING)) totems += off.getCount();
        if (off.is(Items.END_CRYSTAL)) crystals += off.getCount();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) totems += stack.getCount();
            else if (stack.is(Items.END_CRYSTAL)) crystals += stack.getCount();
        }
        String counts = "Totems " + totems + "   Crystals " + crystals;
        int by = g.guiHeight() - font.lineHeight - 10;
        Draw.roundRect(g, 4, by, font.width(counts) + 16, by + font.lineHeight + 6, 5, PILL);
        g.fill(5, by + 3, 7, by + font.lineHeight + 3, accent);
        g.drawString(font, counts, 12, by + 3, 0xFFFFFFFF, false);

        // Nearest flagged base
        Module bf = ModuleManager.find("BaseFinder");
        if (bf instanceof BaseFinder finder && finder.isEnabled()) {
            BaseFinder.Hit h = finder.nearest(player);
            if (h != null) {
                double dx = h.x() - player.getX();
                double dz = h.z() - player.getZ();
                String text = "Base " + h.x() + ", " + h.z() + "  (" + (int) Math.sqrt(dx * dx + dz * dz) + "m)";
                int ty = by - font.lineHeight - 10;
                Draw.roundRect(g, 4, ty, font.width(text) + 16, ty + font.lineHeight + 6, 5, PILL);
                g.fill(5, ty + 3, 7, ty + font.lineHeight + 3, accent);
                g.drawString(font, text, 12, ty + 3, 0xFFFFFFFF, false);
            }
        }

        SwordInspect.render(g, mc);
    }
}
