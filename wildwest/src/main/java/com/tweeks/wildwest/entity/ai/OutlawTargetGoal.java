package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.api.Lawman;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class OutlawTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    public OutlawTargetGoal(Mob mob) {
        super(mob, LivingEntity.class, 5, false, false,
            (target, level) -> (target instanceof Lawman || target instanceof Player) && target.isAlive());
    }
}
