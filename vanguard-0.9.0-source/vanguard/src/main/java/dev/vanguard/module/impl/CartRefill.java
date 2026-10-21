package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

/** Keeps rails, TNT minecarts and a lighter in your hotbar by pulling them from your inventory. */
public class CartRefill extends Module {
    private int timer;

    public CartRefill() {
        super("CartRefill", Category.COMBAT, "Refills rails, TNT minecarts and your lighter in the hotbar");
        setting("railslot", 5);   // hotbar slots (1-9) used when something has to be moved in
        setting("cartslot", 6);
        setting("lightslot", 7);
    }

    private int slot(String key) {
        return Math.max(0, Math.min(8, (int) get(key) - 1));
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = 10;
        if (mc.gameMode == null || player.containerMenu != player.inventoryMenu) return;

        Inv.bring(player, s -> s.is(ItemTags.RAILS), slot("railslot"));
        Inv.bring(player, s -> s.is(Items.TNT_MINECART), slot("cartslot"));
        Inv.bring(player, s -> s.is(Items.FLINT_AND_STEEL) || s.is(Items.FIRE_CHARGE), slot("lightslot"));
    }
}
