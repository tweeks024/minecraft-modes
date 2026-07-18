package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.PalpatineLightningGoal;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Emperor Palpatine: the endgame boss, a server-wide singleton (see
 * {@link PalpatineSavedData}). Unlike the mod's other Sith, he carries no
 * saber and swings feebly in melee — his threat is {@link
 * PalpatineLightningGoal}'s chaining Force lightning, a dreaded burst on a
 * long cooldown. Frail but very hard to close on. Has no natural spawn
 * placement: the throne-room spawner, spawn eggs, and {@code /summon} bring
 * him into a world; the throne spawner seats him in his Coruscant palace.
 */
public class PalpatineEntity extends SwMob implements Enemy {

    public static final double MAX_HEALTH = 150.0;
    public static final double ATTACK_DAMAGE = 5.0;      // feeble melee — lightning is the threat
    public static final double MOVEMENT_SPEED = 0.28;
    public static final double KNOCKBACK_RESISTANCE = 0.85;

    public PalpatineEntity(EntityType<? extends PalpatineEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new PalpatineLightningGoal(this));
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return false; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        return ItemStack.EMPTY;                          // the Emperor needs no weapon
    }

    // ---------- sounds: an evoker's dark-caster cadence ----------

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.EVOKER_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.EVOKER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.EVOKER_DEATH; }

    @Override
    public float getVoicePitch() { return super.getVoicePitch() * 0.7F; }

    // ---------- singleton lifecycle — mirrors DarthVaderEntity exactly ----------

    @Override
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        var result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        var server = level.getLevel().getServer();
        if (server != null) {
            PalpatineSavedData saved = PalpatineSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                this.discard();                          // a Palpatine already reigns
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
                PalpatineSavedData saved = PalpatineSavedData.get(server);
                if (this.getUUID().equals(saved.getCurrentId())) {
                    saved.clear();
                }
            }
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (reason == net.minecraft.world.entity.Entity.RemovalReason.KILLED
                || reason == net.minecraft.world.entity.Entity.RemovalReason.DISCARDED) {
            if (this.level() instanceof ServerLevel sl) {
                var server = sl.getServer();
                if (server != null) {
                    PalpatineSavedData saved = PalpatineSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }
}
