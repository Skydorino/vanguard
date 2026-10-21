package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One-shot TNT minecart macro. Look at the top of a block and switch this on: it places a rail,
 * places a TNT minecart on it, lights the cart with flint and steel or a fire charge, then
 * switches itself off. Needs a rail, a TNT minecart and a lighter in your hotbar.
 */
public class AutoCart extends Module {
    private int stage;
    private int timer;
    private int wait;
    private BlockPos railPos;

    public AutoCart() {
        super("AutoCart", Category.COMBAT, "Places a rail and TNT minecart where you look, then lights it");
        setting("delay", 2);
    }

    @Override
    protected void onEnable() {
        stage = 0;
        timer = 0;
        wait = 0;
        railPos = null;
    }

    private void fail(LocalPlayer player, String reason) {
        player.displayClientMessage(Component.literal("[Vanguard] AutoCart: " + reason), false);
        setEnabled(false);
    }

    private void select(LocalPlayer player, int slot) {
        player.getInventory().setSelectedSlot(slot);
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null || mc.screen != null) return;
        if (timer > 0) {
            timer--;
            return;
        }
        int delay = Math.max(1, (int) get("delay"));

        switch (stage) {
            case 0 -> {
                if (!(mc.hitResult instanceof BlockHitResult hit)
                    || hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
                    fail(player, "look at the top of a block");
                    return;
                }
                int rail = Inv.findHotbar(player, s -> s.is(ItemTags.RAILS));
                if (rail < 0) {
                    fail(player, "no rail in your hotbar");
                    return;
                }
                select(player, rail);
                mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                player.swing(InteractionHand.MAIN_HAND);
                railPos = hit.getBlockPos().above();
                stage = 1;
                wait = 0;
                timer = delay;
            }
            case 1 -> {
                if (!level.getBlockState(railPos).is(BlockTags.RAILS)) {
                    if (++wait > 20) fail(player, "the rail did not place");
                    return;
                }
                int cart = Inv.findHotbar(player, Items.TNT_MINECART);
                if (cart < 0) {
                    fail(player, "no TNT minecart in your hotbar");
                    return;
                }
                select(player, cart);
                BlockHitResult onRail = new BlockHitResult(Vec3.atCenterOf(railPos), Direction.UP, railPos, false);
                mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, onRail);
                player.swing(InteractionHand.MAIN_HAND);
                stage = 2;
                wait = 0;
                timer = delay;
            }
            default -> {
                Entity cart = null;
                for (Entity e : level.getEntities((Entity) null, new AABB(railPos).inflate(1.0))) {
                    if (e.getType() == EntityType.TNT_MINECART) cart = e;
                }
                if (cart == null) {
                    if (++wait > 20) fail(player, "the minecart did not place");
                    return;
                }
                int light = Inv.findHotbar(player, s -> s.is(Items.FLINT_AND_STEEL) || s.is(Items.FIRE_CHARGE));
                if (light < 0) {
                    fail(player, "no flint and steel or fire charge in your hotbar");
                    return;
                }
                select(player, light);
                mc.gameMode.interact(player, cart, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);
                setEnabled(false);
            }
        }
    }
}
