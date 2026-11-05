package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * AutoDrop — automatically drops junk items from your inventory every few ticks.
 * Hardcoded junk list covers common trash drops: rotten flesh, bones, arrows,
 * string, spider eyes, gunpowder, gravel, dirt, sand, cobblestone, diorite,
 * andesite, granite, flint.
 * Does NOT drop tools, armor, food, or anything valuable.
 */
public class AutoDrop extends Module {
    private static final Set<String> JUNK = Set.of(
        "rotten_flesh", "bone", "arrow", "string", "spider_eye", "gunpowder",
        "gravel", "dirt", "sand", "cobblestone", "diorite", "andesite", "granite",
        "flint", "leather", "porkchop", "beef", "chicken", "mutton", "rabbit",
        "poisonous_potato", "ink_sac", "lily_pad", "bowl", "stick"
    );

    private int timer;

    public AutoDrop() {
        super("AutoDrop", Category.DONUTSMP, "Automatically drops junk items from your inventory");
        setting("interval", 20);
    }

    @Override
    protected void onEnable() { timer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null) return;
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (!JUNK.contains(id)) continue;
            // Select slot and drop
            int prev = inv.getSelectedSlot();
            inv.setSelectedSlot(i < 9 ? i : prev);
            if (i < 9) {
                player.drop(false); // drop one at a time
            }
            inv.setSelectedSlot(prev);
            return; // one per interval
        }
    }
}
// END OF FILE