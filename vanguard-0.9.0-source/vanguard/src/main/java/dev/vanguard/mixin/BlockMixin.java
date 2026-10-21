package dev.vanguard.mixin;

import dev.vanguard.module.impl.Xray;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Decides which block faces the terrain draws while Xray is on. require = 0 (and a non-required
 * mixin config) means that if the game's method looks different in your version, this is skipped
 * instead of crashing, and only Xray's ore boxes show.
 */
@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vanguard$xray(BlockState state, BlockState neighborState, Direction direction,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (Xray.seeThrough()) cir.setReturnValue(Xray.visible(state.getBlock()));
    }
}
