package com.tweeks.starwars.item;

import com.tweeks.starwars.item.SaberAssembly.Result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure decision-core tests — no Minecraft registry bootstrap needed. */
class SaberAssemblyTest {

    @Test
    void oneHiltPlusOneCrystalAssembles() {
        Result result = SaberAssembly.evaluate(1, 1, 0, SaberColor.GREEN);
        assertTrue(result.valid());
        assertEquals(SaberColor.GREEN, result.color());
    }

    @Test
    void crystalColourCarriesThroughIncludingSithRed() {
        assertEquals(SaberColor.RED, SaberAssembly.evaluate(1, 1, 0, SaberColor.RED).color());
        assertEquals(SaberColor.PURPLE, SaberAssembly.evaluate(1, 1, 0, SaberColor.PURPLE).color());
    }

    @Test
    void missingEitherHalfIsInvalid() {
        assertFalse(SaberAssembly.evaluate(1, 0, 0, SaberColor.BLUE).valid()); // hilt only
        assertFalse(SaberAssembly.evaluate(0, 1, 0, SaberColor.BLUE).valid()); // crystal only
        assertFalse(SaberAssembly.evaluate(0, 0, 0, SaberColor.BLUE).valid()); // empty grid
    }

    @Test
    void extrasBreakTheRecipe() {
        assertFalse(SaberAssembly.evaluate(1, 1, 1, SaberColor.BLUE).valid()); // junk in grid
        assertFalse(SaberAssembly.evaluate(2, 1, 0, SaberColor.BLUE).valid()); // two hilts
        assertFalse(SaberAssembly.evaluate(1, 2, 0, SaberColor.BLUE).valid()); // two crystals
    }
}
