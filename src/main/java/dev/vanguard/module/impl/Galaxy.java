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
 * hides it), but it covers the vanilla sun, moon and stars.
 */
public class Galaxy extends Module {
    private static final int STARS = 360;
    private static final double RADIUS = 220.0;
    private static final int LON = 48;
    private static final int LAT = 24;

    private final double[][] stars = new double[STARS][4]; // x, y, z, twinkle phase

    public Galaxy() {
        super("Galaxy", Category.VISUALS, "Replaces the sky with a drifting galaxy");
        setting("palette", 0);   // 0 purple/blue, 1 pink/orange, 2 green/teal, 3 rainbow
        setting("speed", 1.0);

        Random rnd = new Random(1337);
        for (double[] s : stars) {
            double z = rnd.nextDouble() * 2.0 - 1.0;
            double a = rnd.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(1.0 - z * z);
            s[0] = r * Math.cos(a);
            s[1] = z;
            s[2] = r * Math.sin(a);
            s[3] = rnd.nextDouble() * Math.PI * 2.0;
        }
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private int color(double x, double y, double z, double t) {
        int palette = (int) get("palette");
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

    private Vec3 dir(double lon, double lat) {
        double cl = Math.cos(lat);
        return new Vec3(cl * Math.cos(lon), Math.sin(lat), cl * Math.sin(lon));
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD) return;
        double t = System.currentTimeMillis() / 1000.0 * get("speed") * 0.05;

        for (int i = 0; i < LON; i++) {
            double l0 = Math.PI * 2.0 * i / LON;
            double l1 = Math.PI * 2.0 * (i + 1) / LON;
            for (int j = 0; j < LAT; j++) {
                double a0 = -Math.PI / 2.0 + Math.PI * j / LAT;
                double a1 = -Math.PI / 2.0 + Math.PI * (j + 1) / LAT;
                Vec3 d00 = dir(l0, a0), d10 = dir(l1, a0), d11 = dir(l1, a1), d01 = dir(l0, a1);
                draw.skyQuad(
                    draw.cam.add(d00.scale(RADIUS)), draw.cam.add(d10.scale(RADIUS)),
                    draw.cam.add(d11.scale(RADIUS)), draw.cam.add(d01.scale(RADIUS)),
                    color(d00.x, d00.y, d00.z, t), color(d10.x, d10.y, d10.z, t),
                    color(d11.x, d11.y, d11.z, t), color(d01.x, d01.y, d01.z, t));
            }
        }

        double now = System.currentTimeMillis() / 1000.0;
        for (double[] s : stars) {
            Vec3 d = new Vec3(s[0], s[1], s[2]);
            Vec3 u = d.cross(Math.abs(d.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            Vec3 v = d.cross(u);
            double size = 0.9;
            Vec3 c = draw.cam.add(d.scale(RADIUS - 1.0));
            int alpha = (int) (170 + 85 * Math.sin(now * 1.7 + s[3]));
            int col = Draw.alpha(0xFFFFFF, alpha);
            draw.skyQuad(c.add(u.scale(size)).add(v.scale(size)), c.subtract(u.scale(size)).add(v.scale(size)),
                c.subtract(u.scale(size)).subtract(v.scale(size)), c.add(u.scale(size)).subtract(v.scale(size)),
                col, col, col, col);
        }
    }
}
