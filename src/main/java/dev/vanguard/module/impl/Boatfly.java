package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Flies the boat you are riding: forward/back follow your keys in the direction you face, Jump
 * goes up and Sneak goes down. Servers that check vehicle movement will rubber-band you.
 */
public class Boatfly extends Module {
    public Boatfly() {
        super("Boatfly", Category.MOVEMENT, "Fly while riding a boat");
        setting("speed", 1.0);    // horizontal blocks per tick
        setting("vspeed", 0.5);   // vertical blocks per tick
    }

    private static boolean isBoat(Entity e) {
        if (e == null) return false;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        return id.endsWith("_boat") || id.endsWith("_raft");
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        Entity boat = player.getVehicle();
        if (!isBoat(boat)) return;

        double vertical = 0.0;
        if (mc.options.keyJump.isDown()) vertical += get("vspeed");
        if (mc.options.keyShift.isDown()) vertical -= get("vspeed");

        Vec3 flat = Vec3.directionFromRotation(0.0f, player.getYRot());
        Vec3 move = Vec3.ZERO;
        if (mc.options.keyUp.isDown()) move = move.add(flat.scale(get("speed")));
        if (mc.options.keyDown.isDown()) move = move.subtract(flat.scale(get("speed")));

        boat.setNoGravity(true);
        boat.setDeltaMovement(move.x, vertical, move.z);
        boat.setYRot(player.getYRot());
    }

    @Override
    protected void onDisable() {
        if (mc.player != null && isBoat(mc.player.getVehicle())) mc.player.getVehicle().setNoGravity(false);
    }
}
