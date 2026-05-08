package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SteveStackerPhaseLogicTest {

    private static final float MAX = 90.0f;

    @Test
    void fullHp_returnsThree() {
        assertEquals((byte) 3, SteveStackerPhase.computeStackHeight(MAX, MAX));
    }

    @Test
    void justAbovePhase2_returnsThree() {
        assertEquals((byte) 3, SteveStackerPhase.computeStackHeight(60.01f, MAX));
    }

    @Test
    void atPhase2Threshold_returnsTwo() {
        assertEquals((byte) 2, SteveStackerPhase.computeStackHeight(60.0f, MAX));
    }

    @Test
    void justAbovePhase3_returnsTwo() {
        assertEquals((byte) 2, SteveStackerPhase.computeStackHeight(30.01f, MAX));
    }

    @Test
    void atPhase3Threshold_returnsOne() {
        assertEquals((byte) 1, SteveStackerPhase.computeStackHeight(30.0f, MAX));
    }

    @Test
    void atZero_returnsOne() {
        assertEquals((byte) 1, SteveStackerPhase.computeStackHeight(0.0f, MAX));
    }

    @Test
    void negativeOverflow_returnsOne() {
        assertEquals((byte) 1, SteveStackerPhase.computeStackHeight(-10.0f, MAX));
    }
}
