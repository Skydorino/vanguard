package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import dev.vanguard.util.Targets;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Circles the nearest player at a set radius by pressing your movement keys for you, and (with
 * aimlock on) turns your own view to keep them centred. The lock is visible on your screen: it
 * moves your real camera, so you cannot look away while someone is being orbited.
 */
public class Orbit extends Module {
    private boolean driving;
    private Player lockTarget;

    public Orbit() {
        super("Orbit", Category.MOVEMENT, "Circles the nearest player and keeps your view locked on them");
        setting("radius", 3.0);     // how close to stay, in blocks
        setting("distance", 16.0);  // only orbit players within this range
        setting("direction", 1);    // 1 = one way round, -1 = the other
        setting("sprint", 1);       // 1 = sprint while orbiting
        setting("speed", 1.6);      // 1 = normal sprint pace, higher = faster circling (0 or 1 turns the boost off)
        setting("aimlock", 1);      // 1 = lock your view onto the player you are orbiting
    }

    @Override
    protected void onDisable() {
        release();
    }

    private void release() {
        lockTarget = null;
        if (driving) {
            KeyMapping.setAll(); // hand the keys back to your real keyboard
            driving = false;
        }
    }

    private static void aimAt(LocalPlayer player, Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        player.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null) {
            release();
            return;
        }
        Player target = Targets.nearestEnemy(player, level, get("distance"));
        if (target == null) {
            release();
            return;
        }
        lockTarget = target;

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double dist = Math.hypot(dx, dz);
        if (dist < 1.0e-3) return;

        double rx = dx / dist;                     // unit vector toward the target
        double rz = dz / dist;
        double dir = get("direction") >= 0 ? 1.0 : -1.0;
        double tx = -rz * dir;                     // sideways around them
        double tz = rx * dir;
        double pull = Math.max(-1.0, Math.min(1.0, (dist - get("radius")) * 0.8)); // close in or back off
        double wx = tx + rx * pull;
        double wz = tz + rz * pull;
        double len = Math.hypot(wx, wz);
        wx /= len;
        wz /= len;

        // Turn the wanted world direction into key presses relative to where you are facing
        double yaw = Math.toRadians(player.getYRot());
        double forward = wx * -Math.sin(yaw) + wz * Math.cos(yaw);
        double right = wx * -Math.cos(yaw) + wz * -Math.sin(yaw);

        mc.options.keyUp.setDown(forward > 0.3);
        mc.options.keyDown.setDown(forward < -0.3);
        mc.options.keyRight.setDown(right > 0.3);
        mc.options.keyLeft.setDown(right < -0.3);
        mc.options.keySprint.setDown(get("sprint") > 0);
        mc.options.keyJump.setDown(player.horizontalCollision && player.onGround());
        // Speed boost: push you along the path at a scaled sprint pace
        double boost = get("speed");
        if (boost > 1.0) {
            double v = 0.28 * boost;
            player.setDeltaMovement(wx * v, player.getDeltaMovement().y, wz * v);
        }
        driving = true;

        if (get("aimlock") > 0) {
            aimAt(player, player.getEyePosition(), target.position().add(0.0, target.getBbHeight() * 0.6, 0.0));
        }
    }

    /** Runs every frame so the lock stays smooth between ticks. */
    @Override
    public void onWorldRender(WorldDraw draw) {
        LocalPlayer player = mc.player;
        if (player == null || lockTarget == null || !lockTarget.isAlive() || get("aimlock") <= 0) return;
        Vec3 eye = draw.pos(player).add(0.0, player.getEyeHeight(), 0.0);
        Vec3 to = draw.pos(lockTarget).add(0.0, lockTarget.getBbHeight() * 0.6, 0.0);
        aimAt(player, eye, to);
    }
}
