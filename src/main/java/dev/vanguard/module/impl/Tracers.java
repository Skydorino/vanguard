package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import dev.vanguard.render.WorldDraw;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Lines from your crosshair to every other player. Friends are green, everyone else red. */
public class Tracers extends Module {
    public Tracers() {
        super("Tracers", Category.VISUALS, "Draws lines from your crosshair to other players");
        setting("width", 0.04);
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        if (mc.level == null || mc.player == null) return;
        Vec3 start = draw.cam.add(mc.player.getViewVector(draw.partial).scale(1.0));
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            Vec3 end = draw.pos(p).add(0.0, p.getBbHeight() / 2.0, 0.0);
            double width = get("width") + start.distanceTo(end) * 0.002;
            int color = ModuleManager.isFriend(p) ? 0xCC33FF66 : 0xCCFF3040;
            draw.line(start, end, width, color);
        }
    }
}
