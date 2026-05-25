package com.tweeks.wildwest.spawning;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Constants smoke test. The primitive {@code public static final} fields are
 * inlined at compile time, so the test JVM never has to load
 * {@link AnomalyVillageSpawner} itself (which would drag in MC types via its
 * handler method's imports). Mirrors {@code AnomalyEntityConstantsTest}'s
 * reasoning.
 */
class AnomalyVillageSpawnerConstantsTest {
    @Test
    void constants_matchSpec() {
        assertEquals(6000, AnomalyVillageSpawner.CHECK_INTERVAL_TICKS);  // 5 minutes
        assertEquals(0.05f, AnomalyVillageSpawner.SPAWN_CHANCE);
        assertEquals(1, AnomalyVillageSpawner.MAX_PER_VILLAGE);
    }
}
