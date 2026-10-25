package dev.vanguard;

import dev.vanguard.gui.Draw;
import dev.vanguard.gui.Theme;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import dev.vanguard.module.impl.BaseFinder;
import dev.vanguard.module.impl.SpotifyHud;
import dev.vanguard.module.impl.SusChunks;
import dev.vanguard.module.impl.SwordInspect;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * In-game overlay: black pills with an accent edge. The lists, counts and strings are rebuilt a
 * few times a second rather than every frame, so drawing the HUD stays cheap.
 */
public final class Hud {
    private static final int PILL = 0xD0000000;

    /** False hides the watermark, module list, counters, FPS/ping and base info. Saved in the config. */
    public static boolean visible = true;

    private static int frames;
    private static int fps;
    private static long fpsStamp;
    private static long cacheStamp;
    private static long spotifyStamp;

    // Rebuilt every ~50 ms
    private static final List<Module> ACTIVE = new ArrayList<>();
    private static final List<String> LABELS = new ArrayList<>();
    private static final List<Integer> WIDTHS = new ArrayList<>();
    private static String fpsText;
    private static String pingText;
    private static String countsText = "";
    private static int countsWidth;
    private static String baseText;
    private static int baseWidth;
    private static String susText;
    private static int susWidth;

    private static String spotifyArtist = "";
    private static String spotifyTitle;
    private static int spotifyW;
    private static int spotifyArtistW;
    private static int spotifyY;
    private static boolean spotifyProgress;

    private Hud() {}

