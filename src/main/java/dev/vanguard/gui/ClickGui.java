package dev.vanguard.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanguard.Config;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dark, rounded menu: sidebar with a logo, feature search and two-line category entries; a header
 * with tabs; and a list of modules, each with an Enable button, a Settings button, tags and a
 * click-to-bind key. Clicks arrive through mouseClicked and are handled on the next render.
 */
public class ClickGui extends Screen {
    private static final int SIDE = 122;
    private static final int ROW_H = 40;
    private static final String[] TABS = {"All", "Enabled", "Disabled"};

    private final Minecraft mc = Minecraft.getInstance();
    private EditBox search;
    private Category selected = Category.COMBAT;
    private int tab;
    private Module settingsFor;
    private Module binding;
    private int scroll;
    private boolean pendingLeft;
    private boolean pendingRight;
    private int px, py, pw, ph;
    private String flash = "";
    private long flashUntil;

    private final ItemStack combatIcon = new ItemStack(Items.DIAMOND_SWORD);
    private final ItemStack movementIcon = new ItemStack(Items.FEATHER);
    private final ItemStack worldIcon = new ItemStack(Items.COMPASS);
    private final ItemStack visualsIcon = new ItemStack(Items.ENDER_EYE);
    private final ItemStack miscIcon = new ItemStack(Items.REDSTONE);
    private final ItemStack themeIcon = new ItemStack(Items.PAINTING);
    private final ItemStack saveIcon = new ItemStack(Items.WRITABLE_BOOK);

    public ClickGui() {
        super(Component.literal("Vanguard"));
    }

    @Override
    protected void init() {
        pw = Math.min(540, width - 16);
        ph = Math.min(320, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        search = new EditBox(font, px + 22, py + 47, SIDE - 40, 10, Component.literal("Search"));
        search.setBordered(false);
        search.setHint(Component.literal("Search features..."));
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
        scroll -= (int) (scrollY * 18);
        return true;
    }

    // ---- small text helpers (the game font only has one size, so scale it) ----

    private void small(GuiGraphics g, String text, int x, int y, int color, float scale) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private void bold(GuiGraphics g, String text, int x, int y, int color, float scale) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.drawString(font, Component.literal(text).withStyle(ChatFormatting.BOLD), 0, 0, color, false);
        g.pose().popMatrix();
    }

    private int smallWidth(String text, float scale) {
        return (int) (font.width(text) * scale);
    }

    private void icon(GuiGraphics g, ItemStack stack, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(0.75f, 0.75f);
        g.renderItem(stack, 0, 0);
        g.pose().popMatrix();
    }

    /** Pill button. Returns true if it was clicked this frame. */
    private boolean button(GuiGraphics g, int x, int y, int w, int h, String label, boolean filled,
                           int mx, int my, boolean click) {
        int accent = Theme.accent();
        boolean hover = Draw.inside(mx, my, x, y, x + w, y + h);
        if (filled) {
            Draw.roundRect(g, x, y, x + w, y + h, 5, hover ? Draw.blend(accent, 0xFFFFFFFF, 0.85f) : accent);
        } else {
            Draw.roundBox(g, x, y, x + w, y + h, 5, hover ? 0xFF2E2E3C : Theme.CARD_BORDER, Theme.CARD);
        }
        int tw = font.width(label);
        g.drawString(font, label, x + (w - tw) / 2, y + (h - 8) / 2, filled ? 0xFFFFFFFF : Theme.TEXT, false);
        return hover && click;
    }

    private static String subtitle(Category c) {
        return switch (c) {
            case COMBAT -> "Crystals, anchors, PvP";
            case MOVEMENT -> "Get around faster";
            case WORLD -> "Find bases and ores";
            case VISUALS -> "See more, react faster";
            case MISC -> "Quality of life tools";
        };
    }

    private ItemStack iconFor(Category c) {
        return switch (c) {
            case COMBAT -> combatIcon;
            case MOVEMENT -> movementIcon;
            case WORLD -> worldIcon;
            case VISUALS -> visualsIcon;
            case MISC -> miscIcon;
        };
    }

