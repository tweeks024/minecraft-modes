// TODO LLM: cache miss; run :translate --with-llm to translate
// Goal: com.tweeks.wildwest.entity.ai.OutlawTargetGoal
//
// This file is a placeholder. Either:
//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or
//   2. Hand-translate the Java below to @minecraft/server event handlers.

/*
Original Java source — translate this:

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

*/

// Empty handler so Bedrock's script engine accepts the file.
export {};
