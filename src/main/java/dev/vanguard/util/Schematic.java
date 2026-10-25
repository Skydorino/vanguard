package dev.vanguard.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads Litematica (.litematic) and vanilla structure (.nbt) files into a flat block list.
 * Coordinates are kept exactly as Litematica stores them, so a Litematica placement origin lines
 * up. "min" is the lowest corner of the schematic's regions, for anchoring at your feet instead.
 */
public final class Schematic {
    public record Entry(BlockPos rel, BlockState state) {}

    public final String name;
    public final List<Entry> entries = new ArrayList<>();
    public BlockPos min = BlockPos.ZERO;

    private Schematic(String name) {
        this.name = name;
    }

    public static Schematic load(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        String lower = fileName.toLowerCase(Locale.ROOT);
        CompoundTag root;
        try (InputStream in = Files.newInputStream(file)) {
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        Schematic s = new Schematic(fileName);
        if (lower.endsWith(".litematic")) s.readLitematic(root);
        else if (lower.endsWith(".nbt")) s.readStructure(root);
        else throw new IOException("Unsupported file type, use .litematic or .nbt");
        if (s.entries.isEmpty()) throw new IOException("No blocks found in " + fileName);
        s.entries.sort(Comparator.<Entry>comparingInt(e -> e.rel().getY())
            .thenComparingInt(e -> e.rel().getX())
            .thenComparingInt(e -> e.rel().getZ()));
        return s;
    }

    private static List<BlockState> readPalette(ListTag tag) {
        List<BlockState> out = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++) {
            out.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag.getCompoundOrEmpty(i)));
        }
        return out;
    }

    private void readStructure(CompoundTag root) {
        List<BlockState> palette = readPalette(root.getListOrEmpty("palette"));
        ListTag blocks = root.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompoundOrEmpty(i);
            ListTag pos = b.getListOrEmpty("pos");
            int idx = b.getIntOr("state", 0);
            if (idx < 0 || idx >= palette.size()) continue;
            BlockState st = palette.get(idx);
            if (st.isAir()) continue;
            entries.add(new Entry(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), st));
        }
    }

    private void readLitematic(CompoundTag root) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        CompoundTag regions = root.getCompoundOrEmpty("Regions");
        for (String key : regions.keySet()) {
            CompoundTag region = regions.getCompoundOrEmpty(key);
            CompoundTag pos = region.getCompoundOrEmpty("Position");
            CompoundTag size = region.getCompoundOrEmpty("Size");
            int rx = size.getIntOr("x", 0);
            int ry = size.getIntOr("y", 0);
            int rz = size.getIntOr("z", 0);
            int ax = Math.abs(rx), ay = Math.abs(ry), az = Math.abs(rz);
            int bx = pos.getIntOr("x", 0) + (rx < 0 ? rx + 1 : 0);
            int by = pos.getIntOr("y", 0) + (ry < 0 ? ry + 1 : 0);
            int bz = pos.getIntOr("z", 0) + (rz < 0 ? rz + 1 : 0);
            minX = Math.min(minX, bx);
            minY = Math.min(minY, by);
            minZ = Math.min(minZ, bz);

            List<BlockState> palette = readPalette(region.getListOrEmpty("BlockStatePalette"));
            long[] states = region.getLongArray("BlockStates").orElse(new long[0]);
            if (palette.isEmpty() || states.length == 0) continue;

            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            long mask = (1L << bits) - 1L;
            for (int y = 0; y < ay; y++) {
                for (int z = 0; z < az; z++) {
                    for (int x = 0; x < ax; x++) {
                        long index = ((long) y * az + z) * ax + x;
                        long bitIdx = index * bits;
                        int word = (int) (bitIdx >>> 6);
                        int off = (int) (bitIdx & 63);
                        if (word >= states.length) continue;
                        long v = states[word] >>> off;
                        int over = off + bits - 64;
                        if (over > 0 && word + 1 < states.length) v |= states[word + 1] << (bits - over);
                        int id = (int) (v & mask);
                        if (id >= palette.size()) continue;
                        BlockState st = palette.get(id);
                        if (st.isAir()) continue;
                        entries.add(new Entry(new BlockPos(bx + x, by + y, bz + z), st));
                    }
                }
            }
        }
        if (minX != Integer.MAX_VALUE) min = new BlockPos(minX, minY, minZ);
    }
}
