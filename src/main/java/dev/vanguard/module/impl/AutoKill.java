package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * AutoKill — attacks the nearest mob (passive or hostile) automatically.
 * Great for mob farm grinding. Attacks once per interval so it doesn't
 * look like a pure aura to basic anticheats.
 */
public class AutoKill extends Module {
    private int timer;

    public AutoKill() {
        super("AutoKill", Category.DONUTSMP, "Automatically attacks nearby mobs for drops");
        setting("range",    4);
        setting("interval", 10); // ticks between attacks
        setting("passive",  1);  // 1 = attack passive mobs (animals)
        setting("hostile",  1);  // 1 = attack hostile mobs
    }

    @Override
    protected void onEnable() { timer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null) return;
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        double r = get("range");
        AABB box = player.getBoundingBox().inflate(r);

        Entity target = null;
        double best = Double.MAX_VALUE;

        if (get("passive") > 0) {
            for (Animal a : level.getEntitiesOfClass(Animal.class, box, e -> !e.isRemoved())) {
                double d = a.distanceToSqr(player);
                if (d < best) { best = d; target = a; }
            }
        }
        if (get("hostile") > 0) {
            for (Monster m : level.getEntitiesOfClass(Monster.class, box, e -> !e.isRemoved())) {
                double d = m.distanceToSqr(player);
                if (d < best) { best = d; target = m; }
            }
        }

        if (target == null) return;
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
// END OF FILE