package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import dev.vanguard.util.Place;
import dev.vanguard.util.Targets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * When the nearest player is boxed in by walls (a corner or corridor), fills in the open sides at
 * feet and head height, then the block above their head, using obsidian from your inventory.
 * Only places blocks you can reach, and skips spots someone is standing in.
 */
public class AutoTrap extends Module {
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private int timer;
    private int restoreSlot = -1;

    public AutoTrap() {
        super("AutoTrap", Category.COMBAT, "Covers in a player who is standing in a corner");
        setting("range", 6);       // how far away a player can be to be trapped
        setting("minwalls", 2);    // walls already around their feet to count as cornered (0 = trap anyone)
        setting("pertick", 1);     // blocks placed per step
        setting("delay", 1);       // ticks between steps
        setting("slot", 9);        // hotbar slot (1-9) obsidian from your inventory is moved into
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

        Player target = Targets.nearestEnemy(player, level, get("range"));
        if (target == null) return;

        BlockPos feet = target.blockPosition();
        BlockPos head = feet.above();

        int walls = 0;
        for (Direction d : SIDES) {
            if (!level.getBlockState(feet.relative(d)).canBeReplaced()) walls++;
        }
        if (walls < (int) get("minwalls")) return;

        // Feet-level sides first, then head-level, then the lid, so each block has something to attach to
        List<BlockPos> plan = new ArrayList<>();
        for (Direction d : SIDES) plan.add(feet.relative(d));
        for (Direction d : SIDES) plan.add(head.relative(d));
        plan.add(head.above());

        int slot = Inv.bring(player, s -> s.is(Items.OBSIDIAN), Math.max(0, Math.min(8, (int) get("slot") - 1)));
        if (slot < 0) return;

        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 eye = player.getEyePosition();
        int current = player.getInventory().getSelectedSlot();
        int budget = Math.max(1, (int) get("pertick"));
        boolean placed = false;

        for (BlockPos pos : plan) {
            if (budget == 0) break;
            if (!level.getBlockState(pos).canBeReplaced()) continue;
            if (Vec3.atCenterOf(pos).distanceTo(eye) > reach) continue;
            if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) continue;
            if (Place.block(player, level, pos, slot)) {
                budget--;
                placed = true;
            }
        }
        if (placed) {
            if (slot != current) restoreSlot = current;
            timer = Math.max(0, (int) get("delay"));
        }
    }
}
