package com.tweeks.starwars.entity.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AT-AT foot stomp: a slow, heavy melee for anything that gets underneath
 * the walker. Structurally a trimmed {@code MeleeAttackGoal} (it reuses the
 * mob's navigation to close and {@code doHurtTarget} to strike) but with a
 * deliberately long {@link #SWING_INTERVAL_TICKS} cadence — the walker
 * plants a foot slowly. Damage is the mob's {@code ATTACK_DAMAGE} attribute.
 */
public class AtAtStompGoal extends Goal {

    /** Slow siege cadence between stomps. */
    public static final int SWING_INTERVAL_TICKS = 30;

    private final PathfinderMob mob;
    private final double speedModifier;
    private int cooldown;
    private int repathDelay;

    public AtAtStompGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.cooldown = 0;
        this.repathDelay = 0;
        this.mob.setAggressive(true);
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Re-path toward the target periodically (cheap for a slow walker).
        if (--this.repathDelay <= 0) {
            this.repathDelay = 10;
            this.mob.getNavigation().moveTo(target, this.speedModifier);
        }

        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.cooldown <= 0
            && this.mob.isWithinMeleeAttackRange(target)
            && this.mob.getSensing().hasLineOfSight(target)) {
            this.cooldown = SWING_INTERVAL_TICKS;
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(getServerLevel(this.mob), target);
        }
    }
}
