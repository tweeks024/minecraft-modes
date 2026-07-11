package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.entity.SwMobConstants;
import com.tweeks.starwars.item.BlasterPistolItem;
import com.tweeks.starwars.item.BlasterRifleItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BlasterAttackGoal extends Goal {

    private final SwMob mob;
    private int cooldown;

    public BlasterAttackGoal(SwMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.usesBlaster()
            && mob.getTarget() != null
            && mob.getTarget().isAlive()
            && mob.getSensing().hasLineOfSight(mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void start() { this.cooldown = SwMobConstants.FIRE_INTERVAL_TICKS; }

    @Override
    public void stop() { this.cooldown = 0; }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30, 30);
        if (--this.cooldown <= 0) {
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
