package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
    private int cooldown;

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT, "Keeps a totem of undying in your offhand");
        setting("delay", 1); // ticks between swaps
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (mc.gameMode == null || player.containerMenu != player.inventoryMenu) return;
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;

        int idx = Inv.findInventory(player, Items.TOTEM_OF_UNDYING);
        if (idx < 0) idx = Inv.findHotbar(player, Items.TOTEM_OF_UNDYING);
        if (idx < 0) return;

        // Button 40 on a SWAP click swaps the clicked slot with the offhand
        Inv.click(player, Inv.menuSlot(idx), 40, ClickType.SWAP);
        cooldown = (int) get("delay");
    }
}
