package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WeaponMode;
import com.tweeks.wildwest.entity.WildWestMob;
import com.tweeks.wildwest.item.PistolItem;
import com.tweeks.wildwest.item.RifleItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class WildWestRangedAttackGoal extends Goal {

    private final WildWestMob mob;
    private int cooldown;

    public WildWestRangedAttackGoal(WildWestMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getWeaponMode() == WeaponMode.RANGED
            && mob.getTarget() != null
            && mob.getTarget().isAlive()
            && mob.getSensing().hasLineOfSight(mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.cooldown = 20;
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
        if (--this.cooldown <= 0) {
            if (mob.usesRifle()) {
                RifleItem.fireFromMob(mob, target);
                this.cooldown = (int) (RifleItem.COOLDOWN_TICKS * 1.5);
            } else {
                PistolItem.fireFromMob(mob, target);
                this.cooldown = (int) (PistolItem.COOLDOWN_TICKS * 1.5);
            }
        }
    }
}
