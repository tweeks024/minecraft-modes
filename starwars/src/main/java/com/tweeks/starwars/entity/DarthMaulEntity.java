package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.MaulLeapGoal;
import com.tweeks.starwars.entity.ai.MaulSpinGoal;
import com.tweeks.starwars.entity.ai.SwTargetGoal;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Darth Maul: a named EMPIRE singleton Sith (see {@link MaulSavedData}) built
 * as the dark mirror of {@link LukeSkywalkerEntity} — a fast, acrobatic
 * saber-duelist rather than {@link DarthVaderEntity}'s heavy bruiser. He
 * wields a double-bladed saberstaff (visually added by the integrator; combat
 * damage is the {@code ATTACK_DAMAGE} attribute), closes with
 * {@link MaulLeapGoal}'s lunge, and punishes crowds with
 * {@link MaulSpinGoal}'s double-saber sweep.
 *
 * <p>No natural spawn placement — only the named-character spawner /
 * structure anchor and spawn eggs / {@code /summon} bring him into a world.
 */
public class DarthMaulEntity extends SwMob implements Enemy {

    public static final double MAX_HEALTH = 80.0;
    public static final double ATTACK_DAMAGE = 10.0;
    /** Faster than a Jedi Knight (0.32) or Vader (0.32) — the acrobat. */
    public static final double MOVEMENT_SPEED = 0.38;
    public static final double KNOCKBACK_RESISTANCE = 0.4;
    public static final double ATTACK_KNOCKBACK = 0.5;
    public static final double FOLLOW_RANGE = 40.0;

    /** Per-tick chance of a low ambient growl (~1 in 500 ≈ once per 25s). */
    private static final int AMBIENT_GROWL_CHANCE = 500;

    public DarthMaulEntity(EntityType<? extends DarthMaulEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
            .add(Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK)
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    /**
     * Full goal set built from scratch — deliberately NOT calling
     * {@code super.registerGoals()} (the {@link ProbeDroidEntity} precedent).
     * SwMob's default melee is speed 1.2 and it wires an inert blaster goal;
     * Maul needs a faster (1.3) flurry and no blaster, plus his two signature
     * powers.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MaulSpinGoal(this));
        this.goalSelector.addGoal(1, new MaulLeapGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new SwTargetGoal(this));
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return false; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        // Saberstaff item is registered + equipped by the integrator; combat
        // damage comes from the ATTACK_DAMAGE attribute, not the held item.
        return ItemStack.EMPTY;
    }

    /** Occasional low, pitched-down Warden growl — a subtle menace cue. */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.random.nextInt(AMBIENT_GROWL_CHANCE) == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_ANGRY, SoundSource.HOSTILE, 0.6F, 0.5F);
        }
    }

    /**
     * Singleton lifecycle — mirrors {@link DarthVaderEntity} exactly.
     * finalizeSpawn claims the singleton (and discards duplicates from spawn
     * eggs or /summon); die() and remove() both clear it, UUID-guarded so a
     * discarded duplicate can't wipe the live Maul's record. die+remove
     * redundancy is intentional: /kill-style discards skip die().
     */
    @Override
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        var result = super.finalizeSpawn(level, difficulty, reason, spawnData);

        // Claim singleton — anchor on overworld SavedData regardless of caller dimension.
        var server = level.getLevel().getServer();
        if (server != null) {
            MaulSavedData saved = MaulSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                // Another Maul already alive — discard this duplicate.
                this.discard();
                return result;
            }
            saved.setAlive(this.getUUID(), level.getLevel().dimension());
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel sl) {
            var server = sl.getServer();
            if (server != null) {
                MaulSavedData saved = MaulSavedData.get(server);
                if (this.getUUID().equals(saved.getCurrentId())) {
                    saved.clear();
                }
            }
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        // Only clear the singleton flag for "real" removals. Chunk unload is
        // not a death. (See die() javadoc — this path is intentionally
        // redundant with die() for non-standard kill paths.)
        if (reason == net.minecraft.world.entity.Entity.RemovalReason.KILLED
                || reason == net.minecraft.world.entity.Entity.RemovalReason.DISCARDED) {
            if (this.level() instanceof ServerLevel sl) {
                var server = sl.getServer();
                if (server != null) {
                    MaulSavedData saved = MaulSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }
}
