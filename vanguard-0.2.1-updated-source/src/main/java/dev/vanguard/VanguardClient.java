package dev.vanguard;

import dev.vanguard.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;

public class VanguardClient implements ClientModInitializer {
    public static final String MOD_ID = "vanguard";

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Hud.init();
        Commands.init();
    }
}
