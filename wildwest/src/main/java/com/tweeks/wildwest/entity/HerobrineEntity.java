package com.tweeks.wildwest.entity;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * Apex boss mob. Singleton across the whole server (see {@link HerobrineSavedData}).
 * Spawns rarely at night under open sky in overworld biomes; teleports often,
 * mixes melee netherite-sword swings, vanilla {@code LightningBolt} casts, and
 * a {@link com.tweeks.wildwest.entity.projectile.MeteorEntity} barrage that
 * creates magma + fire hazard zones around him.
 */
public class HerobrineEntity extends Monster {

    private final ServerBossEvent bossBar;

    public HerobrineEntity(EntityType<? extends HerobrineEntity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            Mth.createInsecureUUID(this.random),
            Component.translatable("entity.wildwest.herobrine"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.wildwest.herobrine"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 200.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return true;
        return super.isInvulnerableTo(level, source);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);

        // Apply Sharpness V + Fire Aspect II using the level's registry access.
        var registries = this.level().registryAccess();
        var enchRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchRegistry.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> fireAspect = enchRegistry.getOrThrow(Enchantments.FIRE_ASPECT);
        sword.enchant(sharpness, 5);
        sword.enchant(fireAspect, 2);

        this.setItemSlot(EquipmentSlot.MAINHAND, sword);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.10f);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        DifficultyInstance difficulty,
                                        EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);

        // Claim singleton — anchor on overworld SavedData regardless of caller dimension.
        var server = level.getLevel().getServer();
        if (server != null) {
            HerobrineSavedData saved = HerobrineSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                // Another Herobrine already alive — discard this duplicate.
                this.discard();
                return result;
            }
            saved.setAlive(this.getUUID(), level.getLevel().dimension());
        }
        return result;
    }

    /**
     * Standard death path. Clears the singleton flag.
     *
     * <p><b>Intentional redundancy with {@link #remove(Entity.RemovalReason)}:</b>
     * a normal kill triggers BOTH {@code die} and {@code remove(KILLED)}, so
     * this clear() runs twice. That is fine — {@code clear()} is idempotent
     * and the strict {@code uuid.equals(currentId)} guard means a no-op on
     * the second call. We keep both paths so weird mod interactions (e.g.,
     * an entity removed via {@code discard()} without a death tick) still
     * release the singleton.
     */
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (this.level() instanceof ServerLevel sl) {
            var server = sl.getServer();
            if (server != null) {
                HerobrineSavedData saved = HerobrineSavedData.get(server);
                if (this.getUUID().equals(saved.getCurrentId())) {
                    saved.clear();
                }
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // Always clear the boss bar — a stuck-flag scenario or forced discard
        // must not leave the bar visible to clients server-side.
        this.bossBar.removeAllPlayers();

        // Only clear the singleton flag for "real" removals. Chunk unload is
        // not a death. (See die() javadoc — this path is intentionally
        // redundant with die() for non-standard kill paths.)
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            if (this.level() instanceof ServerLevel sl) {
                var server = sl.getServer();
                if (server != null) {
                    HerobrineSavedData saved = HerobrineSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // AMBIENT_CAVE is Holder<SoundEvent> in this version; unwrap to SoundEvent.
        return SoundEvents.AMBIENT_CAVE.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        // Slightly muted ambient breath; matches the "creepy distant" feel.
        return 0.6f;
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 100;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    public ServerBossEvent getBossBar() {
        return this.bossBar;
    }
}
