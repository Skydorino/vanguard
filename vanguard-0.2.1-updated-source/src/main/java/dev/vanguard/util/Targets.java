package dev.vanguard.util;

import dev.vanguard.module.ModuleManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Targets {
    private Targets() {}

    /** Nearest living, non-friend player whose hitbox is within range of our eyes. */
    public static Player nearestEnemy(LocalPlayer self, ClientLevel level, double range) {
        Vec3 eye = self.getEyePosition();
        Player best = null;
        double bestSq = range * range;
        for (Player other : level.players()) {
            if (other == self || !other.isAlive() || other.isSpectator() || ModuleManager.isFriend(other)) continue;
            double d = other.getBoundingBox().distanceToSqr(eye);
            if (d <= bestSq) {
                bestSq = d;
                best = other;
            }
        }
        return best;
    }
}
