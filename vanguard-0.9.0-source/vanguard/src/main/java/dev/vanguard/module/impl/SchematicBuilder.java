package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import dev.vanguard.util.Schematic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Builds a loaded schematic block by block, bottom-up, using blocks from your inventory.
 * Limitation: it places the block type only. Block state (stair direction, slab half, log axis,
 * and so on) is not matched, so best suited to full blocks like stone, wool, planks, glass.
 */
public class SchematicBuilder extends Module {
    private Schematic schematic;
    private BlockPos origin;
    private boolean[] done;
    private int placed;
    private int timer;

    public SchematicBuilder() {
        super("SchematicBuilder", Category.WORLD, "Places blocks from a loaded schematic (/vg schem)");
        setting("range", 4.0);
        setting("pertick", 1);
        setting("delay", 1);
    }

    public String load(Path file) throws IOException {
        schematic = Schematic.load(file);
        origin = null;
        done = null;
        placed = 0;
        return schematic.name + ": " + schematic.entries.size() + " blocks";
    }

    public boolean hasSchematic() {
        return schematic != null;
    }

    /** The schematic's lowest corner will sit at this position. */
    public void setOrigin(BlockPos pos) {
        origin = pos;
        if (schematic != null) {
            done = new boolean[schematic.entries.size()];
            placed = 0;
        }
    }

    public void clear() {
        schematic = null;
        origin = null;
        done = null;
        placed = 0;
    }

    public String status() {
        if (schematic == null) return "No schematic loaded";
        if (origin == null) return schematic.name + " loaded, origin not set (use /vg schem origin)";
        return schematic.name + " at " + origin.getX() + ", " + origin.getY() + ", " + origin.getZ()
            + ": " + placed + "/" + schematic.entries.size() + " placed";
    }

    @Override
    public String hudLabel() {
        if (schematic == null || origin == null) return name;
        return name + " " + placed + "/" + schematic.entries.size();
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (schematic == null || origin == null || done == null) return;
        if (mc.gameMode == null || mc.screen != null) return;
        if (timer > 0) {
            timer--;
            return;
        }

        double range = get("range");
        double rangeSq = range * range;
        Vec3 eye = player.getEyePosition();
        int budget = Math.max(1, (int) get("pertick"));
        List<Schematic.Entry> entries = schematic.entries;

        for (int i = 0; i < entries.size() && budget > 0; i++) {
            if (done[i]) continue;
            Schematic.Entry e = entries.get(i);
            BlockPos pos = origin.offset(e.rel());
            if (Vec3.atCenterOf(pos).distanceToSqr(eye) > rangeSq) continue;

            BlockState current = level.getBlockState(pos);
            if (current.is(e.state().getBlock())) {
                done[i] = true;
                placed++;
                continue;
            }
            if (!current.canBeReplaced()) continue;
            if (tryPlace(player, level, pos, e.state())) budget--;
        }
        timer = (int) get("delay");
    }

    private boolean tryPlace(LocalPlayer player, ClientLevel level, BlockPos pos, BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == net.minecraft.world.item.Items.AIR) return false;

        int slot = Inv.findHotbar(player, item);
        if (slot < 0) {
            int inv = Inv.findInventory(player, item);
            if (inv < 0) return false;
            int selected = player.getInventory().getSelectedSlot();
            Inv.click(player, Inv.menuSlot(inv), selected, ClickType.SWAP);
            return true; // item arrives next tick; this spends the budget
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState ns = level.getBlockState(neighbor);
            if (ns.isAir() || !ns.getFluidState().isEmpty() || ns.hasBlockEntity()) continue;

            Direction face = dir.getOpposite();
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(neighbor).relative(face, 0.5), face, neighbor, false);
            player.getInventory().setSelectedSlot(slot);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }
}
