package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;

/** Flag module: when on, AutoCrystal removes a crystal client-side the moment it hits it. */
public class CrystalOptimizer extends Module {
    public CrystalOptimizer() {
        super("CrystalOptimizer", Category.COMBAT, "Removes crystals instantly client-side so the next one can be placed sooner");
    }

    public static boolean active() {
        Module m = ModuleManager.find("CrystalOptimizer");
        return m != null && m.isEnabled();
    }
}
