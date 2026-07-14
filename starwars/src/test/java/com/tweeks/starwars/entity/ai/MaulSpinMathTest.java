package com.tweeks.starwars.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MaulSpinMathTest {

    @Test
    public void radiusBoundaryIsInclusive() {
        double r = MaulSpinMath.SPIN_RADIUS;
        assertTrue(MaulSpinMath.isWithinRadius(r * r));
        assertTrue(MaulSpinMath.isWithinRadius(0.0));
        assertFalse(MaulSpinMath.isWithinRadius(r * r + 0.001));
    }

    @Test
    public void crowdThresholdGatesTheSweep() {
        assertFalse(MaulSpinMath.hasEnoughEnemies(0));
        assertFalse(MaulSpinMath.hasEnoughEnemies(1));
        assertTrue(MaulSpinMath.hasEnoughEnemies(2));
        assertTrue(MaulSpinMath.hasEnoughEnemies(5));
    }

    @Test
    public void thresholdMatchesTheDocumentedMinimum() {
        assertEquals(MaulSpinMath.CROWD_THRESHOLD,
            firstCountThatTriggers(), "hasEnoughEnemies must trip exactly at CROWD_THRESHOLD");
    }

    private static int firstCountThatTriggers() {
        int n = 0;
        while (!MaulSpinMath.hasEnoughEnemies(n)) n++;
        return n;
    }
}
