package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;

/**
 * Harvests fully-grown crops in range then replants them.
 * Works with wheat, carrots, potatoes, beetroot.
 */
public class AutoFarm extends Module {
    private int timer;

    public AutoFarm() {
        super("AutoFarm", Category.DONUTSMP, "Harvests fully-grown crops and replants them automatically");
        setting("range", 4);
        setting("interval", 2);
    }

    @Override
    protected void onEnable() { timer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null) return;
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        int range = (int) get("range");
        BlockPos origin = player.blockPosition();

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();

                    if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
                        // Harvest
                        mc.gameMode.startDestroyBlock(pos, Direction.UP);
                        mc.gameMode.continueDestroyBlock(pos, Direction.UP);
                        mc.gameMode.stopDestroyBlock();

                        // Replant
                        int seedSlot = findSeed(player, block);
                        if (seedSlot != -1) {
                            int prev = player.getInventory().getSelectedSlot();
                            player.getInventory().setSelectedSlot(seedSlot);
                            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                            player.getInventory().setSelectedSlot(prev);
                        }
                        return;
                    }
                }
            }
        }
    }

    private int findSeed(LocalPlayer player, Block crop) {
        Item target = crop == Blocks.WHEAT ? Items.WHEAT_SEEDS
            : crop == Blocks.CARROTS ? Items.CARROT
            : crop == Blocks.POTATOES ? Items.POTATO
            : crop == Blocks.BEETROOTS ? Items.BEETROOT_SEEDS
            : null;
        if (target == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(target)) return i;
        }
        return -1;
    }
}
// END OF FILE