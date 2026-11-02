package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Every hit registers as a critical hit regardless of whether you are jumping.
 * Spoofs fallDistance and Y velocity in onPreTick so vanilla's crit check passes,
 * then restores both in onTick.
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
        if (!player.onGround() && player.fallDistance > 0.1f) return;
        if (player.isInWater() || player.isInLava() || player.onClimbable() || player.isPassenger()) return;

        savedVelocity = player.getDeltaMovement();
        savedFallDistance = (float) player.fallDistance;

        player.setDeltaMovement(savedVelocity.x, -0.1, savedVelocity.z);
        player.fallDistance = 0.12f;
        spoofed = true;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (!spoofed) return;
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