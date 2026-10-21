package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Base for modules that swap to a specific weapon for the click you are making. It runs before
 * the game reads your click, so the swapped weapon is the one that actually swings, and it puts
 * your previous slot back on the next tick.
 */
public abstract class AttackSwap extends Module {
    private boolean wasDown;
    private int restoreSlot = -1;

    protected AttackSwap(String name, Category category, String description) {
        super(name, category, description);
    }

    /** Hotbar slot (0-8) to use for this attack, or -1 to leave your hand alone. */
    protected abstract int slotFor(LocalPlayer player);

    /** Converts a 1-9 setting into a 0-8 hotbar index. */
    protected int slotSetting(String key) {
        return Math.max(0, Math.min(8, (int) get(key) - 1));
    }

    @Override
    protected void onDisable() {
        restoreSlot = -1;
        wasDown = false;
    }

    @Override
    public void onPreTick(LocalPlayer player, ClientLevel level) {
        if (restoreSlot >= 0) {
            player.getInventory().setSelectedSlot(restoreSlot);
            restoreSlot = -1;
        }
        boolean down = mc.options.keyAttack.isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (!pressed || mc.screen != null || mc.gameMode == null) return;
        if (!(mc.hitResult instanceof EntityHitResult)) return;

        int slot = slotFor(player);
        int current = player.getInventory().getSelectedSlot();
        if (slot < 0 || slot == current) return;
        restoreSlot = current;
        player.getInventory().setSelectedSlot(slot);
    }
}
