package dev.vanguard.mixin;

import dev.vanguard.module.impl.Freelook;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns the camera for Freelook after the game has placed it each frame. require = 0 and a
 * non-required mixin config mean that if a name differs in your game version, this is skipped
 * instead of crashing.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("RETURN"), require = 0)
    private void vanguard$freelook(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Freelook.active()) return;
        Freelook.captureAndLock(mc.player);
        this.setRotation(Freelook.yaw(), Freelook.pitch());
    }
}
