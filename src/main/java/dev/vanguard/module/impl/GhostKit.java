package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.lang.reflect.Field;
import java.util.List;

/**
 * GhostKit — gamemode spoof + creative slot injection.
 * Reflects into MultiPlayerGameMode.localPlayerMode, sets CREATIVE,
 * fires ServerboundSetCreativeModeSlotPacket for each kit item into hotbar slots 0-8,
 * then restores the real gamemode. Fires once then disables itself.
 *
 * Kit: elytra, 2x totem, 3x splash healing potion, 16x gap, 16x ender pearl,
 *      diamond sword, diamond pickaxe, bow.
 * No enchantments — plain items only to keep this compiling cleanly.
 */
public class GhostKit extends Module {
    private static Field localPlayerModeField;

    static {
        try {
            Class<?> gm = net.minecraft.client.multiplayer.MultiPlayerGameMode.class;
            localPlayerModeField = gm.getDeclaredField("localPlayerMode");
            localPlayerModeField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            localPlayerModeField = null;
        }
    }

    public GhostKit() {
        super("GhostKit", Category.MISC,
            "Spoofs gamemode to CREATIVE and injects a NethPot kit via creative slot packets");
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        setEnabled(false);

        if (localPlayerModeField == null) {
            player.displayClientMessage(
                Component.literal("[GhostKit] Reflection failed — field not found"), false);
            return;
        }

        try {
            var gm = mc.gameMode;
            GameType realMode = (GameType) localPlayerModeField.get(gm);
            localPlayerModeField.set(gm, GameType.CREATIVE);

            List<ItemStack> kit = List.of(
                new ItemStack(Items.ELYTRA),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                new ItemStack(Items.SPLASH_POTION, 3),
                new ItemStack(Items.GOLDEN_APPLE, 16),
                new ItemStack(Items.ENDER_PEARL, 16),
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.BOW)
            );

            for (int i = 0; i < kit.size(); i++) {
                mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket(36 + i, kit.get(i)));
            }

            localPlayerModeField.set(gm, realMode);
            player.displayClientMessage(
                Component.literal("[GhostKit] Packets sent — check your inventory"), false);

        } catch (Exception e) {
            player.displayClientMessage(
                Component.literal("[GhostKit] Error: " + e.getMessage()), false);
        }
    }
}
// END OF FILE