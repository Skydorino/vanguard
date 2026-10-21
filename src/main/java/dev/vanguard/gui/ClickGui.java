package dev.vanguard.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanguard.Config;
import dev.vanguard.Hud;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
 * Column menu: one rounded panel per category, each listing its modules. Left-click a module to
 * switch it on or off, right-click (or the arrow) to open its settings underneath. A bar at the
 * bottom holds themes and a few client buttons. Clicks arrive through mouseClicked and are handled
 * on the next render, where the layout is known.
 */
public class ClickGui extends Screen {
    private static final int GAP = 8;
    private static final int HEADER = 20;
    private static final int ROW = 16;
    private static final int TOGGLE_ROW = 14;
    private static final int NUMBER_ROW = 24;
    private static final String[] THEME_NAMES = {"Indigo", "Magenta", "Cyan", "Red", "Green", "Amber"};
    private static final Set<String> TOGGLES = Set.of(
        "totem", "selfprotect", "forward", "needfall", "blockclicks", "highlight", "see", "ores", "iron",
        "redstone", "lapis", "coal", "copper", "storage", "bases", "boxes", "auto", "sprint", "aimlock");
    private static final Set<String> SIGNED = Set.of("direction", "pitch", "yaw");

    private final Minecraft mc = Minecraft.getInstance();
    private final Set<Module> expanded = new HashSet<>();
    private final int[] colScroll = new int[Category.values().length];
    private EditBox search;
    private Module binding;
    private boolean pendingLeft;
    private boolean pendingRight;
    private int left, top, colW, panelH, barY;
    private String flash = "";
    private long flashUntil;

    // Filled in during render so scrolling knows what the mouse is over
    private Module hoverSettingModule;
    private String hoverSettingKey;
    private String hoverText;

