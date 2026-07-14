package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.ProbeDroidEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Idle hover-wander for the probe droid: every so often, drift to a random
 * nearby point ~{@link ProbeDroidEntity#HOVER_HEIGHT} blocks above the
 * ground (Vex-style direct move-control orders — no pathfinding needed for
 * a free flier).
 */
public class ProbeHoverGoal extends Goal {

    private static final int WANDER_RADIUS = 6;

    private final ProbeDroidEntity probe;

    public ProbeHoverGoal(ProbeDroidEntity probe) {
        this.probe = probe;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return probe.getTarget() == null
            && !probe.getMoveControl().hasWanted()
            && probe.getRandom().nextInt(reducedTickDelay(20)) == 0;
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        var random = probe.getRandom();
        double x = probe.getX() + random.nextInt(2 * WANDER_RADIUS + 1) - WANDER_RADIUS;
        double z = probe.getZ() + random.nextInt(2 * WANDER_RADIUS + 1) - WANDER_RADIUS;
        double y = probe.hoverTargetY(x, z);
        probe.getMoveControl().setWantedPosition(x, y, z, 1.0);
    }
}
