package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Every hit registers as a critical hit regardless of whether you are jumping.
 * Works by spoofing fallDistance to a small positive value and nudging Y velocity
 * negative in onPreTick so vanilla's crit check passes, then restoring both in onTick.
 * Does nothing if you are in water, lava, on a ladder, or riding — vanilla won't crit there anyway.
 */
public class CritBot extends Module {
    private Vec3 savedVelocity;
    private float savedFallDistance;
    private boolean spoofed;

    public CritBot() {
        super("CritBot", Category.COMBAT, "Makes every hit a critical hit without needing to jump");
    }

    @Override
    public void onPreTick(LocalPlayer player, ClientLevel level) {
        spoofed = false;

        // Only spoof when on the ground or when fallDistance is zero — don't interfere with real crits
        if (!player.onGround() && player.fallDistance > 0.1f) return;

        // Can't crit in water, lava, climbing, riding
        if (player.isInWater() || player.isInLava() || player.onClimbable() || player.isPassenger()) return;

        savedVelocity = player.getDeltaMovement();
        savedFallDistance = player.fallDistance;

        // Spoof: tiny downward motion + small fall distance satisfies the crit predicate
        player.setDeltaMovement(savedVelocity.x, -0.1, savedVelocity.z);
        player.fallDistance = 0.12f;
        spoofed = true;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (!spoofed) return;
        // Restore immediately after attack packets are processed
        player.setDeltaMovement(savedVelocity);
        player.fallDistance = savedFallDistance;
        spoofed = false;
    }

    @Override
    protected void onDisable() {
        if (mc.player == null || !spoofed) return;
        mc.player.setDeltaMovement(savedVelocity);
        mc.player.fallDistance = savedFallDistance;
        spoofed = false;
    }
}
// END OF FILE