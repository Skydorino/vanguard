package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Highlights ores (and spawners) through walls around you. It draws boxes over the ores rather
 * than making stone transparent, and only sees chunks your client has loaded.
 */
public class Xray extends Module {
    private record Found(AABB box, int color) {}

    private final Map<Block, Integer> always = new HashMap<>();
    private final Map<Block, Integer> iron = new HashMap<>();
    private final Map<Block, Integer> redstone = new HashMap<>();
    private final Map<Block, Integer> lapis = new HashMap<>();
    private List<Found> found = new ArrayList<>();
    private int timer;

    public Xray() {
        super("Xray", Category.WORLD, "Highlights ores through walls");
        setting("radius", 16);
        setting("interval", 20);  // ticks between scans
        setting("iron", 0);
        setting("redstone", 0);
        setting("lapis", 0);

        always.put(Blocks.DIAMOND_ORE, 0xAA33E6FF);
        always.put(Blocks.DEEPSLATE_DIAMOND_ORE, 0xAA33E6FF);
        always.put(Blocks.ANCIENT_DEBRIS, 0xAAB5651D);
        always.put(Blocks.EMERALD_ORE, 0xAA33FF66);
        always.put(Blocks.DEEPSLATE_EMERALD_ORE, 0xAA33FF66);
        always.put(Blocks.GOLD_ORE, 0xAAFFD633);
        always.put(Blocks.DEEPSLATE_GOLD_ORE, 0xAAFFD633);
        always.put(Blocks.NETHER_GOLD_ORE, 0xAAFFD633);
        always.put(Blocks.SPAWNER, 0xAAC800FF);
        iron.put(Blocks.IRON_ORE, 0x99D8AF93);
        iron.put(Blocks.DEEPSLATE_IRON_ORE, 0x99D8AF93);
        redstone.put(Blocks.REDSTONE_ORE, 0x99FF2020);
        redstone.put(Blocks.DEEPSLATE_REDSTONE_ORE, 0x99FF2020);
        lapis.put(Blocks.LAPIS_ORE, 0x993355FF);
        lapis.put(Blocks.DEEPSLATE_LAPIS_ORE, 0x993355FF);
    }

    @Override
    protected void onDisable() {
        found = new ArrayList<>();
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        int r = Math.max(1, Math.min(48, (int) get("radius")));
        boolean useIron = get("iron") > 0;
        boolean useRedstone = get("redstone") > 0;
        boolean useLapis = get("lapis") > 0;
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<Found> next = new ArrayList<>();

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    Block block = level.getBlockState(cursor).getBlock();
                    Integer color = always.get(block);
                    if (color == null && useIron) color = iron.get(block);
                    if (color == null && useRedstone) color = redstone.get(block);
                    if (color == null && useLapis) color = lapis.get(block);
                    if (color != null) next.add(new Found(new AABB(cursor).inflate(0.002), color));
                }
            }
        }
        found = next;
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        for (Found f : found) draw.box(f.box(), f.color());
    }
}
