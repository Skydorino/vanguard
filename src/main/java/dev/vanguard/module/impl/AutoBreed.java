package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * AutoBreed — right-clicks nearby breedable animals with the correct food item.
 * Breeds cows/sheep (wheat), pigs (carrot/potato/beetroot), chickens (seeds).
 * Swap the right food into your hotbar; it picks whatever matches.
 */
public class AutoBreed extends Module {
    private int timer;

    public AutoBreed() {
        super("AutoBreed", Category.DONUTSMP, "Automatically breeds nearby animals");
        setting("range",    5);
        setting("interval", 20); // ticks between breed attempts
    }

    @Override
    protected void onEnable() { timer = 0; }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        if (mc.screen != null) return;
        if (--timer > 0) return;
        timer = Math.max(1, (int) get("interval"));

        double r = get("range");
        AABB box = player.getBoundingBox().inflate(r);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, box,
            a -> !a.isBaby() && !a.isInLove() && a.getTarget() == null);

        if (animals.isEmpty()) return;

        for (Animal animal : animals) {
            Item food = foodFor(animal);
            if (food == null) continue;
            int slot = Inv.findHotbar(player, food);
            if (slot == -1) continue;
            int prev = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(slot);
            mc.gameMode.interact(player, animal, InteractionHand.MAIN_HAND);
            player.getInventory().setSelectedSlot(prev);
            return; // one per interval
        }
    }

    private Item foodFor(Animal a) {
        if (a instanceof Cow || a instanceof Sheep) return Items.WHEAT;
        if (a instanceof Pig)     return Items.CARROT;
        if (a instanceof Chicken) return Items.WHEAT_SEEDS;
        if (a instanceof Rabbit)  return Items.DANDELION;
        return null;
    }
}
// END OF FILE