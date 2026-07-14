package com.tweeks.starwars.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tauntaun: rideable snow lizard. Right-click mounts — no saddle needed.
 * The rider steers via the vanilla ridden-travel plumbing (the
 * Camel/AbstractHorse hooks confirmed in the decompiled 26.1 source:
 * {@code getControllingPassenger} + {@code tickRidden} /
 * {@code getRiddenInput} / {@code getRiddenSpeed}, which
 * {@code LivingEntity#travelRidden} wires together). Speed is the
 * MOVEMENT_SPEED attribute, +30% on snow-family ground
 * ({@link TauntaunSpeed}, unit-tested). Panics when hurt only while
 * riderless — a mounted tauntaun trusts its rider.
 */
public class TauntaunEntity extends PathfinderMob {

    public static final double MAX_HEALTH = 30.0;
    public static final double MOVEMENT_SPEED = 0.35;

    public TauntaunEntity(EntityType<? extends TauntaunEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Panic only while riderless: a mounted tauntaun stays steerable.
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return !TauntaunEntity.this.isVehicle() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !TauntaunEntity.this.isVehicle() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ---------- riding ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive() && !this.isVehicle()) {
            if (this.level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            // Mirrors AbstractHorse#doPlayerRide: align the rider's view
            // with the mount before the ride starts.
            player.setYRot(this.getYRot());
            player.setXRot(this.getXRot());
            player.startRiding(this);
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        // AbstractHorse#tickRidden: the mount faces where the rider looks.
        Vec2 rotation = new Vec2(controller.getXRot() * 0.5F, controller.getYRot());
        this.setRot(rotation.y, rotation.x);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        // AbstractHorse#getRiddenInput: half-strength strafe, quarter-speed
        // reverse.
        float sideways = controller.xxa * 0.5F;
        float forward = controller.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(sideways, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float) TauntaunSpeed.riddenSpeed(
            this.getAttributeValue(Attributes.MOVEMENT_SPEED), this.isOnSnow());
    }

    /** True when the block underfoot is in the snow family (snow layer, snow block, powder snow). */
    private boolean isOnSnow() {
        return this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement())
            .is(BlockTags.SNOW);
    }

    // ---------- sounds ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }
}
