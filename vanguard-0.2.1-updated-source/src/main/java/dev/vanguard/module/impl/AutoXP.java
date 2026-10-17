package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Throws experience bottles at your feet while any armor piece is below the threshold. */
public class AutoXP extends Module {
    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private int timer;

    public AutoXP() {
        super("AutoXP", Category.COMBAT, "Repairs armor with experience bottles (looks straight down)");
        setting("threshold", 60); // repair while durability is below this percent
        setting("delay", 2);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        if (timer > 0) {
            timer--;
            return;
        }
        boolean needs = false;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.isEmpty() || !s.isDamaged()) continue;
            double pct = 100.0 - s.getDamageValue() * 100.0 / s.getMaxDamage();
            if (pct < get("threshold")) needs = true;
        }
        if (!needs) return;

        int slot = Inv.findHotbar(player, Items.EXPERIENCE_BOTTLE);
        if (slot < 0) return;
        player.getInventory().setSelectedSlot(slot);
        player.setXRot(90.0f);
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        timer = (int) get("delay");
    }
}
