package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * AutoBuy / ShopBot.
 * The moment you open the villager shop screen it scans every trade offer and clicks the ones
 * you have configured.
 *
 * Config is done through the settings panel:
 *   wool       — buy wool           (iron cost tier)
 *   iron_armor — buy iron armor
 *   shears     — buy shears
 *   sword      — buy iron/diamond sword
 *   pickaxe    — buy iron/diamond pickaxe
 *   axe        — buy iron/diamond axe
 *   bow        — buy bow
 *   arrows     — buy arrows
 *   Each setting: 1 = buy it, 0 = skip.
 *
 * It clicks each matching trade once per shop open. Interval prevents buying so fast the
 * server kicks you.
 */
public class AutoBuy extends Module {
    // Keywords we match against trade result item registry paths
    private static final String[][] SETTING_KEYWORDS = {
        {"wool",       "wool"},
        {"iron_armor", "iron_chestplate", "iron_leggings", "iron_helmet", "iron_boots"},
        {"shears",     "shears"},
        {"sword",      "iron_sword", "diamond_sword"},
        {"pickaxe",    "iron_pickaxe", "diamond_pickaxe"},
        {"axe",        "iron_axe", "diamond_axe"},
        {"bow",        "bow"},
        {"arrows",     "arrow"},
    };

    private boolean bought;
    private int timer;

    public AutoBuy() {
        super("AutoBuy", Category.BEDWARS, "Buys your configured loadout the moment the shop opens");
        setting("wool",       1);
        setting("iron_armor", 1);
        setting("shears",     0);
        setting("sword",      1);
        setting("pickaxe",    1);
        setting("axe",        0);
        setting("bow",        1);
        setting("arrows",     1);
        setting("interval",   4); // ticks between each purchase click
    }

    @Override
    protected void onEnable() { bought = false; timer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (!(mc.screen instanceof MerchantScreen shop)) {
            bought = false; // reset when shop closes so it runs again next open
            timer = 0;
            return;
        }

        if (bought) return;
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        MerchantOffers offers = shop.getMenu().getOffers();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer.isOutOfStock()) continue;

            String resultPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(offer.getResult().getItem()).getPath();

            for (String[] group : SETTING_KEYWORDS) {
                String settingName = group[0];
                if (get(settingName) <= 0) continue;
                for (int k = 1; k < group.length; k++) {
                    if (resultPath.contains(group[k])) {
                        // Click trade index i to select it, then shift-click to buy
                        mc.gameMode.handleInventoryMouseClick(
                            shop.getMenu().containerId,
                            i,       // trade slot index
                            0,
                            net.minecraft.world.inventory.ClickType.PICKUP,
                            player);
                        break;
                    }
                }
            }
        }
        bought = true;
    }
}
// END OF FILE