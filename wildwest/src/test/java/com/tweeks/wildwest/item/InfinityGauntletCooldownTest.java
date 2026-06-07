package com.tweeks.wildwest.item;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfinityGauntletCooldownTest {

    @Test
    void allZeros_neverOnCooldown() {
        List<Long> cds = InfinityCooldowns.emptyCooldowns();
        for (int i = 0; i < 6; i++) {
            assertFalse(InfinityCooldowns.isOnCooldown(cds, i, 1000L));
        }
    }

    @Test
    void applyCooldown_setsExpiryAtNowPlusTicks() {
        List<Long> cds = InfinityCooldowns.emptyCooldowns();
        List<Long> next = InfinityCooldowns.applyCooldown(cds, 2, 1000L, 600);
        assertEquals(1600L, next.get(2));
        assertEquals(0L, next.get(0));
        assertEquals(0L, next.get(5));
    }

    @Test
    void onCooldown_whenNowBeforeExpiry() {
        List<Long> cds = List.of(0L, 0L, 1600L, 0L, 0L, 0L);
        assertTrue(InfinityCooldowns.isOnCooldown(cds, 2, 1500L));
        assertFalse(InfinityCooldowns.isOnCooldown(cds, 2, 1600L));
        assertFalse(InfinityCooldowns.isOnCooldown(cds, 2, 1700L));
    }

    @Test
    void emptyList_treatedAsAllZeros() {
        List<Long> empty = List.of();
        for (int i = 0; i < 6; i++) {
            assertFalse(InfinityCooldowns.isOnCooldown(empty, i, 1000L));
        }
    }

    @Test
    void applyCooldown_onEmptyList_returnsFullSixSlots() {
        List<Long> next = InfinityCooldowns.applyCooldown(List.of(), 3, 500L, 200);
        assertEquals(6, next.size());
        assertEquals(700L, next.get(3));
    }

    @Test
    void getExpiry_returnsZeroForUnsetSlot() {
        assertEquals(0L, InfinityCooldowns.getExpiry(InfinityCooldowns.emptyCooldowns(), 0));
        assertEquals(0L, InfinityCooldowns.getExpiry(List.of(), 3));
        assertEquals(0L, InfinityCooldowns.getExpiry(null, 0));
        assertEquals(0L, InfinityCooldowns.getExpiry(List.of(1L, 2L), 5));
    }
}
