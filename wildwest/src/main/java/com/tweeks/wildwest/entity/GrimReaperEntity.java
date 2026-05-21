package com.tweeks.wildwest.entity;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Fifth apex boss for the :wildwest mod. Singleton, overworld-night spawn.
 * Glass-cannon (100 HP) minion master that raises Sharpness-3 skeletons
 * via {@link com.tweeks.wildwest.entity.ai.GrimReaperRaiseDeadGoal} and
 * lifts players for fall damage via
 * {@link com.tweeks.wildwest.entity.ai.GrimReaperSoulLiftGoal}.
 * Drops the Reaper Scythe.
 */
public class GrimReaperEntity extends Monster {

    private final ServerBossEvent bossBar;

    public GrimReaperEntity(EntityType<? extends GrimReaperEntity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            Mth.createInsecureUUID(this.random),
            Component.translatable("entity.wildwest.grim_reaper"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_10);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.wildwest.grim_reaper"));
        this.setCustomNameVisible(false);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
            new net.minecraft.world.item.ItemStack(
                com.tweeks.wildwest.Registration.REAPER_SCYTHE.get()));
        this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(1, new com.tweeks.wildwest.entity.ai.GrimReaperSoulLiftGoal(this));
        this.goalSelector.addGoal(2, new com.tweeks.wildwest.entity.ai.GrimReaperRaiseDeadGoal(this));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(
            this, net.minecraft.world.entity.player.Player.class, 16.0F));
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
            this, net.minecraft.world.entity.player.Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return true;
        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        DifficultyInstance difficulty,
                                        EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        MinecraftServer server = level.getLevel().getServer();
        if (server != null) {
            GrimReaperSavedData saved = GrimReaperSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                this.discard();
                return result;
            }
            saved.setAlive(this.getUUID(), level.getLevel().dimension());
        }
        return result;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (this.level() instanceof ServerLevel sl) {
            MinecraftServer server = sl.getServer();
            if (server != null) {
                GrimReaperSavedData saved = GrimReaperSavedData.get(server);
                if (this.getUUID().equals(saved.getCurrentId())) {
                    saved.clear();
                }
            }
            // Cleanup raised minions across the whole level — not just within
            // a radius of the reaper's corpse, since a kited fight can leave
            // tagged skeletons far from the final death position.
            sl.getEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(
                    net.minecraft.world.entity.monster.skeleton.Skeleton.class),
                s -> s.getPersistentData().getBooleanOr(
                    com.tweeks.wildwest.entity.ai.GrimReaperRaiseDeadGoal.MINION_NBT_KEY, false))
                .forEach(net.minecraft.world.entity.Entity::discard);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.bossBar.removeAllPlayers();
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            if (this.level() instanceof ServerLevel sl) {
                MinecraftServer server = sl.getServer();
                if (server != null) {
                    GrimReaperSavedData saved = GrimReaperSavedData.get(server);
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
        // WITHER_AMBIENT is a raw SoundEvent (not Holder-wrapped) in this NeoForge version.
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 80;
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
}
