package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class KillAura extends Module {
    public KillAura() {
        super("KillAura", Category.COMBAT, "Attacks the nearest non-friend player in range");
        setting("range", 3.0);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        if (player.getAttackStrengthScale(0.5f) < 1.0f) return; // wait for full attack charge

        Player target = Targets.nearestEnemy(player, level, get("range"));
        if (target == null) return;

        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
