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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
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
public class ThiefEntity extends PathfinderMob implements SecurityHostile, ContainerUser, CrossbowAttackMob {

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

    public static boolean checkSpawnRules(EntityType<ThiefEntity> type,
                                          net.minecraft.world.level.LevelAccessor level,
                                          EntitySpawnReason reason,
                                          BlockPos pos,
                                          net.minecraft.util.RandomSource random) {
        if (reason == EntitySpawnReason.SPAWN_ITEM_USE) return true;
        if (level instanceof ServerLevel sl) {
            return sl.structureManager()
                .getStructureWithPieceAt(pos, net.minecraft.tags.StructureTags.VILLAGE)
                .isValid();
        }
        return false;
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
            if (placed.isPresent()) {
                this.hideoutPos = placed.get();
            } else if (!isPlayerInitiated(reason)) {
                this.discard();
                return spawnData;
            }
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    private static boolean isPlayerInitiated(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.SPAWN_ITEM_USE
            || reason == EntitySpawnReason.COMMAND
            || reason == EntitySpawnReason.DISPENSER;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(2, new com.tweeks.thief.entity.ai.FleeAndFireCrossbowGoal(this));
        this.goalSelector.addGoal(3, new com.tweeks.thief.entity.ai.BlackjackStrikeGoal(this));
        this.goalSelector.addGoal(4, new com.tweeks.thief.entity.ai.ReturnToHideoutGoal(this));
        this.goalSelector.addGoal(5, new com.tweeks.thief.entity.ai.StealFromChestGoal(this));
        this.goalSelector.addGoal(6, new com.tweeks.thief.entity.ai.WanderInVillageGoal(this));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
            net.minecraft.world.entity.player.Player.class, 8.0f));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.6));

        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.tweeks.thief.entity.ai.SecretGuardTargetGoal(this));
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
            net.minecraft.world.entity.player.Player.class, 10, true, false,
            (target, level) -> {
                RevealState s = this.getRevealState();
                return s == RevealState.REVEALED_RANGED || s == RevealState.REVEALED_MELEE;
            }));
    }

    private boolean chargingCrossbow;

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        this.performCrossbowAttack(this, power);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        hasFiredCrossbow = true;
        triggerReveal(getTarget()); // Trigger #5
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.chargingCrossbow = charging;
    }

    public boolean isChargingCrossbow() {
        return chargingCrossbow;
    }

    // --- Reveal pipeline ---

    private static final int SUSPICIOUS_DURATION_TICKS = 40;
    private static final int REVEAL_SWAP_HYSTERESIS_TICKS = 20;
    private static final double MELEE_DISTANCE_BLOCKS = 4.0;

    private int suspiciousTimer;
    private int swapHysteresisTimer;
    private boolean hasFiredCrossbow;

    /**
     * Apply a reveal trigger. {@code triggeringEntity} is the entity whose
     * proximity determines RANGED vs MELEE; pass null when the trigger is
     * environmental (e.g. firing the crossbow).
     */
    public void triggerReveal(@Nullable LivingEntity triggeringEntity) {
        RevealState current = getRevealState();
        if (current == RevealState.SUSPICIOUS || current == RevealState.DISGUISED) {
            transitionTo(pickRevealStateFor(triggeringEntity));
            return;
        }
        if (swapHysteresisTimer > 0) return;
        RevealState desired = pickRevealStateFor(triggeringEntity);
        if (desired != current
                && (desired == RevealState.REVEALED_RANGED || desired == RevealState.REVEALED_MELEE)) {
            transitionTo(desired);
        }
    }

    public void enterSuspicious() {
        if (getRevealState() == RevealState.DISGUISED) {
            transitionTo(RevealState.SUSPICIOUS);
            suspiciousTimer = SUSPICIOUS_DURATION_TICKS;
        }
    }

    private void transitionTo(RevealState next) {
        RevealState current = getRevealState();
        if (!current.canTransitionTo(next)) return;
        setRevealState(next);
        swapHysteresisTimer = REVEAL_SWAP_HYSTERESIS_TICKS;
        if (next == RevealState.REVEALED_RANGED) {
            if (getMainHandItem().isEmpty() || !getMainHandItem().is(net.minecraft.world.item.Items.CROSSBOW)) {
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CROSSBOW));
            }
        }
    }

    private RevealState pickRevealStateFor(@Nullable LivingEntity who) {
        if (who == null) return RevealState.REVEALED_RANGED;
        return distanceTo(who) <= MELEE_DISTANCE_BLOCKS
            ? RevealState.REVEALED_MELEE
            : RevealState.REVEALED_RANGED;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        if (swapHysteresisTimer > 0) swapHysteresisTimer--;

        if (getRevealState() == RevealState.SUSPICIOUS) {
            suspiciousTimer--;
            if (suspiciousTimer <= 0) {
                setRevealState(RevealState.DISGUISED);
            }
        }

        if (getRevealState() == RevealState.DISGUISED && hasAnyStolenItem()) {
            net.minecraft.world.entity.player.Player p = level().getNearestPlayer(this, 8.0);
            if (p != null && p.hasLineOfSight(this)) {
                triggerReveal(p);
            }
        }

        if (getRevealState() == RevealState.DISGUISED) {
            java.util.List<com.tweeks.securityguard.entity.SecurityGuardEntity> guards =
                level().getEntitiesOfClass(com.tweeks.securityguard.entity.SecurityGuardEntity.class,
                    getBoundingBox().inflate(8.0));
            for (com.tweeks.securityguard.entity.SecurityGuardEntity g : guards) {
                if (g.hasLineOfSight(this)) {
                    triggerReveal(g);
                    break;
                }
            }
        }

        if ((getRevealState() == RevealState.REVEALED_RANGED || getRevealState() == RevealState.REVEALED_MELEE)
                && getTarget() != null) {
            triggerReveal(getTarget());
        }

        if (hideoutPos != null && level().getBlockState(hideoutPos).isAir()) {
            hideoutPos = null;
            if (!getRevealState().isHostile()) {
                triggerReveal(level().getNearestPlayer(this, 32.0));
            }
        }
    }

    private boolean hasAnyStolenItem() {
        for (int i = 0; i < stolenItems.getContainerSize(); i++) {
            if (!stolenItems.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource source,
                              float amount) {
        boolean wasHurt = super.hurtServer(level, source, amount);
        if (wasHurt && source.getEntity() instanceof LivingEntity attacker) {
            triggerReveal(attacker);
        }
        return wasHurt;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source,
                                       boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (int i = 0; i < stolenItems.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = stolenItems.getItem(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(level, stack);
                stolenItems.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }
}
