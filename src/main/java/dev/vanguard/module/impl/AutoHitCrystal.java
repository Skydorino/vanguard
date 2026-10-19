package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Damage;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * You place the crystal and aim at it; this hits it for you the moment it is under your
 * crosshair. Normal game reach, nothing is aimed for you.
 */
public class AutoHitCrystal extends Module {
    private int timer;

    public AutoHitCrystal() {
        super("AutoHitCrystal", Category.COMBAT, "Hits any end crystal under your crosshair as soon as you place it");
        setting("delay", 0);        // ticks between hits
        setting("totem", 1);        // 1 = still hit if the blast could kill you but you hold a totem
        setting("enemyrange", 0);   // >0 = only hit when an enemy player is within this many blocks
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        if (timer > 0) {
            timer--;
            return;
        }
        if (!(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof EndCrystal crystal)) return;
        if (!crystal.isAlive()) return;

        if (get("enemyrange") > 0 && Targets.nearestEnemy(player, level, get("enemyrange")) == null) return;

        Vec3 pos = crystal.position();
        double self = Damage.crystal(player, pos);
        double hp = player.getHealth() + player.getAbsorptionAmount();
        boolean totem = player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || player.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
        if (self >= hp && !(get("totem") > 0 && totem)) return;

        mc.gameMode.attack(player, crystal);
        player.swing(InteractionHand.MAIN_HAND);
        if (CrystalOptimizer.active()) crystal.discard();
        timer = (int) get("delay");
    }
}
