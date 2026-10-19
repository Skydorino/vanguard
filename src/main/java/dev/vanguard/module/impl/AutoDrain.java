package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * Anti-drowning: when your head is underwater it turns your view to the nearest water source
 * and scoops it with an empty bucket from your inventory, then puts your slot back.
 */
public class AutoDrain extends Module {
    private int timer;
    private int restoreSlot = -1;

    public AutoDrain() {
        super("AutoDrain", Category.COMBAT, "Scoops nearby water with an empty bucket when you are drowning");
        setting("range", 4);
        setting("slot", 9);   // hotbar slot (1-9) a bucket from your inventory is moved into
        setting("delay", 4);
    }

    @Override
    protected void onDisable() {
        if (restoreSlot >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(restoreSlot);
        restoreSlot = -1;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (restoreSlot >= 0) {
            player.getInventory().setSelectedSlot(restoreSlot);
            restoreSlot = -1;
        }
        if (mc.gameMode == null || mc.screen != null) return;
        if (timer > 0) {
            timer--;
            return;
        }
        if (!player.isUnderWater()) return;

        int bucket = Inv.bring(player, s -> s.is(Items.BUCKET), Math.max(0, Math.min(8, (int) get("slot") - 1)));
        if (bucket < 0) return;

        Vec3 eye = player.getEyePosition();
        int r = Math.max(1, Math.min(8, (int) get("range")));
        BlockPos origin = BlockPos.containing(eye);
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    c.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    FluidState fluid = level.getFluidState(c);
                    if (!fluid.isSource() || !fluid.is(FluidTags.WATER)) continue;
                    double d = Vec3.atCenterOf(c).distanceToSqr(eye);
                    if (d < bestSq) {
                        bestSq = d;
                        best = c.immutable();
                    }
                }
            }
        }
        if (best == null) return;

        Vec3 d = Vec3.atCenterOf(best).subtract(eye);
        player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));

        restoreSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(bucket);
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        timer = Math.max(1, (int) get("delay"));
    }
}
