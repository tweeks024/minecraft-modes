package com.tweeks.wildwest.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfinityStoneTest {

    @Test
    void hasExactlySixStonesInDeclaredOrder() {
        InfinityStone[] stones = InfinityStone.values();
        assertEquals(6, stones.length);
        assertEquals("POWER",   stones[0].name());
        assertEquals("SPACE",   stones[1].name());
        assertEquals("TIME",    stones[2].name());
        assertEquals("MIND",    stones[3].name());
        assertEquals("REALITY", stones[4].name());
        assertEquals("SOUL",    stones[5].name());
    }

    @Test
    void byIndex_returnsCorrectStone() {
        assertSame(InfinityStone.POWER,   InfinityStone.byIndex(0));
        assertSame(InfinityStone.SOUL,    InfinityStone.byIndex(5));
    }

    @Test
    void byIndex_outOfRange_returnsPowerAsDefault() {
        assertSame(InfinityStone.POWER, InfinityStone.byIndex(-1));
        assertSame(InfinityStone.POWER, InfinityStone.byIndex(6));
        assertSame(InfinityStone.POWER, InfinityStone.byIndex(99));
    }

    @Test
    void cooldownsArePositiveTickCounts() {
        for (InfinityStone s : InfinityStone.values()) {
            assertTrue(s.cooldownTicks() > 0, s.name() + " must have positive cooldown");
            assertTrue(s.durabilityCost() > 0, s.name() + " must have positive durability cost");
        }
    }
}
