package com.tweeks.starwars.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwRadialMathTest {

    private static final double CX = 100, CY = 100, DEAD = 18;

    @Test
    void deadzone_returnsMinusOne() {
        assertEquals(-1, SwRadialMath.wedgeFromMouse(105, 105, CX, CY, DEAD, 5));
    }

    @Test
    void straightUp_isWedgeZero() {
        assertEquals(0, SwRadialMath.wedgeFromMouse(100, 40, CX, CY, DEAD, 5));
    }

    @Test
    void clockwiseProgression_fiveWedges() {
        // 72-degree wedges, wedge 0 centered at 12 o'clock. Pointing right
        // (90 degrees) falls inside wedge 1 (span 36..108).
        assertEquals(1, SwRadialMath.wedgeFromMouse(160, 100, CX, CY, DEAD, 5));
        // Straight down (180 degrees) sits on the wedge 2/3 boundary; accept either.
        int down = SwRadialMath.wedgeFromMouse(100, 160, CX, CY, DEAD, 5);
        assertTrue(down == 2 || down == 3, "down was " + down);
    }

    @Test
    void pointingLeft_isWedgeFour() {
        assertEquals(4, SwRadialMath.wedgeFromMouse(40, 100, CX, CY, DEAD, 5));
    }

    @Test
    void neverReturnsWedgeCount() {
        // Sweep 360 degrees at radius 50; result always in [0, wedgeCount).
        for (int deg = 0; deg < 360; deg++) {
            double rad = Math.toRadians(deg);
            int w = SwRadialMath.wedgeFromMouse(
                CX + 50 * Math.sin(rad), CY - 50 * Math.cos(rad), CX, CY, DEAD, 5);
            assertTrue(w >= 0 && w < 5, "deg=" + deg + " w=" + w);
        }
    }
}
