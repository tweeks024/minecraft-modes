package com.tweeks.wildwest.entity;

import javax.annotation.Nullable;
import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.ai.Entity303BowGoal;
import com.tweeks.wildwest.entity.ai.Entity303MeleeGoal;
import com.tweeks.wildwest.entity.ai.Entity303TeleportGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Apex boss mob, peer of Herobrine. Singleton across the whole server (see
 * {@link Entity303SavedData}). Spawns at night in the overworld or any time
 * in The End; fires an enchanted bow at range, swaps to an iron sword in
 * melee, teleports every ~3 s. On damage occasionally spawns a 1-HP visual
 * decoy ({@code Entity303CloneEntity}) and warps behind the attacker.
 */
public class Entity303Entity extends Monster {

    private final ServerBossEvent bossBar;

    private static final int SWAP_COOLDOWN_TICKS = 80; // 4 s
    private static final float SWAP_PROBABILITY = 0.30f;
    private static final double SWAP_BEHIND_MIN = 8.0;
    private static final double SWAP_BEHIND_MAX = 12.0;
    private static final int SWAP_CLEARANCE_RETRIES = 5;

    private int swapCooldown = 0;

    // Goal cooldowns live on the entity (not inside individual Goals) so they
    // decrement every tick in aiStep regardless of which Goal is currently
    // running. Putting them inside each Goal's canUse() makes the cooldown
    // stall when a higher-priority goal (e.g., melee) is active — leading to
    // a "bow ready the instant you leave melee range" exploit.
    private int bowCooldown = 0;
    private int teleportCooldown = 0;

