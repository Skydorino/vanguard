package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/**
 * One-shot: switch it on and it throws an ender pearl, then a wind charge, in the direction you
 * are looking, taking both from your inventory, then switches itself off and puts your slot back.
 */
public class AutoPearlCatch extends Module {
    private int stage;
    private int timer;
    private int previousSlot = -1;

    public AutoPearlCatch() {
        super("AutoPearlCatch", Category.MOVEMENT, "Throws a pearl and a wind charge where you look");
        setting("pearlslot", 8);  // hotbar slots (1-9) used when an item has to be moved in
        setting("windslot", 9);
        setting("delay", 1);      // ticks between the two throws
    }

    private int slot(String key) {
        return Math.max(0, Math.min(8, (int) get(key) - 1));
    }

    @Override
    protected void onEnable() {
        stage = 0;
        timer = 0;
        previousSlot = -1;
    }

    @Override
    protected void onDisable() {
        if (previousSlot >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
    }

    private void fail(LocalPlayer player, String reason) {
        player.displayClientMessage(Component.literal("[Vanguard] AutoPearlCatch: " + reason), false);
        setEnabled(false);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        if (timer > 0) {
            timer--;
            return;
        }
        switch (stage) {
            case 0 -> {
                int pearl = Inv.bring(player, s -> s.is(Items.ENDER_PEARL), slot("pearlslot"));
                if (pearl < 0) {
                    fail(player, "no ender pearl");
                    return;
                }
                previousSlot = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(pearl);
                mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                stage = 1;
                timer = Math.max(1, (int) get("delay"));
            }
            default -> {
                int wind = Inv.bring(player, s -> s.is(Items.WIND_CHARGE), slot("windslot"));
                if (wind < 0) {
                    fail(player, "no wind charge");
                    return;
                }
                player.getInventory().setSelectedSlot(wind);
                mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                setEnabled(false); // onDisable puts your slot back
            }
        }
    }
}
