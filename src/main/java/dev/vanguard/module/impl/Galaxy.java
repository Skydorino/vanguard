package dev.vanguard.module.impl;

import dev.vanguard.gui.Draw;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Paints a slowly drifting galaxy over the Overworld sky. It is drawn behind terrain (terrain
 * hides it), but it covers the vanilla sun, moon and stars. The shape is built once and the
 * colours are refreshed a few times a second, so the per-frame cost is only emitting the quads.
 */
public class Galaxy extends Module {
    private static final int STARS = 360;
    private static final double RADIUS = 220.0;
    private static final int LON = 48;
    private static final int LAT = 24;

    private final double[][] stars = new double[STARS][4];   // x, y, z, twinkle phase
    private final Vec3[][] offsets = new Vec3[LON + 1][LAT + 1];  // sphere points relative to the camera
    private final int[][] colors = new int[LON + 1][LAT + 1];
    private final Vec3[] starCenters = new Vec3[STARS];
    private final Vec3[] starU = new Vec3[STARS];
    private final Vec3[] starV = new Vec3[STARS];
    private final Vec3[][] frame = new Vec3[LON + 1][LAT + 1];
    private long colorStamp;

    public Galaxy() {
        super("Galaxy", Category.VISUALS, "Replaces the sky with a drifting galaxy");
        setting("palette", 0);   // 0 purple/blue, 1 pink/orange, 2 green/teal, 3 rainbow
        setting("speed", 1.0);

        for (int i = 0; i <= LON; i++) {
            double lon = Math.PI * 2.0 * i / LON;
            for (int j = 0; j <= LAT; j++) {
                double lat = -Math.PI / 2.0 + Math.PI * j / LAT;
                offsets[i][j] = dir(lon, lat).scale(RADIUS);
            }
        }

        Random rnd = new Random(1337);
        for (int s = 0; s < STARS; s++) {
            double z = rnd.nextDouble() * 2.0 - 1.0;
            double a = rnd.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(1.0 - z * z);
            stars[s][0] = r * Math.cos(a);
            stars[s][1] = z;
            stars[s][2] = r * Math.sin(a);
            stars[s][3] = rnd.nextDouble() * Math.PI * 2.0;

            Vec3 d = new Vec3(stars[s][0], stars[s][1], stars[s][2]);
            Vec3 u = d.cross(Math.abs(d.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            starCenters[s] = d.scale(RADIUS - 1.0);
            starU[s] = u.scale(0.9);
            starV[s] = d.cross(u).scale(0.9);
        }
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static Vec3 dir(double lon, double lat) {
        double cl = Math.cos(lat);
        return new Vec3(cl * Math.cos(lon), Math.sin(lat), cl * Math.sin(lon));
    }

    private static int color(double x, double y, double z, double t, int palette) {
        double n = clamp01(0.5 + 0.5 * Math.sin(x * 2.3 + t * 3.0) * Math.cos(y * 3.1 - t * 2.0) + 0.35 * Math.sin(z * 4.0 + x * 1.7 + t));
        double m = clamp01(0.5 + 0.5 * Math.sin(x * 5.0 - z * 3.0 + t * 2.0));
        double band = Math.exp(-Math.pow(y * 2.0 + 0.15 * Math.sin(x * 3.0 + t), 2.0)); // bright galactic band

        int dark, c1, c2;
        switch (palette) {
            case 1 -> { dark = 0x12040A; c1 = 0xFF4DA6; c2 = 0xFF8A3D; }
            case 2 -> { dark = 0x02100C; c1 = 0x2BFF9A; c2 = 0x16B8FF; }
            case 3 -> {
                dark = 0x08040F;
                float hue = (float) (((Math.atan2(z, x) / (Math.PI * 2.0)) + 0.5 + t * 0.05) % 1.0);
                c1 = Mth.hsvToRgb(hue, 0.75f, 1.0f);
                c2 = Mth.hsvToRgb((hue + 0.35f) % 1.0f, 0.75f, 1.0f);
            }
            default -> { dark = 0x050310; c1 = 0xB026FF; c2 = 0x2B6BFF; }
        }
        int nebula = Draw.blend(c1, c2, (float) m);
        double strength = clamp01(n * band * 1.4 + 0.12 * n);
        return Draw.blend(nebula, dark | 0xFF000000, (float) strength);
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD) return;
        long nowMs = System.currentTimeMillis();

        // Colours drift slowly, so refresh them about ten times a second
        if (nowMs - colorStamp >= 100) {
            colorStamp = nowMs;
            double t = nowMs / 1000.0 * get("speed") * 0.05;
            int palette = (int) get("palette");
            for (int i = 0; i <= LON; i++) {
                for (int j = 0; j <= LAT; j++) {
                    Vec3 o = offsets[i][j];
                    colors[i][j] = color(o.x / RADIUS, o.y / RADIUS, o.z / RADIUS, t, palette);
                }
            }
        }

        for (int i = 0; i <= LON; i++) {
            for (int j = 0; j <= LAT; j++) frame[i][j] = draw.cam.add(offsets[i][j]);
        }
        for (int i = 0; i < LON; i++) {
            for (int j = 0; j < LAT; j++) {
                draw.skyQuad(frame[i][j], frame[i + 1][j], frame[i + 1][j + 1], frame[i][j + 1],
                    colors[i][j], colors[i + 1][j], colors[i + 1][j + 1], colors[i][j + 1]);
            }
        }

        double now = nowMs / 1000.0;
        for (int s = 0; s < STARS; s++) {
            Vec3 c = draw.cam.add(starCenters[s]);
            Vec3 u = starU[s];
            Vec3 v = starV[s];
            int alpha = (int) (170 + 85 * Math.sin(now * 1.7 + stars[s][3]));
            int col = Draw.alpha(0xFFFFFF, alpha);
            draw.skyQuad(c.add(u).add(v), c.subtract(u).add(v), c.subtract(u).subtract(v), c.add(u).subtract(v),
                col, col, col, col);
        }
    }
}
