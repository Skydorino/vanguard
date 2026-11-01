package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Sends look packets toward the nearest enemy each tick without moving your camera.
 * Your screen stays exactly where you left it; the server thinks you're facing the target.
 * Most useful with TriggerBot — it will fire even though your crosshair isn't on the player.
 * Settings: range (blocks), fov (degree cone — 0 = no cone limit).
 */
public class SilentAim extends Module {
    // Stash the visual angles so we can restore them
    private float savedYaw;
    private float savedPitch;
    private float savedYawO;
    private float savedPitchO;
    private boolean rotated;

    public SilentAim() {
        super("SilentAim", Category.COMBAT, "Silently aims at the nearest enemy without moving your camera");
        setting("range", 6.0);
        setting("fov",   0.0); // 0 = no FOV limit; positive value = degree half-angle cone
    }

    @Override
    public void onPreTick() {
        // Called before vanilla reads input — rotate here so TriggerBot/reach checks see the spoofed angle
        LocalPlayer player = mc.player;
        ClientLevel level  = mc.level;
        if (player == null || level == null || mc.screen != null) {
            rotated = false;
            return;
        }

        Player target = Targets.nearestEnemy(player, level, get("range"));
        if (target == null) {
            rotated = false;
            return;
        }

        Vec3 eye  = player.getEyePosition();
        Vec3 diff = target.getEyePosition().subtract(eye);

        double dx   = diff.x;
        double dy   = diff.y;
        double dz   = diff.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float tYaw   = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float tPitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));

        double halfFov = get("fov");
        if (halfFov > 0) {
            float dYaw   = wrapDeg(tYaw - player.getYRot());
            float dPitch = tPitch - player.getXRot();
            if (Math.abs(dYaw) > halfFov || Math.abs(dPitch) > halfFov) {
                rotated = false;
                return;
            }
        }

        // Save visual angles
        savedYaw   = player.getYRot();
        savedPitch = player.getXRot();
        savedYawO  = player.yRotO;
        savedPitchO = player.xRotO;

        // Spoof to target
        player.setYRot(tYaw);
        player.setXRot(tPitch);
        player.yRotO  = tYaw;
        player.xRotO  = tPitch;
        rotated = true;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        // Restore visual angles immediately after packets are queued
        if (!rotated) return;
        player.setYRot(savedYaw);
        player.setXRot(savedPitch);
        player.yRotO  = savedYawO;
        player.xRotO  = savedPitchO;
        rotated = false;
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = mc.player;
        if (player == null || !rotated) return;
        player.setYRot(savedYaw);
        player.setXRot(savedPitch);
        player.yRotO  = savedYawO;
        player.xRotO  = savedPitchO;
        rotated = false;
    }

    private static float wrapDeg(float d) {
        while (d >  180f) d -= 360f;
        while (d < -180f) d += 360f;
        return d;
    }
}
// END OF FILE