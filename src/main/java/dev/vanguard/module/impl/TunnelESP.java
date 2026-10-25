package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Looks for long, straight, 2-high tunnels with solid walls, floor and roof (the shape players
 * dig) in the chunks you have loaded, and highlights them through walls. Mineshafts and
 * ravines can look similar, so expect some false positives.
 */
public class TunnelESP extends Module {
    private List<AABB> tunnels = new ArrayList<>();
    private int timer;

    public TunnelESP() {
        super("TunnelESP", Category.DONUTSMP, "Highlights long straight tunnels dug by players");
        setting("radius", 24);      // blocks around you to scan
        setting("vrange", 12);      // blocks above and below you to scan
        setting("minlength", 8);    // shortest tunnel to show
        setting("interval", 40);    // ticks between scans
    }

    @Override
    protected void onDisable() {
        tunnels = new ArrayList<>();
    }

    private static boolean solid(ClientLevel level, BlockPos.MutableBlockPos p, int x, int y, int z) {
        p.set(x, y, z);
        BlockState s = level.getBlockState(p);
        return !s.isAir() && s.getFluidState().isEmpty() && s.blocksMotion();
    }

    private static boolean air(ClientLevel level, BlockPos.MutableBlockPos p, int x, int y, int z) {
        p.set(x, y, z);
        return level.getBlockState(p).isAir();
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        int r = Math.max(4, Math.min(64, (int) get("radius")));
        int v = Math.max(2, Math.min(32, (int) get("vrange")));
        int min = Math.max(3, (int) get("minlength"));
        BlockPos o = player.blockPosition();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        Set<Long> xCells = new HashSet<>();
        Set<Long> zCells = new HashSet<>();

        for (int x = o.getX() - r; x <= o.getX() + r; x++) {
            for (int z = o.getZ() - r; z <= o.getZ() + r; z++) {
                for (int y = o.getY() - v; y <= o.getY() + v; y++) {
                    if (!air(level, c, x, y, z) || !air(level, c, x, y + 1, z)) continue;
                    if (!solid(level, c, x, y - 1, z) || !solid(level, c, x, y + 2, z)) continue;
                    boolean wallsZ = solid(level, c, x, y, z - 1) && solid(level, c, x, y, z + 1)
                        && solid(level, c, x, y + 1, z - 1) && solid(level, c, x, y + 1, z + 1);
                    boolean wallsX = solid(level, c, x - 1, y, z) && solid(level, c, x + 1, y, z)
                        && solid(level, c, x - 1, y + 1, z) && solid(level, c, x + 1, y + 1, z);
                    if (wallsZ) xCells.add(BlockPos.asLong(x, y, z));   // corridor running along X
                    if (wallsX) zCells.add(BlockPos.asLong(x, y, z));   // corridor running along Z
                }
            }
        }

        List<AABB> next = new ArrayList<>();
        collect(xCells, true, min, next);
        collect(zCells, false, min, next);
        tunnels = next;
    }

    private static void collect(Set<Long> cells, boolean alongX, int min, List<AABB> out) {
        for (long key : cells) {
            int x = BlockPos.getX(key), y = BlockPos.getY(key), z = BlockPos.getZ(key);
            long before = alongX ? BlockPos.asLong(x - 1, y, z) : BlockPos.asLong(x, y, z - 1);
            if (cells.contains(before)) continue; // not the start of a run
            int len = 1;
            while (cells.contains(alongX ? BlockPos.asLong(x + len, y, z) : BlockPos.asLong(x, y, z + len))) len++;
            if (len >= min) {
                out.add(alongX
                    ? new AABB(x, y, z, x + len, y + 2, z + 1)
                    : new AABB(x, y, z, x + 1, y + 2, z + len));
            }
        }
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        for (AABB b : tunnels) draw.box(b, 0x55FFD633);
    }
}
