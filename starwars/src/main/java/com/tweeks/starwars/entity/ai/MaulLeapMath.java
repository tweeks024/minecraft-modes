package com.tweeks.starwars.entity.ai;

/**
 * Pure leap-vector math for {@link MaulLeapGoal}'s acrobatic lunge. Lives
 * outside any Minecraft class so the trajectory can be unit-tested without
 * booting the FML loader (same split as {@link RallyMath} and
 * {@code SwRadialMath}).
 *
 * <p>Mirrors the inline math in {@code LukeLeapGoal}/{@code YodaLeapGoal}
 * (flatten the offset-to-target into the XZ plane, normalize, scale by the
 * horizontal speed, stamp a fixed vertical boost) but adds a guard for the
 * degenerate case where the target sits directly above/below Maul: with zero
 * horizontal separation, normalizing would divide by zero and yield NaN, so
 * the lunge becomes a straight-up hop instead.
 */
public final class MaulLeapMath {
    private MaulLeapMath() {}

    /** Engagement band: lunge only when the target is 4-10 blocks out. */
    public static final double MIN_RANGE = 4.0;
    public static final double MAX_RANGE = 10.0;
    /** ~6s between lunges — longer than Luke's leap, so it reads as a commitment. */
    public static final int COOLDOWN_TICKS = 120;
    /** Faster and flatter than the Jedi leaps — Maul closes hard. */
    public static final double HORIZONTAL_SPEED = 1.0;
    public static final double VERTICAL_BOOST = 0.52;

    /** Immutable leap velocity, blocks/tick per axis. */
    public record LeapVelocity(double x, double y, double z) {}

    /** Inclusive range gate on the 3D distance to the target. */
    public static boolean inLeapRange(double distance) {
        return distance >= MIN_RANGE && distance <= MAX_RANGE;
    }

    /**
     * Leap velocity toward a target whose horizontal offset from Maul is
     * ({@code dx}, {@code dz}). The horizontal component is that offset
     * re-scaled to length {@code horizontalSpeed}; the vertical component is a
     * constant {@code verticalBoost}. A (near-)zero horizontal offset yields a
     * zero horizontal component (straight-up hop) rather than NaN.
     */
    public static LeapVelocity leapVelocity(double dx, double dz,
                                            double horizontalSpeed, double verticalBoost) {
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz < 1.0e-6) {
            return new LeapVelocity(0.0, verticalBoost, 0.0);
        }
        double scale = horizontalSpeed / horiz;
        return new LeapVelocity(dx * scale, verticalBoost, dz * scale);
    }
}
