package com.tweeks.securitycore.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * A {@link MeleeAttackGoal} that, on each successful hit, applies stun effects
 * (Slowness + Weakness) and a small knockback to the target. Designed to be
 * shared between the Guard's baton and the Thief's blackjack — the caller
 * supplies all numeric parameters so each weapon can have distinct feel.
 */
public class StunningMeleeGoal extends MeleeAttackGoal {

    private final int stunDurationTicks;
    private final int slownessAmplifier;
    private final int weaknessAmplifier;
    private final double knockbackStrength;

    /**
     * @param mob                 the attacker
     * @param speedModifier       movement-speed multiplier while pursuing target (vanilla MeleeAttackGoal arg)
     * @param followingTargetEvenIfNotSeen  vanilla MeleeAttackGoal arg
     * @param stunDurationTicks   how long Slowness + Weakness last on the target (20 ticks = 1 second)
     * @param slownessAmplifier   amplifier for Slowness (0 = level I, 1 = level II)
     * @param weaknessAmplifier   amplifier for Weakness (0 = level I, 1 = level II)
     * @param knockbackStrength   horizontal knockback applied on hit (vanilla unit; 0.4 ≈ standard)
     */
    public StunningMeleeGoal(PathfinderMob mob,
                             double speedModifier,
                             boolean followingTargetEvenIfNotSeen,
                             int stunDurationTicks,
                             int slownessAmplifier,
                             int weaknessAmplifier,
                             double knockbackStrength) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.stunDurationTicks = stunDurationTicks;
        this.slownessAmplifier = slownessAmplifier;
        this.weaknessAmplifier = weaknessAmplifier;
        this.knockbackStrength = knockbackStrength;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(this.mob.getUsedItemHand());
            this.mob.doHurtTarget((ServerLevel) this.mob.level(), target);

            if (target.isAlive()) {
                target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, stunDurationTicks, slownessAmplifier));
                target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, stunDurationTicks, weaknessAmplifier));
                target.knockback(
                    knockbackStrength,
                    this.mob.getX() - target.getX(),
                    this.mob.getZ() - target.getZ());
            }
        }
    }
}
