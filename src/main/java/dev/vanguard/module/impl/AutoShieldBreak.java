package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** When the player under your crosshair raises a shield, swaps to an axe, hits, and swaps back. */
public class AutoShieldBreak extends Module {
    private int previousSlot = -1;

    public AutoShieldBreak() {
        super("AutoShieldBreak", Category.COMBAT, "Axe-hits shields raised by the player you are aiming at");
    }

    @Override
    protected void onDisable() {
        previousSlot = -1;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;

        if (previousSlot >= 0) {
            player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            return;
        }
        Player target = Targets.crosshairPlayer(player);
        if (target == null || !target.isBlocking()) return;
        if (player.getAttackStrengthScale(0.5f) < 0.9f) return;

        int axe = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(ItemTags.AXES)) {
                axe = i;
                break;
            }
        }
        if (axe < 0) return;

        int current = player.getInventory().getSelectedSlot();
        if (current != axe) {
            previousSlot = current;
            player.getInventory().setSelectedSlot(axe);
        }
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
