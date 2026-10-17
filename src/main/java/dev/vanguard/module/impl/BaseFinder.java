package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import java.util.ArrayList;
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

/**
 * Scans the chunks your client has loaded for clusters of storage blocks (chests, barrels,
 * shulkers, hoppers). Chunks with enough of them are logged as possible bases/stashes.
 * Dungeons, mineshafts and villages also have chests, so expect some false positives.
 */
public class BaseFinder extends Module {
    public record Hit(int x, int y, int z, int storage) {}

    private final Map<Long, Hit> found = new LinkedHashMap<>();
    private int timer;

    public BaseFinder() {
        super("BaseFinder", Category.WORLD, "Flags loaded chunks with lots of storage blocks as possible bases");
        setting("interval", 40);   // ticks between scans
        setting("minstorage", 4);  // storage blocks in one chunk to flag it
        setting("radius", 12);     // chunks around you to check (only loaded ones exist)
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

                int storage = 0;
                BlockPos rep = null;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (isStorage(be)) {
                        storage++;
                        rep = be.getBlockPos();
                    }
                }
                if (rep == null || storage < min) continue;

                long key = ChunkPos.asLong(cx + dx, cz + dz);
                Hit old = found.get(key);
                if (old == null || storage > old.storage()) {
                    found.put(key, new Hit(rep.getX(), rep.getY(), rep.getZ(), storage));
                    if (old == null) {
                        player.displayClientMessage(Component.literal("[Vanguard] Possible base near "
                            + rep.getX() + ", " + rep.getY() + ", " + rep.getZ() + " (" + storage + " storage)"), false);
                    }
                }
            }
        }
    }

    private static boolean isStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity
            || be instanceof ShulkerBoxBlockEntity || be instanceof HopperBlockEntity;
    }

    public List<Hit> hits() {
        return new ArrayList<>(found.values());
    }

    public void clear() {
        found.clear();
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
