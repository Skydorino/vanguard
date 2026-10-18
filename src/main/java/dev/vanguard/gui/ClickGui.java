package dev.vanguard.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanguard.Config;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Black module menu: category sidebar, header with search, two-column module cards with a
 * toggle, and a right-click settings panel. Clicks arrive through mouseClicked and are handled
 * on the next render, where the card layout is known.
 */
public class ClickGui extends Screen {
    private static final int SIDEBAR = 92;
    private static final int CARD_H = 28;
    private static final int GAP = 6;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Minecraft mc = Minecraft.getInstance();
    private EditBox search;
    private Category selected = Category.COMBAT;
    private Module settingsFor;
    private Module binding;
    private int scroll;
    private boolean pendingLeft;
    private boolean pendingRight;
    private int px, py, pw, ph, cx1, cx2;
    private String flash = "";
    private long flashUntil;

    public ClickGui() {
        super(Component.literal("Vanguard"));
    }

    @Override
    protected void init() {
        pw = Math.min(450, width - 16);
        ph = Math.min(262, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx1 = px + SIDEBAR + 8;
        cx2 = px + pw - 6;

        search = new EditBox(font, cx2 - 100, py + 13, 86, 12, Component.literal("Search"));
        search.setBordered(false);
        search.setHint(Component.literal("Search modules..."));
        search.setMaxLength(24);
        addWidget(search);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Config.save();
        super.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) pendingLeft = true;
        if (event.button() == 1) pendingRight = true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (binding != null) {
            int key = event.key();
            if (key == InputConstants.KEY_ESCAPE) {
                binding = null;
            } else {
                if (key == InputConstants.KEY_DELETE || key == InputConstants.KEY_BACKSPACE) {
                    binding.key.setKey(InputConstants.UNKNOWN);
                } else {
                    binding.key.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
                }
                KeyMapping.resetMapping();
                mc.options.save();
                binding = null;
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) (scrollY * 16);
        return true;
    }

    private List<Module> visible() {
        String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            boolean match = q.isEmpty() ? m.category == selected : m.name.toLowerCase(Locale.ROOT).contains(q);
            if (match) out.add(m);
        }
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        boolean click = pendingLeft;
        boolean rclick = pendingRight;
        pendingLeft = false;
        pendingRight = false;

        int accent = Theme.accent();
        g.fill(0, 0, width, height, 0xB0000000);
        Draw.glow(g, px, py, px + pw, py + ph, 10, accent);
        Draw.roundRect(g, px, py, px + pw, py + ph, 10, Theme.BG);

        drawSidebar(g, mx, my, click);
        drawHeader(g);
        drawModules(g, mx, my, click, rclick);

        if (!flash.isEmpty() && System.currentTimeMillis() < flashUntil) {
            int w = font.width(flash);
            Draw.roundRect(g, px + pw / 2 - w / 2 - 6, py + ph - 24, px + pw / 2 + w / 2 + 6, py + ph - 8, 5, 0xF0000000);
            g.drawString(font, flash, px + pw / 2 - w / 2, py + ph - 20, accent, false);
        }
        search.render(g, mx, my, delta);
    }

