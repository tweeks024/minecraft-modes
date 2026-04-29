package com.tweeks.securitycore.ai;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Applies the Security Pack stun bundle (Slowness + Weakness + knockback)
 * to a target. Single source of truth for "what does a stunning weapon do
 * on hit?" — used by {@link StunningMeleeGoal} for NPC swings and by
 * mod items (Guard's baton, Thief's blackjack) for player swings.
 */
public final class StunEffects {

    private StunEffects() {}

    /**
     * Applies Slowness + Weakness for {@code durationTicks} and pushes
     * {@code target} away from {@code attacker}. No-op if the target is
     * already dead.
     *
     * @param attacker            entity dealing the blow (used for knockback direction)
     * @param target              entity receiving the stun
     * @param durationTicks       length of both effects in ticks (20 = 1 second)
     * @param slownessAmplifier   0 = Slowness I, 1 = Slowness II
     * @param weaknessAmplifier   0 = Weakness I, 1 = Weakness II
     * @param knockbackStrength   horizontal knockback (vanilla units; 0.4 ≈ standard punch)
     */
    public static void applyStun(LivingEntity attacker,
                                 LivingEntity target,
                                 int durationTicks,
                                 int slownessAmplifier,
                                 int weaknessAmplifier,
                                 double knockbackStrength) {
        if (!target.isAlive()) return;

        target.addEffect(new MobEffectInstance(
            MobEffects.SLOWNESS, durationTicks, slownessAmplifier));
        target.addEffect(new MobEffectInstance(
            MobEffects.WEAKNESS, durationTicks, weaknessAmplifier));
        target.knockback(
            knockbackStrength,
            attacker.getX() - target.getX(),
            attacker.getZ() - target.getZ());
    }
}
