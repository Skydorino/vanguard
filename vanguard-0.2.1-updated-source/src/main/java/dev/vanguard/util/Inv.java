package dev.vanguard.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;

public final class Inv {
    private Inv() {}

    public static int findHotbar(Player p, Item item) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    /** Main inventory index (9..35) of the first stack of this item, or -1. */
    public static int findInventory(Player p, Item item) {
        for (int i = 9; i < 36; i++) {
            if (p.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    /** First hotbar slot that does not hold this item (empty slots count). */
    public static int firstHotbarNot(Player p, Item item) {
        for (int i = 0; i < 9; i++) {
            if (!p.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    /** Menu slot id for an inventory index in the player's own inventory screen handler. */
    public static int menuSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }

    public static void click(LocalPlayer p, int menuSlot, int button, ClickType type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, menuSlot, button, type, p);
    }
}
