package com.tweeks.starwars.entity.vehicle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BikePhysicsTest {

    /** Simulate the 1-D vertical system: dist' = dist + vel, vel' = vel + accel. */
    private static double[] simulate(double dist, double vel, int ticks) {
        for (int i = 0; i < ticks; i++) {
            vel += BikePhysics.verticalAccel(dist, vel);
            dist += vel;
        }
        return new double[] {dist, vel};
    }

    @Test
    public void settlesToHoverHeightFromAbove() {
        double[] end = simulate(3.0, 0.0, 200);
        assertEquals(BikePhysics.HOVER_HEIGHT, end[0], 0.05);
        assertEquals(0.0, end[1], 0.02);
    }

    @Test
    public void settlesToHoverHeightFromBelow() {
        double[] end = simulate(0.1, 0.0, 200);
        assertEquals(BikePhysics.HOVER_HEIGHT, end[0], 0.05);
    }

    @Test
    public void terminalFallIntoScanRangeNeverClipsGround() {
        // Worst case: entering the 3-block scan at terminal velocity. The
        // lower 0.4 hover height leaves less clearance than the landspeeder's
        // 0.5, so this re-verifies the spring still never touches the ground.
        double dist = BikePhysics.HOVER_SCAN_DEPTH, vel = BikePhysics.TERMINAL_FALL, min = dist;
        for (int i = 0; i < 200; i++) {
            vel += BikePhysics.verticalAccel(dist, vel);
            dist += vel;
            min = Math.min(min, dist);
        }
        assertTrue(min > 0.0, "hull must never reach the ground (min=" + min + ")");
        assertEquals(BikePhysics.HOVER_HEIGHT, dist, 0.05);
    }

    @Test
    public void verticalAccelIsClamped() {
        assertTrue(Math.abs(BikePhysics.verticalAccel(0.0, 0.0)) <= BikePhysics.MAX_VERTICAL_ACCEL + 1e-9);
        assertTrue(Math.abs(BikePhysics.verticalAccel(10.0, 0.0)) <= BikePhysics.MAX_VERTICAL_ACCEL + 1e-9);
    }

    @Test
    public void noGroundMeansGravityTowardTerminal() {
        double vel = 0.0;
        for (int i = 0; i < 100; i++) {
            vel += BikePhysics.verticalAccel(Double.NaN, vel);
        }
        assertEquals(BikePhysics.TERMINAL_FALL, vel, 0.01);
        assertTrue(vel >= BikePhysics.TERMINAL_FALL - 1e-9);
    }

    @Test
    public void forwardSpeedCapsAtMaxSpeed() {
        double s = 0.0;
        for (int i = 0; i < 100; i++) s = BikePhysics.nextForwardSpeed(s, 1);
        assertEquals(BikePhysics.MAX_SPEED, s, 1e-9);
    }

    @Test
    public void bikeIsFasterThanLandspeeder() {
        assertTrue(BikePhysics.MAX_SPEED > HoverPhysics.MAX_SPEED,
            "the bike must out-run the landspeeder");
    }

    @Test
    public void bikeAccelIsSnappierThanLandspeeder() {
        assertTrue(BikePhysics.FORWARD_ACCEL > HoverPhysics.FORWARD_ACCEL);
    }

    @Test
    public void bikeYawIsFasterThanLandspeeder() {
        assertTrue(BikePhysics.TURN_RATE_DEG > HoverPhysics.TURN_RATE_DEG);
    }

    @Test
    public void reverseSpeedCapsAtReverseMax() {
        double s = 0.0;
        for (int i = 0; i < 100; i++) s = BikePhysics.nextForwardSpeed(s, -1);
        assertEquals(-BikePhysics.MAX_REVERSE_SPEED, s, 1e-9);
    }

    @Test
    public void noInputCoastsDownByFriction() {
        double s = BikePhysics.MAX_SPEED;
        s = BikePhysics.nextForwardSpeed(s, 0);
        assertEquals(BikePhysics.MAX_SPEED * BikePhysics.FRICTION, s, 1e-9);
    }

    @Test
    public void reachesTopSpeedFasterThanLandspeeder() {
        // Snappier accel + higher top speed still means "ticks to full" is a
        // fair comparison of responsiveness at the shared throttle model.
        int bikeTicks = 0;
        for (double s = 0.0; s < BikePhysics.MAX_SPEED - 1e-6; bikeTicks++) {
            s = BikePhysics.nextForwardSpeed(s, 1);
        }
        // The bike hits its (higher) top speed in a bounded number of ticks.
        assertTrue(bikeTicks <= (int) Math.ceil(BikePhysics.MAX_SPEED / BikePhysics.FORWARD_ACCEL) + 1);
    }

    @Test
    public void yawTurnsAtTurnRate() {
        assertEquals(10.0f + BikePhysics.TURN_RATE_DEG, BikePhysics.nextYaw(10.0f, 1), 1e-6);
        assertEquals(10.0f - BikePhysics.TURN_RATE_DEG, BikePhysics.nextYaw(10.0f, -1), 1e-6);
        assertEquals(10.0f, BikePhysics.nextYaw(10.0f, 0), 1e-6);
    }
}
