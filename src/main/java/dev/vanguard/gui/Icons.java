package dev.vanguard.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Small pixel-art icons drawn with fills, tinted with whatever colour you pass in. Index order
 * matches the Category enum: combat, movement, world, visuals, misc. Each is 11 by 11.
 */
public final class Icons {
    public static final int SIZE = 11;

    private static final String[][] PIXELS = {
        { // combat: crossed blades
            "##.......##",
            "###.....###",
            ".###...###.",
            "..###.###..",
            "...#####...",
            "....###....",
            "...#####...",
            "..###.###..",
            ".###...###.",
            "###.....###",
            "##.......##"},
        { // movement: speed chevrons
            "...........",
            "..#...#....",
            "...#...#...",
            "....#...#..",
            ".....#...#.",
            "......#...#",
            ".....#...#.",
            "....#...#..",
            "...#...#...",
            "..#...#....",
            "..........."},
        { // world: globe
            "...#####...",
            "..#..#..#..",
            ".#...#...#.",
            "#....#....#",
            "#....#....#",
            "###########",
            "#....#....#",
            "#....#....#",
            ".#...#...#.",
            "..#..#..#..",
            "...#####..."},
        { // visuals: eye
            "...........",
            "....###....",
            "..##...##..",
            ".#..###..#.",
            "#..#####..#",
            "#..#####..#",
            "#..#####..#",
            ".#..###..#.",
            "..##...##..",
            "....###....",
            "..........."},
        { // misc: four squares
            "...........",
            ".####.####.",
            ".####.####.",
            ".####.####.",
            ".####.####.",
            "...........",
            ".####.####.",
            ".####.####.",
            ".####.####.",
            ".####.####.",
            "..........."}
    };

    /** For each icon and row: start/length pairs of filled runs, so a row costs one fill per run. */
    private static final int[][][] RUNS = new int[PIXELS.length][][];

    static {
        for (int i = 0; i < PIXELS.length; i++) {
            RUNS[i] = new int[PIXELS[i].length][];
            for (int r = 0; r < PIXELS[i].length; r++) {
                String row = PIXELS[i][r];
                List<Integer> runs = new ArrayList<>();
                int c = 0;
                while (c < row.length()) {
                    if (row.charAt(c) == '#') {
                        int start = c;
                        while (c < row.length() && row.charAt(c) == '#') c++;
                        runs.add(start);
                        runs.add(c - start);
                    } else {
                        c++;
                    }
                }
                RUNS[i][r] = runs.stream().mapToInt(Integer::intValue).toArray();
            }
        }
    }

    private Icons() {}

    public static void draw(GuiGraphics g, int index, int x, int y, int color) {
        if (index < 0 || index >= RUNS.length) return;
        int[][] rows = RUNS[index];
        for (int r = 0; r < rows.length; r++) {
            int[] runs = rows[r];
            for (int k = 0; k < runs.length; k += 2) {
                g.fill(x + runs[k], y + r, x + runs[k] + runs[k + 1], y + r + 1, color);
            }
        }
    }
}
