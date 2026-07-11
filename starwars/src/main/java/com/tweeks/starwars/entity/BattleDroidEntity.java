package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BattleDroidEntity extends SwMob implements Enemy {

    public BattleDroidEntity(EntityType<? extends BattleDroidEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, SwMobConstants.DROID_MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, SwMobConstants.DROID_MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, SwMobConstants.TROOPER_FOLLOW_RANGE);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        return new ItemStack(com.tweeks.starwars.Registration.BLASTER_PISTOL.get());
    }

    @Override
    public boolean fireImmune() { return false; }

    // Droids clank instead of grunt: no ambient sound, metallic hurt sound.
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.IRON_GOLEM_DEATH;
    }
}
