package dev.vanguard.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class Place {
    private Place() {}

    /**
     * Places whatever is in the given hotbar slot at target by clicking a solid neighbouring
     * block. Returns true if a click was sent.
     */
    public static boolean block(LocalPlayer player, ClientLevel level, BlockPos target, int hotbarSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;
        if (!level.getBlockState(target).canBeReplaced()) return false;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = target.relative(dir);
            BlockState ns = level.getBlockState(neighbor);
            if (ns.isAir() || !ns.getFluidState().isEmpty() || ns.hasBlockEntity()) continue;

            Direction face = dir.getOpposite();
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(neighbor).relative(face, 0.5), face, neighbor, false);
            player.getInventory().setSelectedSlot(hotbarSlot);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }
}
