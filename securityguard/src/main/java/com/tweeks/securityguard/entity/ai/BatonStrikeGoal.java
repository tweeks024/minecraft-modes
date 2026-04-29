package com.tweeks.securityguard.entity.ai;

import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class BatonStrikeGoal extends MeleeAttackGoal {

    private static final int STUN_DURATION_TICKS = 60;
    private static final int SLOWNESS_AMPLIFIER = 1;
    private static final int WEAKNESS_AMPLIFIER = 0;
    private static final double KNOCKBACK_STRENGTH = 0.2;

    public BatonStrikeGoal(SecurityGuardEntity guard) {
        super(guard, 1.0, true);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(this.mob.getUsedItemHand());
            this.mob.doHurtTarget((ServerLevel) this.mob.level(), target);

            if (target.isAlive()) {
                target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, STUN_DURATION_TICKS, SLOWNESS_AMPLIFIER));
                target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, STUN_DURATION_TICKS, WEAKNESS_AMPLIFIER));
                target.knockback(
                    KNOCKBACK_STRENGTH,
                    this.mob.getX() - target.getX(),
                    this.mob.getZ() - target.getZ());
            }
        }
    }
}
