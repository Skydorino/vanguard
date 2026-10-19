package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;

/** Shows your frame rate on the HUD (drawn by Hud). */
public class FpsDisplay extends Module {
    public FpsDisplay() {
        super("FpsDisplay", Category.VISUALS, "Shows your FPS on the HUD");
    }
}
