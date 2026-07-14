package com.tweeks.starwars.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure decision-core tests — no Minecraft registry bootstrap needed. */
class SaberstaffAssemblyTest {

    @Test
    void twoHiltsTwoRedCrystalsAssembles() {
        assertTrue(SaberstaffAssembly.valid(2, 2, 0, 0));
    }

    @Test
    void wrongCountsAreInvalid() {
        assertFalse(SaberstaffAssembly.valid(1, 2, 0, 0)); // one hilt
        assertFalse(SaberstaffAssembly.valid(2, 1, 0, 0)); // one red crystal
        assertFalse(SaberstaffAssembly.valid(2, 2, 0, 1)); // junk in grid
        assertFalse(SaberstaffAssembly.valid(0, 0, 0, 0)); // empty
    }

    @Test
    void nonRedCrystalsAreRejected() {
        // The Sith weapon demands red kyber — a blue/green/purple crystal fails.
        assertFalse(SaberstaffAssembly.valid(2, 1, 1, 0)); // one red + one other
        assertFalse(SaberstaffAssembly.valid(2, 0, 2, 0)); // two non-red
    }
}
