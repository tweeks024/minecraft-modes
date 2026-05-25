package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Constants smoke test. Asserts on {@link AnomalyEntityConstants} (a pure helper)
 * because AnomalyEntity extends Monster, which triggers MC bootstrap on class
 * load (same reason as AnomalyBleedSchedule existing alongside AnomalyBleedEffect).
 */
class AnomalyEntityConstantsTest {
    @Test
    void constants_matchSpec() {
        assertEquals(40.0, AnomalyEntityConstants.MAX_HEALTH);
        assertEquals(8.0, AnomalyEntityConstants.ATTACK_DAMAGE);
        assertEquals(0.30, AnomalyEntityConstants.SPEED_REVEALED);
        assertEquals(0.20, AnomalyEntityConstants.SPEED_DISGUISED);
        assertEquals(0.0, AnomalyEntityConstants.KNOCKBACK_RESISTANCE);
        assertEquals(24.0, AnomalyEntityConstants.FOLLOW_RANGE);
        assertEquals(200, AnomalyEntityConstants.RE_DISGUISE_TICKS);
        assertEquals(0.75f, AnomalyEntityConstants.DISGUISED_DAMAGE_MULTIPLIER);
        assertEquals(40, AnomalyEntityConstants.DAMAGE_GRACE_TICKS);
    }
}
