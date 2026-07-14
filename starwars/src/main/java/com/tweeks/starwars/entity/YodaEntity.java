package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.YodaLeapGoal;
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
 * Named hero: server-wide singleton (see {@link YodaSavedData}) — the
 * grand master himself. Small, quick, and a LIGHT-side melee force
 * fighter: closes with {@link YodaLeapGoal}'s Force spring and hits far
 * harder than his size suggests. No natural spawn placement — only the
 * named-character spawner roster and spawn eggs / {@code /summon} bring
 * him into a world. Singleton lifecycle mirrors
 * {@link LukeSkywalkerEntity} exactly.
 */
public class YodaEntity extends SwMob {

    public static final double MAX_HEALTH = 60.0;
    public static final double ATTACK_DAMAGE = 7.0;
    public static final double MOVEMENT_SPEED = 0.35;

    public YodaEntity(EntityType<? extends YodaEntity> type, Level level) {
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
        this.goalSelector.addGoal(1, new YodaLeapGoal(this));
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
     * Singleton lifecycle — mirrors {@code LukeSkywalkerEntity} exactly.
     * finalizeSpawn claims the singleton (and discards duplicates from
     * spawn eggs or /summon); die() and remove() both clear it,
     * UUID-guarded so a discarded duplicate can't wipe the live Yoda's
     * record. die+remove redundancy is intentional: /kill-style discards
     * skip die().
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
            YodaSavedData saved = YodaSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                // Another Yoda already alive — discard this duplicate.
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
                YodaSavedData saved = YodaSavedData.get(server);
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
                    YodaSavedData saved = YodaSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }
}
