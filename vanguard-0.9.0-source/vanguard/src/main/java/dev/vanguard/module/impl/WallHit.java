package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Lets your hits reach a player standing behind blocks. If your crosshair is on a wall but a
 * player is in line behind it within your reach, the attack goes to them. Normally it works while
 * you hold attack; with auto on it hits by itself whenever someone is in line behind a wall.
 * The server decides whether the hit counts, and some servers refuse hits through blocks.
 */
public class WallHit extends Module {
    public WallHit() {
        super("WallHit", Category.COMBAT, "Hit players through walls, within your reach");
        setting("auto", 0);       // 1 = attack by itself, 0 = only while you hold attack
        setting("cooldown", 0.9); // weapon charge needed before hitting
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        if (get("auto") <= 0 && !mc.options.keyAttack.isDown()) return;
        if (mc.hitResult instanceof EntityHitResult) return; // the game can already hit someone
        if (player.getAttackStrengthScale(0.5f) < get("cooldown")) return;

        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));

        Player best = null;
        double bestSq = Double.MAX_VALUE;
        for (Player p : level.players()) {
            if (p == player || !p.isAlive() || p.isSpectator() || ModuleManager.isFriend(p)) continue;
            AABB box = p.getBoundingBox().inflate(p.getPickRadius());
            Optional<Vec3> hit = box.clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceToSqr(hit.get());
            if (d < bestSq) {
                bestSq = d;
                best = p;
            }
        }
        if (best == null) return;

        mc.gameMode.attack(player, best);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
