package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.NullEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Persistent floater goal. Pushes Null toward the target's horizontal position
 * while holding a fixed Y-offset above the local terrain heightmap. No path
 * navigation — sets {@code deltaMovement} directly each tick.
 *
 * <p>Y is a soft spring toward {@code heightmap(here) + 3}, clamped to ±0.4
 * per tick to avoid yo-yoing over tall structures.
 */
public class NullDriftGoal extends Goal {

    private static final double HORIZONTAL_SPEED = 0.20;
    private static final double Y_SPRING = 0.10;
    private static final double Y_CLAMP = 0.4;
    private static final double TARGET_ALTITUDE_OFFSET = 3.0;

    private final NullEntity boss;

    public NullDriftGoal(NullEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.boss.getTarget();
        return target != null && target.isAlive()
            && this.boss.distanceToSqr(target) <= 64.0 * 64.0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) return;

        double dx = target.getX() - this.boss.getX();
        double dz = target.getZ() - this.boss.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 0.001) {
            dx /= len;
            dz /= len;
        }

        BlockPos here = BlockPos.containing(this.boss.getX(), this.boss.getY(), this.boss.getZ());
        int terrainTopY = this.boss.level().getHeight(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, here.getX(), here.getZ());
        double desiredY = terrainTopY + TARGET_ALTITUDE_OFFSET;
        double dy = Mth.clamp((desiredY - this.boss.getY()) * Y_SPRING, -Y_CLAMP, Y_CLAMP);

        this.boss.setDeltaMovement(new Vec3(dx * HORIZONTAL_SPEED, dy, dz * HORIZONTAL_SPEED));
        this.boss.getLookControl().setLookAt(target, 30f, 30f);
    }
}
