package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.HerobrineEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Engages target in melee at distance ≤ 4 blocks. Disables at distance &gt; 5
 * to prevent thrashing across the threshold. Otherwise vanilla behavior.
 */
public class HerobrineMeleeGoal extends MeleeAttackGoal {

    private static final double ENGAGE_RANGE_SQ = 4.0 * 4.0;
    private static final double DISENGAGE_RANGE_SQ = 5.0 * 5.0;

    public HerobrineMeleeGoal(HerobrineEntity boss) {
        super(boss, 1.0, true);
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) return false;
        LivingEntity target = this.mob.getTarget();
        if (target == null) return false;
        return this.mob.distanceToSqr(target) <= ENGAGE_RANGE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        if (!super.canContinueToUse()) return false;
        LivingEntity target = this.mob.getTarget();
        if (target == null) return false;
        return this.mob.distanceToSqr(target) <= DISENGAGE_RANGE_SQ;
    }
}
