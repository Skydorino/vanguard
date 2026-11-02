package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends a chat message or command on a configurable timer.
 * Useful for AFK grinding servers — spam a sell command, vote, or keep a chat channel alive.
 * Set the message with /vg set AutoChat message 0 (use AutoChat.message static field via command).
 * Default message is "." — change via the static field or extend Commands if you want /vg autochat set.
 */
public class AutoChat extends Module {
    public static String message = ".";
    private int timer;

    public AutoChat() {
        super("AutoChat", Category.DONUTSMP, "Sends a chat message or command on a timer");
        setting("interval", 60); // seconds between messages (minimum 5)
    }

    @Override
    protected void onEnable() { timer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(5, (int) get("interval")) * 20;
        if (mc.screen != null) return;
        if (message.startsWith("/")) {
            player.connection.sendCommand(message.substring(1));
        } else {
            player.connection.sendChat(message);
        }
    }
}
// END OF FILE