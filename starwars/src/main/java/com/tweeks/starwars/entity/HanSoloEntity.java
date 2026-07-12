package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.HanQuickdrawGoal;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Named hero: server-wide singleton (see {@link HanSavedData}). Fights with
 * the blaster pistol; opens each engagement with {@link HanQuickdrawGoal}'s
 * "Shoots First" ambush. No natural spawn placement — only the
 * NamedCharacterSpawner roster and spawn eggs / {@code /summon} bring him
 * into a world.
 */
public class HanSoloEntity extends SwMob {

    public static final double MAX_HEALTH = 80.0;
    public static final double ATTACK_DAMAGE = 8.0;
    public static final double MOVEMENT_SPEED = 0.32;

    public HanSoloEntity(EntityType<? extends HanSoloEntity> type, Level level) {
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
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        return new ItemStack(com.tweeks.starwars.Registration.BLASTER_PISTOL.get());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new HanQuickdrawGoal(this));
    }

    /**
     * Singleton lifecycle — mirrors {@code LukeSkywalkerEntity} exactly.
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
            HanSavedData saved = HanSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                // Another Han already alive — discard this duplicate.
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
                HanSavedData saved = HanSavedData.get(server);
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
                    HanSavedData saved = HanSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }
}
