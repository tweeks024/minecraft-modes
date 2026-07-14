package com.tweeks.starwars.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Cantina band droid: an ambient musician that never spawns naturally (it is
 * placed only inside the Mos Eisley cantina). Plain {@link PathfinderMob} —
 * it never fights.
 *
 * <p>Home-anchored to wherever it first spawns: it wanders at most
 * {@link #HOME_RADIUS} blocks away and is walked back when it strays
 * further (vanilla home mechanism — {@code setHomeTo} +
 * {@code MoveTowardsRestrictionGoal}). Every {@link #SCAN_INTERVAL} ticks it
 * looks for a playing jukebox ({@link JukeboxBlock#HAS_RECORD} true) within
 * {@link #JUKEBOX_RANGE}; while one plays it faces the box and puffs colored
 * note particles above its head. Persistent so the cantina keeps its band.
 */
public class BandDroidEntity extends PathfinderMob {

    public static final double MAX_HEALTH = 8.0;
    public static final double MOVEMENT_SPEED = 0.25;
    public static final int HOME_RADIUS = 4;
    public static final int JUKEBOX_RANGE = 8;
    public static final int SCAN_INTERVAL = 10;

    /** Cached playing-jukebox position (revalidated each scan), or null. */
    @Nullable
    private BlockPos jukebox;
    /** Deterministic note-hue cycle index (0..23). */
    private int noteHue;

    public BandDroidEntity(EntityType<? extends BandDroidEntity> type, Level level) {
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
        // Stay near home: walk back when it wanders past HOME_RADIUS.
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
        // Idle wander (respects the home restriction via GoalUtils).
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        this.anchorHome();
        this.setPersistenceRequired();
        return result;
    }

    /** Records the current position as home if one is not already set. */
    private void anchorHome() {
        if (!this.hasHome()) {
            this.setHomeTo(this.blockPosition(), HOME_RADIUS);
        }
    }

    /** Cantina staff never despawn — the band stays put. */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Safety net: anchor home on the first server tick for any spawn path
        // that skipped finalizeSpawn (loaded entities restore it from NBT).
        this.anchorHome();

        if (this.tickCount % SCAN_INTERVAL != 0) {
            return;
        }
        this.jukebox = findPlayingJukebox(level);
        if (this.jukebox != null) {
            // Face the music and puff a colored note above the head.
            this.getLookControl().setLookAt(
                this.jukebox.getX() + 0.5, this.jukebox.getY() + 0.5, this.jukebox.getZ() + 0.5);
            emitNote(level);
        }
    }

    /** Nearest playing jukebox within range, revalidating the cache first. */
    @Nullable
    private BlockPos findPlayingJukebox(ServerLevel level) {
        if (this.jukebox != null && isPlayingJukebox(level, this.jukebox)
            && this.jukebox.closerToCenterThan(this.position(), JUKEBOX_RANGE + 1)) {
            return this.jukebox;
        }
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos bp : BlockPos.betweenClosed(
                origin.offset(-JUKEBOX_RANGE, -4, -JUKEBOX_RANGE),
                origin.offset(JUKEBOX_RANGE, 4, JUKEBOX_RANGE))) {
            if (isPlayingJukebox(level, bp)) {
                double d = bp.distSqr(origin);
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = bp.immutable();
                }
            }
        }
        return best;
    }

    private static boolean isPlayingJukebox(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof JukeboxBlock
            && state.getValue(JukeboxBlock.HAS_RECORD);
    }

    private void emitNote(ServerLevel level) {
        // Deterministic hue cycle: count==0 sends a single NOTE particle whose
        // "color" (0..1) is carried in the x-velocity slot (maxSpeed * xDist).
        double hue = this.noteHue / 24.0;
        this.noteHue = (this.noteHue + 1) % 24;
        double x = this.getX() + (this.random.nextDouble() - 0.5) * 0.4;
        double y = this.getY() + this.getBbHeight() + 0.4;
        double z = this.getZ() + (this.random.nextDouble() - 0.5) * 0.4;
        level.sendParticles(ParticleTypes.NOTE, x, y, z, 0, hue, 0.0, 0.0, 1.0);
    }

    // ---------- sounds ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.NOTE_BLOCK_BANJO.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }
}
