package dev.vanguard.module.impl;

import dev.vanguard.gui.Draw;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * Highlights shulker boxes specifically — the highest-value loot on DonutSMP.
 * Shulkers in the wild almost always mean a stash or a dead player's drop.
 * Announces coords in chat when a new one is spotted. Draws pink boxes through walls.
 */
public class StashRadar extends Module {
    private record Entry(BlockPos pos) {}
    private final Map<BlockPos, Entry> found = new LinkedHashMap<>();
    private int timer;

    public StashRadar() {
        super("StashRadar", Category.DONUTSMP, "Highlights shulker boxes through walls and announces their coords");
        setting("radius", 16);    // chunk radius
        setting("interval", 30);  // ticks between scans
        setting("announce", 1);   // 1 = chat message on new find
    }

    @Override
    protected void onDisable() { found.clear(); }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        int radius = (int) get("radius");

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof ShulkerBoxBlockEntity)) continue;
                    BlockPos pos = be.getBlockPos();
                    if (found.containsKey(pos)) continue;
                    found.put(pos, new Entry(pos));
                    if (get("announce") > 0) {
                        player.displayClientMessage(
                            Component.literal("[Vanguard] Shulker at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                            false);
                    }
                }
            }
        }

        // Clean up entries no longer in loaded chunks
        found.entrySet().removeIf(e -> level.getChunkSource().getChunkNow(
            e.getKey().getX() >> 4, e.getKey().getZ() >> 4) == null);
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        for (Entry e : found.values()) {
            draw.box(new AABB(e.pos()).inflate(0.04), Draw.alpha(0xFF69B4, 180));
        }
    }

    public List<BlockPos> positions() { return new ArrayList<>(found.keySet()); }
    public void clear() { found.clear(); }
}
// END OF FILE