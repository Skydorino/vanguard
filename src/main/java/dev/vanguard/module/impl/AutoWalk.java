package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", Category.MOVEMENT, "Holds the forward key for long trips");
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        mc.options.keyUp.setDown(true);
    }

    @Override
    protected void onDisable() {
        mc.options.keyUp.setDown(false);
    }
}
