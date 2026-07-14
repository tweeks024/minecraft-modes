package com.tweeks.starwars.entity;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class JawaBarterTest {

    // ---------- cooldown ----------

    @Test
    void freshJawaIsReadyImmediately() {
        assertTrue(JawaBarter.isReady(JawaBarter.NEVER_BARTERED, 0));
    }

    @Test
    void cooldownBlocksUntilExactlyElapsed() {
        assertFalse(JawaBarter.isReady(1000, 1000));
        assertFalse(JawaBarter.isReady(1000, 1000 + JawaBarter.COOLDOWN_TICKS - 1));
        assertTrue(JawaBarter.isReady(1000, 1000 + JawaBarter.COOLDOWN_TICKS));
        assertTrue(JawaBarter.isReady(1000, 1000 + JawaBarter.COOLDOWN_TICKS + 1));
    }

    // ---------- weighted pick boundaries ----------

    @Test
    void pickMapsEveryWeightBandToItsItem() {
        // Bands in declaration order: 30 / 30 / 20 / 15 / 5 (sum 100).
        assertEquals(JawaBarter.TechItem.REDSTONE, JawaBarter.pick(0));
        assertEquals(JawaBarter.TechItem.REDSTONE, JawaBarter.pick(29));
        assertEquals(JawaBarter.TechItem.COPPER_INGOT, JawaBarter.pick(30));
        assertEquals(JawaBarter.TechItem.COPPER_INGOT, JawaBarter.pick(59));
        assertEquals(JawaBarter.TechItem.QUARTZ, JawaBarter.pick(60));
        assertEquals(JawaBarter.TechItem.QUARTZ, JawaBarter.pick(79));
        assertEquals(JawaBarter.TechItem.IRON_NUGGET, JawaBarter.pick(80));
        assertEquals(JawaBarter.TechItem.IRON_NUGGET, JawaBarter.pick(94));
        assertEquals(JawaBarter.TechItem.GLOWSTONE_DUST, JawaBarter.pick(95));
        assertEquals(JawaBarter.TechItem.GLOWSTONE_DUST, JawaBarter.pick(99));
    }

    @Test
    void pickRejectsOutOfRangeRolls() {
        assertThrows(IllegalArgumentException.class, () -> JawaBarter.pick(-1));
        assertThrows(IllegalArgumentException.class, () -> JawaBarter.pick(JawaBarter.TOTAL_WEIGHT));
    }

    @Test
    void weightsSumToTotal() {
        int sum = 0;
        for (JawaBarter.TechItem item : JawaBarter.TechItem.values()) {
            sum += item.weight;
        }
        assertEquals(JawaBarter.TOTAL_WEIGHT, sum);
    }

    // ---------- counts ----------

    @Test
    void countsStayInsideAdvertisedRanges() {
        Random random = new Random(1234);
        for (int i = 0; i < 500; i++) {
            assertTrue(inRange(JawaBarter.countFor(JawaBarter.TechItem.REDSTONE, random), 2, 4));
            assertTrue(inRange(JawaBarter.countFor(JawaBarter.TechItem.COPPER_INGOT, random), 2, 3));
            assertEquals(1, JawaBarter.countFor(JawaBarter.TechItem.QUARTZ, random));
            assertEquals(4, JawaBarter.countFor(JawaBarter.TechItem.IRON_NUGGET, random));
            assertEquals(1, JawaBarter.countFor(JawaBarter.TechItem.GLOWSTONE_DUST, random));
        }
    }

    // ---------- full roll with a seeded random ----------

    @Test
    void seededRollsCoverTheTableAndRespectRarity() {
        Random random = new Random(42);
        Map<JawaBarter.TechItem, Integer> hits = new EnumMap<>(JawaBarter.TechItem.class);
        for (int i = 0; i < 2000; i++) {
            JawaBarter.Roll roll = JawaBarter.roll(random);
            assertTrue(roll.count() >= 1 && roll.count() <= 4,
                "count out of range: " + roll);
            hits.merge(roll.item(), 1, Integer::sum);
        }
        // Every table entry is reachable.
        for (JawaBarter.TechItem item : JawaBarter.TechItem.values()) {
            assertTrue(hits.getOrDefault(item, 0) > 0, item + " never rolled");
        }
        // Glowstone dust is the rare entry — strictly rarer than both
        // common entries over 2000 seeded rolls.
        assertTrue(hits.get(JawaBarter.TechItem.GLOWSTONE_DUST) < hits.get(JawaBarter.TechItem.REDSTONE));
        assertTrue(hits.get(JawaBarter.TechItem.GLOWSTONE_DUST) < hits.get(JawaBarter.TechItem.COPPER_INGOT));
    }

    @Test
    void sameSeedGivesSameSequence() {
        Random a = new Random(7);
        Random b = new Random(7);
        for (int i = 0; i < 50; i++) {
            assertEquals(JawaBarter.roll(a), JawaBarter.roll(b));
        }
    }

    private static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
}
