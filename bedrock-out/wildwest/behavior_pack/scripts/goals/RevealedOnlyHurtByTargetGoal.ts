// TODO LLM: cache miss; run :translate --with-llm to translate
// Goal: com.tweeks.wildwest.entity.AnomalyEntity.RevealedOnlyHurtByTargetGoal
//
// This file is a placeholder. Either:
//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or
//   2. Hand-translate the Java below to @minecraft/server event handlers.

/*
Original Java source — translate this:

package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.effect.AnomalyBleedEffect;
import com.tweeks.wildwest.effect.ModEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Disguised-villager ambush mob. While disguised it ambles around like a
 * villager (no aggression, no melee, slow speed, 25% damage resist). The
 * moment a player right-clicks it — or it's pushed past its re-disguise
 * window after being engaged — it reveals: full speed, full target,
 * bleed-applying melee bite.
 *
 * <p>Constants live in {@link AnomalyEntityConstants} so tests can assert
 * on them without bootstrapping Minecraft (Monster.&lt;clinit&gt; pulls
 * BuiltInRegistries). Re-exported here so callers read
 * {@code AnomalyEntity.MAX_HEALTH} rather than the helper class.
 *
 * <p>API notes (NeoForge 26.1.2):
 * <ul>
 *   <li>{@code addAdditionalSaveData(ValueOutput)} / {@code readAdditionalSaveData(ValueInput)}
 *       — this NeoForge build uses the ValueIO API, not raw CompoundTag.</li>
 *   <li>{@code doHurtTarget(ServerLevel, Entity)} returns boolean.</li>
 *   <li>{@code lastHurtByMobTimestamp} is private; use
 *       {@link LivingEntity#getLastHurtByMobTimestamp()}.</li>
 * </ul>
 */
public class AnomalyEntity extends Monster {

    // Re-exports of AnomalyEntityConstants so implementation reads AnomalyEntity.MAX_HEALTH.
    public static final double MAX_HEALTH = AnomalyEntityConstants.MAX_HEALTH;
    public static final double ATTACK_DAMAGE = AnomalyEntityConstants.ATTACK_DAMAGE;
    public static final double SPEED_REVEALED = AnomalyEntityConstants.SPEED_REVEALED;
    public static final double SPEED_DISGUISED = AnomalyEntityConstants.SPEED_DISGUISED;
    public static final double KNOCKBACK_RESISTANCE = AnomalyEntityConstants.KNOCKBACK_RESISTANCE;
    public static final double FOLLOW_RANGE = AnomalyEntityConstants.FOLLOW_RANGE;
    public static final int RE_DISGUISE_TICKS = AnomalyEntityConstants.RE_DISGUISE_TICKS;
    public static final float DISGUISED_DAMAGE_MULTIPLIER = AnomalyEntityConstants.DISGUISED_DAMAGE_MULTIPLIER;
    private static final int DAMAGE_GRACE_TICKS = AnomalyEntityConstants.DAMAGE_GRACE_TICKS;

    private static final EntityDataAccessor<Boolean> DATA_REVEALED =
        SynchedEntityData.defineId(AnomalyEntity.class, EntityDataSerializers.BOOLEAN);

    private int reDisguiseTicks = 0;

