package com.tweeks.starwars.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SwMobConstantsTest {
    @Test
    void trooperStats_matchSpec() {
        assertEquals(20.0, SwMobConstants.TROOPER_MAX_HEALTH);
        assertEquals(0.30, SwMobConstants.TROOPER_MOVEMENT_SPEED);
        assertEquals(24.0, SwMobConstants.TROOPER_FOLLOW_RANGE);
        assertEquals(30, SwMobConstants.FIRE_INTERVAL_TICKS);
        assertEquals(12.0, SwMobConstants.DROID_MAX_HEALTH);
        assertEquals(0.28, SwMobConstants.DROID_MOVEMENT_SPEED);
    }
}
