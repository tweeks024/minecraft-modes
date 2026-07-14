package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.vehicle.FlightPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared base for the atmospheric starfighters (X-wing, TIE fighter). Like
 * {@link LandspeederEntity} it is a {@link VehicleEntity} with client-
 * simulated driving, but instead of hovering it FLIES: the driver's look
 * direction becomes the thrust vector and the throttle spins up a single
 * airspeed scalar. All flight math lives in {@link FlightPhysics}
 * (unit-tested).
 *
 * <p>Rules (per {@link FlightPhysics}): holding forward accelerates toward
 * {@link #maxSpeed()}; releasing decays; the velocity is the look direction
 * scaled by speed with the vertical share clamped; nearly-stopped and
 * airborne it stalls-sinks; on the ground it can only taxi. Riders never
 * take fall damage — the airframe absorbs it. Fighters can cross water and,
 * being non-Mob vehicles, never despawn.
 *
 * <p>Hull health is synched and PERSISTED (a reload must not repair a
 * damaged fighter). Concrete subclasses supply the durability, top speed,
 * and — for the tracer/skin — nothing else; the drop is a shared scrap haul.
 */
public abstract class StarfighterEntity extends VehicleEntity {

    private static final EntityDataAccessor<Float> DATA_HULL_HEALTH =
        SynchedEntityData.defineId(StarfighterEntity.class, EntityDataSerializers.FLOAT);

    /** Current airspeed scalar (blocks/tick). Client-side driving state. */
    private double speed = 0.0;

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    protected StarfighterEntity(EntityType<? extends StarfighterEntity> type, Level level) {
        super(type, level);
    }

    /** Full airspeed for this airframe (blocks/tick). */
    public abstract float maxHullHealth();

    /** Top speed passed to {@link FlightPhysics#nextSpeed}. */
    public abstract double maxSpeed();

    // ---------- synched data + persistence ----------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HULL_HEALTH, this.maxHullHealth());
    }

    public float getHullHealth() {
        return this.entityData.get(DATA_HULL_HEALTH);
    }

    private void setHullHealth(float value) {
        this.entityData.set(DATA_HULL_HEALTH, value);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setHullHealth(input.getFloatOr("HullHealth", this.maxHullHealth()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("HullHealth", this.getHullHealth());
    }

    // ---------- riding ----------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!this.level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();   // single-seat cockpit
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (!this.hasPassenger(passenger)) return;
        move.accept(passenger, this.getX(), this.getY() + 0.5, this.getZ());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Land the pilot safely beside the airframe rather than dropping them
        // wherever the fighter happens to have come to rest.
        Vec3 direction = getCollisionHorizontalEscapeVector(
            this.getBbWidth() * Mth.SQRT_OF_TWO, passenger.getBbWidth(), passenger.getYRot());
        double targetX = this.getX() + direction.x;
        double targetZ = this.getZ() + direction.z;
        BlockPos targetBlockPos = BlockPos.containing(targetX, this.getBoundingBox().maxY, targetZ);
        BlockPos belowBlockPos = targetBlockPos.below();
        if (!this.level().isWaterAt(belowBlockPos)) {
            List<Vec3> targets = new ArrayList<>();
            double targetFloor = this.level().getBlockFloorHeight(targetBlockPos);
            if (DismountHelper.isBlockFloorValid(targetFloor)) {
                targets.add(new Vec3(targetX, targetBlockPos.getY() + targetFloor, targetZ));
            }
            double belowFloor = this.level().getBlockFloorHeight(belowBlockPos);
            if (DismountHelper.isBlockFloorValid(belowFloor)) {
                targets.add(new Vec3(targetX, belowBlockPos.getY() + belowFloor, targetZ));
            }
            for (Pose dismountPose : passenger.getDismountPoses()) {
                for (Vec3 target : targets) {
                    if (DismountHelper.canDismountTo(this.level(), target, passenger, dismountPose)) {
                        passenger.setPose(dismountPose);
                        return target;
                    }
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // ---------- tick / physics ----------

    @Override
    public void tick() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }
        super.tick();
        this.interpolation.interpolate();
        if (this.isLocalInstanceAuthoritative()) {
            this.tickDriven();
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        // The airframe absorbs all fall energy; neither it nor its pilot ever
        // takes fall damage (see causeFallDamage).
        this.fallDistance = 0;
        for (Entity p : this.getPassengers()) {
            p.fallDistance = 0;
        }
    }

    private void tickDriven() {
        boolean forwardHeld = false;
        if (this.getControllingPassenger() instanceof Player driver) {
            // The fighter points where the pilot looks (like AbstractHorse's
            // tickRidden): copy the driver's yaw and pitch, then thrust along
            // that heading. The climb/dive rate is bounded by FlightPhysics'
            // +-0.7*speed vertical clamp, so a full-pitch nose is safe.
            this.setYRot(driver.getYRot());
            this.setXRot(driver.getXRot());
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
            forwardHeld = driver.zza > 0;
        }

        boolean onGround = this.onGround();
        this.speed = FlightPhysics.nextSpeed(this.speed, forwardHeld, onGround, this.maxSpeed());

        double yawRad = Math.toRadians(this.getYRot());
        double pitchRad = Math.toRadians(this.getXRot());
        double cosPitch = Math.cos(pitchRad);
        // Unit look vector (MC convention: +pitch looks down, so y = -sin).
        double lookX = -Math.sin(yawRad) * cosPitch;
        double lookZ = Math.cos(yawRad) * cosPitch;

        double vx = lookX * this.speed;
        double vz = lookZ * this.speed;
        // Vertical share from the nose pitch (clamped to a fraction of the
        // airspeed) plus a gentle stall-sink when nearly stopped in the air.
        double vy = FlightPhysics.verticalComponent(this.speed, -this.getXRot())
            + FlightPhysics.sinkRate(this.speed, onGround);
        this.setDeltaMovement(vx, vy, vz);
    }

    // ---------- damage / destruction ----------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved()) return false;
        if (this.isInvulnerableToBase(source)) return false;
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild) {
            this.destroyFighter(level, false);
            return true;
        }
        this.setHullHealth(this.getHullHealth() - amount);
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.markHurt();
        this.setDamage(this.getDamage() + amount * 10.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 1.0f, 1.1f);
        if (this.getHullHealth() <= 0.0f) {
            this.destroyFighter(level, true);
        }
        return true;
    }

    private void destroyFighter(ServerLevel level, boolean dropScrap) {
        this.ejectPassengers();
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.5, this.getZ(), 24, 1.0, 0.5, 1.0, 0.02);
        level.sendParticles(ParticleTypes.CRIT,
            this.getX(), this.getY() + 0.5, this.getZ(), 14, 1.0, 0.5, 1.0, 0.1);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.0f, 1.2f);
        if (dropScrap && level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            // Downed fighters leave salvage, not a recoverable vehicle item:
            // 3-5 iron ingots + 1 redstone (spec loot).
            int iron = 3 + this.random.nextInt(3);
            this.spawnAtLocation(level, new ItemStack(Items.IRON_INGOT, iron));
            this.spawnAtLocation(level, new ItemStack(Items.REDSTONE, 1));
        }
        this.discard();
    }

    @Override
    protected Item getDropItem() {
        // Required by VehicleEntity, but destroyFighter() drops scrap directly
        // and never uses this. A vanilla fallback avoids depending on a
        // Registration item constant (the placement items are registered by
        // parallel work and are intentionally not recoverable drops).
        return Items.IRON_INGOT;
    }

    // ---------- fall / misc ----------

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        // The airframe eats all fall energy and does NOT propagate it to the
        // pilot (Entity's default would forward the fall to passengers).
        this.fallDistance = 0;
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }
}
