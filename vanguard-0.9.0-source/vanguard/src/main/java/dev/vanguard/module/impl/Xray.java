package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.phys.AABB;

/**
 * Real Xray: the terrain stops drawing every block except ores, storage and base blocks, so you can
 * see them through the ground. BlockMixin asks visible() for each block while chunks are rebuilt.
 * It also switches off cave culling so underground sections are drawn, and (optionally) draws boxes
 * over ores as a backup. Use it with Fullbright; the world looks empty apart from what you picked.
 */
public class Xray extends Module {
    private record Found(AABB box, int color) {}

    private static final Map<Block, Boolean> CACHE = new ConcurrentHashMap<>();
    private static volatile boolean seeThrough;
    private static Xray instance;

    private final Map<Block, Integer> always = new HashMap<>();
    private final Map<Block, Integer> iron = new HashMap<>();
    private final Map<Block, Integer> redstone = new HashMap<>();
    private final Map<Block, Integer> lapis = new HashMap<>();
    private final Map<Block, Integer> coal = new HashMap<>();
    private final Map<Block, Integer> copper = new HashMap<>();
    private List<Found> found = new ArrayList<>();
    private int timer;
    private int signature;
    private boolean oldSmartCull;
    private boolean smartCullSaved;

    public Xray() {
        super("Xray", Category.WORLD, "See ores, storage and bases through the ground");
        instance = this;
        setting("see", 1);        // 1 = make the terrain see-through
        setting("ores", 1);       // diamond, emerald, gold, ancient debris, quartz
        setting("iron", 0);
        setting("redstone", 0);
        setting("lapis", 0);
        setting("coal", 0);
        setting("copper", 0);
        setting("storage", 1);    // chests, barrels, shulker boxes, hoppers, furnaces, spawners
        setting("bases", 1);      // beds, doors, crafting tables, torches, obsidian, beacons
        setting("boxes", 1);      // 1 = also draw boxes over ores (backup if see-through does not work)
        setting("radius", 16);    // box scan radius
        setting("interval", 20);  // ticks between box scans

        always.put(Blocks.DIAMOND_ORE, 0xAA33E6FF);
        always.put(Blocks.DEEPSLATE_DIAMOND_ORE, 0xAA33E6FF);
        always.put(Blocks.ANCIENT_DEBRIS, 0xAAB5651D);
        always.put(Blocks.EMERALD_ORE, 0xAA33FF66);
        always.put(Blocks.DEEPSLATE_EMERALD_ORE, 0xAA33FF66);
        always.put(Blocks.GOLD_ORE, 0xAAFFD633);
        always.put(Blocks.DEEPSLATE_GOLD_ORE, 0xAAFFD633);
        always.put(Blocks.NETHER_GOLD_ORE, 0xAAFFD633);
        always.put(Blocks.NETHER_QUARTZ_ORE, 0xAAEEEEEE);
        always.put(Blocks.SPAWNER, 0xAAC800FF);
        iron.put(Blocks.IRON_ORE, 0x99D8AF93);
        iron.put(Blocks.DEEPSLATE_IRON_ORE, 0x99D8AF93);
        redstone.put(Blocks.REDSTONE_ORE, 0x99FF2020);
        redstone.put(Blocks.DEEPSLATE_REDSTONE_ORE, 0x99FF2020);
        lapis.put(Blocks.LAPIS_ORE, 0x993355FF);
        lapis.put(Blocks.DEEPSLATE_LAPIS_ORE, 0x993355FF);
        coal.put(Blocks.COAL_ORE, 0x99444444);
        coal.put(Blocks.DEEPSLATE_COAL_ORE, 0x99444444);
        copper.put(Blocks.COPPER_ORE, 0x99E07B39);
        copper.put(Blocks.DEEPSLATE_COPPER_ORE, 0x99E07B39);
    }

    // ---- see-through terrain (called by BlockMixin, possibly from chunk-building threads) ----

