package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import dev.vanguard.util.Place;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * When a charged respawn anchor is close, walls you off from it by placing glowstone in the
 * blocks next to you on the side facing the anchor (feet level, then head level). You cannot place
 * a block in your own spot, so this protects from the anchor's side. Anchors only explode
 * outside the Nether, so it stays off there.
 */
public class SafeAnchor extends Module {
    private int timer;
    private int restoreSlot = -1;

    public SafeAnchor() {
        super("SafeAnchor", Category.COMBAT, "Places glowstone between you and a nearby charged anchor");
        setting("range", 6);
        setting("slot", 9);   // hotbar slot (1-9) glowstone from your inventory is moved into
        setting("delay", 1);
    }

    @Override
    protected void onDisable() {
        if (restoreSlot >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(restoreSlot);
        restoreSlot = -1;
    }

    private BlockPos findAnchor(LocalPlayer player, ClientLevel level) {
        int r = Math.max(1, Math.min(12, (int) get("range")));
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    c.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState s = level.getBlockState(c);
                    if (!s.is(Blocks.RESPAWN_ANCHOR) || s.getValue(RespawnAnchorBlock.CHARGE) <= 0) continue;
                    double d = dx * dx + dy * dy + dz * dz;
                    if (d < bestSq) {
                        bestSq = d;
                        best = c.immutable();
                    }
                }
            }
        }
        return best;
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
        if (level.dimension() == Level.NETHER) return;

        BlockPos anchor = findAnchor(player, level);
        if (anchor == null) return;

        Direction dir = Direction.getApproximateNearest(
            anchor.getX() + 0.5 - player.getX(), 0.0, anchor.getZ() + 0.5 - player.getZ());
        BlockPos feet = player.blockPosition().relative(dir);
        BlockPos head = feet.above();

        int slot = Inv.bring(player, s -> s.is(Items.GLOWSTONE), Math.max(0, Math.min(8, (int) get("slot") - 1)));
        if (slot < 0) return;
        int current = player.getInventory().getSelectedSlot();

        boolean placed = Place.block(player, level, feet, slot) || Place.block(player, level, head, slot);
        if (placed) {
            if (slot != current) restoreSlot = current;
            timer = Math.max(0, (int) get("delay"));
        }
    }
}
