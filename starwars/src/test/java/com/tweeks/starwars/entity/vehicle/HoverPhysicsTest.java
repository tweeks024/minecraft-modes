package com.tweeks.starwars.entity.vehicle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HoverPhysicsTest {

    /** Simulate the 1-D vertical system: dist' = dist + vel, vel' = vel + accel. */
    private static double[] simulate(double dist, double vel, int ticks) {
        for (int i = 0; i < ticks; i++) {
            vel += HoverPhysics.verticalAccel(dist, vel);
            dist += vel;
        }
        return new double[] {dist, vel};
    }

    @Test
    public void settlesToHoverHeightFromAbove() {
        double[] end = simulate(3.0, 0.0, 200);
        assertEquals(HoverPhysics.HOVER_HEIGHT, end[0], 0.05);
        assertEquals(0.0, end[1], 0.02);
    }

    @Test
    public void settlesToHoverHeightFromBelow() {
        double[] end = simulate(0.1, 0.0, 200);
        assertEquals(HoverPhysics.HOVER_HEIGHT, end[0], 0.05);
    }

    @Test
    public void terminalFallIntoScanRangeNeverClipsGround() {
        // Worst case: entering the 3-block scan at terminal velocity.
        double dist = 3.0, vel = HoverPhysics.TERMINAL_FALL, min = dist;
        for (int i = 0; i < 200; i++) {
            vel += HoverPhysics.verticalAccel(dist, vel);
            dist += vel;
            min = Math.min(min, dist);
        }
        assertTrue(min > 0.0, "hull must never reach the ground (min=" + min + ")");
        assertEquals(HoverPhysics.HOVER_HEIGHT, dist, 0.05);
    }

    @Test
    public void verticalAccelIsClamped() {
        assertTrue(Math.abs(HoverPhysics.verticalAccel(0.0, 0.0)) <= HoverPhysics.MAX_VERTICAL_ACCEL + 1e-9);
        assertTrue(Math.abs(HoverPhysics.verticalAccel(10.0, 0.0)) <= HoverPhysics.MAX_VERTICAL_ACCEL + 1e-9);
    }

    @Test
    public void noGroundMeansGravityTowardTerminal() {
        double vel = 0.0;
        for (int i = 0; i < 100; i++) {
            vel += HoverPhysics.verticalAccel(Double.NaN, vel);
        }
        assertEquals(HoverPhysics.TERMINAL_FALL, vel, 0.01);
        // Never accelerates below terminal.
        assertTrue(vel >= HoverPhysics.TERMINAL_FALL - 1e-9);
    }

    @Test
    public void forwardSpeedCapsAtMaxSpeed() {
        double s = 0.0;
        for (int i = 0; i < 100; i++) s = HoverPhysics.nextForwardSpeed(s, 1);
        assertEquals(HoverPhysics.MAX_SPEED, s, 1e-9);
    }

    @Test
    public void reverseSpeedCapsAtReverseMax() {
        double s = 0.0;
        for (int i = 0; i < 100; i++) s = HoverPhysics.nextForwardSpeed(s, -1);
        assertEquals(-HoverPhysics.MAX_REVERSE_SPEED, s, 1e-9);
    }

    @Test
    public void noInputCoastsDownByFriction() {
        double s = HoverPhysics.MAX_SPEED;
        s = HoverPhysics.nextForwardSpeed(s, 0);
        assertEquals(HoverPhysics.MAX_SPEED * HoverPhysics.FRICTION, s, 1e-9);
    }

    @Test
    public void yawTurnsAtTurnRate() {
        assertEquals(10.0f + HoverPhysics.TURN_RATE_DEG, HoverPhysics.nextYaw(10.0f, 1), 1e-6);
        assertEquals(10.0f - HoverPhysics.TURN_RATE_DEG, HoverPhysics.nextYaw(10.0f, -1), 1e-6);
        assertEquals(10.0f, HoverPhysics.nextYaw(10.0f, 0), 1e-6);
    }
}
