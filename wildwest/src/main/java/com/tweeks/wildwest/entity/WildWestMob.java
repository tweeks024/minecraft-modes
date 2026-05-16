package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.entity.ai.CannonOperateGoal;
import com.tweeks.wildwest.entity.ai.FollowDecision;
import com.tweeks.wildwest.entity.ai.LawmanTargetGoal;
import com.tweeks.wildwest.entity.ai.OutlawTargetGoal;
import com.tweeks.wildwest.entity.ai.WildWestMeleeAttackGoal;
import com.tweeks.wildwest.entity.ai.WildWestRangedAttackGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class WildWestMob extends PathfinderMob implements FollowDecision.Candidate {

    private static final EntityDataAccessor<Byte> DATA_WEAPON_MODE =
        SynchedEntityData.defineId(WildWestMob.class, EntityDataSerializers.BYTE);

    private static final int CHECK_INTERVAL_TICKS = 5;
    private static final int HYSTERESIS_TICKS = 20;

    private int hysteresisLockTicks = 0;
    private int tickCounter = 0;

    /** The leader this follower is currently tracking, or null. Null for leaders themselves. */
    private WildWestMob followingLeader = null;

    public WildWestMob getFollowingLeader() {
        return this.followingLeader;
    }

    public void setFollowingLeader(WildWestMob leader) {
        this.followingLeader = leader;
    }

    protected WildWestMob(EntityType<? extends WildWestMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WEAPON_MODE, (byte) WeaponMode.RANGED.ordinal());
    }

    public WeaponMode getWeaponMode() {
        byte b = this.entityData.get(DATA_WEAPON_MODE);
        return WeaponMode.values()[b];
    }

    private void setWeaponMode(WeaponMode mode) {
        this.entityData.set(DATA_WEAPON_MODE, (byte) mode.ordinal());
    }

    protected abstract ItemStack getGunStack();
    protected abstract ItemStack getHandWeaponStack();
    public abstract boolean usesRifle();
    public abstract boolean isLawman();
    /** True if this mob is a leader (sherrif, bandit-leader). False for footsoldiers. */
    public abstract boolean isLeader();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CannonOperateGoal(this));
        this.goalSelector.addGoal(2, new WildWestRangedAttackGoal(this));
        this.goalSelector.addGoal(2, new WildWestMeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(4, new com.tweeks.wildwest.entity.ai.FollowLeaderGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new com.tweeks.wildwest.entity.ai.LeaderTargetCopyGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        if (this.isLawman()) {
            this.targetSelector.addGoal(2, new LawmanTargetGoal(this));
        } else {
            this.targetSelector.addGoal(2, new OutlawTargetGoal(this));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;
        if (this.hysteresisLockTicks > 0) this.hysteresisLockTicks--;

        if (++this.tickCounter < CHECK_INTERVAL_TICKS) return;
        this.tickCounter = 0;

        var target = this.getTarget();
        if (target == null) {
            if (this.getWeaponMode() != WeaponMode.RANGED) {
                this.setWeaponMode(WeaponMode.RANGED);
                this.setItemSlot(EquipmentSlot.MAINHAND, this.getGunStack());
                this.hysteresisLockTicks = HYSTERESIS_TICKS;
            }
            return;
        }

        double dist = this.distanceTo(target);
        WeaponMode current = this.getWeaponMode();
        WeaponMode next = WeaponMode.choose(dist, current, this.hysteresisLockTicks);
        if (next != current) {
            this.setWeaponMode(next);
            this.setItemSlot(EquipmentSlot.MAINHAND,
                next == WeaponMode.MELEE ? this.getHandWeaponStack() : this.getGunStack());
            this.hysteresisLockTicks = HYSTERESIS_TICKS;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("WeaponMode", (byte) this.getWeaponMode().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        byte b = input.getByteOr("WeaponMode", (byte) WeaponMode.RANGED.ordinal());
        if (b >= 0 && b < WeaponMode.values().length) {
            this.entityData.set(DATA_WEAPON_MODE, b);
        }
    }
}