    private void drawSidebar(GuiGraphics g, int mx, int my, boolean click) {
        int accent = Theme.accent();
        int x1 = px + 6;
        int y1 = py + 6;
        int x2 = px + SIDEBAR;
        int y2 = py + ph - 6;
        Draw.roundRect(g, x1, y1, x2, y2, 8, Theme.PANEL);

        // Logo
        Draw.roundRect(g, x1 + 8, y1 + 8, x1 + 26, y1 + 26, 9, accent);
        g.drawString(font, "V", x1 + 14, y1 + 13, 0xFF000000, false);
        g.drawString(font, Component.literal("VANGUARD").withStyle(ChatFormatting.BOLD), x1 + 31, y1 + 10, Theme.TEXT, false);
        g.drawString(font, "v0.5.3", x1 + 31, y1 + 20, Theme.DIM, false);

        int y = y1 + 36;
        g.drawString(font, "MODULES", x1 + 8, y, Theme.DIM, false);
        y += 13;
        for (Category c : Category.values()) {
            boolean sel = c == selected && search.getValue().isEmpty();
            boolean hover = Draw.inside(mx, my, x1 + 4, y, x2 - 4, y + 16);
            if (sel) {
                Draw.roundRect(g, x1 + 4, y, x2 - 4, y + 16, 5, Draw.alpha(accent, 48));
                g.fill(x1 + 4, y + 3, x1 + 6, y + 13, accent);
            } else if (hover) {
                Draw.roundRect(g, x1 + 4, y, x2 - 4, y + 16, 5, 0xFF16161D);
            }
            g.drawString(font, c.label, x1 + 14, y + 4, sel ? accent : Theme.TEXT, false);
            if (hover && click) {
                selected = c;
                search.setValue("");
                scroll = 0;
                settingsFor = null;
            }
            y += 18;
        }

        y += 8;
        g.drawString(font, "GENERAL", x1 + 8, y, Theme.DIM, false);
        y += 13;

        boolean hoverTheme = Draw.inside(mx, my, x1 + 4, y, x2 - 4, y + 16);
        if (hoverTheme) Draw.roundRect(g, x1 + 4, y, x2 - 4, y + 16, 5, 0xFF16161D);
        g.drawString(font, "Theme", x1 + 14, y + 4, Theme.TEXT, false);
        Draw.roundRect(g, x2 - 18, y + 4, x2 - 10, y + 12, 4, accent);
        if (hoverTheme && click) Theme.next();
        y += 18;

        boolean hoverCfg = Draw.inside(mx, my, x1 + 4, y, x2 - 4, y + 16);
        if (hoverCfg) Draw.roundRect(g, x1 + 4, y, x2 - 4, y + 16, 5, 0xFF16161D);
        g.drawString(font, "Save config", x1 + 14, y + 4, Theme.TEXT, false);
        if (hoverCfg && click) {
            Config.save();
            flash = "Config saved";
            flashUntil = System.currentTimeMillis() + 1500;
        }

        // Footer: player
        String name = mc.player != null ? mc.player.getName().getString() : "player";
        Draw.roundBox(g, x1 + 4, y2 - 28, x2 - 4, y2 - 4, 7, Theme.CARD_BORDER, Theme.CARD);
        g.drawString(font, name, x1 + 12, y2 - 20, Theme.TEXT, false);
        Draw.roundRect(g, x2 - 16, y2 - 19, x2 - 10, y2 - 13, 3, 0xFF3DFF8A);
    }

    private void drawHeader(GuiGraphics g) {
        int accent = Theme.accent();
        int y1 = py + 6;
        int y2 = py + 32;
        Draw.roundRect(g, cx1, y1, cx2, y2, 8, Theme.PANEL);
        String name = mc.player != null ? mc.player.getName().getString() : "player";
        g.drawString(font, "Hello, " + name, cx1 + 10, y1 + 9, Theme.TEXT, false);
        String time = LocalTime.now().format(TIME);
        g.drawString(font, time, (cx1 + cx2) / 2 - font.width(time) / 2 - 30, y1 + 9, accent, false);
        Draw.roundBox(g, cx2 - 112, y1 + 4, cx2 - 6, y2 - 4, 6, Theme.CARD_BORDER, Theme.CARD);
        g.fill(cx1 + 6, y2 - 2, cx2 - 6, y2 - 1, accent);
    }

    private void drawModules(GuiGraphics g, int mx, int my, boolean click, boolean rclick) {
        int accent = Theme.accent();
        int gy1 = py + 38;
        int gy2 = py + ph - 26;
        List<Module> mods = visible();

        int cw = (cx2 - cx1 - GAP - 6) / 2;
        int rows = (mods.size() + 1) / 2;
        int contentH = rows * (CARD_H + GAP);
        int maxScroll = Math.max(0, contentH - (gy2 - gy1));
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        // Settings panel geometry (drawn last, but its clicks are handled first)
        int[] panel = null;
        if (settingsFor != null) {
            int rowsN = settingsFor.settings().size();
            int w = 164;
            int h = 22 + Math.max(1, rowsN) * 16;
            int x = cx2 - w - 4;
            int y = gy2 - h;
            panel = new int[] {x, y, x + w, y + h};
        }
        boolean overPanel = panel != null && Draw.inside(mx, my, panel[0], panel[1], panel[2], panel[3]);

        Module hovered = null;
        g.enableScissor(cx1, gy1, cx2, gy2);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int x = cx1 + (i % 2) * (cw + GAP);
            int y = gy1 + (i / 2) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < gy1 || y > gy2) continue;

            boolean hover = !overPanel && Draw.inside(mx, my, x, Math.max(y, gy1), x + cw, Math.min(y + CARD_H, gy2));
            boolean overKey = hover && Draw.inside(mx, my, x + 6, y + 14, x + cw - 40, y + 26);
            if (hover) {
                hovered = m;
                if (click) {
                    if (overKey) binding = m;
                    else m.toggle();
                }
                if (rclick) settingsFor = settingsFor == m ? null : m;
            }

