package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CropNotifier — scans nearby blocks every few seconds and notifies you
 * in chat + plays a sound when it finds fully grown crops ready to harvest.
 * Useful for manual harvest runs — you can AFK elsewhere and come back when notified.
 */
public class CropNotifier extends Module {
    private int timer;
    private int lastCount;

    public CropNotifier() {
        super("CropNotifier", Category.DONUTSMP, "Notifies you when nearby crops are ready to harvest");
        setting("range",    16);
        setting("interval", 100); // ticks between scans
    }

    @Override
    protected void onEnable() { timer = 0; lastCount = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        int range = (int) get("range");
        BlockPos origin = player.blockPosition();
        int ready = 0;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                        ready++;
                    }
                }
            }
        }

        if (ready > 0 && ready != lastCount) {
            player.displayClientMessage(
                Component.literal("[Vanguard] " + ready + " crops ready to harvest!"), false);
            level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1.0f, 1.5f, false);
        }
        lastCount = ready;
    }
}
// END OF FILE