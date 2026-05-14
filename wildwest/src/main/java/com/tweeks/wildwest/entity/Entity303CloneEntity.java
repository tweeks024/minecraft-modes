package com.tweeks.wildwest.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Visual decoy spawned by {@link Entity303Entity}'s phantom-swap defensive
 * mechanic. 1 HP, no AI goals, dies on first hit or after 6 seconds. Does
 * NOT touch {@link Entity303SavedData} — it's not the real boss.
 */
public class Entity303CloneEntity extends Monster {

    public static final int LIFETIME_TICKS = 120; // 6 s

    private int lifetime = LIFETIME_TICKS;

    public Entity303CloneEntity(EntityType<? extends Entity303CloneEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.wildwest.entity_303_clone"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty — clone does not move or attack.
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        if (--this.lifetime <= 0) {
            this.discard();
        }
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 0;
    }
}
