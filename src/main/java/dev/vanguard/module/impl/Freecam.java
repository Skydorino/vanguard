package dev.vanguard.module.impl;

import com.mojang.authlib.GameProfile;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Detaches the camera so you can fly around while your character stands still. Your body stays
 * put and your own movement keys are ignored until you turn it off.
 */
public class Freecam extends Module {
    private RemotePlayer camera;
    private ClientInput savedInput;

    public Freecam() {
        super("Freecam", Category.MOVEMENT, "Fly the camera around while your character stays put");
        setting("speed", 1.0);
    }

    @Override
    protected void onEnable() {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;
        camera = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "freecam"));
        camera.copyPosition(player);
        camera.noPhysics = true;
        savedInput = player.input;
        player.input = new ClientInput();
        mc.setCameraEntity(camera);
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = mc.player;
        if (player != null) {
            if (savedInput != null) player.input = savedInput;
            mc.setCameraEntity(player);
        }
        camera = null;
        savedInput = null;
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (camera == null) return;
        camera.setOldPosAndRot();
        camera.setYRot(player.getYRot());
        camera.setXRot(player.getXRot());

        double yaw = Math.toRadians(player.getYRot());
        Vec3 look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 move = Vec3.ZERO;
        if (mc.options.keyUp.isDown()) move = move.add(look);
        if (mc.options.keyDown.isDown()) move = move.subtract(look);
        if (mc.options.keyRight.isDown()) move = move.add(right);
        if (mc.options.keyLeft.isDown()) move = move.subtract(right);
        if (mc.options.keyJump.isDown()) move = move.add(0.0, 1.0, 0.0);
        if (mc.options.keyShift.isDown()) move = move.add(0.0, -1.0, 0.0);
        if (move.lengthSqr() > 0.0) move = move.normalize().scale(get("speed"));
        camera.setPos(camera.getX() + move.x, camera.getY() + move.y, camera.getZ() + move.z);
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        // Keep the view glued to the mouse between ticks
        if (camera != null && mc.player != null) {
            camera.setYRot(mc.player.getYRot());
            camera.setXRot(mc.player.getXRot());
        }
    }
}
