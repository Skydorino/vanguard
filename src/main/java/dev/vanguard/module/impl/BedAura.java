package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Full aggressive bed aura.
 * Phase 0: Finds the nearest bed in range.
 * Phase 1: Breaks surrounding defence blocks (glass, wood, wool, terracotta, end stone)
 *          closest to us first, sequentially.
 * Phase 2: Breaks the bed using the fastest available tool.
 * Also attacks any nearby defender between break packets so you're still fighting.
 */
public class BedAura extends Module {
    private static final java.util.Set<Block> DEFENCE = java.util.Set.of(
        Blocks.GLASS, Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL,
        Blocks.LIGHT_BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.PINK_WOOL,
        Blocks.GRAY_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL,
        Blocks.BLUE_WOOL, Blocks.BROWN_WOOL, Blocks.GREEN_WOOL, Blocks.RED_WOOL,
        Blocks.BLACK_WOOL, Blocks.END_STONE, Blocks.END_STONE_BRICKS,
        Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS,
        Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA,
        Blocks.CYAN_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.RED_TERRACOTTA,
        Blocks.OBSIDIAN
    );

    private enum Phase { SCAN, CLEAR_DEFENCE, BREAK_BED }

    private Phase phase = Phase.SCAN;
    private BlockPos bedPos;
    private List<BlockPos> defenceQueue = new ArrayList<>();
    private int breakTimer;
    private int attackTimer;

    public BedAura() {
        super("BedAura", Category.BEDWARS,
            "Clears defence blocks then breaks the bed; attacks the defender in between");
        setting("range",        6);
        setting("defrange",     2);  // blocks around the bed to treat as defence
        setting("interval",     1);  // ticks between break packets
        setting("attackrange",  4);  // range to attack nearby defender
        setting("selfprotect",  1);  // 1 = stop if health drops below minhealth
        setting("minhealth",    8);
    }

    @Override
    protected void onEnable() {
        phase = Phase.SCAN;
        bedPos = null;
        defenceQueue.clear();
        breakTimer = 0;
        attackTimer = 0;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null || mc.gameMode == null) return;

        if (get("selfprotect") > 0 && player.getHealth() < get("minhealth")) {
            setEnabled(false);
            return;
        }

        // Attack nearby defender between break packets
        if (--attackTimer <= 0) {
            attackTimer = 10;
            if (get("attackrange") > 0) {
                Player defender = Targets.nearestEnemy(player, level, get("attackrange"));
                if (defender != null) {
                    mc.gameMode.attack(player, defender);
                    player.swing(InteractionHand.MAIN_HAND);
                }
            }
        }

        if (--breakTimer > 0) return;
        breakTimer = Math.max(1, (int) get("interval"));

        switch (phase) {
            case SCAN -> scan(player, level);
            case CLEAR_DEFENCE -> clearDefence(player, level);
            case BREAK_BED -> breakBed(player, level);
        }
    }

    private void scan(LocalPlayer player, ClientLevel level) {
        int range = (int) get("range");
        BlockPos origin = player.blockPosition();
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).is(BlockTags.BEDS)) continue;
                    double dist = pos.distSqr(origin);
                    if (dist < bestDist) { bestDist = dist; nearest = pos; }
                }
            }
        }

        if (nearest == null) return;
        bedPos = nearest;

        // Build defence queue — blocks around bed that are defence material, sorted closest first
        int dr = (int) get("defrange");
        defenceQueue.clear();
        for (int dx = -dr; dx <= dr; dx++) {
            for (int dy = -dr; dy <= dr; dy++) {
                for (int dz = -dr; dz <= dr; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos pos = bedPos.offset(dx, dy, dz);
                    if (pos.equals(bedPos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (DEFENCE.contains(state.getBlock())) defenceQueue.add(pos);
                }
            }
        }
        Vec3 eye = player.getEyePosition();
        defenceQueue.sort(Comparator.comparingDouble(p -> p.distSqr(bedPos)));

        phase = defenceQueue.isEmpty() ? Phase.BREAK_BED : Phase.CLEAR_DEFENCE;
    }

    private void clearDefence(LocalPlayer player, ClientLevel level) {
        // Remove already-broken blocks from queue
        defenceQueue.removeIf(p -> level.getBlockState(p).isAir());

        if (defenceQueue.isEmpty()) {
            phase = Phase.BREAK_BED;
            return;
        }

        BlockPos target = defenceQueue.get(0);
        selectBestTool(player, level.getBlockState(target));
        mc.gameMode.startDestroyBlock(target, Direction.UP);
        mc.gameMode.continueDestroyBlock(target, Direction.UP);
        mc.gameMode.stopDestroyBlock();
    }

    private void breakBed(LocalPlayer player, ClientLevel level) {
        if (bedPos == null || !level.getBlockState(bedPos).is(BlockTags.BEDS)) {
            // Bed is gone — done
            setEnabled(false);
            return;
        }
        selectAxe(player);
        mc.gameMode.startDestroyBlock(bedPos, Direction.UP);
        mc.gameMode.continueDestroyBlock(bedPos, Direction.UP);
        mc.gameMode.stopDestroyBlock();
    }

    private void selectBestTool(LocalPlayer player, BlockState state) {
        // Prefer pickaxe for stone/end stone, axe for wood/wool
        int slot = Inv.findHotbar(player, Items.DIAMOND_PICKAXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.IRON_PICKAXE);
        if (slot == -1) slot = selectAxe(player);
        if (slot != -1) player.getInventory().setSelectedSlot(slot);
    }

    private int selectAxe(LocalPlayer player) {
        int slot = Inv.findHotbar(player, Items.DIAMOND_AXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.IRON_AXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.GOLDEN_AXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.STONE_AXE);
        if (slot == -1) slot = Inv.findHotbar(player, Items.WOODEN_AXE);
        if (slot != -1) player.getInventory().setSelectedSlot(slot);
        return slot;
    }
}
// END OF FILE