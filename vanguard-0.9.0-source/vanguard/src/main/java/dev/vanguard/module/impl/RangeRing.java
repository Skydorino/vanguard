package dev.vanguard.module.impl;

import dev.vanguard.gui.Draw;
import dev.vanguard.gui.Theme;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import net.minecraft.world.phys.Vec3;

/** A ring on the ground around you showing a fixed radius (3 blocks by default). */
public class RangeRing extends Module {
    public RangeRing() {
        super("RangeRing", Category.VISUALS, "Draws a ring around you at a set range");
        setting("radius", 3.0);
        setting("thickness", 0.08);
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        if (mc.player == null) return;
        double r = get("radius");
        double half = get("thickness") / 2.0;
        Vec3 center = draw.pos(mc.player).add(0.0, 0.03, 0.0);
        draw.ring(center, Math.max(0.05, r - half), r + half, 64, Draw.alpha(Theme.accent(), 170));
    }
}
