package dev.vanguard.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/**
 * Tilts the items in your hands (first person). The actual tilt is applied by
 * ItemInHandRendererMixin, which calls apply() just before each hand is drawn.
 */
public class ItemLean extends Module {
    private double current;

    public ItemLean() {
        super("ItemLean", Category.VISUALS, "Tilts the items in your hands");
        setting("roll", 18);    // degrees of sideways lean
        setting("pitch", 0);    // degrees of forward/back tilt
        setting("yaw", 0);      // degrees of turn
        setting("sway", 8);     // extra lean in the direction you strafe
    }

    @Override
    protected void onEnable() {
        current = get("roll");
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        double strafe = (mc.options.keyRight.isDown() ? 1.0 : 0.0) - (mc.options.keyLeft.isDown() ? 1.0 : 0.0);
        double target = get("roll") + get("sway") * strafe;
        current += (target - current) * 0.3;
    }

    /** Called by the mixin with the pose used to draw a held item. */
    public void apply(PoseStack stack) {
        stack.mulPose(Axis.ZP.rotationDegrees((float) current));
        if (get("pitch") != 0.0) stack.mulPose(Axis.XP.rotationDegrees((float) get("pitch")));
        if (get("yaw") != 0.0) stack.mulPose(Axis.YP.rotationDegrees((float) get("yaw")));
    }
}
