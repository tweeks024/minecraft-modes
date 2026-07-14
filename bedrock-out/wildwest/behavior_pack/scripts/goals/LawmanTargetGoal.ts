// TODO LLM: cache miss; run :translate --with-llm to translate
// Goal: com.tweeks.wildwest.entity.ai.LawmanTargetGoal
//
// This file is a placeholder. Either:
//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or
//   2. Hand-translate the Java below to @minecraft/server event handlers.

/*
Original Java source — translate this:

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

*/

// Empty handler so Bedrock's script engine accepts the file.
export {};
