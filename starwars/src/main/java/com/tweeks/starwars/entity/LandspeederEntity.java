package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.vehicle.HoverPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
 * The X-34 landspeeder: a two-seat hover vehicle. Not a Mob — no AI, no
 * attributes; extends {@link VehicleEntity} for the boat-family hurt/wobble
 * plumbing and uses boat-style client-simulated driving (the controlling
 * player's client runs the physics; vanilla vehicle-move packets sync it).
 * All tuning math lives in {@link HoverPhysics} (unit-tested).
 *
 * <p>Hull health (40) is synched and PERSISTED — a chunk reload must not
 * heal the speeder (spec §5.4). No regeneration.
 *
 * <p>Driving input: on the driving player's own client,
 * {@code isLocalInstanceAuthoritative()} is true and that client's
 * {@code Player} instance is the live {@code LocalPlayer}, whose
 * {@code xxa}/{@code zza} fields are refreshed from WASD every tick
 * regardless of ride state (confirmed in decompiled
 * {@code LocalPlayer#applyInput} — unlike {@code AbstractBoat}, which wires
 * a dedicated {@code inputLeft/Right/Up/Down} + packet, this entity reads
 * the passenger's own movement-impulse fields directly since the vanilla
 * vehicle-move packet already syncs the result to the server).
 */
public class LandspeederEntity extends VehicleEntity {

    public static final float MAX_HULL_HEALTH = 40.0f;

    private static final EntityDataAccessor<Float> DATA_HULL_HEALTH =
        SynchedEntityData.defineId(LandspeederEntity.class, EntityDataSerializers.FLOAT);

    /** Signed forward speed (blocks/tick), driver's local frame. Client-side driving state. */
    private double forwardSpeed = 0.0;

    // VERIFY resolved: mirrors decompiled AbstractBoat's own field exactly
    // (AbstractBoat.java:64) — without it, remote clients snap to synced
    // positions instead of lerping between them.
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    public LandspeederEntity(EntityType<? extends LandspeederEntity> type, Level level) {
        super(type, level);
    }

    // ---------- synched data + persistence ----------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // VERIFY resolved: AbstractBoat.defineSynchedData calls
        // super.defineSynchedData(entityData) before adding its own fields —
        // VehicleEntity registers DATA_ID_HURT/HURTDIR/DAMAGE there, so the
        // super call is required.
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
        // VERIFY resolved: ValueInput.getFloatOr(String, float) exists
        // (decompiled ValueInput.java) alongside getIntOr used by
        // NullRiftEntity.
        this.setHullHealth(input.getFloatOr("HullHealth", MAX_HULL_HEALTH));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("HullHealth", this.getHullHealth());
    }

    // ---------- riding ----------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        // VERIFY resolved: decompiled Entity#interact / AbstractBoat#interact
        // both take a trailing Vec3 hit-location parameter in this version
        // (the brief's 2-arg signature does not override anything and would
        // silently fail to be called).
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        // Deliberately does not call super.interact() (VehicleEntity/Entity's
        // default handles lead-attaching) — the speeder is not leadable.
        if (!this.level().isClientSide()) {
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
        // VERIFY resolved: AbstractBoat#getControllingPassenger returns
        // LivingEntity (not Player) and accepts any LivingEntity as driver;
        // the spec for this task restricts driving to a Player first
        // passenger, so we narrow accordingly rather than copying the boat's
        // broader rule.
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        // VERIFY resolved: Entity.MoveFunction is a nested @FunctionalInterface
        // (`void accept(Entity target, double x, double y, double z)`) —
        // matches the brief's usage exactly.
        if (!this.hasPassenger(passenger)) return;
        int index = this.getPassengers().indexOf(passenger);
        // Side-by-side X-34 cockpit: driver left, passenger right. Offsets
        // derive from the bbmodel geometry — the modeled seat cubes are
        // centered at ±0.25 from the hull's local origin.
        double lateral = index == 0 ? -0.25 : 0.25;
        double yawRad = Math.toRadians(this.getYRot());
        // VERIFY resolved: AbstractBoat.getPassengerAttachmentPoint uses
        // Vec3(0, height, offset).yRot(-yaw) — i.e. a lateral (Z-axis, "side")
        // offset rotated by -yaw, which is algebraically the same rotation
        // this local (cos, sin) pair already applies (x' = cos*lateral,
        // z' = sin*lateral is the +90-degree-rotated companion of the boat's
        // yRot(-yaw) applied to a pure-Z vector). Kept as-is; sign confirmed
        // sane by inspection, final left/right feel to be confirmed in
        // manual dev-client smoke (deferred, see report).
        double ox = Math.cos(yawRad) * lateral;
        double oz = Math.sin(yawRad) * lateral;
        move.accept(passenger, this.getX() + ox, this.getY() + 0.35, this.getZ() + oz);
        // Speeder riders keep free look (no clampRotation like AbstractBoat).
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // VERIFY resolved: mirrors decompiled AbstractBoat#getDismountLocationForPassenger
        // (AbstractBoat.java:641-669), adapted minimally — no paddle logic to
        // drop, since this entity has none. Searches for a safe dry-land spot
        // beside the hull before falling back to Entity's bare default
        // (directly above the hull), which can strand a dismounting rider
        // over open water or a void.
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
        // VERIFY resolved: decompiled AbstractBoat#tick decays the
        // VehicleEntity hurt/damage wobble fields by 1 per tick before the
        // super.tick() call (AbstractBoat.java:223-228) — without this,
        // hurtServer()'s setHurtTime(10)/setDamage(+=amount*10) would leave
        // a permanent shake and unbounded damage once a renderer reads
        // them. Mirrored here since LandspeederEntity has no boat-specific
        // super to inherit the decay from (VehicleEntity itself doesn't
        // decay these fields — only AbstractBoat's own tick() does).
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
            // VERIFY resolved: mirrors decompiled AbstractBoat's own else
            // branch exactly (AbstractBoat.java:245-247) — without it, a
            // non-authoritative instance keeps whatever delta-movement
            // fluid/collision pushes accumulated onto it (e.g. from being
            // shoved by another entity or a flowing-water tick) instead of
            // zeroing out, which can launch the speeder on rider dismount.
            this.setDeltaMovement(Vec3.ZERO);
        }
        // Deliberately omits AbstractBoat's applyEffectsFromBlocks() call —
        // this is a hover vehicle and intentionally ignores ground-contact
        // hazards (e.g. magma, powder snow) that a hull skimming above the
        // ground never touches.
        // Speeders never cause fall damage — per-tick assignment, one-shot
        // fails (see LukeLeapGoal's comment on fallDistance).
        this.fallDistance = 0;
        for (Entity p : this.getPassengers()) {
            p.fallDistance = 0;
        }
    }

    private void tickDriven() {
        Vec3 vel = this.getDeltaMovement();
        double vy = vel.y + HoverPhysics.verticalAccel(this.sampleGroundDistance(), vel.y);

        int fwd = 0;
        int turn = 0;
        if (this.getControllingPassenger() instanceof Player driver) {
            // VERIFY resolved: decompiled LocalPlayer#applyInput sets
            // this.xxa/this.zza from this.input.getMoveVector() every tick
            // whenever isControlledCamera() is true, with no gating on
            // isPassenger(); LivingEntity.xxa/zza are public fields readable
            // here. tickDriven() only runs when isLocalInstanceAuthoritative()
            // is true, which (per Entity#isLocalInstanceAuthoritative /
            // #isLocalClientAuthoritative) is exactly the driving player's own
            // client — so driver IS that client's LocalPlayer and these
            // fields reflect live WASD input. fwd>0 = forward key (zza>0
            // matches Input.forward() -> positive z in getMoveVector());
            // xxa>0/<0 mirrors AbstractBoat's inputLeft/inputRight repurposing
            // of the strafe keys for turning.
            fwd = driver.zza > 0 ? 1 : (driver.zza < 0 ? -1 : 0);
            turn = driver.xxa > 0 ? 1 : (driver.xxa < 0 ? -1 : 0);
            if (turn != 0) {
                this.setYRot(HoverPhysics.nextYaw(this.getYRot(), -turn));
            }
        }
        this.forwardSpeed = HoverPhysics.nextForwardSpeed(this.forwardSpeed, fwd);

        double yawRad = Math.toRadians(this.getYRot());
        Vec3 facing = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        Vec3 desired = facing.scale(this.forwardSpeed);
        // Drift: blend horizontal velocity toward the facing direction.
        double bx = vel.x + (desired.x - vel.x) * HoverPhysics.VELOCITY_BLEND;
        double bz = vel.z + (desired.z - vel.z) * HoverPhysics.VELOCITY_BLEND;
        this.setDeltaMovement(bx, vy, bz);
    }

    /**
     * Distance from hull origin straight down to ground, fluid surfaces
     * included (the speeder skims water — spec §5.3). NaN = nothing within
     * scan depth.
     */
    private double sampleGroundDistance() {
        Vec3 from = this.position();
        Vec3 to = from.add(0.0, -HoverPhysics.HOVER_SCAN_DEPTH, 0.0);
        BlockHitResult hit = this.level().clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() == HitResult.Type.MISS) return Double.NaN;
        return from.y - hit.getLocation().y;
    }

    // ---------- damage / destruction ----------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved()) return false;
        // VERIFY resolved: Entity (not LivingEntity) has no
        // isInvulnerableTo(ServerLevel, DamageSource) method — that overload
        // lives on LivingEntity/Player. VehicleEntity itself (this class's
        // superclass) uses the protected-final Entity#isInvulnerableToBase
        // (DamageSource) for this check, so we do the same.
        if (this.isInvulnerableToBase(source)) return false;
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild) {
            this.destroySpeeder(level, false);
            return true;
        }
        this.setHullHealth(this.getHullHealth() - amount);
        // VERIFY resolved: VehicleEntity declares public setHurtDir/setHurtTime
        // /setDamage + getDamage backed by its own DATA_ID_HURT/HURTDIR/DAMAGE
        // synced fields (registered via the super.defineSynchedData() call
        // above), purely for renderer shake — independent of our hull-health
        // field. Drive them the same way VehicleEntity's own default
        // hurtServer does (including markHurt(), which that default calls
        // and this override — being a full replacement, not a super call —
        // otherwise would have silently dropped) so the renderer's hurt
        // wobble behaves identically to a boat's.
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.markHurt();
        this.setDamage(this.getDamage() + amount * 10.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 1.0f, 1.2f);
        if (this.getHullHealth() <= 0.0f) {
            this.destroySpeeder(level, true);
        }
        return true;
    }

    private void destroySpeeder(ServerLevel level, boolean dropItem) {
        this.ejectPassengers();
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.8, 0.4, 0.8, 0.02);
        level.sendParticles(ParticleTypes.CRIT,
            this.getX(), this.getY() + 0.5, this.getZ(), 12, 0.8, 0.4, 0.8, 0.1);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_GOLEM_DEATH, SoundSource.NEUTRAL, 1.0f, 1.3f);
        if (dropItem && level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            // VERIFY resolved: mirrors VehicleEntity.destroy(ServerLevel, Item)
            // (VehicleEntity.java:68-75) — gate the drop on the ENTITY_DROPS
            // gamerule (so /gamerule doEntityDrops false suppresses vehicle
            // drops like every other vehicle) and preserve a custom name onto
            // the dropped stack via the CUSTOM_NAME data component.
            ItemStack itemStack = new ItemStack(this.getDropItem());
            itemStack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
            this.spawnAtLocation(level, itemStack);
        }
        this.discard();
    }

    @Override
    protected Item getDropItem() {
        // VERIFY resolved: VehicleEntity declares `protected abstract Item
        // getDropItem()` — matches the brief exactly.
        return com.tweeks.starwars.Registration.LANDSPEEDER.get();
    }

    @Override
    public ItemStack getPickResult() {
        // VERIFY resolved: Entity#getPickResult() returns @Nullable ItemStack
        // (default null) and is not abstract/final — safe to override
        // returning a concrete non-null ItemStack.
        return new ItemStack(com.tweeks.starwars.Registration.LANDSPEEDER.get());
    }

    // ---------- misc ----------

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    // VERIFY resolved: mirrors decompiled AbstractBoat's own override exactly
    // (AbstractBoat.java:199-202, placed alongside isPickable()) — without
    // it, remote clients snap to synced positions instead of lerping between
    // them.
    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        // VERIFY resolved: Entity#canBeCollidedWith(@Nullable Entity other)
        // takes an Entity parameter in this version (default returns false);
        // AbstractBoat overrides it the same way, always returning true.
        // Nullable annotation matches decompiled Entity.java's own import
        // (org.jspecify.annotations.Nullable).
        return true;
    }
}
