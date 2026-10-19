package dev.vanguard.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import dev.vanguard.module.impl.ItemLean;
import dev.vanguard.module.impl.SwingAnimations;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks two small methods of the first-person hand renderer. require = 0 (and the config is not
 * marked required), so if a method is named differently in your version the hook is skipped
 * instead of crashing the game.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    /** Runs right after the game moves the pose to where a held item is drawn. */
    @Inject(method = "applyItemArmTransform", at = @At("TAIL"), require = 0)
    private void vanguard$lean(PoseStack poseStack, HumanoidArm arm, float equippedProg, CallbackInfo ci) {
        Module m = ModuleManager.find("ItemLean");
        if (m instanceof ItemLean lean && lean.isEnabled()) lean.apply(poseStack, arm);
    }

    /** Replaces the game's swing rotation when SwingAnimations is on. */
    @Inject(method = "applyItemArmAttackTransform", at = @At("HEAD"), cancellable = true, require = 0)
    private void vanguard$swing(PoseStack poseStack, HumanoidArm arm, float swingProgress, CallbackInfo ci) {
        Module m = ModuleManager.find("SwingAnimations");
        if (m instanceof SwingAnimations anim && anim.isEnabled() && anim.apply(poseStack, arm, swingProgress)) {
            ci.cancel();
        }
    }
}
