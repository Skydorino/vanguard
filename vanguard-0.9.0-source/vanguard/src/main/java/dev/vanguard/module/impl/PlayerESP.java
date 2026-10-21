package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import dev.vanguard.render.WorldDraw;
import net.minecraft.world.entity.player.Player;

/** Boxes around other players, visible through walls. Friends are green, everyone else red. */
public class PlayerESP extends Module {
    public PlayerESP() {
        super("PlayerESP", Category.VISUALS, "Highlights players through walls");
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        if (mc.level == null) return;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            int color = ModuleManager.isFriend(p) ? 0x6633FF66 : 0x66FF3040;
            draw.box(draw.boundsOf(p), color);
        }
    }
}
