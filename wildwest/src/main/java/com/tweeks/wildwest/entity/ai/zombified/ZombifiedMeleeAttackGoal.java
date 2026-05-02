package com.tweeks.wildwest.entity.ai.zombified;

import com.tweeks.wildwest.effect.ModEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class ZombifiedMeleeAttackGoal extends MeleeAttackGoal {
    private final PathfinderMob self;

    public ZombifiedMeleeAttackGoal(PathfinderMob self) {
        super(self, 1.0, /* mustReachTarget */ true);
        this.self = self;
    }

    @Override
    public boolean canUse() {
        if (!self.hasEffect(ModEffects.ZOMBIFIED)) return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!self.hasEffect(ModEffects.ZOMBIFIED)) return false;
        return super.canContinueToUse();
    }
}
