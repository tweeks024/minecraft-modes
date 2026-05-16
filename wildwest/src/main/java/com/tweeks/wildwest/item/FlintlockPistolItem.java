package com.tweeks.wildwest.item;

/**
 * Pirate-themed pistol. Mechanically identical to PistolItem but
 * deals 12 damage per shot. Cosmetic model differs (brown wood / iron barrel).
 *
 * <p>Dropped at 3% from Skeleton Pirate kills.
 */
public class FlintlockPistolItem extends PistolItem {

    public static final float FLINTLOCK_DAMAGE = 12.0F;

    public FlintlockPistolItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDamage() {
        return FLINTLOCK_DAMAGE;
    }
}
