package com.tweeks.starwars.entity.vehicle;

/**
 * Pure hover/drive math for the speeder bike — a landspeeder clone tuned
 * fast and twitchy. No engine imports, so it unit-tests without an MC
 * bootstrap (same pattern as {@link HoverPhysics}, which this deliberately
 * mirrors function-for-function rather than modifies).
 *
 * <p>Differences from {@link HoverPhysics}: a lower {@link #HOVER_HEIGHT}
 * (0.4 — the bike skims closer to the ground), a much higher
 * {@link #MAX_SPEED} (1.05), snappier {@link #FORWARD_ACCEL}, a faster
 * {@link #TURN_RATE_DEG} yaw response, and a heavier {@link #VELOCITY_BLEND}
 * so the nose whips toward the facing direction (the "twitchy" feel). The
 * vertical spring constants are copied verbatim from {@link HoverPhysics}
 * because they are numerically proven to settle without ground contact even
 * when entering the scan window at terminal velocity (see
 * {@link com.tweeks.starwars.entity.vehicle.BikePhysicsTest}, which re-runs
 * that worst-case simulation for the 0.4 hover height).
 */
public final class BikePhysics {
    private BikePhysics() {}

    /** Resting skim height — raised so the bike floats clear of 1-block terrain
     *  instead of nosing into it (the old 0.4 snagged on every ledge). */
    public static final double HOVER_HEIGHT = 1.6;
    /** Extra lift added to the target height while the rider holds jump, so
     *  space climbs the bike up and over taller obstacles. */
    public static final double BOOST_HEIGHT = 3.0;
    public static final double HOVER_SCAN_DEPTH = 6.0;
    public static final double SPRING_STIFFNESS = 0.14;
    public static final double SPRING_DAMPING = 0.42;
    public static final double MAX_VERTICAL_ACCEL = 0.22;
    public static final double GRAVITY = -0.04;
    public static final double TERMINAL_FALL = -0.6;
    /** Snappier than the landspeeder's 0.06. */
    public static final double FORWARD_ACCEL = 0.09;
    public static final double REVERSE_ACCEL = FORWARD_ACCEL * 0.4;
    /** ~2.5x vanilla boat top speed (blocks/tick) — a fast racing bike. */
    public static final double MAX_SPEED = 1.05;
    public static final double MAX_REVERSE_SPEED = 0.3;
    public static final double FRICTION = 0.95;
    /** Faster than the landspeeder's 3.5 — the bike is twitchy. */
    public static final float TURN_RATE_DEG = 5.0f;
    /** Per-tick blend of horizontal velocity toward the facing (drift feel). */
    public static final double VELOCITY_BLEND = 0.30;

    /**
     * Vertical acceleration for this tick.
     *
     * @param distToGround hull-bottom to sensed-ground distance, or
     *                     {@code Double.NaN} when no ground within
     *                     {@link #HOVER_SCAN_DEPTH}
     * @param verticalVel  current vertical velocity (blocks/tick)
     */
    public static double verticalAccel(double distToGround, double verticalVel) {
        return verticalAccel(distToGround, verticalVel, false);
    }

    /**
     * Vertical acceleration for this tick.
     *
     * @param distToGround hull-bottom to sensed-ground distance, or
     *                     {@code Double.NaN} when no ground within
     *                     {@link #HOVER_SCAN_DEPTH}
     * @param verticalVel  current vertical velocity (blocks/tick)
     * @param boosting     rider is holding jump — raise the target height so
     *                     the bike climbs
     */
    public static double verticalAccel(double distToGround, double verticalVel, boolean boosting) {
        double target = boosting ? HOVER_HEIGHT + BOOST_HEIGHT : HOVER_HEIGHT;
        if (Double.isNaN(distToGround)) {
            // No ground sensed. Holding jump still lifts (climb out of a pit);
            // otherwise sink gently under gravity.
            if (boosting) {
                return Math.min(MAX_VERTICAL_ACCEL, SPRING_STIFFNESS * BOOST_HEIGHT - SPRING_DAMPING * verticalVel);
            }
            return Math.max(GRAVITY, TERMINAL_FALL - verticalVel);
        }
        double accel = SPRING_STIFFNESS * (target - distToGround) - SPRING_DAMPING * verticalVel;
        return Math.max(-MAX_VERTICAL_ACCEL, Math.min(MAX_VERTICAL_ACCEL, accel));
    }

    /** @param forwardInput -1 (reverse), 0 (coast), or 1 (forward) */
    public static double nextForwardSpeed(double current, int forwardInput) {
        if (forwardInput > 0) return Math.min(MAX_SPEED, current + FORWARD_ACCEL);
        if (forwardInput < 0) return Math.max(-MAX_REVERSE_SPEED, current - REVERSE_ACCEL);
        return current * FRICTION;
    }

    /** @param turnInput -1 (right), 0, or 1 (left) — sign matched to the yaw convention in SpeederBikeEntity */
    public static float nextYaw(float yawDegrees, int turnInput) {
        return yawDegrees + turnInput * TURN_RATE_DEG;
    }
}
