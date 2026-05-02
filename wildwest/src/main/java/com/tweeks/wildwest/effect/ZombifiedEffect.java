package com.tweeks.wildwest.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ZombifiedEffect extends MobEffect {
    public static final Identifier SPEED_MOD_ID =
        Identifier.fromNamespaceAndPath("wildwest", "zombified_speed");

    public ZombifiedEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A7C2E);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MOD_ID, 0.30,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.tickCount % 10 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                2, 0.3, 0.3, 0.3, 0.0);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
