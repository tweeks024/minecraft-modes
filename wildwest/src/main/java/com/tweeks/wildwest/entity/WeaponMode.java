package com.tweeks.wildwest.entity;

public enum WeaponMode {
    RANGED,
    MELEE;

    /** Distance threshold (blocks). At-or-below = MELEE, above = RANGED. */
    public static final double MELEE_RANGE = 4.0;

    /**
     * Pick the next weapon mode given the current state.
     *
     * @param distanceToTarget   distance from this mob to its target, in blocks
     * @param current            mob's current mode
     * @param hysteresisRemaining ticks until the swap-lock expires; 0 = swap allowed
     * @return the new mode (may equal current — caller decides if that's a no-op)
     */
    public static WeaponMode choose(double distanceToTarget, WeaponMode current, int hysteresisRemaining) {
        WeaponMode desired = (distanceToTarget <= MELEE_RANGE) ? MELEE : RANGED;
        if (desired == current) return current;
        if (hysteresisRemaining > 0) return current;
        return desired;
    }
}
