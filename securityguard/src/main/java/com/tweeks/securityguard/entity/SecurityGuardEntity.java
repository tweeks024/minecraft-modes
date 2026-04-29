package com.tweeks.securityguard.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

/**
 * The Security Guard entity. Subclasses {@link IronGolem} for its target-selection,
 * persistence, friendly-mob defaults, and iron-ingot repair behavior. Combat goals
 * are swapped in Task 11.
 */
public class SecurityGuardEntity extends IronGolem {

    public SecurityGuardEntity(EntityType<? extends SecurityGuardEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new com.tweeks.securityguard.entity.ai.BatonStrikeGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal(this, 0.9, 32.0f));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal(this, 0.6, false));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal(this, 0.6));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.OfferFlowerGoal(this));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
            net.minecraft.world.entity.player.Player.class, 6.0f));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal(this));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new GuardTargetHostilesGoal(this));
        this.targetSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return com.tweeks.securityguard.sound.ModSounds.AMBIENT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return com.tweeks.securityguard.sound.ModSounds.HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return com.tweeks.securityguard.sound.ModSounds.DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.85f * super.getVoicePitch();
    }

    /** Targets hostile mobs (Mob+Enemy) within follow range, except creepers. */
    public static class GuardTargetHostilesGoal
            extends net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<net.minecraft.world.entity.Mob> {
        public GuardTargetHostilesGoal(SecurityGuardEntity guard) {
            super(guard, net.minecraft.world.entity.Mob.class, 5, false, false,
                (target, level) -> target instanceof net.minecraft.world.entity.monster.Enemy
                                && !(target instanceof net.minecraft.world.entity.monster.Creeper));
        }
    }
}
