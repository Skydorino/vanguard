package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Scaffold / FastBridge.
 * Places a block beneath your feet each tick you are moving and the block below is air.
 * Bypass approach:
 *  - Uses rotationless placement (clicks the side face of the block one step below foot level,
 *    not directly down) so the packet looks like a normal side-place rather than a silent downward place.
 *  - Sends a sneak packet before placing (sets player.setPose sneak) to avoid "placing into air" flags.
 *  - Enforces a minimum interval between placements so packet rate stays human-plausible.
 *  - Does NOT rotate your camera.
 */
public class Scaffold extends Module {
    private int placeTimer;

    public Scaffold() {
        super("Scaffold", Category.BEDWARS, "Places blocks beneath your feet as you run");
        setting("interval", 2);   // ticks between placements (lower = faster, more detectable)
        setting("sneak",    1);   // 1 = send sneak before placing (helps bypass)
    }

    @Override
    protected void onEnable() { placeTimer = 0; }

    @Override
    protected void onDisable() {
        if (mc.player != null) mc.options.keyShift.setDown(false);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null || mc.gameMode == null) return;
        if (--placeTimer > 0) return;

        // Only scaffold when moving
        if (Math.abs(player.getDeltaMovement().x) < 0.01 && Math.abs(player.getDeltaMovement().z) < 0.01) return;

        BlockPos feet   = player.blockPosition();
        BlockPos below  = feet.below();
        BlockPos below2 = below.below();

        // Only place if the block directly under feet is air
        if (!level.getBlockState(below).isAir()) return;

        // Find a placeable block in hotbar
        int slot = Inv.findHotbar(player, s -> !s.isEmpty() && s.getItem() instanceof BlockItem);
        if (slot == -1) return;

        // Find a solid neighbor of 'below' to click on
        // We prefer the block two below, then lateral neighbors
        Direction placeDir = null;
        BlockPos neighbor  = null;

        if (!level.getBlockState(below2).isAir()) {
            placeDir = Direction.UP;
            neighbor = below2;
        } else {
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos side = below.relative(dir);
                if (!level.getBlockState(side).isAir()) {
                    placeDir = dir.getOpposite();
                    neighbor = side;
                    break;
                }
            }
        }

        if (neighbor == null || placeDir == null) return;

        // Sneak to make the placement look legit
        if (get("sneak") > 0) mc.options.keyShift.setDown(true);

        int prev = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);

        Vec3 hitVec = Vec3.atCenterOf(neighbor).relative(placeDir, 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, placeDir, neighbor, false);
        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);

        player.getInventory().setSelectedSlot(prev);
        if (get("sneak") > 0) mc.options.keyShift.setDown(false);

        placeTimer = Math.max(1, (int) get("interval"));
    }
}
// END OF FILE