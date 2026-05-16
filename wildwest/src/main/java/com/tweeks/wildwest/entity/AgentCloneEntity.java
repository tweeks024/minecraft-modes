package com.tweeks.wildwest.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Visual decoy spawned by {@link AgentEntity}'s phantom-swap defensive
 * mechanic. 1 HP, dies on first hit or after 6 seconds. Weakly melees
 * the nearest player (½ heart per swing) while alive — flavor pressure,
 * not real threat. Does NOT touch {@link AgentSavedData} — it's not the
 * real boss.
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
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
