package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;

/** Shows your ping to the server on the HUD (drawn by Hud). */
public class PingDisplay extends Module {
    public PingDisplay() {
        super("PingDisplay", Category.VISUALS, "Shows your ping on the HUD");
    }
}
