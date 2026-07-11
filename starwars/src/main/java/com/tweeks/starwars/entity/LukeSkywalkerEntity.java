package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.LukeLeapGoal;
import com.tweeks.starwars.faction.SwFaction;
import com.tweeks.starwars.item.LightsaberItem;
import com.tweeks.starwars.item.SaberColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Named hero: server-wide singleton (see {@link LukeSavedData}). Wields a
 * green lightsaber, opens engagements by closing distance with
 * {@link LukeLeapGoal}'s Force leap, and has no natural spawn placement —
 * only the Task 18 spawner / structure anchor and spawn eggs / {@code /summon}
 * bring him into a world.
 */
public class LukeSkywalkerEntity extends SwMob {

    public static final double MAX_HEALTH = 100.0;
    public static final double ATTACK_DAMAGE = 10.0;
    public static final double MOVEMENT_SPEED = 0.34;

    public LukeSkywalkerEntity(EntityType<? extends LukeSkywalkerEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new LukeLeapGoal(this));
    }

    @Override
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    @Override
    public boolean usesBlaster() { return false; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        return LightsaberItem.stackWithColor(SaberColor.GREEN);
    }

    /**
     * Singleton lifecycle — mirrors {@code HerobrineEntity} exactly. finalizeSpawn
     * claims the singleton (and discards duplicates from spawn eggs or
     * /summon); die() and remove() both clear it, UUID-guarded so a
     * discarded duplicate can't wipe the live Luke's record. die+remove
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
            LukeSavedData saved = LukeSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                // Another Luke already alive — discard this duplicate.
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
                LukeSavedData saved = LukeSavedData.get(server);
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
                    LukeSavedData saved = LukeSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }
}
