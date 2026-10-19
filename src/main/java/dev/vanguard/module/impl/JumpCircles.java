package dev.vanguard.module.impl;

import dev.vanguard.gui.Draw;
import dev.vanguard.gui.Theme;
import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import dev.vanguard.render.WorldDraw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** A ring spreads out and fades from your feet every time you jump. */
public class JumpCircles extends Module {
    private record Circle(Vec3 pos, long startMs) {}

    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround = true;

    public JumpCircles() {
        super("JumpCircles", Category.VISUALS, "A fading ring appears under you when you jump");
        setting("size", 2.0);
        setting("lifetime", 900); // milliseconds
    }

    @Override
    protected void onDisable() {
        circles.clear();
    }

    @Override
    public void onTick(LocalPlayer player, ClientLevel level) {
        boolean onGround = player.onGround();
        if (wasOnGround && !onGround && player.getDeltaMovement().y > 0.1) {
            circles.add(new Circle(player.position().add(0.0, 0.03, 0.0), System.currentTimeMillis()));
        }
        wasOnGround = onGround;
    }

    @Override
    public void onWorldRender(WorldDraw draw) {
        long now = System.currentTimeMillis();
        double life = Math.max(100.0, get("lifetime"));
        circles.removeIf(c -> now - c.startMs() > life);
        for (Circle c : circles) {
            double t = (now - c.startMs()) / life;
            double r = 0.3 + t * get("size");
            int alpha = (int) ((1.0 - t) * 200.0);
            draw.ring(c.pos(), Math.max(0.05, r - 0.06), r + 0.06, 48, Draw.alpha(Theme.accent(), alpha));
        }
    }
}
