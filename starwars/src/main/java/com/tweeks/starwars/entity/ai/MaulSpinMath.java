package com.tweeks.starwars.entity.ai;

/**
 * Pure geometry/cadence math for {@link MaulSpinGoal}'s double-saber sweep.
 * Split out for unit testing (same pattern as {@link RallyMath}).
 *
 * <p>The per-victim faction/alignment decision is deliberately NOT here — the
 * goal reuses the already-tested {@link TargetPredicates#shouldTarget}, so
 * Maul sweeps exactly the set of entities his target goal would acquire.
 */
public final class MaulSpinMath {
    private MaulSpinMath() {}

    /** Sweep reaches 3 blocks in every direction. */
    public static final double SPIN_RADIUS = 3.0;
    /** Trigger only when swarmed: 2+ enemies in reach. */
    public static final int CROWD_THRESHOLD = 2;
    /** ~4s between sweeps — short, so a crowd gets punished repeatedly. */
    public static final int COOLDOWN_TICKS = 80;

    /** Spherical radius gate (inclusive at the boundary) on squared distance. */
    public static boolean isWithinRadius(double distSq) {
        return distSq <= SPIN_RADIUS * SPIN_RADIUS;
    }

    /** True once enough enemies crowd into reach to justify the sweep. */
    public static boolean hasEnoughEnemies(int count) {
        return count >= CROWD_THRESHOLD;
    }
}
