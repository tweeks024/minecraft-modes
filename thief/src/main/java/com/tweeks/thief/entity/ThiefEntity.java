package com.tweeks.thief.entity;

import com.tweeks.securitycore.api.SecurityHostile;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The Thief entity. Disguises as a villager, steals from chests, hides loot.
 * Implements {@link SecurityHostile} but reports
 * {@link SecurityHostile#isCurrentlyHostile()} based on its current
 * {@link RevealState}, so Guards ignore disguised Thieves.
 */
public class ThiefEntity extends PathfinderMob implements SecurityHostile {

    private static final EntityDataAccessor<Byte> DATA_REVEAL_STATE =
        SynchedEntityData.defineId(ThiefEntity.class, EntityDataSerializers.BYTE);

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

    @Override
    public boolean isCurrentlyHostile() {
        return getRevealState().isHostile();
    }
}
