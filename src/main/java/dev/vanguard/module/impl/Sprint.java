package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", Category.MOVEMENT, "Sprint automatically while moving forward");
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.options.keyUp.isDown()) mc.options.keySprint.setDown(true);
    }

    @Override
    protected void onDisable() {
        mc.options.keySprint.setDown(false);
    }
}
