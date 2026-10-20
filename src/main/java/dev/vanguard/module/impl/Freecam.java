package dev.vanguard.module.impl;

import com.mojang.authlib.GameProfile;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Detaches the camera so you can fly around while your character stands still and keeps facing the
 * same way. WASD moves along the way the camera faces (flat), Jump goes up, Sneak goes down, and
 * your sprint key flies faster. Movement eases in and out, and turning is applied every frame so
 * it stays smooth. Attack, use and pick-block clicks are swallowed while it is on, so nothing can be
 * attacked, placed or used from the camera's view.
 */
public class Freecam extends Module {
    private RemotePlayer camera;
    private ClientInput savedInput;
    private float lockYaw;
    private float lockPitch;
    private float camYaw;
    private float camPitch;
    private Vec3 velocity = Vec3.ZERO;

    public Freecam() {
        super("Freecam", Category.MOVEMENT, "Fly the camera around while your character stays put");
        setting("speed", 0.8);   // blocks per tick at full speed
        setting("fast", 2.5);    // speed multiplier while your sprint key is held
        setting("smooth", 0.35); // 1 = instant, lower = floatier
        setting("blockclicks", 1); // 1 = ignore attack/use/pick clicks while detached
    }

    @Override
    protected void onEnable() {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        lockYaw = camYaw = player.getYRot();
        lockPitch = camPitch = player.getXRot();
        velocity = Vec3.ZERO;

        camera = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "freecam"));
        camera.copyPosition(player);
        camera.noPhysics = true;
        camera.setYRot(camYaw);
        camera.setXRot(camPitch);
        camera.yRotO = camYaw;
        camera.xRotO = camPitch;
        camera.setOldPosAndRot();

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
        velocity = Vec3.ZERO;
        KeyMapping.setAll(); // give the mouse buttons back to your real state
    }

    /** Runs before the game reads your clicks, so they can be dropped while the camera is detached. */
    @Override
    public void onPreTick(LocalPlayer player, ClientLevel level) {
        if (camera == null || get("blockclicks") <= 0) return;
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
        while (mc.options.keyAttack.consumeClick()) { }
        while (mc.options.keyUse.consumeClick()) { }
        while (mc.options.keyPickItem.consumeClick()) { }
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (camera == null) return;
        camera.setOldPosAndRot();

        double yaw = Math.toRadians(camYaw);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));

        Vec3 wish = Vec3.ZERO;
        if (mc.options.keyUp.isDown()) wish = wish.add(forward);
        if (mc.options.keyDown.isDown()) wish = wish.subtract(forward);
        if (mc.options.keyRight.isDown()) wish = wish.add(right);
        if (mc.options.keyLeft.isDown()) wish = wish.subtract(right);
        if (mc.options.keyJump.isDown()) wish = wish.add(0.0, 1.0, 0.0);
        if (mc.options.keyShift.isDown()) wish = wish.add(0.0, -1.0, 0.0);

        double speed = get("speed") * (mc.options.keySprint.isDown() ? get("fast") : 1.0);
        if (wish.lengthSqr() > 0.0) wish = wish.normalize().scale(speed);

        double smooth = Math.max(0.05, Math.min(1.0, get("smooth")));
        velocity = velocity.add(wish.subtract(velocity).scale(smooth));
        if (velocity.lengthSqr() < 1.0e-6) velocity = Vec3.ZERO;

        camera.setPos(camera.getX() + velocity.x, camera.getY() + velocity.y, camera.getZ() + velocity.z);
    }

    /** Every frame: turn the mouse into camera rotation and keep your real character facing forward. */
    @Override
    public void onWorldRender(WorldDraw draw) {
        LocalPlayer player = mc.player;
        if (camera == null || player == null) return;

        camYaw += Mth.wrapDegrees(player.getYRot() - lockYaw);
        camPitch = Mth.clamp(camPitch + (player.getXRot() - lockPitch), -90.0f, 90.0f);
        player.setYRot(lockYaw);
        player.setXRot(lockPitch);

        camera.setYRot(camYaw);
        camera.setXRot(camPitch);
        camera.yRotO = camYaw;
        camera.xRotO = camPitch;
    }
}
