package com.tweeks.starwars.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MaulLeapMathTest {

    private static final double EPS = 1.0e-9;

    @Test
    public void rangeBoundariesAreInclusive() {
        assertTrue(MaulLeapMath.inLeapRange(MaulLeapMath.MIN_RANGE));
        assertTrue(MaulLeapMath.inLeapRange(MaulLeapMath.MAX_RANGE));
        assertTrue(MaulLeapMath.inLeapRange(7.0));
        assertFalse(MaulLeapMath.inLeapRange(MaulLeapMath.MIN_RANGE - 0.001));
        assertFalse(MaulLeapMath.inLeapRange(MaulLeapMath.MAX_RANGE + 0.001));
    }

    @Test
    public void horizontalComponentIsRescaledToSpeed() {
        // 3-4-5 triangle: horizontal magnitude must equal the requested speed.
        MaulLeapMath.LeapVelocity v = MaulLeapMath.leapVelocity(3.0, 4.0, 1.0, 0.52);
        assertEquals(0.6, v.x(), EPS);
        assertEquals(0.8, v.z(), EPS);
        double horizMag = Math.sqrt(v.x() * v.x() + v.z() * v.z());
        assertEquals(1.0, horizMag, EPS);
    }

    @Test
    public void horizontalDirectionIsPreserved() {
        MaulLeapMath.LeapVelocity v = MaulLeapMath.leapVelocity(-2.0, 0.0, 0.9, 0.5);
        // Points purely along -x, magnitude 0.9.
        assertEquals(-0.9, v.x(), EPS);
        assertEquals(0.0, v.z(), EPS);
    }

    @Test
    public void verticalIsAlwaysTheBoost() {
        assertEquals(0.52, MaulLeapMath.leapVelocity(5.0, 0.0, 1.0, 0.52).y(), EPS);
        assertEquals(0.3, MaulLeapMath.leapVelocity(0.0, 0.0, 1.0, 0.3).y(), EPS);
    }

    @Test
    public void zeroHorizontalOffsetHopsStraightUpWithoutNaN() {
        MaulLeapMath.LeapVelocity v = MaulLeapMath.leapVelocity(0.0, 0.0, 1.0, 0.52);
        assertEquals(0.0, v.x(), EPS);
        assertEquals(0.0, v.z(), EPS);
        assertEquals(0.52, v.y(), EPS);
        assertFalse(Double.isNaN(v.x()) || Double.isNaN(v.z()));
    }

    @Test
    public void defaultConstantsAreSaneAndAcrobatic() {
        // Faster/flatter than the Jedi leaps, longer commitment than a choke.
        assertTrue(MaulLeapMath.HORIZONTAL_SPEED >= 0.9);
        assertTrue(MaulLeapMath.VERTICAL_BOOST > 0.0);
        assertTrue(MaulLeapMath.COOLDOWN_TICKS > 0);
        assertTrue(MaulLeapMath.MIN_RANGE < MaulLeapMath.MAX_RANGE);
    }
}
