package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.util.Inv;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

/** When you click on an entity while falling, swaps to your mace for that hit. */
public class AutoMace extends AttackSwap {
    public AutoMace() {
        super("AutoMace", Category.COMBAT, "Equips your mace when you attack while falling");
        setting("minfall", 1.5);
        setting("slot", 8); // hotbar slot (1-9) a mace from your inventory is moved into
    }

    @Override
    protected int slotFor(LocalPlayer player) {
        if (player.onGround() || player.fallDistance < get("minfall")) return -1;
        return Inv.bring(player, s -> s.is(Items.MACE), slotSetting("slot"));
    }
}
