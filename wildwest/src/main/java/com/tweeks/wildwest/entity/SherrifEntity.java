package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.api.Lawman;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SherrifEntity extends WildWestMob implements Lawman {

    public SherrifEntity(EntityType<? extends SherrifEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 28.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected ItemStack getGunStack() { return new ItemStack(com.tweeks.wildwest.Registration.RIFLE.get()); }

    @Override
    protected ItemStack getHandWeaponStack() { return new ItemStack(com.tweeks.wildwest.Registration.BILLY_CLUB.get()); }

    @Override
    public boolean usesRifle() { return true; }

    @Override
    public boolean isLawman() { return true; }
}
