package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.vehicle.BikePhysics;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The 74-Z speeder bike: a two-seat hover vehicle. A tuned clone of
 * {@link LandspeederEntity} — same {@link VehicleEntity} boat-family
 * plumbing and client-simulated driving — but faster and twitchier, with
 * all drive math in {@link BikePhysics} (unit-tested) instead of
 * {@code HoverPhysics}. The two seats sit fore (driver) and aft (passenger)
 * along the frame rather than side-by-side.
 *
 * <p>Unlike the recoverable landspeeder, a destroyed bike leaves only
 * scrap (2 iron + 1 redstone) — a cheaper, more disposable racer.
 */
public class SpeederBikeEntity extends VehicleEntity {

    public static final float MAX_HULL_HEALTH = 30.0f;

    private static final EntityDataAccessor<Float> DATA_HULL_HEALTH =
        SynchedEntityData.defineId(SpeederBikeEntity.class, EntityDataSerializers.FLOAT);

    /** Signed forward speed (blocks/tick), driver's local frame. Client-side driving state. */
    private double forwardSpeed = 0.0;

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    public SpeederBikeEntity(EntityType<? extends SpeederBikeEntity> type, Level level) {
        super(type, level);
    }

    // ---------- synched data + persistence ----------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HULL_HEALTH, MAX_HULL_HEALTH);
    }

    public float getHullHealth() {
        return this.entityData.get(DATA_HULL_HEALTH);
    }

    private void setHullHealth(float value) {
        this.entityData.set(DATA_HULL_HEALTH, value);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setHullHealth(input.getFloatOr("HullHealth", MAX_HULL_HEALTH));
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
            // A second player boarding while the driver seat is taken simply
            // becomes the aft passenger (canAddPassenger caps the count at 2).
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 2;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (!this.hasPassenger(passenger)) return;
        int index = this.getPassengers().indexOf(passenger);
        // Fore/aft saddle: driver forward on the frame, passenger behind.
        // Offset is along the facing direction (not lateral like the
        // side-by-side landspeeder cockpit).
        double along = index == 0 ? 0.35 : -0.55;
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 facing = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        move.accept(passenger,
            this.getX() + facing.x * along, this.getY() + 0.45, this.getZ() + facing.z * along);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Mirrors LandspeederEntity/AbstractBoat: find a safe dry-land spot
        // beside the frame before falling back to Entity's bare default.
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
        // Hover vehicle: never accrue fall damage on itself or its riders.
        this.fallDistance = 0;
        for (Entity p : this.getPassengers()) {
            p.fallDistance = 0;
        }
    }

    private void tickDriven() {
        Vec3 vel = this.getDeltaMovement();
        double vy = vel.y + BikePhysics.verticalAccel(this.sampleGroundDistance(), vel.y);

        int fwd = 0;
        int turn = 0;
        if (this.getControllingPassenger() instanceof Player driver) {
            fwd = driver.zza > 0 ? 1 : (driver.zza < 0 ? -1 : 0);
            turn = driver.xxa > 0 ? 1 : (driver.xxa < 0 ? -1 : 0);
            if (turn != 0) {
                this.setYRot(BikePhysics.nextYaw(this.getYRot(), -turn));
            }
        }
        this.forwardSpeed = BikePhysics.nextForwardSpeed(this.forwardSpeed, fwd);

        double yawRad = Math.toRadians(this.getYRot());
        Vec3 facing = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        Vec3 desired = facing.scale(this.forwardSpeed);
        double bx = vel.x + (desired.x - vel.x) * BikePhysics.VELOCITY_BLEND;
        double bz = vel.z + (desired.z - vel.z) * BikePhysics.VELOCITY_BLEND;
        this.setDeltaMovement(bx, vy, bz);
    }

    /** Distance from frame origin straight down to ground, fluid surfaces included. */
    private double sampleGroundDistance() {
        Vec3 from = this.position();
        Vec3 to = from.add(0.0, -BikePhysics.HOVER_SCAN_DEPTH, 0.0);
        BlockHitResult hit = this.level().clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() == HitResult.Type.MISS) return Double.NaN;
        return from.y - hit.getLocation().y;
    }

    // ---------- damage / destruction ----------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved()) return false;
        if (this.isInvulnerableToBase(source)) return false;
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild) {
            this.destroyBike(level, false);
            return true;
        }
        this.setHullHealth(this.getHullHealth() - amount);
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.markHurt();
        this.setDamage(this.getDamage() + amount * 10.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 1.0f, 1.4f);
        if (this.getHullHealth() <= 0.0f) {
            this.destroyBike(level, true);
        }
        return true;
    }

    private void destroyBike(ServerLevel level, boolean dropScrap) {
        this.ejectPassengers();
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.4, this.getZ(), 16, 0.6, 0.3, 0.6, 0.02);
        level.sendParticles(ParticleTypes.CRIT,
            this.getX(), this.getY() + 0.4, this.getZ(), 10, 0.6, 0.3, 0.6, 0.1);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_GOLEM_DEATH, SoundSource.NEUTRAL, 1.0f, 1.5f);
        if (dropScrap && level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            // A wrecked bike leaves scrap, not a recoverable vehicle item:
            // 2 iron ingots + 1 redstone (spec loot).
            this.spawnAtLocation(level, new ItemStack(Items.IRON_INGOT, 2));
            this.spawnAtLocation(level, new ItemStack(Items.REDSTONE, 1));
        }
        this.discard();
    }

    @Override
    protected Item getDropItem() {
        // VehicleEntity requires this abstract override, but the bike's own
        // destroyBike() drops scrap directly and never routes through the
        // VehicleEntity default destroy path, so this value is unused for
        // drops. A benign vanilla fallback avoids referencing a Registration
        // item constant (the speeder_bike placement item is registered by
        // parallel work and is intentionally not a recoverable drop).
        return Items.IRON_INGOT;
    }

    // ---------- misc ----------

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
