package com.tweeks.wildwest.item;

/**
 * Pirate Captain's pistol. Drops from Pirate Captain at 100%. 16 damage per
 * shot. Gold-trim model variant; mechanics otherwise identical to
 * FlintlockPistolItem.
 */
public class CaptainPistolItem extends FlintlockPistolItem {

    public static final float CAPTAIN_DAMAGE = 16.0F;

    public CaptainPistolItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDamage() {
        return CAPTAIN_DAMAGE;
    }
}
