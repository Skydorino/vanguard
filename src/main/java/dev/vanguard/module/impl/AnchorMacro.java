package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Damage;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Aim at a respawn anchor: charges it with glowstone from your hotbar, then detonates it with
 * any other item. Skips the detonation if the estimated blast would kill you.
 */
public class AnchorMacro extends Module {
    private int timer;

    public AnchorMacro() {
        super("AnchorMacro", Category.COMBAT, "Charges and detonates the respawn anchor you are looking at");
        setting("delay", 1);
        setting("selfprotect", 1); // 1 = skip detonation if the blast would be lethal
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        if (timer > 0) {
            timer--;
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!state.is(Blocks.RESPAWN_ANCHOR)) return;

        int charge = state.getValue(RespawnAnchorBlock.CHARGE);
        if (charge == 0) {
            int slot = Inv.findHotbar(player, Items.GLOWSTONE);
            if (slot < 0) return;
            player.getInventory().setSelectedSlot(slot);
        } else {
            if (get("selfprotect") > 0) {
                Vec3 center = Vec3.atCenterOf(hit.getBlockPos());
                if (Damage.explosion(player, center, 5.0) >= player.getHealth() + player.getAbsorptionAmount()) return;
            }
            int slot = Inv.firstHotbarNot(player, Items.GLOWSTONE);
            if (slot < 0) return;
            player.getInventory().setSelectedSlot(slot);
        }
        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);
        timer = (int) get("delay");
    }
}
