package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StormtrooperEntity extends SwMob implements Enemy {

    public StormtrooperEntity(EntityType<? extends StormtrooperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, SwMobConstants.TROOPER_MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, SwMobConstants.TROOPER_MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, SwMobConstants.TROOPER_FOLLOW_RANGE);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return true; }

    @Override
    protected ItemStack getWeaponStack() {
        return new ItemStack(com.tweeks.starwars.Registration.BLASTER_RIFLE.get());
    }
}