    public AnomalyEntity(EntityType<? extends AnomalyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, SPEED_DISGUISED)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REVEALED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RevealedOnlyMeleeGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // HurtByTarget fires earlier (priority 0) than the LOS-required
        // NearestAttackableTarget so a player who breaks line-of-sight and
        // then hits the Anomaly from behind still gets re-aggroed. Both
        // target goals are gated on isRevealed() so disguised hits never
        // trigger aggression — that's the spec's "stay disguised under
        // first hit" contract.
        targetSelector.addGoal(0, new RevealedOnlyHurtByTargetGoal(this));
        targetSelector.addGoal(1, new RevealedOnlyTargetGoal(this));
    }

    public boolean isRevealed() {
        return this.entityData.get(DATA_REVEALED);
    }

    private void setRevealed(boolean revealed) {
        this.entityData.set(DATA_REVEALED, revealed);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
            revealed ? SPEED_REVEALED : SPEED_DISGUISED);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Consume the right-click on BOTH sides while disguised so the
        // player's held item doesn't also fire (no torch-placing through
        // the entity, no food-eating, no spawn-egg misfire). Server runs
        // the actual state change; client just acknowledges the consume.
        if (!isRevealed()) {
            if (!level().isClientSide()) {
                setRevealed(true);
                this.setTarget(player);
                this.reDisguiseTicks = 0;
                level().playSound(null, blockPosition(),
                    com.tweeks.wildwest.ModSounds.ANOMALY_REVEAL.get(),
                    SoundSource.HOSTILE, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(
                new MobEffectInstance(
                    ModEffects.ANOMALY_BLEED,
                    AnomalyBleedEffect.BLEED_DURATION_TICKS,
                    0),
                this);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && isRevealed()) {
            boolean engaged = getTarget() != null
                || (tickCount - getLastHurtByMobTimestamp()) < DAMAGE_GRACE_TICKS;
            if (engaged) {
                reDisguiseTicks = 0;
            } else {
                reDisguiseTicks++;
                if (reDisguiseTicks >= RE_DISGUISE_TICKS) {
                    setRevealed(false);
                    reDisguiseTicks = 0;
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Revealed", isRevealed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setRevealed(input.getBooleanOr("Revealed", false));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    /**
     * Pin XP drop to 5 explicitly (spec §109). Vanilla {@code Monster}
     * defaults {@code xpReward} to 5 today, but overriding
     * {@code getBaseExperienceReward} guards against an upstream default
     * change silently shifting the value. NeoForge 26.1.2 patches the
     * signature to take a {@link ServerLevel} (vanilla 26.1.2 is no-arg);
     * {@code getExperienceReward(ServerLevel, Entity)} itself is
     * {@code final}, so this base method is the override hook.
     */
    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 5;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return com.tweeks.wildwest.ModSounds.ANOMALY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.tweeks.wildwest.ModSounds.ANOMALY_DEATH.get();
    }

    private static final class RevealedOnlyMeleeGoal extends MeleeAttackGoal {
        private final AnomalyEntity anomaly;
        RevealedOnlyMeleeGoal(AnomalyEntity anomaly, double speed, boolean followIfNotSeen) {
            super(anomaly, speed, followIfNotSeen);
            this.anomaly = anomaly;
        }
        @Override public boolean canUse() { return anomaly.isRevealed() && super.canUse(); }
        @Override public boolean canContinueToUse() { return anomaly.isRevealed() && super.canContinueToUse(); }
    }

    private static final class RevealedOnlyTargetGoal extends NearestAttackableTargetGoal<Player> {
        private final AnomalyEntity anomaly;
        RevealedOnlyTargetGoal(AnomalyEntity anomaly) {
            super(anomaly, Player.class, true);
            this.anomaly = anomaly;
        }
        @Override public boolean canUse() { return anomaly.isRevealed() && super.canUse(); }
        @Override public boolean canContinueToUse() { return anomaly.isRevealed() && super.canContinueToUse(); }
    }

    private static final class RevealedOnlyHurtByTargetGoal extends HurtByTargetGoal {
        private final AnomalyEntity anomaly;
        RevealedOnlyHurtByTargetGoal(AnomalyEntity anomaly) {
            super(anomaly);
            this.anomaly = anomaly;
        }
        @Override public boolean canUse() { return anomaly.isRevealed() && super.canUse(); }
        @Override public boolean canContinueToUse() { return anomaly.isRevealed() && super.canContinueToUse(); }
    }
}

*/

// Empty handler so Bedrock's script engine accepts the file.
export {};
