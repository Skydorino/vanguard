package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;

/** Creative-style flight. Servers that check movement will rubber-band or kick you. */
public class Flight extends Module {
    public Flight() {
        super("Flight", Category.MOVEMENT, "Creative-style flight (only works where the server allows it)");
        setting("speed", 1.5);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        Abilities ab = player.getAbilities();
        ab.mayfly = true;
        ab.flying = true;
        ab.setFlyingSpeed((float) (0.05 * get("speed")));
    }

    @Override
    protected void onDisable() {
        LocalPlayer p = mc.player;
        if (p == null || p.isCreative() || p.isSpectator()) return;
        Abilities ab = p.getAbilities();
        ab.flying = false;
        ab.mayfly = false;
        ab.setFlyingSpeed(0.05f);
    }
}
