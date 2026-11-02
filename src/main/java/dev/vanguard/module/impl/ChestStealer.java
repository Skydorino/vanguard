package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * When you open any container (chest, barrel, shulker box, etc), automatically shift-clicks
 * every item into your inventory as fast as the server will allow.
 * Useful for looting stashes or quickly emptying shop chests.
 * Set interval higher if the server kicks you for clicking too fast.
 */
public class ChestStealer extends Module {
    private int timer;

    public ChestStealer() {
        super("ChestStealer", Category.DONUTSMP, "Shift-clicks all items from open containers into your inventory");
        setting("interval", 2); // ticks between each slot click
    }

    @Override
    protected void onEnable() { timer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        // Find the first non-empty container slot (not the player inventory)
        var menu = screen.getMenu();
        int containerSize = menu.slots.size() - 36; // subtract player inv slots
        for (int i = 0; i < containerSize; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            // Shift-click: quick move
            mc.gameMode.handleInventoryMouseClick(
                menu.containerId, slot.index, 0,
                net.minecraft.world.inventory.ClickType.QUICK_MOVE,
                player);
            return; // one slot per interval
        }
    }
}
// END OF FILE