    public static boolean seeThrough() {
        return seeThrough;
    }

    /** True if this block should still be drawn while Xray is on. */
    public static boolean visible(Block block) {
        return CACHE.computeIfAbsent(block, Xray::compute);
    }

    private static boolean compute(Block b) {
        Xray x = instance;
        if (x == null) return true;
        if (x.get("ores") > 0 && (x.always.containsKey(b))) return true;
        if (x.get("iron") > 0 && x.iron.containsKey(b)) return true;
        if (x.get("redstone") > 0 && x.redstone.containsKey(b)) return true;
        if (x.get("lapis") > 0 && x.lapis.containsKey(b)) return true;
        if (x.get("coal") > 0 && x.coal.containsKey(b)) return true;
        if (x.get("copper") > 0 && x.copper.containsKey(b)) return true;
        if (x.get("storage") > 0 && (b instanceof ChestBlock || b instanceof EnderChestBlock || b instanceof BarrelBlock
            || b instanceof ShulkerBoxBlock || b instanceof HopperBlock || b instanceof AbstractFurnaceBlock
            || b instanceof SpawnerBlock)) return true;
        if (x.get("bases") > 0 && (b instanceof BedBlock || b instanceof DoorBlock || b instanceof CraftingTableBlock
            || b instanceof BeaconBlock || b == Blocks.OBSIDIAN || b == Blocks.CRYING_OBSIDIAN
            || b == Blocks.TORCH || b == Blocks.WALL_TORCH || b == Blocks.SOUL_TORCH || b == Blocks.SOUL_WALL_TORCH
            || b == Blocks.LANTERN || b == Blocks.SOUL_LANTERN)) return true;
        return false;
    }

    private int currentSignature() {
        return Objects.hash(get("see"), get("ores"), get("iron"), get("redstone"), get("lapis"),
            get("coal"), get("copper"), get("storage"), get("bases"));
    }

    private void setSmartCull(boolean off) {
        try {
            String name = FabricLoader.getInstance().getMappingResolver()
                .mapFieldName("named", "net.minecraft.client.Minecraft", "smartCull", "Z");
            Field f = Minecraft.class.getDeclaredField(name);
            f.setAccessible(true);
            if (!smartCullSaved) {
                oldSmartCull = f.getBoolean(mc);
                smartCullSaved = true;
            }
            f.setBoolean(mc, off ? false : oldSmartCull);
            if (!off) smartCullSaved = false;
        } catch (Throwable ignored) {
            // If this field is gone, only sections reachable through open air will show
        }
    }

    private void refresh(boolean on) {
        boolean want = on && get("see") > 0;
        CACHE.clear();
        seeThrough = want;
        setSmartCull(want);
        signature = currentSignature();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged(); // rebuild chunks with the new rules
    }

    @Override
    protected void onEnable() {
        refresh(true);
    }

    @Override
    protected void onDisable() {
        found = new ArrayList<>();
        refresh(false);
    }

    // ---- backup ore boxes ----

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (currentSignature() != signature) refresh(true);

        if (get("boxes") <= 0) {
            found = new ArrayList<>();
            return;
        }
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        int r = Math.max(1, Math.min(48, (int) get("radius")));
        boolean useIron = get("iron") > 0;
        boolean useRedstone = get("redstone") > 0;
        boolean useLapis = get("lapis") > 0;
        boolean useCoal = get("coal") > 0;
        boolean useCopper = get("copper") > 0;
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<Found> next = new ArrayList<>();

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    Block block = level.getBlockState(cursor).getBlock();
                    Integer color = get("ores") > 0 || block == Blocks.SPAWNER ? always.get(block) : null;
                    if (color == null && useIron) color = iron.get(block);
                    if (color == null && useRedstone) color = redstone.get(block);
                    if (color == null && useLapis) color = lapis.get(block);
                    if (color == null && useCoal) color = coal.get(block);
                    if (color == null && useCopper) color = copper.get(block);
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
