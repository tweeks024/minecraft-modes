package com.tweeks.starwars.item;

import com.tweeks.starwars.network.S2CBlasterTracerPacket;
import net.minecraft.world.entity.LivingEntity;

public class BlasterRifleItem extends BlasterPistolItem {

    public static final float RIFLE_DAMAGE = 8.0F;
    public static final int RIFLE_COOLDOWN_TICKS = 20;

    public BlasterRifleItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDamage() { return RIFLE_DAMAGE; }

    @Override
    public int getCooldownTicks() { return RIFLE_COOLDOWN_TICKS; }

    public static void fireFromMobRifle(LivingEntity shooter, LivingEntity target, int tracerColor) {
        fireFromMob(shooter, target, RIFLE_DAMAGE, tracerColor);
    }
}
