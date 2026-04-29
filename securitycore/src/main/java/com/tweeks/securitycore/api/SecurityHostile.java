package com.tweeks.securitycore.api;

/**
 * Marker interface for entities that Security Guards (and other Security Pack
 * defenders) should target as hostile.
 *
 * <p>Implementing alone is not enough — a Guard's target predicate also calls
 * {@link #isCurrentlyHostile()}, which lets implementers gate hostility on
 * runtime state. Use this for mobs that look harmless until revealed (e.g. a
 * disguised Thief is still {@code instanceof SecurityHostile} but reports
 * {@code isCurrentlyHostile() == false} until reveal).
 */
public interface SecurityHostile {

    /**
     * @return true if this entity should be attacked by Security defenders right now.
     *         Default: true (always hostile, suitable for permanently-hostile mobs).
     */
    default boolean isCurrentlyHostile() {
        return true;
    }
}
