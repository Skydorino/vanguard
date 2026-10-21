package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Place;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Looks a couple of ticks ahead of your movement; if there is nothing to stand on (a drop of
 * "drop" blocks or more), places a block under where your feet will be. Uses a block from your
 * hotbar (plain solid blocks only: no sand/gravel, TNT or containers).
 */
public class AirWalk extends Module {
    private int restoreSlot = -1;

    public AirWalk() {
        super("AirWalk", Category.MOVEMENT, "Places blocks under your feet when you are about to fall");
        setting("slot", 0);       // 0 = pick a block from your hotbar automatically, 1-9 = that slot
        setting("lookahead", 3);  // ticks of movement to look ahead
        setting("drop", 2);       // only place if the fall would be at least this many blocks
    }

    @Override
    protected void onDisable() {
        if (restoreSlot >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(restoreSlot);
        restoreSlot = -1;
    }

    private static boolean usable(ItemStack s) {
        if (!(s.getItem() instanceof BlockItem bi)) return false;
        Block b = bi.getBlock();
        return b != Blocks.TNT && !(b instanceof FallingBlock) && !b.defaultBlockState().hasBlockEntity();
    }

    private int pickSlot(LocalPlayer player) {
        int chosen = (int) get("slot");
        if (chosen >= 1 && chosen <= 9) return usable(player.getInventory().getItem(chosen - 1)) ? chosen - 1 : -1;
        for (int i = 0; i < 9; i++) {
            if (usable(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (restoreSlot >= 0) {
            player.getInventory().setSelectedSlot(restoreSlot);
            restoreSlot = -1;
        }
        if (mc.gameMode == null || mc.screen != null) return;
        if (player.getAbilities().flying || player.isInWater() || player.isInLava() || player.isPassenger()) return;

        Vec3 dm = player.getDeltaMovement();
        if (dm.y > 0.0) return; // jumping up
        double ahead = Math.max(0.0, get("lookahead"));
        BlockPos support = BlockPos.containing(player.getX() + dm.x * ahead, player.getY() - 0.5, player.getZ() + dm.z * ahead);

        int drop = Math.max(1, (int) get("drop"));
        for (int i = 0; i < drop; i++) {
            if (!level.getBlockState(support.below(i)).canBeReplaced()) return; // something to land on soon
        }

        int slot = pickSlot(player);
        if (slot < 0) return;
        int current = player.getInventory().getSelectedSlot();
        if (Place.block(player, level, support, slot) && slot != current) restoreSlot = current;
    }
}
