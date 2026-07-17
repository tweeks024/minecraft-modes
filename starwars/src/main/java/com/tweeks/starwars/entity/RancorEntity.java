package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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

/**
 * Rancor: the caged horror beneath Jabba's throne. A boss-tier beast — huge,
 * tanky, and slow but hits like a landspeeder and flings its prey with each
 * swipe ({@link Attributes#ATTACK_KNOCKBACK}). NEUTRAL in the faction war
 * ({@link SwCombatant} marker) but hunts any player on its own account. A
 * generous {@link Attributes#STEP_HEIGHT} lets it clamber the pit ledges so it
 * can't be cheesed by standing one block up. Spawned only by the Jabba's
 * Palace structure piece, never naturally.
 */
public class RancorEntity extends Monster implements SwCombatant {

    public static final double MAX_HEALTH = 120.0;
    public static final double ATTACK_DAMAGE = 14.0;
    public static final double ATTACK_KNOCKBACK = 2.0;
    public static final double MOVEMENT_SPEED = 0.30;
    public static final double KNOCKBACK_RESISTANCE = 0.9;
    public static final double STEP_HEIGHT = 1.5;
    public static final double FOLLOW_RANGE = 40.0;

    public RancorEntity(EntityType<? extends RancorEntity> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
            .add(Attributes.STEP_HEIGHT, STEP_HEIGHT)
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.NEUTRAL; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---------- sounds: the closest vanilla beast is the ravager ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F;
    }
}
