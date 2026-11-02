package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/**
 * Queues up to 5 commands and fires them in sequence on enable, with a configurable delay between each.
 * Great for vote macros, multi-step sell sequences, or any DonutSMP money command chain.
 * Set commands via the static fields: AutoCommand.cmds[0..4] = "vote", "sell all", etc.
 * The module disables itself when the sequence finishes.
 */
public class AutoCommand extends Module {
    public static String[] cmds = {"sell all", "", "", "", ""};
    private int step;
    private int timer;

    public AutoCommand() {
        super("AutoCommand", Category.DONUTSMP, "Fires a sequence of commands in order then turns off");
        setting("delay", 20); // ticks between each command
    }

    @Override
    protected void onEnable() {
        step = 0;
        timer = 5; // small initial delay
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null) return;
        if (--timer > 0) return;

        // Find next non-empty command
        while (step < cmds.length && (cmds[step] == null || cmds[step].isBlank())) step++;
        if (step >= cmds.length) { setEnabled(false); return; }

        String cmd = cmds[step].trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        player.connection.sendCommand(cmd);
        step++;
        timer = Math.max(5, (int) get("delay"));

        if (step >= cmds.length) setEnabled(false);
    }
}
// END OF FILE