    public Entity303Entity(EntityType<? extends Entity303Entity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            Mth.createInsecureUUID(this.random),
            Component.translatable("entity.wildwest.entity_303"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.wildwest.entity_303"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 160.0)
            .add(Attributes.MOVEMENT_SPEED, 0.45)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new Entity303MeleeGoal(this));
        this.goalSelector.addGoal(2, new Entity303BowGoal(this));
        this.goalSelector.addGoal(3, new Entity303TeleportGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);

        var registries = this.level().registryAccess();
        var enchRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> power = enchRegistry.getOrThrow(Enchantments.POWER);
        Holder<Enchantment> flame = enchRegistry.getOrThrow(Enchantments.FLAME);
        Holder<Enchantment> sharpness = enchRegistry.getOrThrow(Enchantments.SHARPNESS);

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(power, 5);
        bow.enchant(flame, 1);

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(sharpness, 4);

        // Default stance: bow in mainhand, sword in offhand. Goals swap as
        // combat distance changes (see Entity303MeleeGoal / Entity303BowGoal).
        this.setItemSlot(EquipmentSlot.MAINHAND, bow);
        this.setItemSlot(EquipmentSlot.OFFHAND, sword);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.10f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.10f);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        DifficultyInstance difficulty,
                                        EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);

        var server = level.getLevel().getServer();
        if (server != null) {
            Entity303SavedData saved = Entity303SavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                // Another 303 already alive — discard this duplicate.
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
            var server = sl.getServer();
            if (server != null) {
                Entity303SavedData saved = Entity303SavedData.get(server);
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
        // not a death.
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            if (this.level() instanceof ServerLevel sl) {
                var server = sl.getServer();
                if (server != null) {
                    Entity303SavedData saved = Entity303SavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Phantom swap: roll once per incoming non-zero damage when off cooldown.
        if (amount > 0.0f && this.swapCooldown == 0
                && this.getRandom().nextFloat() < SWAP_PROBABILITY) {

            Entity attacker = source.getEntity();
            if (tryPhantomSwap(level, attacker)) {
                this.swapCooldown = SWAP_COOLDOWN_TICKS;
                // Damage replaced by the swap — we return true to indicate the
                // hit was handled, but skip the HP deduction by NOT delegating
                // to super.hurtServer. Manually register the attacker as the
                // last-hurt-by source so HurtByTargetGoal still aggros — otherwise
                // a swapped arrow hit would never trigger 303 to target the
                // shooter, which would feel like a bug to the player.
                if (attacker instanceof LivingEntity livingAttacker) {
                    this.setLastHurtByMob(livingAttacker);
                }
                return true;
            }
            // tryPhantomSwap returned false (no valid spot) — fall through to vanilla.
        }
        return super.hurtServer(level, source, amount);
    }

    /**
     * @return true if a clone was spawned and 303 teleported; false if no
     *         valid destination was found and the caller should apply damage
     *         normally.
     */
    private boolean tryPhantomSwap(ServerLevel level, @Nullable Entity attacker) {
        // Pick the swap direction: behind the attacker if available, else random.
        double dirX;
        double dirZ;
        if (attacker != null) {
            Vec3 look = attacker.getLookAngle();
            // "Behind" the attacker is along -look (the attacker is facing 303;
            // we want 303 to land on the side away from the attacker's facing).
            double mag = Math.sqrt(look.x * look.x + look.z * look.z);
            if (mag < 1.0e-6) {
                double angle = this.getRandom().nextDouble() * 2.0 * Math.PI;
                dirX = Math.cos(angle);
                dirZ = Math.sin(angle);
            } else {
                dirX = -look.x / mag;
                dirZ = -look.z / mag;
            }
        } else {
            double angle = this.getRandom().nextDouble() * 2.0 * Math.PI;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
        }

        double anchorX = attacker != null ? attacker.getX() : this.getX();
        double anchorZ = attacker != null ? attacker.getZ() : this.getZ();

        // Validate clearance with up to N retries (perturbing direction on miss).
        double destX = 0, destZ = 0, destY = -1;
        for (int retry = 0; retry < SWAP_CLEARANCE_RETRIES; retry++) {
            double dist = SWAP_BEHIND_MIN
                + this.getRandom().nextDouble() * (SWAP_BEHIND_MAX - SWAP_BEHIND_MIN);
            destX = anchorX + dirX * dist;
            destZ = anchorZ + dirZ * dist;

            BlockPos topPos = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(destX, this.getY(), destZ));

            if (level.getBlockState(topPos).isAir()
                && level.getBlockState(topPos.above()).isAir()) {
                destY = topPos.getY();
                break;
            }
            BlockPos above = topPos.above();
            if (level.getBlockState(above).isAir()
                && level.getBlockState(above.above()).isAir()) {
                destY = above.getY();
                break;
            }
            // Clearance miss — resample direction uniformly. This loses the
            // "behind the attacker" intent on retry, but a graceful random
            // fallback beats getting stuck mid-swap.
            double angle = this.getRandom().nextDouble() * 2.0 * Math.PI;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
        }
        if (destY < 0) return false;

        // Spawn the clone at the current position before we teleport.
        Entity303CloneEntity clone = ModEntities.ENTITY_303_CLONE.get().create(
            level, EntitySpawnReason.MOB_SUMMONED);
        if (clone != null) {
            clone.setPos(this.getX(), this.getY(), this.getZ());
            clone.setYRot(this.getYRot());
            clone.setXRot(this.getXRot());
            level.addFreshEntity(clone);
        }

        // Particles + sound at source.
        level.sendParticles(ParticleTypes.SMOKE,
            this.getX(), this.getY() + 1.0, this.getZ(), 16, 0.5, 1.0, 0.5, 0.0);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 1.2f);

        // Move.
        this.teleportTo(destX, destY, destZ);
        this.fallDistance = 0;

        // Particles + sound at destination.
        level.sendParticles(ParticleTypes.SMOKE,
            destX, destY + 1.0, destZ, 16, 0.5, 1.0, 0.5, 0.0);
        level.playSound(null, destX, destY, destZ,
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 1.2f);

        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT_LAND;
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
        return 0.4f;
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 80;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        if (this.swapCooldown > 0) this.swapCooldown--;
        if (this.bowCooldown > 0) this.bowCooldown--;
        if (this.teleportCooldown > 0) this.teleportCooldown--;
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
    }

    public int getBowCooldown() { return this.bowCooldown; }
    public void setBowCooldown(int ticks) { this.bowCooldown = ticks; }

    public int getTeleportCooldown() { return this.teleportCooldown; }
    public void setTeleportCooldown(int ticks) { this.teleportCooldown = ticks; }

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