    public static void init() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(VanguardClient.MOD_ID, "hud"),
            Hud::render);
    }

    private static void infoPill(GuiGraphics g, Font font, String text, int y, int accent) {
        Draw.pill(g, 4, y, font.width(text) + 16, y + font.lineHeight + 6, PILL);
        g.fill(5, y + 3, 7, y + font.lineHeight + 3, accent);
        g.drawString(font, text, 12, y + 3, 0xFFFFFFFF, false);
    }

    private static void updateSpotify(Minecraft mc, Font font) {
        spotifyTitle = null;
        Module sp = ModuleManager.find("SpotifyHud");
        if (!(sp instanceof SpotifyHud spotify) || !spotify.isEnabled()) return;
        String t = SpotifyHud.title();
        if (t.isEmpty() || !(SpotifyHud.playing() || spotify.get("showpaused") > 0)) return;

        String a = SpotifyHud.artist();
        String sep = a.isEmpty() ? "" : "  -  ";
        int maxPx = (int) (mc.getWindow().getGuiScaledWidth() * Math.max(10.0, Math.min(90.0, spotify.get("maxwidth"))) / 100.0);
        int avail = Math.max(40, maxPx - 30);
        if (font.width(a + sep + t) > avail) {
            if (avail - font.width(a + sep) < 30) a = font.plainSubstrByWidth(a, avail / 3);
            t = font.plainSubstrByWidth(t, Math.max(10, avail - font.width(a + sep)));
        }
        spotifyArtist = a + sep;
        spotifyTitle = t;
        spotifyArtistW = font.width(spotifyArtist);
        spotifyW = spotifyArtistW + font.width(t);
        spotifyY = (int) spotify.get("y");
        spotifyProgress = spotify.get("progress") > 0;
    }

    private static void drawSpotify(GuiGraphics g, Font font, int accent, long nowMs) {
        if (spotifyTitle == null) return;
        int w = spotifyW + 30;
        int x1 = (g.guiWidth() - w) / 2;
        int y1 = spotifyY;
        int h = font.lineHeight + 9;
        Draw.pill(g, x1, y1, x1 + w, y1 + h, PILL);

        boolean playing = SpotifyHud.playing();
        for (int b = 0; b < 3; b++) { // tiny equalizer
            int bh = playing ? 3 + (int) (4 * (0.5 + 0.5 * Math.sin(nowMs / 140.0 + b * 1.7))) : 3;
            int bx = x1 + 8 + b * 3;
            g.fill(bx, y1 + 11 - bh, bx + 2, y1 + 11, accent);
        }
        int tx = x1 + 22;
        g.drawString(font, spotifyArtist, tx, y1 + 4, 0xFF9A9AAE, false);
        g.drawString(font, spotifyTitle, tx + spotifyArtistW, y1 + 4, 0xFFFFFFFF, false);

        long len = SpotifyHud.lengthMs();
        if (spotifyProgress && len > 0 && SpotifyHud.positionMs() >= 0) {
            long pos = SpotifyHud.positionMs() + (playing ? nowMs - SpotifyHud.sampleAt() : 0);
            float ratio = Math.max(0f, Math.min(1f, pos / (float) len));
            g.fill(x1 + 7, y1 + h - 3, x1 + w - 7, y1 + h - 2, 0xFF22222E);
            g.fill(x1 + 7, y1 + h - 3, x1 + 7 + (int) ((w - 14) * ratio), y1 + h - 2, accent);
        }
    }

    private static void rebuild(Minecraft mc, LocalPlayer player, Font font) {
        ACTIVE.clear();
        for (Module m : ModuleManager.all()) {
            if (m.isEnabled()) ACTIVE.add(m);
        }
        ACTIVE.sort(Comparator.comparingInt((Module m) -> -font.width(m.hudLabel())));
        LABELS.clear();
        WIDTHS.clear();
        for (Module m : ACTIVE) {
            String label = m.hudLabel();
            LABELS.add(label);
            WIDTHS.add(font.width(label));
        }

        Module fpsModule = ModuleManager.find("FpsDisplay");
        fpsText = fpsModule != null && fpsModule.isEnabled() ? "FPS " + fps : null;
        Module pingModule = ModuleManager.find("PingDisplay");
        if (pingModule != null && pingModule.isEnabled()) {
            PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(player.getUUID()) : null;
            pingText = info != null ? "Ping " + info.getLatency() + " ms" : "Ping --";
        } else {
            pingText = null;
        }

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
        countsText = "Totems " + totems + "   Crystals " + crystals;
        countsWidth = font.width(countsText);

        baseText = null;
        Module bf = ModuleManager.find("BaseFinder");
        if (bf instanceof BaseFinder finder && finder.isEnabled()) {
            BaseFinder.Hit h = finder.nearest(player);
            if (h != null) {
                double dx = h.x() - player.getX();
                double dz = h.z() - player.getZ();
                baseText = "Base " + h.x() + ", " + h.z() + "  (" + (int) Math.sqrt(dx * dx + dz * dz) + "m)";
                baseWidth = font.width(baseText);
            }
        }

        susText = null;
        Module sc = ModuleManager.find("SusChunks");
        if (sc instanceof SusChunks sus && sus.isEnabled()) {
            SusChunks.Sus s = sus.nearest(player);
            if (s != null) {
                double dx = s.x() - player.getX();
                double dz = s.z() - player.getZ();
                susText = "Sus chunk " + s.x() + ", " + s.z() + "  (" + (int) Math.sqrt(dx * dx + dz * dz) + "m)";
                susWidth = font.width(susText);
            }
        }
    }

    private static void render(GuiGraphics g, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        Font font = mc.font;
        int accent = Theme.accent();
        long nowMs = System.currentTimeMillis();

        if (nowMs - spotifyStamp >= 50) {
            spotifyStamp = nowMs;
            updateSpotify(mc, font);
        }
        if (!visible) {
            drawSpotify(g, font, accent, nowMs);   // you switched it on yourself, so it stays
            SwordInspect.render(g, mc);
            return;
        }

        frames++;
        if (nowMs - fpsStamp >= 1000) {
            fps = frames;
            frames = 0;
            fpsStamp = nowMs;
        }
        if (nowMs - cacheStamp >= 50) {
            cacheStamp = nowMs;
            rebuild(mc, player, font);
        }

        // Watermark
        Draw.pill(g, 4, 4, font.width("VANGUARD") + 18, 18, PILL);
        g.fill(5, 7, 7, 15, accent);
        g.drawString(font, "VANGUARD", 12, 7, 0xFFFFFFFF, false);

        // FPS and ping pills under the watermark
        int infoY = 22;
        if (fpsText != null) {
            infoPill(g, font, fpsText, infoY, accent);
            infoY += font.lineHeight + 8;
        }
        if (pingText != null) infoPill(g, font, pingText, infoY, accent);

        // Enabled modules, right-aligned, longest first
        int y = 4;
        int x2 = g.guiWidth() - 4;
        for (int i = 0; i < ACTIVE.size(); i++) {
            int x1 = x2 - WIDTHS.get(i) - 12;
            Draw.pill(g, x1, y, x2, y + font.lineHeight + 4, PILL);
            g.fill(x2 - 3, y + 2, x2 - 1, y + font.lineHeight + 2, accent);
            g.drawString(font, LABELS.get(i), x1 + 5, y + 3, 0xFFFFFFFF, false);
            y += font.lineHeight + 6;
        }

        // Totem and crystal counts, bottom-left
        int by = g.guiHeight() - font.lineHeight - 10;
        Draw.pill(g, 4, by, countsWidth + 16, by + font.lineHeight + 6, PILL);
        g.fill(5, by + 3, 7, by + font.lineHeight + 3, accent);
        g.drawString(font, countsText, 12, by + 3, 0xFFFFFFFF, false);

        // Nearest flagged base and sus chunk
        int ty = by - font.lineHeight - 10;
        if (baseText != null) {
            Draw.pill(g, 4, ty, baseWidth + 16, ty + font.lineHeight + 6, PILL);
            g.fill(5, ty + 3, 7, ty + font.lineHeight + 3, accent);
            g.drawString(font, baseText, 12, ty + 3, 0xFFFFFFFF, false);
            ty -= font.lineHeight + 8;
        }
        if (susText != null) {
            Draw.pill(g, 4, ty, susWidth + 16, ty + font.lineHeight + 6, PILL);
            g.fill(5, ty + 3, 7, ty + font.lineHeight + 3, accent);
            g.drawString(font, susText, 12, ty + 3, 0xFFFFFFFF, false);
        }

        drawSpotify(g, font, accent, nowMs);
        SwordInspect.render(g, mc);
    }
}
