package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** When a hotbar slot runs empty, pulls the same item in from your main inventory. */
public class AutoRefill extends Module {
    private final Item[] last = new Item[9];

    public AutoRefill() {
        super("AutoRefill", Category.COMBAT, "Refills emptied hotbar slots from your inventory");
    }

    @Override
    protected void onEnable() {
        java.util.Arrays.fill(last, null);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || player.containerMenu != player.inventoryMenu) return;
        for (int i = 0; i < 9; i++) {
            ItemStack cur = player.getInventory().getItem(i);
            if (!cur.isEmpty()) {
                last[i] = cur.getItem();
                continue;
            }
            if (last[i] == null) continue;
            int src = Inv.findInventory(player, last[i]);
            last[i] = null;
            if (src >= 0) {
                // SWAP click: button = hotbar slot number
                Inv.click(player, Inv.menuSlot(src), i, ClickType.SWAP);
                return; // one refill per tick
            }
        }
    }
}
