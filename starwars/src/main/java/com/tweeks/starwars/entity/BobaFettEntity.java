package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.BobaJetpackGoal;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Named elite: server-wide singleton (see {@link BobaFettSavedData}). Wields
 * a blaster rifle, opens engagements with {@link BobaJetpackGoal}'s burst
 * leap, and has no natural spawn placement — only the Task 29 spawner /
 * structure anchor and spawn eggs / {@code /summon} bring him into a world.
 */
public class BobaFettEntity extends SwMob implements Enemy {

    public static final double MAX_HEALTH = 60.0;
    public static final double ATTACK_DAMAGE = 6.0;
    public static final double MOVEMENT_SPEED = 0.33;
    public static final double KNOCKBACK_RESISTANCE = 0.8;

    public BobaFettEntity(EntityType<? extends BobaFettEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new BobaJetpackGoal(this));
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return true; }

    @Override
    protected ItemStack getWeaponStack() {
        return new ItemStack(com.tweeks.starwars.Registration.BLASTER_RIFLE.get());
    }

    /**
     * Singleton lifecycle — mirrors {@code HerobrineEntity} exactly. finalizeSpawn
     * claims the singleton (and discards duplicates from spawn eggs or
     * /summon); die() and remove() both clear it, UUID-guarded so a
     * discarded duplicate can't wipe the live Boba Fett's record. die+remove
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
            BobaFettSavedData saved = BobaFettSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                // Another Boba Fett already alive — discard this duplicate.
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
                BobaFettSavedData saved = BobaFettSavedData.get(server);
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
                    BobaFettSavedData saved = BobaFettSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }
}
