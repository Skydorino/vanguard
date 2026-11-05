package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.util.Inv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * AutoBreed — right-clicks nearby breedable animals with the correct food item.
 * Cows/sheep take wheat, pigs take carrots, chickens take seeds, rabbits take dandelions.
 * The food must be in your hotbar; it swaps to it, feeds the animal, then swaps back.
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
            a -> !a.isBaby() && !a.isInLove());

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
        EntityType<?> t = a.getType();
        if (t == EntityType.COW || t == EntityType.SHEEP)   return Items.WHEAT;
        if (t == EntityType.PIG)                            return Items.CARROT;
        if (t == EntityType.CHICKEN)                        return Items.WHEAT_SEEDS;
        if (t == EntityType.RABBIT)                         return Items.DANDELION;
        return null;
    }
}
// END OF FILE
