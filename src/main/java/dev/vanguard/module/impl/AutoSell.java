package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Sends a chat command on a timer, by default "/sell all". Change the command with
 * /vg autosell <command> if your server uses something else. It never runs faster than every
 * 5 seconds, and can wait until your inventory is mostly full. Check the server's rules first:
 * many servers do not allow automated commands.
 */
public class AutoSell extends Module {
    /** The command to send, without the leading slash. Saved in the config. */
    public static String command = "sell all";

    private int timer;

    public AutoSell() {
        super("AutoSell", Category.DONUTSMP, "Runs your sell command on a timer");
        setting("interval", 30);  // seconds between sells (minimum 5)
        setting("minfull", 0);    // only sell when at least this many inventory slots are used (0 = always)
    }

    @Override
    protected void onEnable() {
        timer = 0;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(5, (int) get("interval")) * 20;
        if (mc.screen != null || command == null || command.isBlank()) return;

        int minFull = (int) get("minfull");
        if (minFull > 0) {
            int used = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!s.isEmpty()) used++;
            }
            if (used < minFull) return;
        }
        player.connection.sendCommand(command);
    }
}
