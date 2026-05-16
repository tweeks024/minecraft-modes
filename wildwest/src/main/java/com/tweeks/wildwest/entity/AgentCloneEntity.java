package com.tweeks.wildwest.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Visual decoy spawned by {@link AgentEntity}'s phantom-swap defensive
 * mechanic. 1 HP, no AI goals, dies on first hit or after 6 seconds. Does
 * NOT touch {@link AgentSavedData} — it's not the real boss.
 */
public class AgentCloneEntity extends Monster {

    public static final int LIFETIME_TICKS = 120; // 6 s

    private int lifetime = LIFETIME_TICKS;

    public AgentCloneEntity(EntityType<? extends AgentCloneEntity> type, Level level) {
        super(type, level);
        // Intentionally NOT setPersistenceRequired: the clone is ephemeral
        // (6 s timer in aiStep). Without persistence, vanilla despawns it if
        // its chunk unloads with no player nearby, so a clone can't outlive
        // the boss in some far-away unloaded chunk.
        this.setCustomName(Component.translatable("entity.wildwest.the_agent_clone"));
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
