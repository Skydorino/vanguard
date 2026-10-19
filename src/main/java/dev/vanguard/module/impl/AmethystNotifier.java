package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Amethyst only grows while its chunk is loaded, so growth you did not cause means a player (or
 * a chunk loader) is keeping that area active. Tracks amethyst buds in loaded chunks and tells
 * you when enough of them have grown between scans.
 */
public class AmethystNotifier extends Module {
    private final Map<Long, Integer> stages = new HashMap<>();
    private final Set<Long> scanned = new HashSet<>();
    private int timer;

    public AmethystNotifier() {
        super("AmethystNotifier", Category.WORLD, "Alerts you when amethyst grows in loaded chunks");
        setting("radius", 5);       // chunks around you to check
        setting("interval", 100);   // ticks between scans
        setting("threshold", 3);    // growth events in one scan to alert
    }

    @Override
    protected void onDisable() {
        stages.clear();
        scanned.clear();
    }

    private static int stageOf(Block b) {
        if (b == Blocks.SMALL_AMETHYST_BUD) return 1;
        if (b == Blocks.MEDIUM_AMETHYST_BUD) return 2;
        if (b == Blocks.LARGE_AMETHYST_BUD) return 3;
        if (b == Blocks.AMETHYST_CLUSTER) return 4;
        return 0;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        int radius = Math.max(1, Math.min(10, (int) get("radius")));
        int growth = 0;
        int atX = 0, atY = 0, atZ = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;
                long chunkKey = ChunkPos.asLong(cx + dx, cz + dz);
                boolean known = scanned.contains(chunkKey);   // new chunks only set a baseline
                ChunkPos cp = chunk.getPos();

                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section.hasOnlyAir() || !section.maybeHas(s -> s.is(Blocks.BUDDING_AMETHYST))) continue;
                    int baseY = (chunk.getMinSectionY() + i) << 4;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                int stage = stageOf(section.getBlockState(x, y, z).getBlock());
                                if (stage == 0) continue;
                                int wx = cp.getMinBlockX() + x;
                                int wy = baseY + y;
                                int wz = cp.getMinBlockZ() + z;
                                long key = net.minecraft.core.BlockPos.asLong(wx, wy, wz);
                                Integer old = stages.get(key);
                                if (known && (old == null || stage > old)) {
                                    growth++;
                                    atX = wx;
                                    atY = wy;
                                    atZ = wz;
                                }
                                stages.put(key, stage);
                            }
                        }
                    }
                }
                scanned.add(chunkKey);
            }
        }

        if (growth >= Math.max(1, (int) get("threshold"))) {
            player.displayClientMessage(Component.literal("[Vanguard] Amethyst grew " + growth
                + " times near " + atX + ", " + atY + ", " + atZ + " - that area is being kept loaded"), false);
        }
    }
}
