package com.tweeks.wildwest.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialMathTest {

    @Test
    void mouseAtCenter_returnsMinusOne() {
        assertEquals(-1, RadialMath.wedgeFromMouse(0, 0, 0, 0, 10.0));
    }

    @Test
    void mouseInsideDeadzone_returnsMinusOne() {
        assertEquals(-1, RadialMath.wedgeFromMouse(105, 100, 100, 100, 10.0));
    }

    @Test
    void mouseAbove_isWedgeZero() {
        assertEquals(0, RadialMath.wedgeFromMouse(100, 50, 100, 100, 10.0));
    }

    @Test
    void wedgesAreSixtyDegreesEach_inClockwiseOrder() {
        int up         = RadialMath.wedgeFromMouse(100,  50, 100, 100, 10.0);
        int upperRight = RadialMath.wedgeFromMouse(140,  70, 100, 100, 10.0);
        int lowerRight = RadialMath.wedgeFromMouse(140, 130, 100, 100, 10.0);
        int down       = RadialMath.wedgeFromMouse(100, 150, 100, 100, 10.0);
        int lowerLeft  = RadialMath.wedgeFromMouse( 60, 130, 100, 100, 10.0);
        int upperLeft  = RadialMath.wedgeFromMouse( 60,  70, 100, 100, 10.0);
        assertEquals(0, up);
        assertEquals(1, upperRight);
        assertEquals(2, lowerRight);
        assertEquals(3, down);
        assertEquals(4, lowerLeft);
        assertEquals(5, upperLeft);
    }
}
