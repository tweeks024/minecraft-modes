package com.tweeks.starwars.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TauntaunSpeedTest {

    @Test
    void offSnowIsBaseSpeed() {
        assertEquals(0.35, TauntaunSpeed.riddenSpeed(0.35, false), 1e-9);
    }

    @Test
    void onSnowIsThirtyPercentFaster() {
        assertEquals(0.35 * 1.30, TauntaunSpeed.riddenSpeed(0.35, true), 1e-9);
        assertEquals(TauntaunSpeed.SNOW_MULTIPLIER * 0.35,
            TauntaunSpeed.riddenSpeed(0.35, true), 1e-9);
    }

    @Test
    void zeroBaseStaysZeroEitherWay() {
        assertEquals(0.0, TauntaunSpeed.riddenSpeed(0.0, false), 1e-9);
        assertEquals(0.0, TauntaunSpeed.riddenSpeed(0.0, true), 1e-9);
    }

    @Test
    void snowBoostScalesLinearly() {
        double slow = TauntaunSpeed.riddenSpeed(0.2, true);
        double fast = TauntaunSpeed.riddenSpeed(0.4, true);
        assertEquals(2.0, fast / slow, 1e-9);
    }
}
