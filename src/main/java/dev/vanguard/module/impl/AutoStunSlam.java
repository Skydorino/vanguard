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
 * Aim at a player while falling. If their shield is up, it axes the shield and mace-smashes in the
 * same tick (so knockback from the axe can't carry them out of range first); otherwise it just
 * smashes. The axe and mace can be anywhere in your inventory, and your held slot is put back.
 */
public class AutoStunSlam extends Module {
    private int previousSlot = -1;

    public AutoStunSlam() {
        super("AutoStunSlam", Category.COMBAT, "Axe-stuns a raised shield and mace-smashes in the same tick");
        setting("needfall", 1);  // 1 = only while falling; 0 = also on the ground (smash will be weak)
        setting("minfall", 1.5);
        setting("axeslot", 7);   // hotbar slot (1-9) an axe from your inventory is moved into
        setting("maceslot", 8);  // hotbar slot (1-9) a mace from your inventory is moved into
    }

    @Override
    protected void onDisable() {
        previousSlot = -1;
    }

    private int slotSetting(String key) {
        return Math.max(0, Math.min(8, (int) get(key) - 1));
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;

        if (previousSlot >= 0) {
            player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            return;
        }
        if (get("needfall") > 0 && (player.onGround() || player.fallDistance < get("minfall"))) return;

        Player target = Targets.crosshairPlayer(player);
        if (target == null) return;

        int mace = Inv.bring(player, s -> s.is(Items.MACE), slotSetting("maceslot"));
        if (mace < 0) return;
        int current = player.getInventory().getSelectedSlot();

        if (target.isBlocking()) {
            int axe = Inv.bring(player, s -> s.is(ItemTags.AXES), slotSetting("axeslot"));
            if (axe >= 0) {
                previousSlot = current;
                // Both hits in the same tick: the axe's knockback has not moved them yet
                player.getInventory().setSelectedSlot(axe);
                mc.gameMode.attack(player, target);
                player.getInventory().setSelectedSlot(mace);
                mc.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND);
                return;
            }
        }

        // No raised shield (or no axe): just smash, once your weapon is charged
        if (player.getAttackStrengthScale(0.5f) < 0.9f) return;
        previousSlot = current;
        player.getInventory().setSelectedSlot(mace);
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
