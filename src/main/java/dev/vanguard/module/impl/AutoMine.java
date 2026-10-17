package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Targets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Holds the attack button (and optionally forward) so you can mine while AFK. Aim where you want
 * to dig first. It switches itself off if your health drops or another player shows up.
 */
public class AutoMine extends Module {
    public AutoMine() {
        super("AutoMine", Category.MISC, "Holds left-click to mine while AFK, with safety stops");
        setting("forward", 1);          // 1 = also hold forward
        setting("minhealth", 12);       // stop below this health (half-hearts)
        setting("playerrange", 32);     // stop if another player is this close (0 = ignore)
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (player.getHealth() < get("minhealth")) {
            stop(player, "health dropped");
            return;
        }
        if (get("playerrange") > 0 && Targets.nearestEnemy(player, level, get("playerrange")) != null) {
            stop(player, "a player is nearby");
            return;
        }
        if (mc.screen != null) return;
        mc.options.keyAttack.setDown(true);
        mc.options.keyUp.setDown(get("forward") > 0);
    }

    private void stop(LocalPlayer player, String reason) {
        player.displayClientMessage(Component.literal("[Vanguard] AutoMine stopped: " + reason), false);
        setEnabled(false);
    }

    @Override
    protected void onDisable() {
        mc.options.keyAttack.setDown(false);
        mc.options.keyUp.setDown(false);
    }
}
