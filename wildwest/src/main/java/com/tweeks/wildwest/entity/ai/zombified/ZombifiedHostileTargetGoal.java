package com.tweeks.wildwest.entity.ai.zombified;

import com.tweeks.wildwest.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class ZombifiedHostileTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    public ZombifiedHostileTargetGoal(Mob mob) {
        super(
            mob,
            LivingEntity.class,
            10,                          // randomInterval — only re-scan every ~10 ticks
            true,                        // mustSee — uses cached LOS check
            false,                       // mustReach
            // TargetingConditions.Selector: (target, level) -> boolean
            (target, level) -> target != null
                && target.isAlive()
                && !target.hasEffect(ModEffects.ZOMBIFIED)
                && !InfectionImmunity.isImmune(target)
        );
    }

    @Override
    public boolean canUse() {
        if (!this.mob.hasEffect(ModEffects.ZOMBIFIED)) return false;
        if (this.mob.hasEffect(ModEffects.CURING_SHAKE)) return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.mob.hasEffect(ModEffects.ZOMBIFIED)) return false;
        if (this.mob.hasEffect(ModEffects.CURING_SHAKE)) return false;
        return super.canContinueToUse();
    }
}
