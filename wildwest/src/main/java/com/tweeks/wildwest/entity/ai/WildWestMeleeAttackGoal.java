package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WeaponMode;
import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class WildWestMeleeAttackGoal extends MeleeAttackGoal {

    private final WildWestMob mob;

    public WildWestMeleeAttackGoal(WildWestMob mob, double speedMultiplier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedMultiplier, followingTargetEvenIfNotSeen);
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return mob.getWeaponMode() == WeaponMode.MELEE && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getWeaponMode() == WeaponMode.MELEE && super.canContinueToUse();
    }
}
