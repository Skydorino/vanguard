package dev.vanguard.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import dev.vanguard.module.impl.ItemLean;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hooks the first-person hand renderer. require = 0 means that if the game's method has a
 * different name in your version, the hook is skipped instead of crashing the game.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private PoseStack vanguard$lean(PoseStack poseStack) {
        Module m = ModuleManager.find("ItemLean");
        if (m instanceof ItemLean lean && lean.isEnabled()) lean.apply(poseStack);
        return poseStack;
    }
}
