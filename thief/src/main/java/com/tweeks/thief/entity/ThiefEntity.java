package com.tweeks.thief.entity;

import com.tweeks.securitycore.api.SecurityHostile;
import com.tweeks.thief.world.HideoutPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The Thief entity. Disguises as a villager, steals from chests, hides loot.
 * Implements {@link SecurityHostile} but reports
 * {@link SecurityHostile#isCurrentlyHostile()} based on its current
 * {@link RevealState}, so Guards ignore disguised Thieves.
 */
public class ThiefEntity extends PathfinderMob implements SecurityHostile, ContainerUser {

    private static final EntityDataAccessor<Byte> DATA_REVEAL_STATE =
        SynchedEntityData.defineId(ThiefEntity.class, EntityDataSerializers.BYTE);

    @Nullable
    private BlockPos hideoutPos;

    private final net.minecraft.world.SimpleContainer stolenItems = new net.minecraft.world.SimpleContainer(8);

    /** Tracks which chest pos this Thief currently has open (set by StealFromChestGoal). */
    @Nullable
    private BlockPos openContainerPos;

    public net.minecraft.world.SimpleContainer getStolenItems() {
        return stolenItems;
    }

    public void setOpenContainerPos(@Nullable BlockPos pos) {
        this.openContainerPos = pos;
    }

    // --- ContainerUser ---

    @Override
    public boolean hasContainerOpen(ContainerOpenersCounter container, BlockPos blockPos) {
        return openContainerPos != null && openContainerPos.equals(blockPos);
    }

    @Override
    public double getContainerInteractionRange() {
        return 4.0;
    }

    public ThiefEntity(EntityType<? extends ThiefEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REVEAL_STATE, RevealState.DISGUISED.toByte());
    }

    public RevealState getRevealState() {
        return RevealState.fromByte(this.entityData.get(DATA_REVEAL_STATE));
    }

    public void setRevealState(RevealState state) {
        this.entityData.set(DATA_REVEAL_STATE, state.toByte());
    }

    public Optional<BlockPos> getHideoutPos() {
        return Optional.ofNullable(hideoutPos);
    }

    public void setHideoutPos(@Nullable BlockPos pos) {
        this.hideoutPos = pos;
    }

    @Override
    public boolean isCurrentlyHostile() {
        return getRevealState().isHostile();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (hideoutPos != null) {
            output.store("hideout_pos", BlockPos.CODEC, hideoutPos);
        }
        net.minecraft.world.level.storage.ValueOutput.TypedOutputList<net.minecraft.world.item.ItemStack> list =
            output.list("stolen_items", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC);
        for (int i = 0; i < stolenItems.getContainerSize(); i++) {
            list.add(stolenItems.getItem(i));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.hideoutPos = input.read("hideout_pos", BlockPos.CODEC).orElse(null);
        stolenItems.clearContent();
        int[] idx = {0};
        input.listOrEmpty("stolen_items", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC)
            .stream()
            .limit(stolenItems.getContainerSize())
            .forEach(stack -> stolenItems.setItem(idx[0]++, stack));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        DifficultyInstance difficulty,
                                        EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        if (level instanceof ServerLevel serverLevel) {
            Optional<BlockPos> placed = HideoutPlacer.place(serverLevel, this.blockPosition());
            if (placed.isEmpty()) {
                this.discard();
                return spawnData;
            }
            this.hideoutPos = placed.get();
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(5, new com.tweeks.thief.entity.ai.StealFromChestGoal(this));
        this.goalSelector.addGoal(6, new com.tweeks.thief.entity.ai.WanderInVillageGoal(this));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
            net.minecraft.world.entity.player.Player.class, 8.0f));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.6));
    }
}
