package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JediRuinLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = JediRuinLayout.placements().stream()
            .filter(p -> p.kind() == JediRuinLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void exactlyTwoGuardians() {
        long guardians = JediRuinLayout.placements().stream()
            .filter(p -> p.kind() == JediRuinLayout.Kind.GUARDIAN_JEDI).count();
        assertEquals(2, guardians);
    }

    @Test
    void exactlySixCrackedPillarTops() {
        long cracked = JediRuinLayout.placements().stream()
            .filter(p -> p.kind() == JediRuinLayout.Kind.PILLAR_CRACKED).count();
        assertEquals(6, cracked);
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : JediRuinLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < JediRuinLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < JediRuinLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < JediRuinLayout.SIZE_Z, p.toString());
        }
    }
}
