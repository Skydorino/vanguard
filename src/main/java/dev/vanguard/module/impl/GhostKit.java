package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * GhostKit — gamemode spoof + creative slot injection.
 * Reflects into MultiPlayerGameMode.localPlayerMode, sets it to CREATIVE,
 * fires ServerboundSetCreativeModeSlotPacket for each kit item, then restores SURVIVAL.
 * Enable once → fires → disables itself.
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

            List<ItemStack> kit = buildKit(level);
            for (int i = 0; i < Math.min(kit.size(), 9); i++) {
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

    private List<ItemStack> buildKit(ClientLevel level) {
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        addEnchant(elytra, level, "unbreaking", 3);
        addEnchant(elytra, level, "mending", 1);

        ItemStack totem1 = new ItemStack(Items.TOTEM_OF_UNDYING);
        ItemStack totem2 = new ItemStack(Items.TOTEM_OF_UNDYING);

        ItemStack pots = new ItemStack(Items.SPLASH_POTION, 3);
        applyPotion(pots, "strong_healing");

        ItemStack gaps   = new ItemStack(Items.GOLDEN_APPLE, 16);
        ItemStack pearls = new ItemStack(Items.ENDER_PEARL, 16);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        addEnchant(sword, level, "sharpness",   5);
        addEnchant(sword, level, "unbreaking",  3);
        addEnchant(sword, level, "mending",     1);
        addEnchant(sword, level, "fire_aspect", 2);
        addEnchant(sword, level, "looting",     3);

        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        addEnchant(pick, level, "efficiency", 5);
        addEnchant(pick, level, "unbreaking", 3);
        addEnchant(pick, level, "mending",    1);

        ItemStack bow = new ItemStack(Items.BOW);
        addEnchant(bow, level, "power",     5);
        addEnchant(bow, level, "infinity",  1);
        addEnchant(bow, level, "unbreaking",3);
        addEnchant(bow, level, "flame",     1);

        return List.of(elytra, totem1, totem2, pots, gaps, pearls, sword, pick, bow);
    }

    /** Looks up an enchantment by its minecraft:path registry key and applies it. */
    private void addEnchant(ItemStack stack, ClientLevel level, String path, int lvl) {
        try {
            var registry = level.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            // Build the resource key manually to avoid importing ResourceLocation
            var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ENCHANTMENT,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", path));
            Optional<Holder.Reference<Enchantment>> ref = registry.get(key);
            if (ref.isEmpty()) return;

            ItemEnchantments existing = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
            mutable.set(ref.get(), lvl);
            stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mutable.toImmutable());
        } catch (Exception ignored) {}
    }

    private void applyPotion(ItemStack stack, String path) {
        try {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", path);
            var reg = BuiltInRegistries.POTION;
            var potion = reg.get(id);
            if (potion != null) {
                stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                    new PotionContents(
                        Optional.of(reg.wrapAsHolder(potion)),
                        Optional.empty(),
                        List.of()));
            }
        } catch (Exception ignored) {}
    }
}
// END OF FILE