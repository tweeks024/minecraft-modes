package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.api.Outlaw;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Undead pirate. Burns in sunlight (like vanilla skeleton). Wields a rapier
 * for melee and a flintlock pistol for ranged. Both AI behaviors come from
 * the {@link WildWestMob} base — this class only fills in the weapon stacks,
 * the daylight-burn check, and a couple of undead immunities.
 */
public class SkeletonPirateEntity extends WildWestMob implements Outlaw, Enemy {

    public SkeletonPirateEntity(EntityType<? extends SkeletonPirateEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected ItemStack getGunStack() {
        return new ItemStack(com.tweeks.wildwest.Registration.FLINTLOCK_PISTOL.get());
    }

    @Override
    protected ItemStack getHandWeaponStack() {
        return new ItemStack(com.tweeks.wildwest.Registration.RAPIER.get());
    }

    @Override
    public boolean usesRifle() { return false; }

    @Override
    public boolean isLawman() { return false; }

    @Override
    public boolean isLeader() { return false; }

    /**
     * Undead immunity to poison — vanilla skeleton behaviour. In 26.1.2
     * {@link MobEffects#POISON} is a {@code Holder<MobEffect>}, so we compare
     * via {@link MobEffectInstance#is}.
     */
    @Override
    public boolean canBeAffected(MobEffectInstance instance) {
        if (instance.is(MobEffects.POISON)) return false;
        return super.canBeAffected(instance);
    }

    /**
     * Daylight burn (vanilla skeleton pattern). In 26.1.2 vanilla wires daylight
     * burn through the {@code EntityTypeTags.BURN_IN_DAYLIGHT} entity-type tag
     * inside {@code Mob.aiStep} via a private {@code burnUndead}. Until the
     * entity type is added to that tag (datapack work) we drive the burn
     * manually here. Notable API drift in this build:
     *   - {@code ServerLevel.isDay()} → {@code Level.isBrightOutside()}.
     *   - {@code igniteForSeconds} takes a {@code float}, not {@code int}.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isAlive() && !this.level().isClientSide()
            && this.level() instanceof ServerLevel sl
            && sl.isBrightOutside()
            && !this.isInWaterOrRain()
            && sl.canSeeSky(this.blockPosition())) {
            this.igniteForSeconds(8.0F);
        }
    }

    /**
     * Drown immunity — pirates aboard a galleon will get wet legs but should
     * not die to it. Mirrors {@link HerobrineEntity}'s {@code isInvulnerableTo}
     * signature (ServerLevel + DamageSource overload exists in this build).
     */
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_DROWNING)) return true;
        return super.isInvulnerableTo(level, source);
    }
}
