package com.tweeks.wildwest.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfinityGauntletCooldownTest {

    @Test
    void allZeros_neverOnCooldown() {
        long[] cds = new long[6];
        for (int i = 0; i < 6; i++) {
            assertFalse(InfinityCooldowns.isOnCooldown(cds, i, 1000L));
        }
    }

    @Test
    void applyCooldown_setsExpiryAtNowPlusTicks() {
        long[] cds = new long[6];
        long[] next = InfinityCooldowns.applyCooldown(cds, 2, 1000L, 600);
        assertEquals(1600L, next[2]);
        assertEquals(0L, next[0]);
        assertEquals(0L, next[5]);
    }

    @Test
    void onCooldown_whenNowBeforeExpiry() {
        long[] cds = { 0, 0, 1600L, 0, 0, 0 };
        assertTrue(InfinityCooldowns.isOnCooldown(cds, 2, 1500L));
        assertFalse(InfinityCooldowns.isOnCooldown(cds, 2, 1600L));
        assertFalse(InfinityCooldowns.isOnCooldown(cds, 2, 1700L));
    }

    @Test
    void emptyArray_treatedAsAllZeros() {
        long[] empty = new long[0];
        for (int i = 0; i < 6; i++) {
            assertFalse(InfinityCooldowns.isOnCooldown(empty, i, 1000L));
        }
    }

    @Test
    void applyCooldown_onEmptyArray_returnsFullSixSlots() {
        long[] next = InfinityCooldowns.applyCooldown(new long[0], 3, 500L, 200);
        assertEquals(6, next.length);
        assertEquals(700L, next[3]);
    }
}
