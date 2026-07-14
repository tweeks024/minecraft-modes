package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.item.BlasterPistolItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AT-AT chin-blaster: the siege walker's ranged weapon. Reuses
 * {@link BlasterPistolItem#fireFromMob} (the same hitscan every mob blaster
 * fires) with structure lifted from {@link BlasterAttackGoal}, but with the
 * walker's own tuning — {@link #DAMAGE} per shot, one shot per
 * {@link #COOLDOWN_TICKS}, only inside {@link #RANGE}. The shot originates
 * from the shooter's eye position (set very high on the AT-AT), so bolts
 * rain down from the head onto targets below.
 */
public class AtAtChinBlasterGoal extends Goal {

    public static final float DAMAGE = 8.0F;
    public static final int COOLDOWN_TICKS = 40;
    public static final double RANGE = 32.0;

    private final SwMob mob;
    private int cooldown;

    public AtAtChinBlasterGoal(SwMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null
            && target.isAlive()
            && mob.distanceTo(target) <= RANGE
            && mob.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public void stop() {
        this.cooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30, 30);
        if (--this.cooldown <= 0
            && mob.distanceTo(target) <= RANGE
            && mob.getSensing().hasLineOfSight(target)) {
            BlasterPistolItem.fireFromMob(mob, target, DAMAGE, mob.getTracerColor());
            this.cooldown = COOLDOWN_TICKS;
        }
    }
}