            boolean on = m.isEnabled();
            if (on) {
                Draw.glow(g, x, y, x + cw, y + CARD_H, 7, accent);
                Draw.roundBox(g, x, y, x + cw, y + CARD_H, 7, accent, Draw.blend(accent, Theme.CARD, 0.10f));
            } else {
                Draw.roundBox(g, x, y, x + cw, y + CARD_H, 7, hover ? 0xFF2C2C38 : Theme.CARD_BORDER, Theme.CARD);
            }

            g.drawString(font, m.name, x + 9, y + 5, Theme.TEXT, false);
            int iw = x + 9 + font.width(m.name) + 5;
            Draw.roundRect(g, iw, y + 5, iw + 8, y + 13, 4, Theme.DIM);
            g.drawString(font, "i", iw + 3, y + 5, Theme.BG, false);

            String keyText = m.key != null ? m.key.getTranslatedKeyMessage().getString() : "None";
            if (binding == m) {
                g.drawString(font, "Press a key... (Del clears)", x + 9, y + 16, accent, false);
            } else {
                g.drawString(font, "KeyBind: " + keyText, x + 9, y + 16, overKey ? Theme.TEXT : Theme.DIM, false);
            }

            // Toggle switch
            int sx2 = x + cw - 9;
            int sx1 = sx2 - 24;
            int sy1 = y + 9;
            Draw.roundRect(g, sx1, sy1, sx2, sy1 + 11, 5, on ? accent : Theme.OFF);
            int kx = on ? sx2 - 10 : sx1 + 1;
            Draw.roundRect(g, kx, sy1 + 1, kx + 9, sy1 + 10, 4, on ? 0xFFFFFFFF : 0xFF7A7A88);
        }
        g.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int trackH = gy2 - gy1;
            int barH = Math.max(16, trackH * trackH / contentH);
            int barY = gy1 + (trackH - barH) * scroll / maxScroll;
            Draw.roundRect(g, cx2 - 3, barY, cx2, barY + barH, 1, Draw.alpha(accent, 160));
        }

        // Footer text
        String foot = hovered != null ? hovered.description : "Click toggles. Click KeyBind to bind. Right-click for settings.";
        g.drawString(font, foot, cx1 + 4, py + ph - 17, Theme.DIM, false);

        if (panel != null) drawSettings(g, panel, mx, my, click, rclick);
    }

    private void drawSettings(GuiGraphics g, int[] p, int mx, int my, boolean click, boolean rclick) {
        int accent = Theme.accent();
        Module m = settingsFor;
        Draw.glow(g, p[0], p[1], p[2], p[3], 8, accent);
        Draw.roundBox(g, p[0], p[1], p[2], p[3], 8, accent, 0xFF0A0A0F);
        g.drawString(font, m.name + " settings", p[0] + 8, p[1] + 6, Theme.TEXT, false);
        // close button
        boolean overX = Draw.inside(mx, my, p[2] - 16, p[1] + 4, p[2] - 6, p[1] + 14);
        g.drawString(font, "x", p[2] - 13, p[1] + 6, overX ? accent : Theme.DIM, false);
        if (overX && click) {
            settingsFor = null;
            return;
        }
        if (Draw.inside(mx, my, p[0], p[1], p[2], p[3]) && rclick) {
            settingsFor = null;
            return;
        }

        if (m.settings().isEmpty()) {
            g.drawString(font, "No settings", p[0] + 8, p[1] + 22, Theme.DIM, false);
            return;
        }
        int y = p[1] + 20;
        int rx = p[2] - 8;
        for (Map.Entry<String, Double> e : new ArrayList<>(m.settings().entrySet())) {
            double val = e.getValue();
            g.drawString(font, e.getKey(), p[0] + 8, y + 2, Theme.TEXT, false);

            boolean overMinus = Draw.inside(mx, my, rx - 62, y, rx - 50, y + 12);
            boolean overPlus = Draw.inside(mx, my, rx - 12, y, rx, y + 12);
            Draw.roundRect(g, rx - 62, y, rx - 50, y + 12, 3, overMinus ? 0xFF2C2C38 : Theme.CARD_BORDER);
            Draw.roundRect(g, rx - 12, y, rx, y + 12, 3, overPlus ? 0xFF2C2C38 : Theme.CARD_BORDER);
            g.drawString(font, "-", rx - 58, y + 2, Theme.TEXT, false);
            g.drawString(font, "+", rx - 9, y + 2, Theme.TEXT, false);

            String text = fmt(val);
            g.drawString(font, text, rx - 32 - font.width(text) / 2, y + 2, accent, false);

            double step = Math.abs(val) >= 10 ? 1.0 : 0.5;
            if (click && overMinus) m.set(e.getKey(), Math.max(0.0, round1(val - step)));
            if (click && overPlus) m.set(e.getKey(), round1(val + step));
            y += 16;
        }
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}
// END OF FILE
