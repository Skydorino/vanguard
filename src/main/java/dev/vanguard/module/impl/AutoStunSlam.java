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
 * then mace smash next tick; otherwise just smash. The axe and mace can be anywhere in your
 * inventory; whatever you are holding is put back afterwards.
 */
public class AutoStunSlam extends Module {
    private int stage;          // 0 idle, 1 = smash next tick, 2 = restore slot next tick
    private int previousSlot = -1;
    private int maceSlot = -1;

    public AutoStunSlam() {
        super("AutoStunSlam", Category.COMBAT, "Axe-stuns a shield, then mace-smashes, while falling");
        setting("minfall", 1.5);
        setting("axeslot", 7);   // hotbar slot (1-9) an axe from your inventory is moved into
        setting("maceslot", 8);  // hotbar slot (1-9) a mace from your inventory is moved into
    }

    @Override
    protected void onDisable() {
        stage = 0;
        previousSlot = -1;
        maceSlot = -1;
    }

    private int slotSetting(String key) {
        return Math.max(0, Math.min(8, (int) get(key) - 1));
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
            Player target = Targets.crosshairPlayer(player);
            if (maceSlot >= 0 && target != null) {
                player.getInventory().setSelectedSlot(maceSlot);
                mc.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND);
            }
            stage = 2;
            return;
        }

        if (player.onGround() || player.fallDistance < get("minfall")) return;
        Player target = Targets.crosshairPlayer(player);
        if (target == null) return;

        int mace = Inv.bring(player, s -> s.is(Items.MACE), slotSetting("maceslot"));
        if (mace < 0) return;
        int current = player.getInventory().getSelectedSlot();

        if (target.isBlocking()) {
            int axe = Inv.bring(player, s -> s.is(ItemTags.AXES), slotSetting("axeslot"));
            if (axe >= 0) {
                previousSlot = current;
                maceSlot = mace;
                player.getInventory().setSelectedSlot(axe);
                mc.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND);
                stage = 1;
                return;
            }
        }
        if (player.getAttackStrengthScale(0.5f) < 0.9f) return;
        previousSlot = current;
        player.getInventory().setSelectedSlot(mace);
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
        stage = 2;
    }
}
