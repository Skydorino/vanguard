package dev.vanguard.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Leans the items in your hands toward the middle of the screen, each one rotated in place and
 * mirrored between the two hands. The tilt is applied by ItemInHandRendererMixin, which calls
 * apply() right after the game positions each held item.
 */
public class ItemLean extends Module {
    private double sway;

    public ItemLean() {
        super("ItemLean", Category.VISUALS, "Leans your held items inward, each rotated in place");
        setting("rightroll", 55);  // degrees the right-hand item leans toward the centre
        setting("leftroll", 80);   // degrees the left-hand item leans toward the centre
        setting("pitch", 0);       // extra forward/back tilt for both
        setting("yaw", 0);         // extra turn for both
        setting("sway", 8);        // extra lean in the direction you strafe
    }

    @Override
    protected void onEnable() {
        sway = 0.0;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        double strafe = (mc.options.keyRight.isDown() ? 1.0 : 0.0) - (mc.options.keyLeft.isDown() ? 1.0 : 0.0);
        sway += (get("sway") * strafe - sway) * 0.3;
    }

    /** Called by the mixin with the pose already moved to where the item is drawn. */
    public void apply(PoseStack stack, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        double roll = right ? get("rightroll") : get("leftroll");
        float side = right ? 1.0f : -1.0f;
        stack.mulPose(Axis.ZP.rotationDegrees((float) (side * roll - sway)));
        if (get("pitch") != 0.0) stack.mulPose(Axis.XP.rotationDegrees((float) get("pitch")));
        if (get("yaw") != 0.0) stack.mulPose(Axis.YP.rotationDegrees((float) get("yaw")));
    }
}