    private final ItemStack[] icons = {
        new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.FEATHER), new ItemStack(Items.COMPASS),
        new ItemStack(Items.ENDER_EYE), new ItemStack(Items.REDSTONE)
    };

    public ClickGui() {
        super(Component.literal("Vanguard"));
    }

    @Override
    protected void init() {
        int cats = Category.values().length;
        colW = Math.max(80, Math.min(104, (width - 40 - (cats - 1) * GAP) / cats));
        int totalW = cats * colW + (cats - 1) * GAP;
        left = (width - totalW) / 2;
        panelH = Math.max(120, Math.min(250, height - 96));
        int blockTop = (height - (panelH + 50)) / 2;
        top = blockTop + 24;
        barY = top + panelH + 8;

        search = new EditBox(font, width / 2 - 58, blockTop + 3, 116, 10, Component.literal("Search"));
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
        if (hoverSettingModule != null && hoverSettingKey != null) {
            adjust(hoverSettingModule, hoverSettingKey, scrollY > 0 ? 1 : -1);
            return true;
        }
        for (int i = 0; i < colScroll.length; i++) {
            int x = left + i * (colW + GAP);
            if (mouseX >= x && mouseX < x + colW) {
                colScroll[i] -= (int) (scrollY * 14);
                return true;
            }
        }
        return true;
    }

    // ---- helpers ----

    private void small(GuiGraphics g, String text, int x, int y, int color, float scale) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private void smallBold(GuiGraphics g, String text, int x, int y, int color, float scale) {
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
        g.pose().scale(0.7f, 0.7f);
        g.renderItem(stack, 0, 0);
        g.pose().popMatrix();
    }

    private List<Module> modulesOf(Category cat) {
        String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (m.category != cat) continue;
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)
                && !m.description.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    private int settingsHeight(Module m) {
        int h = TOGGLE_ROW + 4; // bind row plus padding
        for (String key : m.settings().keySet()) h += TOGGLES.contains(key) ? TOGGLE_ROW : NUMBER_ROW;
        return h;
    }

    private double stepFor(Module m, String key) {
        double val = m.get(key);
        double def = m.defaultOf(key);
        boolean intLike = def == Math.floor(def) && val == Math.floor(val);
        if (intLike) return Math.abs(val) >= 100 ? 10.0 : 1.0;
        return Math.abs(val) < 2.0 ? 0.1 : 0.5;
    }

    private void adjust(Module m, String key, int dir) {
        double next = Math.round((m.get(key) + dir * stepFor(m, key)) * 100.0) / 100.0;
        if (!SIGNED.contains(key)) next = Math.max(0.0, next);
        m.set(key, next);
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v);
    }

    // ---- frame ----

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        boolean click = pendingLeft;
        boolean rclick = pendingRight;
        pendingLeft = false;
        pendingRight = false;
        hoverSettingModule = null;
        hoverSettingKey = null;
        hoverText = null;

        g.fill(0, 0, width, height, 0x38000000);

        // Search pill
        Draw.roundBox(g, width / 2 - 66, top - 22, width / 2 + 66, top - 8, 6, 0xFF22222E, 0xE60B0B12);

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) drawColumn(g, i, cats[i], mx, my, click, rclick);
        drawBar(g, mx, my, click);

        if (hoverText != null) {
            int w = smallWidth(hoverText, 0.85f);
            small(g, hoverText, (width - w) / 2, barY + 24, Theme.DIM, 0.85f);
        }
        if (!flash.isEmpty() && System.currentTimeMillis() < flashUntil) {
            int w = font.width(flash);
            Draw.roundBox(g, (width - w) / 2 - 8, barY - 24, (width + w) / 2 + 8, barY - 8, 6, Theme.accent(), 0xF0000000);
            g.drawString(font, flash, (width - w) / 2, barY - 20, Theme.TEXT, false);
        }
        search.render(g, mx, my, delta);
    }

    private void drawColumn(GuiGraphics g, int i, Category cat, int mx, int my, boolean click, boolean rclick) {
        int accent = Theme.accent();
        int x = left + i * (colW + GAP);

        Draw.roundBox(g, x, top, x + colW, top + panelH, 8, 0xFF1D1D29, 0xE60B0B12);
        Draw.roundRect(g, x + 1, top + 1, x + colW - 1, top + HEADER, 7, 0xFF0D0D15);
        int titleW = font.width(cat.label);
        int startX = x + (colW - (11 + 4 + titleW)) / 2;
        icon(g, icons[i], startX, top + 5);
        g.drawString(font, Component.literal(cat.label).withStyle(ChatFormatting.BOLD), startX + 15, top + 6, Theme.TEXT, false);
        g.fill(x + 8, top + HEADER, x + colW - 8, top + HEADER + 1, Draw.alpha(accent, 70));

        int bodyTop = top + HEADER + 3;
        int bodyBottom = top + panelH - 4;
        boolean overCol = mx >= x && mx < x + colW && my >= bodyTop && my < bodyBottom;

        List<Module> mods = modulesOf(cat);
        int contentH = 0;
        for (Module m : mods) contentH += ROW + (expanded.contains(m) ? settingsHeight(m) : 0);
        int maxScroll = Math.max(0, contentH - (bodyBottom - bodyTop));
        colScroll[i] = Math.max(0, Math.min(maxScroll, colScroll[i]));

        g.enableScissor(x + 2, bodyTop, x + colW - 2, bodyBottom);
        int y = bodyTop - colScroll[i];
        for (Module m : mods) {
            boolean rowHover = overCol && Draw.inside(mx, my, x + 3, y, x + colW - 3, y + ROW);
            boolean overArrow = rowHover && mx >= x + colW - 18;
            boolean on = m.isEnabled();
            boolean open = expanded.contains(m);

            if (rowHover) {
                Draw.roundRect(g, x + 3, y, x + colW - 3, y + ROW - 1, 5, 0xFF14141D);
                hoverText = m.name + ": " + m.description;
                if (click) {
                    if (overArrow) toggleOpen(m); else m.toggle();
                }
                if (rclick) toggleOpen(m);
            } else if (on) {
                Draw.roundRect(g, x + 3, y, x + colW - 3, y + ROW - 1, 5, Draw.alpha(accent, 26));
            }

            String label = m.name;
            float scale = Math.min(0.92f, (colW - 28) / (float) Math.max(1, font.width(label)));
            int tw = smallWidth(label, scale);
            int color = on ? accent : 0xFF9A9AAE;
            if (on) smallBold(g, label, x + (colW - 14 - tw) / 2 + 2, y + 4, color, scale);
            else small(g, label, x + (colW - 14 - tw) / 2 + 2, y + 4, color, scale);
            small(g, open ? "v" : ">", x + colW - 13, y + 4, Theme.DIM, 0.9f);
            y += ROW;

            if (open) y = drawSettings(g, m, x, y, overCol, mx, my, click);
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int trackH = bodyBottom - bodyTop;
            int barH = Math.max(12, trackH * trackH / contentH);
            int barPos = bodyTop + (trackH - barH) * colScroll[i] / maxScroll;
            Draw.roundRect(g, x + colW - 4, barPos, x + colW - 2, barPos + barH, 1, Draw.alpha(accent, 140));
        }
    }

    private void toggleOpen(Module m) {
        if (!expanded.remove(m)) expanded.add(m);
    }

    private int drawSettings(GuiGraphics g, Module m, int x, int y, boolean overCol, int mx, int my, boolean click) {
        int accent = Theme.accent();
        int h = settingsHeight(m);
        Draw.roundRect(g, x + 5, y, x + colW - 5, y + h - 2, 5, 0xFF08080D);

        // Key bind row
        boolean overBind = overCol && Draw.inside(mx, my, x + 5, y, x + colW - 5, y + TOGGLE_ROW);
        String keyText = m.key != null ? m.key.getTranslatedKeyMessage().getString() : "None";
        String bindText = binding == m ? "Press a key..." : "Bind: " + keyText;
        int bw = smallWidth(bindText, 0.78f);
        small(g, bindText, x + (colW - bw) / 2, y + 3, binding == m ? accent : (overBind ? Theme.TEXT : Theme.DIM), 0.78f);
        if (overBind && click) binding = m;
        y += TOGGLE_ROW;

        for (Map.Entry<String, Double> e : new ArrayList<>(m.settings().entrySet())) {
            String key = e.getKey();
            double val = e.getValue();

            if (TOGGLES.contains(key)) {
                boolean over = overCol && Draw.inside(mx, my, x + 5, y, x + colW - 5, y + TOGGLE_ROW);
                boolean on = val > 0;
                Draw.roundBox(g, x + 10, y + 3, x + 19, y + 12, 2, on ? accent : 0xFF2B2B38, on ? accent : 0xFF11111A);
                if (on) g.fill(x + 12, y + 6, x + 17, y + 9, 0xFFFFFFFF);
                small(g, key, x + 23, y + 3, over ? Theme.TEXT : 0xFFB9B9C8, 0.78f);
                if (over && click) m.set(key, on ? 0.0 : 1.0);
                y += TOGGLE_ROW;
            } else {
                boolean over = overCol && Draw.inside(mx, my, x + 5, y, x + colW - 5, y + NUMBER_ROW);
                if (over) {
                    hoverSettingModule = m;
                    hoverSettingKey = key;
                }
                small(g, key, x + 10, y + 2, 0xFFB9B9C8, 0.78f);
                String text = fmt(val);
                small(g, text, x + colW - 10 - smallWidth(text, 0.78f), y + 2, accent, 0.78f);

                // Track with arrows at each end
                int tx1 = x + 20;
                int tx2 = x + colW - 20;
                double def = m.defaultOf(key);
                double ratio = Math.max(0.0, Math.min(1.0, val / Math.max(1.0, def * 2.5)));
                Draw.roundRect(g, tx1, y + 14, tx2, y + 17, 1, 0xFF22222E);
                int fillX = tx1 + (int) ((tx2 - tx1) * ratio);
                if (fillX > tx1) Draw.roundRect(g, tx1, y + 14, fillX, y + 17, 1, accent);
                Draw.roundRect(g, Math.max(tx1, fillX - 2), y + 12, Math.min(tx2, fillX + 2), y + 19, 2, 0xFFFFFFFF);

                boolean overMinus = overCol && Draw.inside(mx, my, x + 7, y + 11, x + 18, y + 21);
                boolean overPlus = overCol && Draw.inside(mx, my, x + colW - 18, y + 11, x + colW - 7, y + 21);
                small(g, "<", x + 9, y + 12, overMinus ? accent : Theme.DIM, 0.95f);
                small(g, ">", x + colW - 15, y + 12, overPlus ? accent : Theme.DIM, 0.95f);
                if (click && overMinus) adjust(m, key, -1);
                if (click && overPlus) adjust(m, key, 1);
                y += NUMBER_ROW;
            }
        }
        return y + 2;
    }

    private void drawBar(GuiGraphics g, int mx, int my, boolean click) {
        int accent = Theme.accent();
        int swatches = Theme.ACCENTS.length;
        String[] labels = {Hud.visible ? "Hide HUD" : "Show HUD", "All off", "Save"};
        int btnW = 40;
        int totalW = swatches * 14 + 10 + labels.length * (btnW + 4);
        int x = (width - totalW) / 2;
        Draw.roundBox(g, x - 8, barY, x + totalW + 4, barY + 18, 7, 0xFF1D1D29, 0xE60B0B12);

        for (int i = 0; i < swatches; i++) {
            int sx = x + i * 14;
            boolean over = Draw.inside(mx, my, sx, barY + 3, sx + 12, barY + 15);
            if (i == Theme.index) Draw.roundRect(g, sx - 1, barY + 2, sx + 13, barY + 16, 7, 0xFFFFFFFF);
            Draw.roundRect(g, sx, barY + 3, sx + 12, barY + 15, 6, Theme.ACCENTS[i]);
            if (over) {
                hoverText = "Theme: " + THEME_NAMES[i];
                if (click) {
                    Theme.index = i;
                    Config.save();
                }
            }
        }

        int bx = x + swatches * 14 + 10;
        for (int i = 0; i < labels.length; i++) {
            boolean over = Draw.inside(mx, my, bx, barY + 3, bx + btnW, barY + 15);
            Draw.roundBox(g, bx, barY + 3, bx + btnW, barY + 15, 5, over ? Draw.alpha(accent, 160) : 0xFF2A2A38, 0xFF13131B);
            small(g, labels[i], bx + (btnW - smallWidth(labels[i], 0.78f)) / 2, barY + 6, over ? Theme.TEXT : 0xFFB9B9C8, 0.78f);
            if (over && click) {
                if (i == 0) {
                    Hud.visible = !Hud.visible;
                    Config.save();
                    flash = Hud.visible ? "HUD shown" : "HUD hidden";
                } else if (i == 1) {
                    ModuleManager.disableAll();
                    flash = "All modules off";
                } else {
                    Config.save();
                    flash = "Config saved";
                }
                flashUntil = System.currentTimeMillis() + 1500;
            }
            bx += btnW + 4;
        }
    }
}
