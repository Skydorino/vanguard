package dev.vanguard.module.impl;

import dev.vanguard.VanguardClient;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Adds to your attack and block reach on the client. Your crosshair, TriggerBot, AutoHitCrystal
 * and so on all use it automatically. The server still decides whether a far hit counts: vanilla
 * and Paper reject hits past roughly the normal reach plus a little leniency, so big numbers only
 * work on servers that do not check.
 */
public class Reach extends Module {
    private static final Identifier ENTITY_ID = Identifier.fromNamespaceAndPath(VanguardClient.MOD_ID, "reach_entity");
    private static final Identifier BLOCK_ID = Identifier.fromNamespaceAndPath(VanguardClient.MOD_ID, "reach_block");

    public Reach() {
        super("Reach", Category.COMBAT, "Extends how far you can hit and place (the server may still reject far hits)");
        setting("entity", 3.0);  // extra blocks of attack reach (vanilla is 3, so 3 here means 6)
        setting("block", 0.0);   // extra blocks of block reach (vanilla is 4.5)
    }

    private static void apply(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(id);
        if (amount <= 0.0) {
            if (current != null) attribute.removeModifier(id);
            return;
        }
        if (current != null && current.amount() == amount) return;
        if (current != null) attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        // Re-applied every tick because the server resets your attributes when it updates them
        apply(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_ID, get("entity"));
        apply(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_ID, get("block"));
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = mc.player;
        if (player == null) return;
        apply(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_ID, 0.0);
        apply(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_ID, 0.0);
    }
}
