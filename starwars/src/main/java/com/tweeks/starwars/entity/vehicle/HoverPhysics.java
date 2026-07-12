package com.tweeks.starwars.entity.vehicle;

/**
 * Pure hover/drive math for the landspeeder. No engine imports — lives
 * outside MC classes for unit testing (spec §5.3; same pattern as
 * {@link com.tweeks.starwars.faction.Alignment}).
 *
 * <p>Spring-damper hover: accelerate toward {@link #HOVER_HEIGHT} above
 * sensed ground, damped against vertical velocity, clamped to
 * {@link #MAX_VERTICAL_ACCEL} per tick. Numerically verified to settle
 * without ground contact even entering the scan window at terminal
 * velocity (see HoverPhysicsTest).
 */
public final class HoverPhysics {
    private HoverPhysics() {}

    public static final double HOVER_HEIGHT = 0.5;
    public static final double HOVER_SCAN_DEPTH = 3.0;
    public static final double SPRING_STIFFNESS = 0.10;
    public static final double SPRING_DAMPING = 0.40;
    public static final double MAX_VERTICAL_ACCEL = 0.15;
    public static final double GRAVITY = -0.04;
    public static final double TERMINAL_FALL = -0.6;
    public static final double FORWARD_ACCEL = 0.06;
    public static final double REVERSE_ACCEL = FORWARD_ACCEL * 0.4;
    /** ~1.7x vanilla boat top speed (blocks/tick). */
    public static final double MAX_SPEED = 0.7;
    public static final double MAX_REVERSE_SPEED = 0.2;
    public static final double FRICTION = 0.95;
    public static final float TURN_RATE_DEG = 3.5f;
    /** Per-tick blend of horizontal velocity toward the facing (drift feel). */
    public static final double VELOCITY_BLEND = 0.20;

    /**
     * Vertical acceleration for this tick.
     *
     * @param distToGround hull-bottom to sensed-ground distance, or
     *                     {@code Double.NaN} when no ground within
     *                     {@link #HOVER_SCAN_DEPTH}
     * @param verticalVel  current vertical velocity (blocks/tick)
     */
    public static double verticalAccel(double distToGround, double verticalVel) {
        if (Double.isNaN(distToGround)) {
            // Free fall: gravity, but never accelerate past terminal velocity
            // (and gently decelerate back to terminal if somehow beyond it).
            return Math.max(GRAVITY, TERMINAL_FALL - verticalVel);
        }
        double accel = SPRING_STIFFNESS * (HOVER_HEIGHT - distToGround)
            - SPRING_DAMPING * verticalVel;
        return Math.max(-MAX_VERTICAL_ACCEL, Math.min(MAX_VERTICAL_ACCEL, accel));
    }

    /** @param forwardInput -1 (reverse), 0 (coast), or 1 (forward) */
    public static double nextForwardSpeed(double current, int forwardInput) {
        if (forwardInput > 0) return Math.min(MAX_SPEED, current + FORWARD_ACCEL);
        if (forwardInput < 0) return Math.max(-MAX_REVERSE_SPEED, current - REVERSE_ACCEL);
        return current * FRICTION;
    }

    /** @param turnInput -1 (right), 0, or 1 (left) — sign matched to yaw convention in LandspeederEntity */
    public static float nextYaw(float yawDegrees, int turnInput) {
        return yawDegrees + turnInput * TURN_RATE_DEG;
    }
}
