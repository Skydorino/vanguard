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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Builds a loaded schematic block by block, bottom-up, using blocks from your inventory. Load
 * from the Litematica folder (/vg schem list|load) or from a saved Litematica placement
 * (/vg schem litematica, then /vg schem use N), which keeps Litematica's own origin. It tells you
 * what is holding it up (missing item, out of range, nothing to attach to) next to its name on the
 * HUD. It places the block type only, not block state, so it suits full blocks.
 */
public class SchematicBuilder extends Module {
    private static final int PLACED = 0;
    private static final int NO_ITEM = 1;
    private static final int NO_SUPPORT = 2;
    private static final int SWAPPED = 3;

    private Schematic schematic;
    private BlockPos origin;
    private boolean placementOrigin;
    private boolean[] done;
    private int placed;
    private int timer;
    private String issue = "";

    public SchematicBuilder() {
        super("SchematicBuilder", Category.DONUTSMP, "Builds a Litematica schematic from your inventory");
        setting("range", 4.0);
        setting("pertick", 1);
        setting("delay", 1);
    }

    public String load(Path file) throws IOException {
        schematic = Schematic.load(file);
        origin = null;
        done = null;
        placed = 0;
        issue = "";
        return schematic.name + ": " + schematic.entries.size() + " blocks";
    }

    public boolean hasSchematic() {
        return schematic != null;
    }

    /**
     * placementOrigin false: the schematic's lowest corner sits at pos (stand at the corner).
     * placementOrigin true: pos is a Litematica placement origin, used exactly as Litematica does.
     */
    public void setOrigin(BlockPos pos, boolean placementOrigin) {
        origin = pos;
        this.placementOrigin = placementOrigin;
        if (schematic != null) {
            done = new boolean[schematic.entries.size()];
            placed = 0;
            issue = "";
        }
    }

    public void clear() {
        schematic = null;
        origin = null;
        done = null;
        placed = 0;
        issue = "";
    }

    private BlockPos worldPos(Schematic.Entry e) {
        BlockPos rel = placementOrigin ? e.rel() : e.rel().subtract(schematic.min);
        return origin.offset(rel);
    }

    public String status() {
        if (schematic == null) return "No schematic loaded";
        if (origin == null) return schematic.name + " loaded, origin not set (use /vg schem origin)";
        return schematic.name + " at " + origin.getX() + ", " + origin.getY() + ", " + origin.getZ()
            + ": " + placed + "/" + schematic.entries.size() + " placed" + (issue.isEmpty() ? "" : " (" + issue + ")");
    }

    @Override
    public String hudLabel() {
        if (schematic == null || origin == null) return name;
        return name + " " + placed + "/" + schematic.entries.size() + (issue.isEmpty() ? "" : " - " + issue);
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
        boolean placedAny = false;
        boolean anyInRange = false;
        String blocked = "";
        List<Schematic.Entry> entries = schematic.entries;

        for (int i = 0; i < entries.size() && budget > 0; i++) {
            if (done[i]) continue;
            Schematic.Entry e = entries.get(i);
            BlockPos pos = worldPos(e);
            if (Vec3.atCenterOf(pos).distanceToSqr(eye) > rangeSq) continue;
            anyInRange = true;

            BlockState current = level.getBlockState(pos);
            if (current.is(e.state().getBlock())) {
                done[i] = true;
                placed++;
                continue;
            }
            if (!current.canBeReplaced()) continue;

            int result = tryPlace(player, level, pos, e.state());
            if (result == PLACED || result == SWAPPED) {
                budget--;
                placedAny = true;
            } else if (result == NO_ITEM) {
                blocked = "need " + BuiltInRegistries.ITEM.getKey(e.state().getBlock().asItem()).getPath();
            } else if (result == NO_SUPPORT) {
                blocked = "nothing to attach to";
            }
        }
        if (placedAny) issue = "";
        else if (!anyInRange) issue = placed >= entries.size() ? "done" : "move closer (range " + range + ")";
        else issue = blocked;
        timer = (int) get("delay");
    }

    private int tryPlace(LocalPlayer player, ClientLevel level, BlockPos pos, BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == net.minecraft.world.item.Items.AIR) return NO_ITEM;

        int slot = Inv.findHotbar(player, item);
        if (slot < 0) {
            int inv = Inv.findInventory(player, item);
            if (inv < 0) return NO_ITEM;
            int selected = player.getInventory().getSelectedSlot();
            Inv.click(player, Inv.menuSlot(inv), selected, ClickType.SWAP);
            return SWAPPED; // the item arrives next tick; this spends the budget
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
            return PLACED;
        }
        return NO_SUPPORT;
    }
}
