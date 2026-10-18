package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Targets;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Circles the nearest player at a set radius by pressing your movement keys for you. It is
 * movement only: it does not turn your view or attack, so you still aim and fight yourself.
 */
public class Orbit extends Module {
    private boolean driving;

    public Orbit() {
        super("Orbit", Category.MOVEMENT, "Circles the nearest player (movement only, you still aim)");
        setting("radius", 3.0);     // how close to stay, in blocks
        setting("distance", 16.0);  // only orbit players within this range
        setting("direction", 1);    // 1 = one way round, -1 = the other
        setting("sprint", 1);       // 1 = sprint while orbiting
    }

    @Override
    protected void onDisable() {
        release();
    }

    private void release() {
        if (driving) {
            KeyMapping.setAll(); // hand the keys back to your real keyboard
            driving = false;
        }
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
        driving = true;
    }
}
