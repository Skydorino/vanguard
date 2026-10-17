package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * While falling at the player under your crosshair: if they are blocking, axe the shield first,
 * then mace smash next tick; otherwise just smash. Needs a mace (and ideally an axe) in the hotbar.
 */
public class AutoStunSlam extends Module {
    private int stage;          // 0 idle, 1 = smash next tick, 2 = restore slot next tick
    private int previousSlot = -1;

    public AutoStunSlam() {
        super("AutoStunSlam", Category.COMBAT, "Axe-stuns a shield, then mace-smashes, while falling");
        setting("minfall", 1.5);
    }

    @Override
    protected void onDisable() {
        stage = 0;
        previousSlot = -1;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;

        if (stage == 2) {
            if (previousSlot >= 0) player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            stage = 0;
            return;
        }
        if (stage == 1) {
            int mace = Inv.findHotbar(player, Items.MACE);
            Player target = Targets.crosshairPlayer(player);
            if (mace >= 0 && target != null) {
                player.getInventory().setSelectedSlot(mace);
                mc.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND);
            }
            stage = 2;
            return;
        }

        if (player.onGround() || player.fallDistance < get("minfall")) return;
        Player target = Targets.crosshairPlayer(player);
        if (target == null) return;
        int mace = Inv.findHotbar(player, Items.MACE);
        if (mace < 0) return;

        int axe = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(ItemTags.AXES)) {
                axe = i;
                break;
            }
        }

        previousSlot = player.getInventory().getSelectedSlot();
        if (target.isBlocking() && axe >= 0) {
            player.getInventory().setSelectedSlot(axe);
            mc.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            stage = 1;
        } else {
            if (player.getAttackStrengthScale(0.5f) < 0.9f) {
                previousSlot = -1;
                return;
            }
            player.getInventory().setSelectedSlot(mace);
            mc.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            stage = 2;
        }
    }
}
