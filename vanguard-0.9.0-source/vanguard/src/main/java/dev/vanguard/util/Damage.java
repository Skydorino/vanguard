package dev.vanguard.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Damage {
    private Damage() {}

    public static double crystal(LivingEntity entity, Vec3 center) {
        return explosion(entity, center, 6.0);
    }

    /**
     * Rough explosion damage estimate. Assumes full exposure (no blocks in the way) and ignores
     * enchantments and potion effects, so treat it as an approximation.
     */
    public static double explosion(LivingEntity entity, Vec3 center, double power) {
        double q = power * 2.0;
        double dist = Math.sqrt(entity.distanceToSqr(center)) / q;
        if (dist > 1.0) return 0.0;
        double impact = 1.0 - dist;
        double raw = (impact * impact + impact) / 2.0 * 7.0 * q + 1.0;

        if (entity instanceof Player && Minecraft.getInstance().level != null) {
            switch (Minecraft.getInstance().level.getDifficulty()) {
                case PEACEFUL -> raw = 0.0;
                case EASY -> raw = Math.min(raw / 2.0 + 1.0, raw);
                case HARD -> raw = raw * 1.5;
                default -> { }
            }
        }

        double armor = entity.getAttributeValue(Attributes.ARMOR);
        double toughness = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double f = 2.0 + toughness / 4.0;
        double g = Math.min(20.0, Math.max(armor - raw / f, armor * 0.2));
        return raw * (1.0 - g / 25.0);
    }
}
