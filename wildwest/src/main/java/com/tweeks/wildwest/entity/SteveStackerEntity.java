package com.tweeks.wildwest.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
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

public class SteveStackerEntity extends Monster {

    private static final EntityDataAccessor<Byte> STACK_HEIGHT =
        SynchedEntityData.defineId(SteveStackerEntity.class, EntityDataSerializers.BYTE);

    private final ServerBossEvent bossBar;

    public SteveStackerEntity(EntityType<? extends SteveStackerEntity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            this.getUUID(),
            Component.translatable("entity.wildwest.steve_stacker"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.wildwest.steve_stacker"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 90.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STACK_HEIGHT, (byte) 3);
    }

    public byte getStackHeight() {
        return this.entityData.get(STACK_HEIGHT);
    }

    private void setStackHeight(byte value) {
        this.entityData.set(STACK_HEIGHT, value);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        byte stack = this.getStackHeight();
        if (stack < 1) stack = 1;
        if (stack > 3) stack = 3;
        return EntityDimensions.scalable(0.6f, 1.95f * stack);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("StackHeight", this.getStackHeight());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setStackHeight(input.getByteOr("StackHeight", (byte) 3));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;

        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());

        byte previous = this.getStackHeight();
        byte target = SteveStackerPhase.computeStackHeight(this.getHealth(), this.getMaxHealth());
        if (target < previous) {
            // Capture the y-position of the Steve that just fell off BEFORE we shrink.
            // Each Steve occupies 1.95 blocks of vertical bbox; the "top" of the pre-shrink
            // stack is at this.getY() + (previous * 1.95). Subtract 0.95 to land near the
            // mid-chest of the falling Steve for a centered particle burst.
            double poofY = this.getY() + (previous * 1.95) - 0.95;
            double poofX = this.getX();
            double poofZ = this.getZ();

            this.entityData.set(STACK_HEIGHT, target);
            this.refreshDimensions();
            applyPhaseAttributes(target);

            ServerLevel server = (ServerLevel) this.level();
            for (int i = 0; i < 24; i++) {
                server.sendParticles(ParticleTypes.POOF,
                    poofX + (this.random.nextDouble() - 0.5) * 0.6,
                    poofY + (this.random.nextDouble() - 0.5) * 0.4,
                    poofZ + (this.random.nextDouble() - 0.5) * 0.6,
                    1, 0.0, 0.0, 0.0, 0.02);
            }
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6f, 1.2f);
        }
    }

    private void applyPhaseAttributes(byte stackHeight) {
        double speed;
        double damage;
        switch (stackHeight) {
            case 3 -> { speed = 0.25; damage = 4.0; }
            case 2 -> { speed = 0.30; damage = 6.0; }
            default -> { speed = 0.38; damage = 8.0; } // stackHeight == 1 (or clamped)
        }
        var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(speed);
        var damageAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) damageAttr.setBaseValue(damage);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide()) {
            applyPhaseAttributes(this.getStackHeight());
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STACK_HEIGHT.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PLAYER_BREATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 50;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.bossBar.removeAllPlayers();
        super.remove(reason);
    }
}
