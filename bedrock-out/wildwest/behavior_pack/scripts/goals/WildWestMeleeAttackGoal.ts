// TODO LLM: cache miss; run :translate --with-llm to translate
// Goal: com.tweeks.wildwest.entity.ai.WildWestMeleeAttackGoal
//
// This file is a placeholder. Either:
//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or
//   2. Hand-translate the Java below to @minecraft/server event handlers.

/*
Original Java source — translate this:

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

*/

// Empty handler so Bedrock's script engine accepts the file.
export {};
