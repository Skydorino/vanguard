package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Look around freely while your character keeps facing (and moving) the way it was. Your mouse
 * turns the camera only; your real aim stays where it was when you switched this on. The camera
 * is turned by CameraMixin, which calls captureAndLock() each frame.
 */
public class Freelook extends Module {
    private static boolean active;
    private static float yaw;
    private static float pitch;
    private static float lockYaw;
    private static float lockPitch;

    public Freelook() {
        super("Freelook", Category.VISUALS, "Look around without turning your character");
    }

    @Override
    protected void onEnable() {
        LocalPlayer p = mc.player;
        if (p == null) return;
        yaw = lockYaw = p.getYRot();
        pitch = lockPitch = p.getXRot();
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    /** Freelook yields to Freecam, which has its own camera. */
    public static boolean active() {
        Module freecam = ModuleManager.find("Freecam");
        return active && (freecam == null || !freecam.isEnabled());
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }

    /** Turns this frame's mouse movement into camera rotation and puts your real rotation back. */
    public static void captureAndLock(LocalPlayer p) {
        yaw += Mth.wrapDegrees(p.getYRot() - lockYaw);
        pitch = Mth.clamp(pitch + (p.getXRot() - lockPitch), -90.0f, 90.0f);
        p.setYRot(lockYaw);
        p.setXRot(lockPitch);
    }
}
