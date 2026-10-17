package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * Attacks the player under your crosshair once your weapon is charged. You still have to aim,
 * and reach is the normal game reach.
 */
public class TriggerBot extends Module {
    public TriggerBot() {
        super("TriggerBot", Category.COMBAT, "Attacks the player under your crosshair when your weapon is charged");
        setting("cooldown", 0.9); // 1.0 = wait for a full charge
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        Player target = Targets.crosshairPlayer(player);
        if (target == null) return;
        if (player.getAttackStrengthScale(0.5f) < get("cooldown")) return;

        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
