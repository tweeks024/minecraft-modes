package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.ProbeBlasterGoal;
import com.tweeks.starwars.entity.ai.ProbeHoverGoal;
import com.tweeks.starwars.entity.ai.SwTargetGoal;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Imperial probe droid: a flying EMPIRE recon unit. No gravity +
 * {@link FlyingMoveControl} + {@link FlyingPathNavigation} (the Bee wiring
 * from the decompiled 26.1 source); hovers ~{@link #HOVER_HEIGHT} blocks
 * above ground, approaches players, and pesters targets with a weak
 * blaster ({@link ProbeBlasterGoal}). The first time it acquires a player
 * target it raises the alarm: a pitched note-block beep plus
 * {@link MobEffects#GLOWING} for {@link #ALARM_GLOW_TICKS}. Immune to fall
 * damage.
 *
 * <p>Extends {@link SwMob} for the faction seam ({@code SwTargetGoal}
 * takes an {@code SwMob}) but replaces the whole ground-mob goal set with
 * flight goals — {@code registerGoals} deliberately does not call super.
 */
public class ProbeDroidEntity extends SwMob implements Enemy {

    public static final double MAX_HEALTH = 20.0;
    public static final double MOVEMENT_SPEED = 0.25;
    public static final double FLYING_SPEED = 0.6;
    /** Preferred hover clearance above the ground. */
    public static final double HOVER_HEIGHT = 3.0;
    /** How far below to scan for ground when picking a hover height. */
    public static final double HOVER_SCAN_DEPTH = 12.0;
    /** GLOWING duration when the alarm trips. */
    public static final int ALARM_GLOW_TICKS = 100;

    public ProbeDroidEntity(EntityType<? extends ProbeDroidEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FLYING_SPEED, FLYING_SPEED)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        // Blaster is built into the chassis — nothing held.
        return ItemStack.EMPTY;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        // Deliberately NOT super.registerGoals(): SwMob's set is for ground
        // troops (float/stroll/melee); the probe flies its own goals.
        this.goalSelector.addGoal(2, new ProbeBlasterGoal(this));
        this.goalSelector.addGoal(7, new ProbeHoverGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new SwTargetGoal(this));
        // Recon unit: reports (and pesters) ANY player it spots — unlike
        // ground troopers, which only engage declared light-side champions.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * The alarm: on first acquiring a player target, beep and glow for
     * {@link #ALARM_GLOW_TICKS} so the player knows they have been made.
     * Re-arms once the player target is dropped.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean newPlayerTarget = target instanceof Player && !(this.getTarget() instanceof Player);
        super.setTarget(target);
        if (newPlayerTarget && !this.level().isClientSide()) {
            this.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 1.0F, 1.8F);
            this.addEffect(new MobEffectInstance(MobEffects.GLOWING, ALARM_GLOW_TICKS, 0));
        }
    }

    /**
     * Hover-height solver: clip straight down from ({@code x}, own y+1,
     * {@code z}); target {@link #HOVER_HEIGHT} above whatever it hits
     * (fluid surfaces included — the probe skims swamps). No ground within
     * {@link #HOVER_SCAN_DEPTH} means drift gently downward.
     */
    public double hoverTargetY(double x, double z) {
        Vec3 from = new Vec3(x, this.getY() + 1.0, z);
        Vec3 to = from.add(0.0, -HOVER_SCAN_DEPTH, 0.0);
        BlockHitResult hit = this.level().clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() == HitResult.Type.MISS) {
            return this.getY() - 1.0;
        }
        return hit.getLocation().y + HOVER_HEIGHT;
    }

    // Flier: never takes fall damage (mirrors Bee's empty override).
    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    // ---------- sounds ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;   // servo warble
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }
}