    private List<Module> visible() {
        String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            boolean match = q.isEmpty()
                ? m.category == selected
                : m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q);
            if (!match) continue;
            if (tab == 1 && !m.isEnabled()) continue;
            if (tab == 2 && m.isEnabled()) continue;
            out.add(m);
        }
        return out;
    }

    private int enabledCount() {
        int n = 0;
        for (Module m : ModuleManager.all()) if (m.isEnabled()) n++;
        return n;
    }

    // ---- frame ----

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        boolean click = pendingLeft;
        boolean rclick = pendingRight;
        pendingLeft = false;
        pendingRight = false;

        g.fill(0, 0, width, height, 0x70000000);
        Draw.glow(g, px, py, px + pw, py + ph, 12, 0x000000);
        Draw.roundBox(g, px, py, px + pw, py + ph, 12, Theme.CARD_BORDER, Theme.BG);
        g.fill(px + SIDE, py + 10, px + SIDE + 1, py + ph - 10, Theme.CARD_BORDER);

        drawSidebar(g, mx, my, click);
        drawMain(g, mx, my, click, rclick);

        if (!flash.isEmpty() && System.currentTimeMillis() < flashUntil) {
            int w = font.width(flash);
            int cx = px + SIDE + (pw - SIDE) / 2;
            Draw.roundBox(g, cx - w / 2 - 8, py + ph - 30, cx + w / 2 + 8, py + ph - 14, 6, Theme.accent(), 0xF0000000);
            g.drawString(font, flash, cx - w / 2, py + ph - 26, Theme.TEXT, false);
        }
        search.render(g, mx, my, delta);
    }

    private void drawSidebar(GuiGraphics g, int mx, int my, boolean click) {
        int accent = Theme.accent();

        // Logo and name
        Draw.roundRect(g, px + 12, py + 12, px + 34, py + 34, 7, accent);
        g.drawString(font, Component.literal("V").withStyle(ChatFormatting.BOLD), px + 20, py + 19, 0xFFFFFFFF, false);
        bold(g, "Vanguard", px + 42, py + 13, Theme.TEXT, 1.0f);
        small(g, "Utility client 0.8", px + 42, py + 25, Theme.DIM, 0.75f);

        // Search pill
        Draw.roundBox(g, px + 10, py + 42, px + SIDE - 10, py + 58, 6, Theme.CARD_BORDER, Theme.CARD);
        small(g, "o", px + 15, py + 46, Theme.DIM, 1.0f);

        // Navigation
        int avail = ph - 66 - 46;
        int itemH = Math.max(20, Math.min(28, avail / 7));
        boolean twoLine = itemH >= 24;
        int ny = py + 64;
        for (Category c : Category.values()) {
            boolean sel = c == selected && search.getValue().isEmpty();
            if (navItem(g, px + 6, ny, SIDE - 12, itemH, iconFor(c), c.label, subtitle(c), twoLine, sel, mx, my, click)) {
                selected = c;
                search.setValue("");
                scroll = 0;
                settingsFor = null;
            }
            ny += itemH;
        }
        ny += 4;
        if (navItem(g, px + 6, ny, SIDE - 12, itemH, themeIcon, "Themes", "Choose your color", twoLine, false, mx, my, click)) {
            Theme.next();
        }
        ny += itemH;
        if (navItem(g, px + 6, ny, SIDE - 12, itemH, saveIcon, "Save config", "Keep your settings", twoLine, false, mx, my, click)) {
            Config.save();
            flash = "Config saved";
            flashUntil = System.currentTimeMillis() + 1500;
        }

        // Status card
        int cy1 = py + ph - 40;
        Draw.roundBox(g, px + 10, cy1, px + SIDE - 10, py + ph - 10, 7, Theme.CARD_BORDER, Theme.CARD);
        Draw.roundRect(g, px + 17, cy1 + 12, px + 22, cy1 + 17, 2, 0xFF3DFF8A);
        bold(g, "Vanguard", px + 28, cy1 + 7, Theme.TEXT, 0.9f);
        small(g, enabledCount() + " modules enabled", px + 28, cy1 + 18, Theme.DIM, 0.72f);
    }

    private boolean navItem(GuiGraphics g, int x, int y, int w, int h, ItemStack icon, String title, String sub,
                            boolean twoLine, boolean selected, int mx, int my, boolean click) {
        int accent = Theme.accent();
        boolean hover = Draw.inside(mx, my, x, y, x + w, y + h);
        if (selected) {
            Draw.roundRect(g, x, y, x + w, y + h - 2, 6, Draw.alpha(accent, 40));
            g.fill(x, y + 5, x + 2, y + h - 7, accent);
        } else if (hover) {
            Draw.roundRect(g, x, y, x + w, y + h - 2, 6, 0x16FFFFFF);
        }
        int boxY = y + (h - 2 - 18) / 2;
        Draw.roundRect(g, x + 7, boxY, x + 25, boxY + 18, 5, selected ? Draw.alpha(accent, 70) : 0xFF16161F);
        icon(g, icon, x + 10, boxY + 3);
        if (twoLine) {
            g.drawString(font, title, x + 31, y + 4, selected ? Theme.TEXT : 0xFFC9C9D6, false);
            small(g, sub, x + 31, y + 15, Theme.DIM, 0.7f);
        } else {
            g.drawString(font, title, x + 31, y + (h - 2 - 8) / 2, selected ? Theme.TEXT : 0xFFC9C9D6, false);
        }
        return hover && click;
    }

    private void drawMain(GuiGraphics g, int mx, int my, boolean click, boolean rclick) {
        int accent = Theme.accent();
        int left = px + SIDE + 18;
        int right = px + pw - 18;
        boolean searching = !search.getValue().trim().isEmpty();
        List<Module> mods = visible();

        // Header
        bold(g, searching ? "Search" : selected.label, left, py + 13, Theme.TEXT, 1.4f);
        small(g, searching ? mods.size() + " matches" : subtitle(selected), left, py + 31, Theme.DIM, 0.8f);

        // Tabs
        int tx = left;
        int ty = py + 46;
        for (int i = 0; i < TABS.length; i++) {
            int w = font.width(TABS[i]);
            boolean active = i == tab;
            g.drawString(font, TABS[i], tx, ty, active ? Theme.TEXT : Theme.DIM, false);
            if (active) g.fill(tx - 2, ty + 12, tx + w + 2, ty + 14, accent);
            if (click && Draw.inside(mx, my, tx - 4, ty - 3, tx + w + 4, ty + 16)) {
                tab = i;
                scroll = 0;
            }
            tx += w + 18;
        }
        String count = mods.size() + " modules";
        small(g, count, right - smallWidth(count, 0.8f), ty + 1, Theme.DIM, 0.8f);
        g.fill(px + SIDE + 10, py + 64, px + pw - 10, py + 65, 0xFF14141C);

        // List
        int ly1 = py + 66;
        int ly2 = py + ph - 14;
        int rx1 = px + SIDE + 12;
        int rx2 = px + pw - 14;
        int contentH = mods.size() * ROW_H;
        int maxScroll = Math.max(0, contentH - (ly2 - ly1));
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        int[] panel = null;
        if (settingsFor != null) {
            int rowsN = settingsFor.settings().size();
            int w = 176;
            int h = 24 + Math.max(1, rowsN) * 16;
            int x = px + pw - 14 - w;
            int y = Math.max(py + 70, py + ph - 14 - h);
            panel = new int[] {x, y, x + w, y + h};
        }
        boolean overPanel = panel != null && Draw.inside(mx, my, panel[0], panel[1], panel[2], panel[3]);

        g.enableScissor(rx1, ly1, rx2 + 4, ly2);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int y = ly1 + i * ROW_H - scroll;
            if (y + ROW_H < ly1 || y > ly2) continue;

            boolean hover = !overPanel && Draw.inside(mx, my, rx1, Math.max(y, ly1), rx2, Math.min(y + ROW_H, ly2));
            if (hover) {
                Draw.roundRect(g, rx1, y + 1, rx2, y + ROW_H - 2, 7, 0xFF0F0F17);
                if (rclick) settingsFor = settingsFor == m ? null : m;
            }
            g.fill(rx1 + 6, y + ROW_H - 1, rx2 - 6, y + ROW_H, 0xFF14141C);

            // Buttons (right side)
            int b2x = rx2 - 8 - 46;
            int b1x = b2x - 4 - 46;
            boolean on = m.isEnabled();
            if (button(g, b1x, y + 7, 46, 15, on ? "Enabled" : "Enable", on, mx, my, click && !overPanel)) m.toggle();
            if (button(g, b2x, y + 7, 46, 15, "Settings", false, mx, my, click && !overPanel)) {
                settingsFor = settingsFor == m ? null : m;
            }

            // Name and description
            int textRight = b1x - 8;
            bold(g, m.name, rx1 + 8, y + 6, Theme.TEXT, 1.0f);
            String desc = font.plainSubstrByWidth(m.description, (int) ((textRight - (rx1 + 8)) / 0.78f));
            small(g, desc, rx1 + 8, y + 18, Theme.DIM, 0.78f);

            // Tag chip (category) and key bind
            String tag = m.category.label.toLowerCase(Locale.ROOT);
            int cw = smallWidth(tag, 0.7f) + 10;
            Draw.roundBox(g, rx1 + 8, y + 27, rx1 + 8 + cw, y + 37, 4, Draw.alpha(accent, 110), Draw.alpha(accent, 22));
            small(g, tag, rx1 + 13, y + 29, accent, 0.7f);
            String settingsText = m.settings().size() + (m.settings().size() == 1 ? " setting" : " settings");
            small(g, settingsText, rx1 + 8 + cw + 8, y + 29, Theme.DIM, 0.7f);

            String keyText = m.key != null ? m.key.getTranslatedKeyMessage().getString() : "None";
            String bindText = binding == m ? "Press a key... (Del clears)" : "Bind: " + keyText;
            int bindW = smallWidth(bindText, 0.75f);
            int bindX = rx2 - 10 - bindW;
            boolean overBind = !overPanel && Draw.inside(mx, my, bindX - 3, y + 25, rx2 - 6, y + 38);
            small(g, bindText, bindX, y + 29, binding == m ? accent : (overBind ? Theme.TEXT : Theme.DIM), 0.75f);
            if (overBind && click) binding = m;
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int trackH = ly2 - ly1;
            int barH = Math.max(14, trackH * trackH / contentH);
            int barY = ly1 + (trackH - barH) * scroll / maxScroll;
            Draw.roundRect(g, px + pw - 8, barY, px + pw - 5, barY + barH, 1, Draw.alpha(accent, 150));
        }

        if (mods.isEmpty()) {
            String none = "Nothing here";
            small(g, none, left, ly1 + 14, Theme.DIM, 0.9f);
        }
        if (panel != null) drawSettings(g, panel, mx, my, click, rclick);
    }

    private void drawSettings(GuiGraphics g, int[] p, int mx, int my, boolean click, boolean rclick) {
        int accent = Theme.accent();
        Module m = settingsFor;
        Draw.glow(g, p[0], p[1], p[2], p[3], 8, 0x000000);
        Draw.roundBox(g, p[0], p[1], p[2], p[3], 8, Draw.alpha(accent, 150), 0xFF0B0B12);
        bold(g, m.name + " settings", p[0] + 9, p[1] + 7, Theme.TEXT, 0.95f);
        boolean overX = Draw.inside(mx, my, p[2] - 18, p[1] + 4, p[2] - 6, p[1] + 16);
        g.drawString(font, "x", p[2] - 14, p[1] + 7, overX ? accent : Theme.DIM, false);
        if (overX && click) {
            settingsFor = null;
            return;
        }
        if (Draw.inside(mx, my, p[0], p[1], p[2], p[3]) && rclick) {
            settingsFor = null;
            return;
        }
        if (m.settings().isEmpty()) {
            small(g, "No settings", p[0] + 9, p[1] + 26, Theme.DIM, 0.9f);
            return;
        }
        int y = p[1] + 22;
        int rx = p[2] - 9;
        for (Map.Entry<String, Double> e : new ArrayList<>(m.settings().entrySet())) {
            double val = e.getValue();
            g.drawString(font, e.getKey(), p[0] + 9, y + 2, 0xFFC9C9D6, false);

            boolean overMinus = Draw.inside(mx, my, rx - 62, y, rx - 50, y + 12);
            boolean overPlus = Draw.inside(mx, my, rx - 12, y, rx, y + 12);
            Draw.roundRect(g, rx - 62, y, rx - 50, y + 12, 3, overMinus ? 0xFF2E2E3C : Theme.CARD_BORDER);
            Draw.roundRect(g, rx - 12, y, rx, y + 12, 3, overPlus ? 0xFF2E2E3C : Theme.CARD_BORDER);
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
