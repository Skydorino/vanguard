package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

/**
 * Automatically fishes. Casts, waits for a bite (the hook bobs down), reels in, then casts again.
 * Switches to a fishing rod in your hotbar automatically. Stops if no rod is found or health is low.
 */
public class AutoFish extends Module {
    private int castDelay;
    private int timeout;

    public AutoFish() {
        super("AutoFish", Category.DONUTSMP, "Automatically casts and reels in a fishing rod");
        setting("minhealth", 10); // stop below this health
        setting("timeout", 600);  // ticks before assuming the hook is stuck and recasting
    }

    @Override
    protected void onEnable() {
        castDelay = 10;
        timeout = 0;
    }

    @Override
    protected void onDisable() {
        mc.options.keyUse.setDown(false);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null) return;
        if (player.getHealth() < get("minhealth")) {
            player.displayClientMessage(Component.literal("[Vanguard] AutoFish stopped: low health"), false);
            setEnabled(false);
            return;
        }

        // Make sure we're holding a rod
        if (!player.getMainHandItem().is(Items.FISHING_ROD)) {
            int slot = Inv.findHotbar(Items.FISHING_ROD);
            if (slot == -1) {
                player.displayClientMessage(Component.literal("[Vanguard] AutoFish: no fishing rod in hotbar"), false);
                setEnabled(false);
                return;
            }
            player.getInventory().selected = slot;
        }

        if (castDelay > 0) { castDelay--; return; }

        FishingHook hook = player.fishing;

        if (hook == null) {
            // Cast
            mc.options.keyUse.setDown(true);
            castDelay = 4;
            timeout = (int) get("timeout");
            return;
        }

        mc.options.keyUse.setDown(false);

        // Timeout guard — reel in and recast if stuck
        if (--timeout <= 0) {
            reel(player);
            return;
        }

        // Detect bite: hook velocity goes sharply negative (bobs under water)
        if (hook.getDeltaMovement().y < -0.1) {
            reel(player);
        }
    }

    private void reel(LocalPlayer player) {
        mc.options.keyUse.setDown(true);
        castDelay = 15; // wait a moment then cast again
    }
}
// END OF FILE