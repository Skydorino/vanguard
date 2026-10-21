package dev.vanguard.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Replaces the first-person swing with one of a few styles. The animation is applied by
 * ItemInHandRendererMixin, which calls apply() in place of the game's own swing rotation.
 * style: 0 vanilla, 1 slash, 2 stab, 3 spin, 4 overhead chop, 5 tap, 6 drop.
 */
public class SwingAnimations extends Module {
    public SwingAnimations() {
        super("SwingAnimations", Category.VISUALS, "Custom first-person swing animations");
        setting("style", 1);       // 0 vanilla, 1 slash, 2 stab, 3 spin, 4 overhead chop, 5 tap, 6 drop
        setting("strength", 1.0);  // multiplies the size of the motion
    }

    /** Returns true if this replaced the vanilla swing rotation. */
    public boolean apply(PoseStack stack, HumanoidArm arm, float progress) {
        int style = (int) get("style");
        if (style <= 0) return false;

        float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        float k = (float) get("strength");
        float s = Mth.sin(Mth.sqrt(progress) * (float) Math.PI);            // 0 -> 1 -> 0 across the swing
        float q = Mth.sin(progress * progress * (float) Math.PI);

        switch (style) {
            case 1 -> { // wide slash
                stack.mulPose(Axis.YP.rotationDegrees(side * (45.0f + q * -20.0f)));
                stack.mulPose(Axis.ZP.rotationDegrees(side * s * -75.0f * k));
                stack.mulPose(Axis.XP.rotationDegrees(s * -45.0f * k));
                stack.mulPose(Axis.YP.rotationDegrees(side * -45.0f));
            }
            case 2 -> { // stab forward
                stack.translate(0.0f, 0.0f, -s * 0.55f * k);
                stack.mulPose(Axis.XP.rotationDegrees(s * -12.0f * k));
            }
            case 3 -> { // full spin
                stack.mulPose(Axis.ZP.rotationDegrees(side * progress * 360.0f * k));
                stack.mulPose(Axis.XP.rotationDegrees(s * -30.0f));
            }
            case 4 -> { // overhead chop
                stack.mulPose(Axis.XP.rotationDegrees(s * -105.0f * k));
                stack.translate(0.0f, s * 0.1f, 0.0f);
            }
            case 6 -> { // drop: the item sinks down the screen and tips forward, then comes back
                stack.translate(side * s * -0.1f * k, -s * 0.55f * k, -s * 0.15f * k);
                stack.mulPose(Axis.XP.rotationDegrees(s * -40.0f * k));
                stack.mulPose(Axis.ZP.rotationDegrees(side * s * 10.0f * k));
            }
            default -> { // short snappy tap
                stack.mulPose(Axis.XP.rotationDegrees(s * -28.0f * k));
                stack.mulPose(Axis.ZP.rotationDegrees(side * s * -8.0f * k));
                stack.translate(0.0f, 0.0f, -s * 0.15f);
            }
        }
        return true;
    }
}
