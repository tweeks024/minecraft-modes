package com.tweeks.wildwest.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bleed-cadence and constant tests for {@link AnomalyBleedEffect}.
 *
 * <p>NOTE: Any reference to {@code AnomalyBleedEffect} from plain JUnit
 * triggers {@code MobEffect.<clinit>}, which requires Minecraft Bootstrap +
 * the FML loader (unavailable in this module's unit-test JVM). The schedule
 * is therefore asserted against {@link AnomalyBleedSchedule}, which is what
 * {@code AnomalyBleedEffect#shouldApplyEffectTickThisTick} delegates to and
 * which exposes the same {@code BLEED_DURATION_TICKS} / {@code DAMAGE_PER_TICK}
 * constants that the effect re-exports.
 */
class AnomalyBleedEffectTest {

    @Test
    void appliesOnceEverySecond_atTickBoundaries() {
        assertTrue(AnomalyBleedSchedule.shouldTickAt(80));
        assertTrue(AnomalyBleedSchedule.shouldTickAt(60));
        assertTrue(AnomalyBleedSchedule.shouldTickAt(40));
        assertTrue(AnomalyBleedSchedule.shouldTickAt(20));
        assertFalse(AnomalyBleedSchedule.shouldTickAt(79));
        assertFalse(AnomalyBleedSchedule.shouldTickAt(10));
    }

    @Test
    void bleedDamagePerTick_isOneHalfHeart() {
        assertEquals(1.0f, AnomalyBleedSchedule.DAMAGE_PER_TICK);
    }

    @Test
    void bleedDurationOnHit_isFourSeconds() {
        assertEquals(80, AnomalyBleedSchedule.BLEED_DURATION_TICKS);
    }
}
