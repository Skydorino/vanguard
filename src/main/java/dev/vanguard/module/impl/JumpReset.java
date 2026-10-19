package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/** Jumps the moment you take a hit while on the ground, to cut the knockback. */
public class JumpReset extends Module {
    private boolean jumped;
    private int release;

    public JumpReset() {
        super("JumpReset", Category.COMBAT, "Jumps automatically when you get hit");
    }

    @Override
    protected void onDisable() {
        release = 0;
        jumped = false;
        KeyMapping.setAll();
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (release > 0) {
            if (--release == 0) KeyMapping.setAll();
            return;
        }
        if (player.hurtTime == 0) jumped = false;
        if (!jumped && player.hurtTime >= 8 && player.onGround() && mc.screen == null) {
            mc.options.keyJump.setDown(true);
            release = 2;
            jumped = true;
        }
    }
}
