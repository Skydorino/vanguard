package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;

/** Numeric toggles for the built-in HUD; 1 means visible, 0 means hidden. */
public final class HudOptions extends Module {
    public HudOptions() {
        super("HudOptions", Category.VISUALS, "Configure individual Vanguard HUD elements");
        setting("watermark", 1);
        setting("modules", 1);
        setting("itemcounts", 1);
        setting("basefinder", 1);
        setting("swordinspect", 1);
    }

    public boolean visible(String element) {
        return get(element) >= 0.5;
    }
}
