package dev.vanguard.util;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class Inv {
    private Inv() {}

    public static int findHotbar(Player p, Item item) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    public static int findHotbar(Player p, Predicate<ItemStack> match) {
        for (int i = 0; i < 9; i++) {
            if (match.test(p.getInventory().getItem(i))) return i;
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

    /**
     * Makes sure a matching item is in the hotbar and returns its slot. If it only exists in the
     * main inventory it is swapped into hotbarSlot. Returns -1 if you have none.
     */
    public static int bring(LocalPlayer p, Predicate<ItemStack> match, int hotbarSlot) {
        int inHotbar = findHotbar(p, match);
        if (inHotbar >= 0) return inHotbar;
        for (int i = 9; i < 36; i++) {
            if (match.test(p.getInventory().getItem(i))) {
                click(p, menuSlot(i), hotbarSlot, ClickType.SWAP);
                return hotbarSlot;
            }
        }
        return -1;
    }

    /** Spears are matched by registry name so this does not depend on item constants. */
    public static boolean isSpear(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_spear");
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
