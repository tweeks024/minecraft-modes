package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.faction.SwFaction;

/**
 * Pure eligibility/cadence math for Leia's "Rebel Rally" aura. Lives outside
 * MC classes for unit testing (same pattern as
 * {@link com.tweeks.starwars.faction.Alignment}).
 *
 * <p>Effect choice (Resistance, not Strength) is deliberate: blaster damage
 * is a flat constant passed straight to {@code hurtServer(...)} and never
 * reads the ATTACK_DAMAGE attribute Strength modifies — Strength would be a
 * silent no-op for every blaster-wielding ally. Resistance applies on the
 * victim side of the damage pipeline and benefits everyone. (Spec §4.3.)
 */
public final class RallyMath {
    private RallyMath() {}

    /** One pulse at most every 12 seconds. */
    public static final int RALLY_INTERVAL_TICKS = 240;
    /** Resistance I + Regeneration I for 8 seconds. */
    public static final int RALLY_DURATION_TICKS = 160;
    public static final double RALLY_RADIUS = 12.0;

    public static boolean isEligibleFaction(SwFaction faction) {
        return faction == SwFaction.LIGHT;
    }

    /** Players rally only when strictly Light-aligned (score > 0). */
    public static boolean isEligibleScore(int alignmentScore) {
        return alignmentScore > 0;
    }

    /** Squared-distance radius check, inclusive at the boundary. */
    public static boolean isWithinRadius(double distSq) {
        return distSq <= RALLY_RADIUS * RALLY_RADIUS;
    }

    /** Cadence gate on game time (survives goal stop/start; no per-tick counter). */
    public static boolean isReady(long now, long lastPulseGameTime) {
        return now - lastPulseGameTime >= RALLY_INTERVAL_TICKS;
    }
}
