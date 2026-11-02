package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Continuously breaks the nearest enemy bed within range.
 * Swaps to an axe automatically. Works through other blocks — it just fires break packets
 * at the bed position regardless of line of sight.
 */
public class BedNuker extends Module {
    private BlockPos target;
    private int breakTimer;

    public BedNuker() {
        super("BedNuker", Category.BEDWARS, "Automatically breaks the nearest enemy bed");
        setting("range", 5);
        setting("interval", 1); // ticks between break packets
    }

    @Override
    protected void onEnable() { target = null; breakTimer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null || mc.gameMode == null) return;
        if (--breakTimer > 0) return;
        breakTimer = Math.max(1, (int) get("interval"));

        int range = (int) get("range");
        BlockPos origin = player.blockPosition();
        target = null;

        // Scan for a bed block in range
        outer:
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.BEDS)) {
                        target = pos;
                        break outer;
                    }
                }
            }
        }

        if (target == null) return;

        // Prefer axe, fallback to any tool
        int slot = Inv.findHotbar(player, Items.DIAMOND_AXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.IRON_AXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.GOLDEN_AXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.STONE_AXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.WOODEN_AXE);
        if (slot != -1) player.getInventory().setSelectedSlot(slot);

        mc.gameMode.startDestroyBlock(target, Direction.UP);
        mc.gameMode.continueDestroyBlock(target, Direction.UP);
        mc.gameMode.stopDestroyBlock();
    }
}
// END OF FILE