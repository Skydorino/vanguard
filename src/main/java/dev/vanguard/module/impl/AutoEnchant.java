package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.ClickType;

/**
 * AutoEnchant — when an enchanting table screen is open, automatically clicks
 * the highest available enchantment tier (slot 2 first, then 1, then 0).
 * Fires once when the screen opens then waits until you close and reopen.
 */
public class AutoEnchant extends Module {
    private boolean done;

    public AutoEnchant() {
        super("AutoEnchant", Category.DONUTSMP, "Automatically picks the highest enchantment when you open an enchanting table");
        setting("tier", 2); // 0=cheapest, 1=mid, 2=highest
    }

    @Override
    protected void onEnable() { done = false; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (!(mc.screen instanceof EnchantmentScreen screen)) {
            done = false; // reset when closed
            return;
        }
        if (done) return;

        EnchantmentMenu menu = screen.getMenu();
        int tier = (int) get("tier");

        // Try preferred tier first, fall back down
        for (int t = tier; t >= 0; t--) {
            if (menu.costs[t] > 0) {
                mc.gameMode.handleInventoryMouseClick(
                    menu.containerId, t, t, ClickType.PICKUP, player);
                done = true;
                return;
            }
        }
    }
}
// END OF FILE