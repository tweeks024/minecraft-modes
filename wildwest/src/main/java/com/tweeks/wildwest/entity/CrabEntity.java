package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.entity.ai.CrabAlertSwarmHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class CrabEntity extends Animal implements NeutralMob {

    public final AnimationState pinchState = new AnimationState();

    private static final UniformInt PERSISTENT_ANGER_TIME =
        TimeUtil.rangeOfSeconds(CrabEntityConstants.ANGER_SECONDS_MIN, CrabEntityConstants.ANGER_SECONDS_MAX);

    private long persistentAngerEndTime = NeutralMob.NO_ANGER_END_TIME;
    private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

    public CrabEntity(EntityType<? extends CrabEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, CrabEntityConstants.MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, CrabEntityConstants.MOVEMENT_SPEED)
            .add(Attributes.ATTACK_DAMAGE, CrabEntityConstants.ATTACK_DAMAGE)
            .add(Attributes.FOLLOW_RANGE, CrabEntityConstants.FOLLOW_RANGE);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SEAGRASS);
    }

    @Override
    public @Nullable CrabEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return com.tweeks.wildwest.ModEntities.CRAB.get().create(level, EntitySpawnReason.BREEDING);
    }

    // NeutralMob impl
    @Override public long getPersistentAngerEndTime() { return persistentAngerEndTime; }
    @Override public void setPersistentAngerEndTime(long endTime) { this.persistentAngerEndTime = endTime; }
    @Override public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() { return persistentAngerTarget; }
    @Override public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> target) { this.persistentAngerTarget = target; }
    @Override public void startPersistentAngerTimer() {
        setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(random));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            protected void checkAndPerformAttack(LivingEntity target) {
                super.checkAndPerformAttack(target);
                // Broadcast pinch animation event to client.
                if (CrabEntity.this.isWithinMeleeAttackRange(target)
                        && CrabEntity.this.getAttackAnim(1.0F) <= 0.01F) {
                    CrabEntity.this.level().broadcastEntityEvent(
                        CrabEntity.this, CrabEntityConstants.EVENT_ID_PINCH);
                }
            }
        });
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2,
            s -> s.is(Items.SEAGRASS), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean result = super.hurtServer(level, source, amount);
        if (result && source.getEntity() instanceof LivingEntity attacker) {
            CrabAlertSwarmHelper.alertNearby(this, attacker);
        }
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide()) {
            updatePersistentAnger((ServerLevel) level(), true);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == CrabEntityConstants.EVENT_ID_PINCH) {
            this.pinchState.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }
}
