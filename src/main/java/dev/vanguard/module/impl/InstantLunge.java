package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.util.Inv;
import net.minecraft.client.player.LocalPlayer;

/** When you click on an entity, swaps to a spear from your hotbar or inventory for that hit. */
public class InstantLunge extends AttackSwap {
    public InstantLunge() {
        super("InstantLunge", Category.COMBAT, "Equips your spear the moment you attack");
        setting("slot", 9); // hotbar slot (1-9) a spear from your inventory is moved into
    }

    @Override
    protected int slotFor(LocalPlayer player) {
        return Inv.bring(player, Inv::isSpear, slotSetting("slot"));
    }
}
