package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.entity.SwMobConstants;
import com.tweeks.starwars.item.BlasterPistolItem;
import com.tweeks.starwars.item.BlasterRifleItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * A blaster-armed {@link SwMob} fights like ranged infantry: it advances on
 * its target when out of range or when cover blocks the shot, holds at a
 * standoff distance once it has a clear line, and fires on the fire-interval
 * cooldown. Owning {@link Flag#MOVE} (not just LOOK) is what lets troopers
 * and droids pursue instead of standing still as turrets.
 */
public class BlasterAttackGoal extends Goal {

    /** Stay roughly this far out; advance if farther or if LoS is blocked. */
    private static final double STANDOFF = 9.0;
    private static final double STANDOFF_SQ = STANDOFF * STANDOFF;
    /** Only fire with a clear line and within this range. */
    private static final double FIRE_RANGE_SQ = 18.0 * 18.0;
    private static final double MOVE_SPEED = 1.0;

    private final SwMob mob;
    private int cooldown;
    private int repathDelay;

    public BlasterAttackGoal(SwMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return mob.usesBlaster() && target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.cooldown = SwMobConstants.FIRE_INTERVAL_TICKS;
        this.repathDelay = 0;
    }

    @Override
    public void stop() {
        this.cooldown = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30, 30);

        double distSq = mob.distanceToSqr(target);
        boolean hasLineOfSight = mob.getSensing().hasLineOfSight(target);

        // Close the gap when out of the standoff band or when cover blocks
        // the shot; otherwise hold position and shoot.
        if (distSq > STANDOFF_SQ || !hasLineOfSight) {
            if (--this.repathDelay <= 0) {
                this.repathDelay = 10;
                mob.getNavigation().moveTo(target, MOVE_SPEED);
            }
        } else {
            mob.getNavigation().stop();
        }

        if (hasLineOfSight && distSq <= FIRE_RANGE_SQ && --this.cooldown <= 0) {
            int color = mob.getTracerColor();
            if (mob.usesRifleBlaster()) {
                BlasterRifleItem.fireFromMobRifle(mob, target, color);
            } else {
                BlasterPistolItem.fireFromMob(mob, target, BlasterPistolItem.DAMAGE, color);
            }
            this.cooldown = SwMobConstants.FIRE_INTERVAL_TICKS;
        }
    }
}
