package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.api.Outlaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class LawmanTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    public LawmanTargetGoal(Mob mob) {
        super(mob, LivingEntity.class, 5, false, false,
            (target, level) -> target instanceof Outlaw && target.isAlive());
    }
}
