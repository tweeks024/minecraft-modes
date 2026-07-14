package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.GroguForceNap;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Grogu ("Baby Yoda"): a very rare, precious, LIGHT-side non-combatant. He
 * never attacks — he wanders slowly, panics from and avoids hostiles, and
 * looks at players.
 *
 * <p><b>Carry:</b> an empty-hand interact picks him up. He is <em>not</em> a
 * true passenger — {@link EntityType#PLAYER} is {@code noSave()}, so
 * {@code startRiding(player)} is rejected server-side; instead a synched
 * {@link #DATA_CARRIER_ID} reference glues him to the carrier's shoulders
 * every tick (client and server, so it never lags). While carried he is
 * invulnerable ({@link #hurtServer} ignores damage) and emits occasional
 * happy particles. A carrying player who sneaks (or interacts with him again)
 * sets him down in front of them; if the carrier vanishes he drops in place.
 * The carry link is transient — a reloaded Grogu is simply on the ground.
 *
 * <p><b>Force nap:</b> rarely, when a hostile is within
 * {@link #FORCE_NAP_RADIUS} and a player is nearby (or carrying him), he uses
 * the Force ({@link GroguForceNap} state machine) to lull nearby
 * Empire/hostile mobs — SLOWNESS + WEAKNESS for a few seconds with a soft
 * chime and an END_ROD puff — then a long cooldown.
 */
public class GroguEntity extends PathfinderMob implements SwCombatant {

    public static final double MAX_HEALTH = 20.0;
    public static final double MOVEMENT_SPEED = 0.20;
    /** Hostiles within this radius trigger (and receive) the Force nap. */
    public static final double FORCE_NAP_RADIUS = 6.0;
    /** How near a player must be (when not carried) for a nap to matter. */
    public static final double NAP_PLAYER_RANGE = 16.0;
    /** SLOWNESS + WEAKNESS duration applied by a nap (~5s). */
    public static final int NAP_EFFECT_TICKS = 100;
    /** Fraction of the carrier's height Grogu's feet ride at (shoulders). */
    private static final double CARRY_HEIGHT_FRACTION = 0.72;

    /** Network id of the carrying player, or -1 when not carried. */
    private static final EntityDataAccessor<Integer> DATA_CARRIER_ID =
        SynchedEntityData.defineId(GroguEntity.class, EntityDataSerializers.INT);

    /** Force-nap power timer (pure state machine; unit-tested). */
    private final GroguForceNap forceNap = new GroguForceNap();

    public GroguEntity(EntityType<? extends GroguEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CARRIER_ID, -1);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        // Skitter away from vanilla monsters and any Empire combatant.
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LivingEntity.class, 8.0F, 1.4, 1.6,
            living -> living instanceof Enemy
                || (living instanceof SwCombatant sc && sc.getFaction() == SwFaction.EMPIRE)));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Deliberately no target goals — Grogu never attacks.
    }

    // ---------- carry ----------

    @Nullable
    private Player getCarrier() {
        int id = this.entityData.get(DATA_CARRIER_ID);
        if (id < 0) return null;
        Entity e = this.level().getEntity(id);
        return e instanceof Player player ? player : null;
    }

    private void setCarrier(@Nullable Player player) {
        this.entityData.set(DATA_CARRIER_ID, player == null ? -1 : player.getId());
    }

    public boolean isCarried() {
        return this.entityData.get(DATA_CARRIER_ID) >= 0;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Already carried by this player: interacting again puts him down.
        if (this.getCarrier() == player) {
            if (!this.level().isClientSide()) this.setDownInFrontOf(player);
            return InteractionResult.SUCCESS;
        }
        // Empty-hand, non-sneaking interact picks him up.
        if (!this.isCarried() && player.getItemInHand(hand).isEmpty() && !player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                this.setCarrier(player);
                this.getNavigation().stop();
                this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.8F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Glue him to the carrier's shoulders/head. Runs on both sides. */
    private void rideOnCarrier(Player carrier) {
        double y = carrier.getY() + carrier.getBbHeight() * CARRY_HEIGHT_FRACTION;
        this.setPos(carrier.getX(), y, carrier.getZ());
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(carrier.getYRot());
        this.setYHeadRot(carrier.getYRot());
        this.setYBodyRot(carrier.getYRot());
        this.resetFallDistance();
    }

    private void setDownInFrontOf(Player player) {
        this.setCarrier(null);
        double yaw = Math.toRadians(player.getYRot());
        // Horizontal facing from yaw (no NaN when looking straight up/down).
        this.snapTo(player.getX() - Math.sin(yaw), player.getY(), player.getZ() + Math.cos(yaw),
            player.getYRot(), 0.0F);
    }

    /** Safe in the player's arms: ignore all damage while carried. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isCarried()) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        Player carrier = this.getCarrier();
        if (carrier != null) {
            this.rideOnCarrier(carrier);   // both sides, so there is no lag
        }

        if (this.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) this.level();

        if (carrier != null) {
            if (!carrier.isAlive() || carrier.isRemoved() || carrier.isSpectator()) {
                this.setCarrier(null);         // carrier gone: drop in place
                carrier = null;
            } else if (carrier.isShiftKeyDown()) {
                this.setDownInFrontOf(carrier);
                carrier = null;
            } else if (this.tickCount % 40 == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + 0.3, this.getZ(), 3, 0.15, 0.15, 0.15, 0.0);
            }
        }

        // Force nap: only worth scanning while ready and near a player.
        boolean nearPlayer = carrier != null
            || this.level().getNearestPlayer(this, NAP_PLAYER_RANGE) != null;
        boolean hostileNear = nearPlayer && this.forceNap.isReady() && !this.nearbyHostiles(level).isEmpty();
        if (this.forceNap.tick(hostileNear) == GroguForceNap.Result.NAP) {
            this.performForceNap(level);
        }
    }

    private List<LivingEntity> nearbyHostiles(ServerLevel level) {
        return level.getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(FORCE_NAP_RADIUS),
            e -> e.isAlive() && e != this
                && (e instanceof Enemy
                    || (e instanceof SwCombatant sc && sc.getFaction() == SwFaction.EMPIRE)));
    }

    private void performForceNap(ServerLevel level) {
        for (LivingEntity hostile : this.nearbyHostiles(level)) {
            hostile.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, NAP_EFFECT_TICKS, 1));
            hostile.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, NAP_EFFECT_TICKS, 0));
        }
        level.sendParticles(ParticleTypes.END_ROD,
            this.getX(), this.getY() + 0.4, this.getZ(), 20, 0.5, 0.5, 0.5, 0.02);
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.4F);
    }

    // Grogu is unique-ish and precious — he never despawns.
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }
}
