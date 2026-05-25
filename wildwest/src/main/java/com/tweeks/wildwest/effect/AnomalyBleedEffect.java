package com.tweeks.wildwest.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class AnomalyBleedEffect extends MobEffect {

    public static final int BLEED_DURATION_TICKS = AnomalyBleedSchedule.BLEED_DURATION_TICKS;
    public static final float DAMAGE_PER_TICK = AnomalyBleedSchedule.DAMAGE_PER_TICK;

    public AnomalyBleedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        DamageSource source = level.damageSources().source(DamageTypes.GENERIC);
        entity.hurtServer(level, source, DAMAGE_PER_TICK);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return AnomalyBleedSchedule.shouldTickAt(duration);
    }
}
