package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;

/**
 * AutoSmelt — when a furnace is open, automatically:
 *   1. Moves ore/smeltable from your inventory into the input slot.
 *   2. Moves fuel (coal/charcoal/wood) into the fuel slot.
 * Runs once per interval so it doesn't spam clicks.
 */
public class AutoSmelt extends Module {
    private int timer;

    public AutoSmelt() {
        super("AutoSmelt", Category.DONUTSMP, "Automatically loads furnaces with ore and fuel when opened");
        setting("interval", 10);
    }

    @Override
    protected void onEnable() { timer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (!(mc.screen instanceof FurnaceScreen screen)) return;
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        FurnaceMenu menu = screen.getMenu();
        // Slot 0 = ingredient, slot 1 = fuel, slot 2 = output
        // Slots 3-29 = furnace inventory (not player), 30+ = player inventory

        Slot ingredientSlot = menu.slots.get(0);
        Slot fuelSlot       = menu.slots.get(1);

        // Fill ingredient slot if empty
        if (!ingredientSlot.hasItem()) {
            fillSlot(menu, player, 0, false);
        }

        // Fill fuel slot if empty
        if (!fuelSlot.hasItem()) {
            fillSlot(menu, player, 1, true);
        }
    }

    private void fillSlot(FurnaceMenu menu, LocalPlayer player, int targetSlot, boolean wantFuel) {
        // Scan player inventory (slots 3 onward in furnace menu)
        int menuSize = menu.slots.size();
        for (int i = 3; i < menuSize; i++) {
            Slot s = menu.slots.get(i);
            if (!s.hasItem()) continue;
            ItemStack stack = s.getItem();
            boolean isFuel = stack.is(Items.COAL) || stack.is(Items.CHARCOAL)
                || stack.is(Items.OAK_LOG) || stack.is(Items.SPRUCE_LOG)
                || stack.is(Items.BIRCH_LOG) || stack.is(Items.OAK_PLANKS);
            if (isFuel != wantFuel) continue;

            // Shift-click to move into furnace
            mc.gameMode.handleInventoryMouseClick(
                menu.containerId, s.index, 0, ClickType.QUICK_MOVE, player);
            return;
        }
    }
}
// END OF FILE