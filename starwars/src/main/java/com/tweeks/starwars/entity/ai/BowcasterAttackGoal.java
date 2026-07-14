package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.ChewbaccaEntity;
import com.tweeks.starwars.item.BlasterPistolItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Chewbacca's bowcaster: a hitscan ranged attack for a <em>tamed</em>
 * Chewbacca, mirroring {@link BlasterAttackGoal}'s structure but firing
 * through the shared {@link BlasterPistolItem#fireFromMob} hitscan at
 * bowcaster power — {@link #DAMAGE} per bolt, one bolt per
 * {@link #COOLDOWN_TICKS}, only while the target is inside {@link #RANGE}
 * with line of sight. Wild (untamed) Chewbacca never uses it: {@link #canUse}
 * gates on {@link ChewbaccaEntity#isTame()}, so an untamed wookiee defends
 * himself with fists (the melee goal) and never shoots unprovoked.
 */
public class BowcasterAttackGoal extends Goal {

    public static final float DAMAGE = 7.0F;
    public static final int COOLDOWN_TICKS = 25;
    public static final double RANGE = 18.0;

    private final ChewbaccaEntity chewie;
    private int cooldown;

    public BowcasterAttackGoal(ChewbaccaEntity chewie) {
        this.chewie = chewie;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = chewie.getTarget();
        return chewie.isTame()
            && target != null
            && target.isAlive()
            && chewie.distanceTo(target) <= RANGE
            && chewie.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void start() { this.cooldown = COOLDOWN_TICKS; }

    @Override
    public void stop() { this.cooldown = 0; }

    @Override
    public void tick() {
        LivingEntity target = chewie.getTarget();
        if (target == null) return;
        chewie.getLookControl().setLookAt(target, 30, 30);
        if (--this.cooldown <= 0) {
            BlasterPistolItem.fireFromMob(chewie, target, DAMAGE, chewie.getTracerColor());
            this.cooldown = COOLDOWN_TICKS;
        }
    }
}
