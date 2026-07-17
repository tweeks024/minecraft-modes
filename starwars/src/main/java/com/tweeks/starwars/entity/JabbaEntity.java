package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Jabba the Hutt: the crime lord on his dais, the palace's namesake. A vast,
 * near-immovable slug ({@link Attributes#KNOCKBACK_RESISTANCE} 1.0) who does
 * not wander and does not hunt — he has no stroll goal and no target-seeking,
 * so he holds court where the structure places him. NEUTRAL in the faction
 * war, but poke him and he lashes back with a sluggish bite
 * ({@link HurtByTargetGoal}). Tanky enough that offing him feels earned; the
 * real danger is the pit below. Spawned only by the Jabba's Palace piece.
 */
public class JabbaEntity extends PathfinderMob implements SwCombatant {

    public static final double MAX_HEALTH = 80.0;
    public static final double ATTACK_DAMAGE = 6.0;
    public static final double MOVEMENT_SPEED = 0.12;
    public static final double KNOCKBACK_RESISTANCE = 1.0;
    public static final double FOLLOW_RANGE = 16.0;

    public JabbaEntity(EntityType<? extends JabbaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.NEUTRAL; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Retaliation only: a slow melee that engages once HurtByTargetGoal
        // hands him a target. No stroll goal, so he never leaves the dais.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    // ---------- sounds: guttural hoglin grunts, pitched down for bulk ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }
}
