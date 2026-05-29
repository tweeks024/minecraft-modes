package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mirrors AnomalyEntityConstantsTest: tests on a pure helper because
 * CrabEntity extends Animal, which triggers MC bootstrap on class load.
 */
class CrabEntityConstantsTest {
    @Test
    void constants_matchSpec() {
        assertEquals(6.0, CrabEntityConstants.MAX_HEALTH);
        assertEquals(2.0, CrabEntityConstants.ATTACK_DAMAGE);
        assertEquals(0.5, CrabEntityConstants.MOVEMENT_SPEED);
        assertEquals(16.0, CrabEntityConstants.FOLLOW_RANGE);
        assertEquals(8.0, CrabEntityConstants.SWARM_RADIUS);
        assertEquals(20, CrabEntityConstants.ANGER_SECONDS_MIN);
        assertEquals(39, CrabEntityConstants.ANGER_SECONDS_MAX);
        assertEquals(6, CrabEntityConstants.SPAWN_WEIGHT);
        assertEquals(2, CrabEntityConstants.SPAWN_GROUP_MIN);
        assertEquals(4, CrabEntityConstants.SPAWN_GROUP_MAX);
        assertEquals((byte) 60, CrabEntityConstants.EVENT_ID_PINCH);
    }
}
