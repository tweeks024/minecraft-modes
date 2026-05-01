package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.api.Outlaw;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BanditEntity extends WildWestMob implements Outlaw, Enemy {

    public BanditEntity(EntityType<? extends BanditEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected ItemStack getGunStack() { return new ItemStack(com.tweeks.wildwest.Registration.PISTOL.get()); }

    @Override
    protected ItemStack getHandWeaponStack() { return new ItemStack(com.tweeks.wildwest.Registration.BANDIT_KNIFE.get()); }

    @Override
    public boolean usesRifle() { return false; }

    @Override
    public boolean isLawman() { return false; }

    @Override
    public boolean isLeader() { return false; }
}
