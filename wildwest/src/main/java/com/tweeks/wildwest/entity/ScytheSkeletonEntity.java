package com.tweeks.wildwest.entity;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Player-summoned minion. Persistent, owner-tagged. Sun-immune (clears fire
 * every tick in {@link #aiStep()}). Iron sword + iron helmet on spawn.
 *
 * <p>Owner UUID is persisted via NBT but NOT synced to clients: all
 * owner-following and targeting logic runs server-side only.
 *
 * <p>AI goals are registered via {@link #registerGoals()} in a later task.
 */
public class ScytheSkeletonEntity extends Skeleton {

    @Nullable
    private UUID ownerUUID;

    private int idleTicks = 0;

    public ScytheSkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public Optional<UUID> getOwnerUUID() {
        return Optional.ofNullable(this.ownerUUID);
    }

    public void setOwnerUUID(@Nullable UUID owner) {
        this.ownerUUID = owner;
    }

    @Nullable
    public Player getOwnerPlayer() {
        if (this.ownerUUID == null) return null;
        if (!(this.level() instanceof ServerLevel sl)) {
            return this.level().getPlayerByUUID(this.ownerUUID);
        }
        var entity = sl.getEntity(this.ownerUUID);
        return entity instanceof Player p ? p : null;
    }

    public int getIdleTicks() {
        return this.idleTicks;
    }

    public void resetIdleTicks() {
        this.idleTicks = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSkeleton.createAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(3, new com.tweeks.wildwest.entity.ai.ScytheSkeletonMineOreGoal(this));
        this.goalSelector.addGoal(4, new com.tweeks.wildwest.entity.ai.ScytheSkeletonFollowOwnerGoal(
            this, 1.0D, 5.0F, 10.0F));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(
            this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new com.tweeks.wildwest.entity.ai.ScytheSkeletonTargetHostilesGoal(this));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // Override vanilla skeleton (which gives a bow). Custom loadout: iron sword + helmet.
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.setDropChance(EquipmentSlot.HEAD, 0.0f);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getRemainingFireTicks() > 0) {
            this.setRemainingFireTicks(0);
        }
        // Bump idle counter when no target, no active path, and not adjacent to owner.
        Player owner = this.getOwnerPlayer();
        if (this.getTarget() != null
            || this.getNavigation().isInProgress()
            || (owner != null && this.distanceToSqr(owner) < 5.0 * 5.0)) {
            this.idleTicks = 0;
        } else {
            this.idleTicks++;
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.ownerUUID != null) {
            output.store("OwnerUUID", UUIDUtil.CODEC, this.ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.ownerUUID = input.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
    }

    /**
     * Generic counter helper — delegates to {@link MinionCounter#countMatching}
     * so callers can reference it via {@code ScytheSkeletonEntity::countMatching}.
     * Testable without a {@link ServerLevel} because {@link MinionCounter} has
     * no Minecraft superclass.
     */
    public static <T> int countMatching(Iterable<T> candidates,
                                        UUID owner,
                                        java.util.function.Predicate<T> aliveProbe,
                                        java.util.function.Function<T, Optional<UUID>> ownerProbe) {
        return MinionCounter.countMatching(candidates, owner, aliveProbe, ownerProbe);
    }

    /**
     * Count alive scythe-skeleton minions in the given level whose owner UUID
     * matches the given player. Used by {@code ReaperScytheItem} for cap-check
     * before spawning a new minion.
     */
    public static int countMinionsOwnedBy(ServerLevel level, UUID owner) {
        var minions = new java.util.ArrayList<ScytheSkeletonEntity>();
        for (var entity : level.getAllEntities()) {
            if (entity instanceof ScytheSkeletonEntity minion) {
                minions.add(minion);
            }
        }
        return MinionCounter.countMatching(minions, owner,
            net.minecraft.world.entity.LivingEntity::isAlive,
            ScytheSkeletonEntity::getOwnerUUID);
    }
}
