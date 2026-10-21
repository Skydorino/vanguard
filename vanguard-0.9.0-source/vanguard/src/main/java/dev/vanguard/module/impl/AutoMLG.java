package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Water-bucket clutch: places water just before you hit the ground, then scoops it back up. */
public class AutoMLG extends Module {
    private boolean placed;
    private int scoopTimer;

    public AutoMLG() {
        super("AutoMLG", Category.MOVEMENT, "Places a water bucket before a long fall lands, then picks it up");
        setting("minfall", 3.5);   // fall distance before it kicks in
        setting("height", 4.0);    // blocks between your eyes and the ground to place water
    }

    @Override
    protected void onDisable() {
        placed = false;
        scoopTimer = 0;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null) return;

        if (placed) {
            if (scoopTimer > 0) {
                scoopTimer--;
                return;
            }
            if (player.onGround() || player.isInWater()) {
                int bucket = Inv.findHotbar(player, Items.BUCKET);
                if (bucket >= 0) {
                    player.getInventory().setSelectedSlot(bucket);
                    player.setXRot(90.0f);
                    mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                }
                placed = false;
            }
            return;
        }

        if (player.onGround() || player.isInWater() || player.fallDistance < get("minfall")) return;
        if (player.getDeltaMovement().y > -0.4) return;

        Vec3 eye = player.getEyePosition();
        BlockHitResult below = level.clip(new ClipContext(eye, eye.add(0.0, -6.0, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (below.getType() != HitResult.Type.BLOCK) return;
        if (eye.y - below.getLocation().y > get("height")) return;

        int water = Inv.findHotbar(player, Items.WATER_BUCKET);
        if (water < 0) return;
        player.getInventory().setSelectedSlot(water);
        player.setXRot(90.0f);
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        placed = true;
        scoopTimer = 3;
    }
}
