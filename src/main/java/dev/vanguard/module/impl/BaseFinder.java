package dev.vanguard.module.impl;

import dev.vanguard.gui.Draw;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * Scans the chunks your client has loaded for clusters of storage blocks. Chunks with enough of
 * them are logged as possible bases, and the storage is highlighted through walls:
 * chests orange, shulker boxes pink, barrels brown, hoppers grey.
 * Dungeons, mineshafts and villages also have chests, so expect some false positives.
 */
public class BaseFinder extends Module {
    public record Hit(int x, int y, int z, int storage, int chests, int shulkers, int barrels) {}

    private record Marker(BlockPos pos, int color) {}

    private static final int CHEST = 0xFF8800;
    private static final int SHULKER = 0xFF69B4;
    private static final int BARREL = 0x8B5A2B;
    private static final int HOPPER = 0x9AA0A6;

    private final Map<Long, Hit> found = new LinkedHashMap<>();
    private final Map<Long, List<Marker>> markers = new HashMap<>();
    private int timer;

    public BaseFinder() {
        super("BaseFinder", Category.DONUTSMP, "Flags chunks full of storage and highlights it through walls");
        setting("interval", 40);   // ticks between scans
        setting("minstorage", 4);  // storage blocks in one chunk to flag it
        setting("radius", 12);     // chunks around you to check (only loaded ones exist)
        setting("highlight", 1);   // 1 = draw boxes through walls
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        int radius = (int) get("radius");
        int min = (int) get("minstorage");

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;

                int chests = 0, shulkers = 0, barrels = 0, hoppers = 0;
                BlockPos rep = null;
                List<Marker> list = new ArrayList<>();
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    int color;
                    if (be instanceof ChestBlockEntity) { chests++; color = CHEST; }
                    else if (be instanceof ShulkerBoxBlockEntity) { shulkers++; color = SHULKER; }
                    else if (be instanceof BarrelBlockEntity) { barrels++; color = BARREL; }
                    else if (be instanceof HopperBlockEntity) { hoppers++; color = HOPPER; }
                    else continue;
                    rep = be.getBlockPos();
                    list.add(new Marker(rep, color));
                }
                int storage = chests + shulkers + barrels + hoppers;
                long key = ChunkPos.asLong(cx + dx, cz + dz);

                if (rep == null || storage < min) {
                    markers.remove(key);
                    continue;
                }
                markers.put(key, list);
                Hit old = found.get(key);
                found.put(key, new Hit(rep.getX(), rep.getY(), rep.getZ(), storage, chests, shulkers, barrels));
                if (old == null) announce(player, rep, chests, shulkers, barrels);
            }
        }
    }

    private static void announce(LocalPlayer player, BlockPos at, int chests, int shulkers, int barrels) {
        player.displayClientMessage(Component.literal("[Vanguard] Possible base near "
                + at.getX() + ", " + at.getY() + ", " + at.getZ() + ": ")
            .append(Component.literal(chests + " chests ").withStyle(s -> s.withColor(CHEST)))
            .append(Component.literal(shulkers + " shulkers ").withStyle(s -> s.withColor(SHULKER)))
            .append(Component.literal(barrels + " barrels").withStyle(s -> s.withColor(BARREL))), false);
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        if (get("highlight") <= 0) return;
        for (List<Marker> list : markers.values()) {
            for (Marker m : list) {
                draw.box(new AABB(m.pos()).inflate(0.003), Draw.alpha(m.color(), 120));
            }
        }
    }

    public List<Hit> hits() {
        return new ArrayList<>(found.values());
    }

    public void clear() {
        found.clear();
        markers.clear();
    }

    /** Closest flagged base to the player, or null. */
    public Hit nearest(LocalPlayer player) {
        Hit best = null;
        double bestSq = Double.MAX_VALUE;
        for (Hit h : found.values()) {
            double dx = h.x() - player.getX();
            double dz = h.z() - player.getZ();
            double d = dx * dx + dz * dz;
            if (d < bestSq) {
                bestSq = d;
                best = h;
            }
        }
        return best;
    }
}
