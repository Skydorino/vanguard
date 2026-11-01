package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Smoothly pulls your crosshair toward the nearest enemy within FOV and range.
 * Your camera actually moves — the server sees normal look packets.
 * Settings: range (blocks), fov (degrees cone), speed (degrees per tick pulled).
 */
public class AimAssist extends Module {
    public AimAssist() {
        super("AimAssist", Category.COMBAT, "Smoothly moves your crosshair toward the nearest enemy");
        setting("range", 6.0);
        setting("fov",   60.0); // only pulls if target is within this cone half-angle
        setting("speed", 5.0);  // max degrees moved per tick
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null) return;

        Player target = Targets.nearestEnemy(player, level, get("range"));
        if (target == null) return;

        // Aim at the target's eye level
        Vec3 eye    = player.getEyePosition();
        Vec3 toEye  = target.getEyePosition().subtract(eye);

        double dx   = toEye.x;
        double dy   = toEye.y;
        double dz   = toEye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw   = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));

        float currentYaw   = player.getYRot();
        float currentPitch = player.getXRot();

        // Wrap yaw delta into [-180, 180]
        float dYaw   = wrapDeg(targetYaw - currentYaw);
        float dPitch = targetPitch - currentPitch;

        // Only pull if inside FOV cone
        double halfFov = get("fov");
        if (Math.abs(dYaw) > halfFov || Math.abs(dPitch) > halfFov) return;

        float speed = (float) get("speed");
        float moveYaw   = clamp(dYaw,   -speed, speed);
        float movePitch = clamp(dPitch, -speed, speed);

        player.setYRot(currentYaw   + moveYaw);
        player.setXRot(currentPitch + movePitch);
        player.yRotO  = player.getYRot();
        player.xRotO  = player.getXRot();
    }

    private static float wrapDeg(float d) {
        while (d >  180f) d -= 360f;
        while (d < -180f) d += 360f;
        return d;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
// END OF FILE