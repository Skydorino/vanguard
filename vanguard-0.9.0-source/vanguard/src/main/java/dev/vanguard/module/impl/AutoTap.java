package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;

/** After each attack on an entity, briefly releases W (W-tap) or taps S as well (S-tap). */
public class AutoTap extends Module {
    private int remaining;

    public AutoTap() {
        super("AutoTap", Category.COMBAT, "Automatic W-tap / S-tap after you hit something");
        setting("mode", 1);    // 1 = W-tap, 2 = S-tap
        setting("length", 1);  // ticks the key is held off
    }

    @Override
    protected void onDisable() {
        remaining = 0;
        KeyMapping.setAll();
    }

    private void apply() {
        mc.options.keyUp.setDown(false);
        if (get("mode") >= 2) mc.options.keyDown.setDown(true);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (remaining > 0) {
            apply();
            if (--remaining == 0) KeyMapping.setAll(); // hand the keys back to your real keyboard state
            return;
        }
        if (mc.screen != null) return;
        // The tick a swing starts, and an entity is under the crosshair
        if (player.swinging && player.swingTime == 0 && mc.hitResult instanceof EntityHitResult) {
            remaining = Math.max(1, (int) get("length"));
            apply();
        }
    }
}
