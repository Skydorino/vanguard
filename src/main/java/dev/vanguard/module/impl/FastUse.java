package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * While you hold the use button: places blocks and throws throwables (pearls, snowballs, eggs,
 * XP bottles, splash potions, wind charges, rockets) every few ticks instead of the game's slow
 * repeat rate. Eating, drinking and bows are left alone.
 */
public class FastUse extends Module {
    private static final Set<Item> THROWABLES = Set.of(
        Items.ENDER_PEARL, Items.SNOWBALL, Items.EGG, Items.EXPERIENCE_BOTTLE,
        Items.SPLASH_POTION, Items.WIND_CHARGE, Items.FIREWORK_ROCKET);

    private int timer;

    public FastUse() {
        super("FastUse", Category.MISC, "Places blocks and throws items much faster while you hold use");
        setting("delay", 1);  // ticks between uses
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null || !mc.options.keyUse.isDown()) return;
        if (timer > 0) {
            timer--;
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof BlockItem
            && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
            && !level.getBlockState(hit.getBlockPos()).hasBlockEntity()) {
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            player.swing(InteractionHand.MAIN_HAND);
            timer = Math.max(0, (int) get("delay"));
        } else if (THROWABLES.contains(held.getItem())) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            timer = Math.max(0, (int) get("delay"));
        }
    }
}
