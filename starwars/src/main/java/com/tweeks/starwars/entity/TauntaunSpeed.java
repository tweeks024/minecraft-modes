package com.tweeks.starwars.entity;

/**
 * Pure tauntaun ridden-speed math: base speed, +30% on snow-family blocks.
 * Lives outside MC classes so it can be unit-tested without booting the
 * FML loader — the entity supplies the base attribute value and whether the
 * block underfoot is in the snow tag.
 */
public final class TauntaunSpeed {
    private TauntaunSpeed() {}

    /** +30% on snow-family ground. */
    public static final double SNOW_MULTIPLIER = 1.30;

    public static double riddenSpeed(double baseSpeed, boolean onSnow) {
        return onSnow ? baseSpeed * SNOW_MULTIPLIER : baseSpeed;
    }
}
