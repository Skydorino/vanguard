package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import net.minecraft.client.Options;

/** Turns down render distance and a few costly options while on, and restores them when off. */
public class PerformanceMode extends Module {
    private int renderDistance;
    private int simulationDistance;
    private int biomeBlend;
    private double entityScale;
    private boolean entityShadows;
    private boolean saved;

    public PerformanceMode() {
        super("PerformanceMode", Category.MISC, "Lowers render distance and other costly settings for smoother play");
        setting("renderdistance", 8);
        setting("simulationdistance", 6);
    }

    @Override
    protected void onEnable() {
        Options o = mc.options;
        renderDistance = o.renderDistance().get();
        simulationDistance = o.simulationDistance().get();
        biomeBlend = o.biomeBlendRadius().get();
        entityScale = o.entityDistanceScaling().get();
        entityShadows = o.entityShadows().get();
        saved = true;

        o.renderDistance().set(Math.max(2, Math.min(renderDistance, (int) get("renderdistance"))));
        o.simulationDistance().set(Math.max(5, Math.min(simulationDistance, (int) get("simulationdistance"))));
        o.biomeBlendRadius().set(0);
        o.entityDistanceScaling().set(0.5);
        o.entityShadows().set(false);
    }

    @Override
    protected void onDisable() {
        if (!saved) return;
        Options o = mc.options;
        o.renderDistance().set(renderDistance);
        o.simulationDistance().set(simulationDistance);
        o.biomeBlendRadius().set(biomeBlend);
        o.entityDistanceScaling().set(entityScale);
        o.entityShadows().set(entityShadows);
        saved = false;
    }
}
