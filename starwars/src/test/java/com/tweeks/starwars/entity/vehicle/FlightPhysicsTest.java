package com.tweeks.starwars.entity.vehicle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FlightPhysicsTest {

    @Test
    public void throttleAccceleratesTowardXwingMax() {
        double s = 0.0;
        for (int i = 0; i < 200; i++) {
            s = FlightPhysics.nextSpeed(s, true, false, FlightPhysics.XWING_MAX_SPEED);
        }
        assertEquals(FlightPhysics.XWING_MAX_SPEED, s, 1e-9);
    }

    @Test
    public void throttleAcceleratesTowardTieMax() {
        double s = 0.0;
        for (int i = 0; i < 200; i++) {
            s = FlightPhysics.nextSpeed(s, true, false, FlightPhysics.TIE_MAX_SPEED);
        }
        assertEquals(FlightPhysics.TIE_MAX_SPEED, s, 1e-9);
    }

    @Test
    public void tieIsFasterThanXwing() {
        assertTrue(FlightPhysics.TIE_MAX_SPEED > FlightPhysics.XWING_MAX_SPEED);
    }

    @Test
    public void oneThrottleTickAddsAccel() {
        assertEquals(0.5 + FlightPhysics.ACCEL,
            FlightPhysics.nextSpeed(0.5, true, false, FlightPhysics.XWING_MAX_SPEED), 1e-9);
    }

    @Test
    public void neverExceedsMax() {
        double s = FlightPhysics.nextSpeed(FlightPhysics.XWING_MAX_SPEED, true, false, FlightPhysics.XWING_MAX_SPEED);
        assertEquals(FlightPhysics.XWING_MAX_SPEED, s, 1e-9);
    }

    @Test
    public void releasedThrottleDecaysByFactor() {
        assertEquals(1.0 * FlightPhysics.DECAY,
            FlightPhysics.nextSpeed(1.0, false, false, FlightPhysics.XWING_MAX_SPEED), 1e-9);
    }

    @Test
    public void releasedThrottleDecaysTowardZero() {
        double s = 1.0;
        for (int i = 0; i < 500; i++) {
            s = FlightPhysics.nextSpeed(s, false, false, FlightPhysics.XWING_MAX_SPEED);
        }
        assertEquals(0.0, s, 1e-3);
    }

    @Test
    public void onGroundCapsToTaxiSpeed() {
        // Even at full airspeed with throttle held, ground contact caps it.
        double s = FlightPhysics.nextSpeed(FlightPhysics.XWING_MAX_SPEED, true, true, FlightPhysics.XWING_MAX_SPEED);
        assertEquals(FlightPhysics.TAXI_CAP, s, 1e-9);
    }

    @Test
    public void verticalComponentIsZeroLevel() {
        assertEquals(0.0, FlightPhysics.verticalComponent(1.0, 0.0), 1e-9);
    }

    @Test
    public void verticalComponentClimbsWhenNoseUp() {
        assertTrue(FlightPhysics.verticalComponent(1.0, 45.0) > 0.0);
    }

    @Test
    public void verticalComponentDivesWhenNoseDown() {
        assertTrue(FlightPhysics.verticalComponent(1.0, -45.0) < 0.0);
    }

    @Test
    public void verticalComponentClampedToFractionOfSpeed() {
        double speed = 1.0;
        double cap = FlightPhysics.VERTICAL_FRACTION * speed;
        assertEquals(cap, FlightPhysics.verticalComponent(speed, 90.0), 1e-9);
        assertEquals(-cap, FlightPhysics.verticalComponent(speed, -90.0), 1e-9);
    }

    @Test
    public void verticalComponentScalesWithSpeed() {
        // Same steep dive, twice the speed -> twice the (clamped) descent.
        double slow = FlightPhysics.verticalComponent(0.4, 90.0);
        double fast = FlightPhysics.verticalComponent(0.8, 90.0);
        assertEquals(2.0 * slow, fast, 1e-9);
    }

    @Test
    public void sinksWhenSlowAndAirborne() {
        assertEquals(FlightPhysics.SINK_RATE, FlightPhysics.sinkRate(0.1, false), 1e-9);
    }

    @Test
    public void noSinkAtCruiseSpeed() {
        assertEquals(0.0, FlightPhysics.sinkRate(1.0, false), 1e-9);
    }

    @Test
    public void noSinkOnGround() {
        assertEquals(0.0, FlightPhysics.sinkRate(0.1, true), 1e-9);
    }

    @Test
    public void sinkThresholdIsExclusive() {
        // Exactly at the threshold is "not slow enough" to sink.
        assertEquals(0.0, FlightPhysics.sinkRate(FlightPhysics.SINK_SPEED_THRESHOLD, false), 1e-9);
    }
}
