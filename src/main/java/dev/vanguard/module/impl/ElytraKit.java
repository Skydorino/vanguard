package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * For the host of a singleplayer, LAN or Essential world. Puts a real elytra on your chest (your
 * chestplate is kept aside and given back when you switch this off) and keeps fireworks in your
 * inventory, so you can glide and boost. It changes your inventory on the built-in server, so only
 * the host can use it; someone who joined your world, or anyone on a normal multiplayer server, cannot.
 * It also refuses to work in worlds with more than maxplayers people online (10 by default), and
 * switches itself off if the count goes over while it is on.
 */
public class ElytraKit extends Module {
    private volatile ItemStack savedChest = ItemStack.EMPTY;
    private volatile boolean applied;
    private int timer;

    public ElytraKit() {
        super("ElytraKit", Category.MOVEMENT, "Host of a small world: wear an elytra and keep fireworks stocked");
        setting("fireworks", 64);   // restock when you have fewer than a quarter of this
        setting("maxplayers", 10);  // refuse (or switch off) when more than this many players are online
    }

    private void refuse(LocalPlayer player, String reason) {
        player.displayClientMessage(Component.literal("[Vanguard] ElytraKit: " + reason), false);
    }

    @Override
    protected void onEnable() {
        LocalPlayer player = mc.player;
        if (player == null) {
            mc.execute(() -> setEnabled(false));
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            refuse(player, "you are connected to someone else's server. Only the host of a singleplayer, LAN or "
                + "Essential world can use it, because it changes the host's built-in server.");
            mc.execute(() -> setEnabled(false));
            return;
        }
        int count = server.getPlayerList().getPlayerCount();
        int max = (int) get("maxplayers");
        if (count > max) {
            refuse(player, "this world has " + count + " players online, over the limit of " + max + ".");
            mc.execute(() -> setEnabled(false));
            return;
        }

        UUID id = player.getUUID();
        timer = 0;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp == null) return;
            savedChest = sp.getItemBySlot(EquipmentSlot.CHEST).copy();
            sp.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            applied = true;
        });
    }

    @Override
    protected void onDisable() {
        IntegratedServer server = mc.getSingleplayerServer();
        LocalPlayer player = mc.player;
        if (server == null || player == null || !applied) return;
        UUID id = player.getUUID();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp == null) return;
            sp.setItemSlot(EquipmentSlot.CHEST, savedChest);
            savedChest = ItemStack.EMPTY;
            applied = false;
        });
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = 20;
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return;

        int count = server.getPlayerList().getPlayerCount();
        int max = (int) get("maxplayers");
        if (count > max) {
            refuse(player, count + " players are online now, over the limit of " + max + ". Switching off.");
            setEnabled(false); // onDisable gives your chestplate back
            return;
        }

        int have = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(Items.FIREWORK_ROCKET)) have += s.getCount();
        }
        int target = Math.max(4, (int) get("fireworks"));
        if (have >= target / 4) return;

        UUID id = player.getUUID();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) sp.getInventory().add(new ItemStack(Items.FIREWORK_ROCKET, target));
        });
    }
}
