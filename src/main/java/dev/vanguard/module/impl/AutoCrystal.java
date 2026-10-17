package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Damage;
import dev.vanguard.util.Inv;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Places end crystals on obsidian/bedrock near the closest enemy and breaks crystals that
 * would hurt them more than they hurt you. Damage numbers are in half-hearts.
 */
public class AutoCrystal extends Module {
    private int breakTimer;
    private int placeTimer;

    public AutoCrystal() {
        super("AutoCrystal", Category.COMBAT, "Places and breaks end crystals around enemies");
        setting("targetrange", 10.0);
        setting("placerange", 4.5);
        setting("breakrange", 3.0);
        setting("mindamage", 6.0);
        setting("maxselfdamage", 8.0);
        setting("breakdelay", 1);
        setting("placedelay", 2);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;

        Player target = Targets.nearestEnemy(player, level, get("targetrange"));
        if (target == null) return;

        if (breakTimer == 0 && breakCrystal(player, level, target)) {
            breakTimer = (int) get("breakdelay");
        }
        if (placeTimer == 0 && placeCrystal(player, level, target)) {
            placeTimer = (int) get("placedelay");
        }
    }

    private boolean isSafe(LocalPlayer player, Vec3 crystalPos) {
        double self = Damage.crystal(player, crystalPos);
        return self <= get("maxselfdamage") && self < player.getHealth() + player.getAbsorptionAmount();
    }

    private boolean breakCrystal(LocalPlayer player, ClientLevel level, Player target) {
        Vec3 eye = player.getEyePosition();
        double range = get("breakrange");
        EndCrystal best = null;
        double bestScore = -Double.MAX_VALUE;

        for (EndCrystal crystal : level.getEntitiesOfClass(EndCrystal.class, player.getBoundingBox().inflate(range + 1.0))) {
            if (!crystal.isAlive() || crystal.getBoundingBox().distanceToSqr(eye) > range * range) continue;
            Vec3 pos = crystal.position();
            if (!isSafe(player, pos)) continue;
            double dealt = Damage.crystal(target, pos);
            if (dealt < get("mindamage")) continue;
            double score = dealt - Damage.crystal(player, pos);
            if (score > bestScore) {
                bestScore = score;
                best = crystal;
            }
        }
        if (best == null) return false;

        mc.gameMode.attack(player, best);
        player.swing(InteractionHand.MAIN_HAND);
        if (CrystalOptimizer.active()) best.discard();
        return true;
    }

    private boolean placeCrystal(LocalPlayer player, ClientLevel level, Player target) {
        boolean offhand = player.getOffhandItem().is(Items.END_CRYSTAL);
        int hotbarSlot = offhand ? -1 : Inv.findHotbar(player, Items.END_CRYSTAL);
        if (!offhand && hotbarSlot < 0) return false;

        Vec3 eye = player.getEyePosition();
        double range = get("placerange");
        BlockPos origin = target.blockPosition();
        BlockPos bestBase = null;
        double bestScore = -Double.MAX_VALUE;

        for (BlockPos base : BlockPos.betweenClosed(origin.offset(-4, -3, -4), origin.offset(4, 1, 4))) {
            if (Vec3.atCenterOf(base).distanceTo(eye) > range) continue;
            BlockState state = level.getBlockState(base);
            if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) continue;

            BlockPos above = base.above();
            if (!level.getBlockState(above).isAir()) continue;
            AABB box = new AABB(above.getX(), above.getY(), above.getZ(),
                above.getX() + 1, above.getY() + 2, above.getZ() + 1);
            if (!level.getEntities((Entity) null, box).isEmpty()) continue;

            Vec3 crystalPos = new Vec3(above.getX() + 0.5, above.getY(), above.getZ() + 0.5);
            if (!isSafe(player, crystalPos)) continue;
            double dealt = Damage.crystal(target, crystalPos);
            if (dealt < get("mindamage")) continue;
            double score = dealt - Damage.crystal(player, crystalPos);
            if (score > bestScore) {
                bestScore = score;
                bestBase = base.immutable(); // betweenClosed reuses one mutable BlockPos
            }
        }
        if (bestBase == null) return false;

        InteractionHand hand = InteractionHand.OFF_HAND;
        if (!offhand) {
            player.getInventory().setSelectedSlot(hotbarSlot);
            hand = InteractionHand.MAIN_HAND;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(bestBase).add(0.0, 0.5, 0.0), Direction.UP, bestBase, false);
        mc.gameMode.useItemOn(player, hand, hit);
        player.swing(hand);
        return true;
    }
}
