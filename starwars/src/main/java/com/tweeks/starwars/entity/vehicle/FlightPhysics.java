package com.tweeks.starwars.entity.vehicle;

/**
 * Pure flight math for the starfighters (X-wing, TIE fighter). No engine
 * imports, so it unit-tests without an MC bootstrap (same seam as
 * {@link HoverPhysics}).
 *
 * <p>Model: the fighter carries a single scalar {@code speed}. Holding
 * forward accelerates it toward the type's max ({@link #XWING_MAX_SPEED} /
 * {@link #TIE_MAX_SPEED}) at {@link #ACCEL} per tick; releasing decays it by
 * {@link #DECAY}. The velocity vector is the look direction scaled by speed,
 * with the vertical component taken from the nose pitch and clamped to
 * ±{@link #VERTICAL_FRACTION}·speed so a dive/climb can never dump or gain
 * more than that share of the airspeed. When nearly stopped and airborne the
 * fighter sinks gently ({@link #SINK_RATE}); on the ground the speed is
 * capped to a {@link #TAXI_CAP} crawl.
 */
public final class FlightPhysics {
    private FlightPhysics() {}

    public static final double ACCEL = 0.06;
    public static final double DECAY = 0.94;
    /** Airspeed below which an airborne fighter starts to sink. */
    public static final double SINK_SPEED_THRESHOLD = 0.25;
    /** Per-tick vertical velocity added when sinking (a gentle stall). */
    public static final double SINK_RATE = -0.04;
    /** On the ground the fighter can only taxi this fast. */
    public static final double TAXI_CAP = 0.25;
    /** Vertical velocity is at most this fraction of the current airspeed. */
    public static final double VERTICAL_FRACTION = 0.7;

    public static final double XWING_MAX_SPEED = 1.3;
    public static final double TIE_MAX_SPEED = 1.55;

    /**
     * Advance the airspeed scalar one tick.
     *
     * @param current     current airspeed (blocks/tick)
     * @param forwardHeld throttle held this tick
     * @param onGround    fighter is taxiing on solid ground
     * @param maxSpeed    the type's top speed
     */
    public static double nextSpeed(double current, boolean forwardHeld, boolean onGround, double maxSpeed) {
        double s = forwardHeld ? Math.min(maxSpeed, current + ACCEL) : current * DECAY;
        if (onGround) {
            s = Math.min(s, TAXI_CAP);
        }
        return s;
    }

    /**
     * Vertical velocity contributed by the nose pitch at this airspeed,
     * clamped to ±{@link #VERTICAL_FRACTION}·speed.
     *
     * @param speed        current airspeed (blocks/tick, non-negative)
     * @param noseUpDegrees nose-up pitch in degrees (positive climbs); the
     *                      entity passes {@code -getXRot()} since MC pitch is
     *                      positive looking down
     */
    public static double verticalComponent(double speed, double noseUpDegrees) {
        double raw = Math.sin(Math.toRadians(noseUpDegrees)) * speed;
        double cap = VERTICAL_FRACTION * speed;
        return Math.max(-cap, Math.min(cap, raw));
    }

    /** Extra vertical velocity for a gentle airborne stall-sink; 0 unless slow and off the ground. */
    public static double sinkRate(double speed, boolean onGround) {
        return (!onGround && speed < SINK_SPEED_THRESHOLD) ? SINK_RATE : 0.0;
    }
}
