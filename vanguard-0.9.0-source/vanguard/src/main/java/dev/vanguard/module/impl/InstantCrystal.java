package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Damage;
import dev.vanguard.util.Targets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Event driven: the moment the game finishes adding a new end crystal from the server's spawn
 * packet, it sends one attack at it, instead of waiting for the next tick or for your crosshair.
 * One attack per crystal, only for crystals already inside your reach.
 */
public class InstantCrystal extends Module {
    public InstantCrystal() {
        super("InstantCrystal", Category.COMBAT, "Attacks a new end crystal the moment it appears, if it is in reach");
        setting("totem", 1);       // 1 = still hit if the blast could kill you but you hold a totem
        setting("enemyrange", 0);  // >0 = only when an enemy player is within this many blocks
        ClientEntityEvents.ENTITY_LOAD.register(this::onLoad);
    }

    private void onLoad(Entity entity, ClientLevel level) {
        if (!isEnabled() || !(entity instanceof EndCrystal crystal)) return;
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || level != mc.level) return;

        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (crystal.getBoundingBox().distanceToSqr(player.getEyePosition()) > reach * reach) return;
        if (get("enemyrange") > 0 && Targets.nearestEnemy(player, level, get("enemyrange")) == null) return;

        Vec3 pos = crystal.position();
        double hp = player.getHealth() + player.getAbsorptionAmount();
        boolean totem = player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || player.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
        if (Damage.crystal(player, pos) >= hp && !(get("totem") > 0 && totem)) return;

        mc.gameMode.attack(player, crystal);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
