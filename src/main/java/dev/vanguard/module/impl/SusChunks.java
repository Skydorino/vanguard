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
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

/**
 * Scores every loaded chunk by how many blocks that almost never generate naturally it contains
 * (redstone parts, concrete, glass, obsidian, metal blocks, shulker boxes, beacons...). Chunks over
 * the threshold are logged and marked with a tall beam through walls. Villages, mineshafts and
 * the like add a few of these, so raise minscore if you get false hits.
 */
public class SusChunks extends Module {
    public record Sus(int x, int y, int z, int score) {}

    private static final Set<String> THREE = Set.of(
        "piston", "sticky_piston", "observer", "repeater", "comparator", "dispenser", "dropper", "hopper",
        "redstone_lamp", "note_block", "daylight_detector", "lever", "tripwire_hook", "target",
        "redstone_block", "tnt", "ender_chest");
    private static final Set<String> TWO = Set.of(
        "obsidian", "crying_obsidian", "iron_block", "gold_block", "diamond_block", "emerald_block",
        "lapis_block", "netherite_block", "quartz_block", "smooth_quartz", "enchanting_table", "anvil",
        "chipped_anvil", "damaged_anvil", "respawn_anchor", "lodestone");

    private static Map<Block, Integer> weights;

    private final Map<Long, Sus> found = new LinkedHashMap<>();
    private int timer;

    public SusChunks() {
        super("SusChunks", Category.DONUTSMP, "Flags chunks full of blocks that rarely generate naturally");
        setting("radius", 8);       // chunks around you to check
        setting("interval", 100);   // ticks between scans
        setting("minscore", 12);    // points a chunk needs to be flagged
        setting("highlight", 1);    // 1 = draw a beam over flagged chunks
    }

    private static int weightOf(String p) {
        if (p.endsWith("_concrete") || p.endsWith("_concrete_powder") || p.endsWith("glazed_terracotta")) return 1;
        if (p.equals("glass") || p.equals("glass_pane") || p.contains("stained_glass")) return 1;
        if (p.endsWith("shulker_box")) return 3;
        if (p.equals("beacon")) return 8;
        if (THREE.contains(p)) return 3;
        if (TWO.contains(p)) return 2;
        return 0;
    }

    private static synchronized Map<Block, Integer> weights() {
        if (weights == null) {
            Map<Block, Integer> map = new HashMap<>();
            for (Block b : BuiltInRegistries.BLOCK) {
                int w = weightOf(BuiltInRegistries.BLOCK.getKey(b).getPath());
                if (w > 0) map.put(b, w);
            }
            weights = map;
        }
        return weights;
    }

    @Override
    protected void onDisable() {
        found.clear();
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        Map<Block, Integer> w = weights();
        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        int radius = Math.max(1, Math.min(16, (int) get("radius")));
        int min = (int) get("minscore");

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;

                int score = 0;
                int bestW = 0;
                int rx = 0, ry = 0, rz = 0;
                ChunkPos cp = chunk.getPos();
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section.hasOnlyAir() || !section.maybeHas(s -> w.containsKey(s.getBlock()))) continue;
                    int baseY = (chunk.getMinSectionY() + i) << 4;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                Integer weight = w.get(section.getBlockState(x, y, z).getBlock());
                                if (weight == null) continue;
                                score += weight;
                                if (weight >= bestW) {
                                    bestW = weight;
                                    rx = cp.getMinBlockX() + x;
                                    ry = baseY + y;
                                    rz = cp.getMinBlockZ() + z;
                                }
                            }
                        }
                    }
                }

                long key = ChunkPos.asLong(cx + dx, cz + dz);
                if (score < min) {
                    found.remove(key);
                    continue;
                }
                boolean isNew = !found.containsKey(key);
                found.put(key, new Sus(rx, ry, rz, score));
                if (isNew) {
                    player.displayClientMessage(Component.literal("[Vanguard] Sus chunk near " + rx + ", " + ry + ", " + rz
                        + " (score " + score + ")").withStyle(s -> s.withColor(0xFF8A00)), false);
                }
            }
        }
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        if (get("highlight") <= 0) return;
        for (Sus s : found.values()) {
            int alpha = Math.min(0xA0, 0x40 + s.score() * 2);
            draw.box(new AABB(s.x() + 0.35, s.y() - 64, s.z() + 0.35, s.x() + 0.65, s.y() + 64, s.z() + 0.65),
                Draw.alpha(0xFF8A00, alpha));
            draw.box(new AABB(s.x(), s.y(), s.z(), s.x() + 1, s.y() + 1, s.z() + 1), Draw.alpha(0xFF8A00, 0xA0));
        }
    }

    public List<Sus> hits() {
        return new ArrayList<>(found.values());
    }

    public void clear() {
        found.clear();
    }

    /** Closest flagged chunk to the player, or null. */
    public Sus nearest(LocalPlayer player) {
        Sus best = null;
        double bestSq = Double.MAX_VALUE;
        for (Sus s : found.values()) {
            double dx = s.x() - player.getX();
            double dz = s.z() - player.getZ();
            double d = dx * dx + dz * dz;
            if (d < bestSq) {
                bestSq = d;
                best = s;
            }
        }
        return best;
    }
}
